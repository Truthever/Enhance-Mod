package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.data.PlayerDataManager;
import com.weaponhouse.enhance.events.AdvancementEventHandler;
import com.weaponhouse.enhance.util.GiftAchievementHelper;
import com.weaponhouse.enhance.util.GiftConfigReader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
@Mod.EventBusSubscriber(modid = "enhance")
public class RedGiftHandler {
    private static final String RED_GIFT_ID = "enhance:red_gift";
    public static final String KEY_MULTI_BUFF = "message.enhance.red_gift.multi";
    private static final ConcurrentHashMap<UUID, Long> lastRedGiftProcessTime = new ConcurrentHashMap<>();
    private static final long RED_GIFT_COOLDOWN_MS = 1000L;
    @SubscribeEvent
    public static void onRedGiftRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) return;
        PlayerEntity player = event.getPlayer();
        Hand hand = event.getHand();
        ItemStack heldStack = player.getHeldItem(hand);
        World world = event.getWorld();
        if (!isRedGiftItem(heldStack)) return;
        if (!(player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        UUID playerId = serverPlayer.getUniqueID();
        long currentTime = System.currentTimeMillis();
        if (lastRedGiftProcessTime.containsKey(playerId)) {
            long lastTime = lastRedGiftProcessTime.get(playerId);
            if (currentTime - lastTime < RED_GIFT_COOLDOWN_MS) {
                return;
            }
        }
        lastRedGiftProcessTime.put(playerId, currentTime);
        Difficulty worldDifficulty = world.getDifficulty();
        executeRedGiftFunction(serverPlayer, heldStack, worldDifficulty);
        event.setCancellationResult(ActionResultType.SUCCESS);
        event.setCanceled(true);
        cleanupOldEntries();
    }
    private static boolean isRedGiftItem(ItemStack stack) {
        if (stack.getItem().getRegistryName() == null) {
            return false;
        }
        return stack.getItem().getRegistryName().toString().equals(RED_GIFT_ID);
    }
    private static void executeRedGiftFunction(ServerPlayerEntity player, ItemStack giftStack, Difficulty difficulty) {
        GiftConfigReader.GiftConfig config = GiftConfigReader.readGiftConfig("red_gift");
        int buffCount = Math.max(1, config.buffCount);
        List<String> obtainedBuffs = new ArrayList<>();
        boolean lifeBuffApplied = false;
        boolean attackBuffApplied = false;
        for (int i = 0; i < buffCount; i++) {
            String randomBuffKey = getRandomBuffKey(config, difficulty);
            int randomBuffLevel = getRandomBuffLevel(config, randomBuffKey, difficulty);
            String finalRandomBuffKey = randomBuffKey;
            while (obtainedBuffs.stream().anyMatch(buff -> buff.contains(finalRandomBuffKey))) {
                randomBuffKey = getRandomBuffKey(config, difficulty);
                randomBuffLevel = getRandomBuffLevel(config, randomBuffKey, difficulty);
            }
            saveBuffToPlayerNBT(player, randomBuffKey, randomBuffLevel);
            if (!lifeBuffApplied && "life".equals(randomBuffKey)) {
                triggerLifeBuffEffect(player);
                lifeBuffApplied = true;
            }
            if (!attackBuffApplied && "attack".equals(randomBuffKey)) {
                triggerAttackBuffEffect(player);
                attackBuffApplied = true;
            }
            String localizedBuffName = getLocalizedBuffName(randomBuffKey);
            obtainedBuffs.add(String.format("%s Lv.%d", localizedBuffName, randomBuffLevel));
        }
        checkAndTriggerFirstRedGiftRewards(player);
        GiftAchievementHelper.checkAndTrigger(player);
        playUpgradeSound(player);
        sendMultiBuffFeedbackToPlayer(player, obtainedBuffs);
        consumeRedGiftItem(player, giftStack);
    }
    private static void checkAndTriggerFirstRedGiftRewards(ServerPlayerEntity player) {
        CompoundNBT permanentData = AdvancementEventHandler.getPermanentData(player);
        String redGiftTriggeredKey = "red_gift_triggered";
        String redGiftLevelIncreasedKey = "red_gift_level_increased";
        if (!permanentData.getBoolean(redGiftTriggeredKey)) {
            try {
                permanentData.putBoolean(redGiftTriggeredKey, true);
                if (Enhance.EXTREME_REALM_TRIGGER != null) {
                    Enhance.LOGGER.info("[DEBUG] 触发极致领域成就");
                    Enhance.EXTREME_REALM_TRIGGER.trigger(player);
                }
                if (!permanentData.getBoolean(redGiftLevelIncreasedKey)) {
                    CompoundNBT fullData = PlayerDataManager.getPermanentPlayerData(player);
                    CompoundNBT buffs = fullData.contains("Buffs") ? fullData.getCompound("Buffs") : new CompoundNBT();
                    int currentLevel = buffs.getInt(AdvancementEventHandler.ENHANCE_LEVEL_TAG);
                    int newLevel = Math.min(currentLevel + 1, AdvancementEventHandler.MAX_ENHANCE_LEVEL);
                    if (newLevel > currentLevel) {
                        buffs.putInt(AdvancementEventHandler.ENHANCE_LEVEL_TAG, newLevel);
                        fullData.put("Buffs", buffs);
                        PlayerDataManager.savePermanentPlayerData(player, fullData);
                        player.getPersistentData().put(AdvancementEventHandler.BUFF_TAG, buffs);
                        player.sendMessage(
                                new TranslationTextComponent("command.enhance.level_up", newLevel)
                                        .mergeStyle(TextFormatting.GREEN),
                                player.getUniqueID()
                        );
                        permanentData.putBoolean(redGiftLevelIncreasedKey, true);
                    }
                }
                AdvancementEventHandler.savePermanentData(player, permanentData);
            } catch (Exception ignored) {}
        }
    }
    private static void playUpgradeSound(ServerPlayerEntity player) {
        World world = player.world;
        world.playSound(
                null,
                player.getPosX(),
                player.getPosY(),
                player.getPosZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS,
                0.5F,
                1.0F
        );
    }
    private static void triggerLifeBuffEffect(ServerPlayerEntity player) {
        LifeHandler.applyLifeBuff(player);
    }
    private static void triggerAttackBuffEffect(ServerPlayerEntity player) {
        AttackHandler.applyAttackBuff(player);
    }
    private static String getRandomBuffKey(GiftConfigReader.GiftConfig config, Difficulty difficulty) {
        List<String> targetBuffPool = config.availableBuffs;
        if (targetBuffPool.isEmpty()) {
            return "life";
        }
        Random random = new Random();
        int randomIndex = random.nextInt(targetBuffPool.size());
        return targetBuffPool.get(randomIndex);
    }
    private static int getRandomBuffLevel(GiftConfigReader.GiftConfig config, String buffKey, Difficulty difficulty) {
        int[] levelRange = config.buffRanges.getOrDefault(buffKey, new int[]{1, 1});
        int minLevel = levelRange[0];
        int maxLevel = levelRange[1];
        if (minLevel == maxLevel) {
            return minLevel;
        } else {
            Random random = new Random();
            return minLevel + random.nextInt(maxLevel - minLevel + 1);
        }
    }
    private static void saveBuffToPlayerNBT(ServerPlayerEntity player, String buffKey, int level) {
        CompoundNBT fullData = PlayerDataManager.getPermanentPlayerData(player);
        CompoundNBT buffs = fullData.contains("Buffs") ? fullData.getCompound("Buffs") : new CompoundNBT();
        buffs.putInt(buffKey, level);
        fullData.put("Buffs", buffs);
        PlayerDataManager.savePermanentPlayerData(player, fullData);
        CompoundNBT playerPersistentData = player.getPersistentData();
        CompoundNBT weaponHouseBuffsTag;
        if (playerPersistentData.contains("WeaponHouseBuffs")) {
            weaponHouseBuffsTag = playerPersistentData.getCompound("WeaponHouseBuffs");
        } else {
            weaponHouseBuffsTag = new CompoundNBT();
        }
        weaponHouseBuffsTag.putInt(buffKey, level);
        playerPersistentData.put("WeaponHouseBuffs", weaponHouseBuffsTag);
    }
    private static void consumeRedGiftItem(ServerPlayerEntity player, ItemStack giftStack) {
        if (player.abilities.isCreativeMode) {
            return;
        }
        giftStack.shrink(1);
    }
    private static String getLocalizedBuffName(String buffKey) {
        return new TranslationTextComponent("buff.enhance." + buffKey).getString();
    }
    private static void sendMultiBuffFeedbackToPlayer(ServerPlayerEntity player, List<String> obtainedBuffs) {
        StringBuilder buffsText = new StringBuilder();
        for (String buffInfo : obtainedBuffs) {
            buffsText.append(buffInfo).append("\n- ");
        }
        if (buffsText.length() > 0) {
            buffsText.delete(buffsText.length() - 3, buffsText.length());
        }
        TranslationTextComponent message = new TranslationTextComponent(
                RedGiftHandler.KEY_MULTI_BUFF,
                obtainedBuffs.size(),
                buffsText.toString()
        );
        player.sendMessage(new StringTextComponent(message.getString()), player.getUniqueID());
    }
    private static void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        lastRedGiftProcessTime.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > RED_GIFT_COOLDOWN_MS * 5
        );
    }
}