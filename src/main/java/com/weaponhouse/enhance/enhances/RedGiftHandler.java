package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.util.ConfigLoader;
import com.weaponhouse.enhance.util.GiftAchievementHelper;
import net.minecraft.client.resources.I18n;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid = "enhance")
public class RedGiftHandler {
    private static final String RED_GIFT_ID = "enhance:red_gift";
    public static final String KEY_MULTI_BUFF = "message.enhance.red_gift.multi";
    public static final String KEY_LIFE_TRIGGER = "message.enhance.red_gift.life_trigger";
    private static final String FIRST_RED_GIFT_MARK = "first_red_gift_opened";
    private static String getLocalizedBuffName(String buffKey) {
        String translationKey = "buff.enhance." + buffKey;
        return I18n.format(translationKey, buffKey);
    }
    private static void sendMultiBuffFeedbackToPlayer(ServerPlayerEntity player, List<String> obtainedBuffs) {
        StringBuilder buffsText = new StringBuilder();
        for (String buffInfo : obtainedBuffs) {
            buffsText.append(buffInfo).append("\n- ");
        }
        if (buffsText.length() > 0) {
            buffsText.delete(buffsText.length() - 3, buffsText.length());
        }
        String message = I18n.format(
                RedGiftHandler.KEY_MULTI_BUFF,
                obtainedBuffs.size(),
                buffsText.toString()
        );
        player.sendMessage(new StringTextComponent(message), player.getUniqueID());
    }
    @SubscribeEvent
    public static void onRedGiftRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) return;
        PlayerEntity player = event.getPlayer();
        Hand hand = event.getHand();
        ItemStack heldStack = player.getHeldItem(hand);
        World world = event.getWorld();
        if (!isRedGiftItem(heldStack)) return;
        if (!(player instanceof ServerPlayerEntity)) return;
        Difficulty worldDifficulty = world.getDifficulty();
        executeRedGiftFunction((ServerPlayerEntity) player, heldStack, worldDifficulty);
        event.setCancellationResult(ActionResultType.SUCCESS);
        event.setCanceled(true);
    }
    private static boolean isRedGiftItem(ItemStack stack) {
        if (stack.getItem().getRegistryName() == null) {
            return false;
        }
        return stack.getItem().getRegistryName().toString().equals(RED_GIFT_ID);
    }
    private static void executeRedGiftFunction(ServerPlayerEntity player, ItemStack giftStack, Difficulty difficulty) {
        int buffCount = ConfigLoader.RED_GIFT_BUFF_COUNT;
        buffCount = Math.max(1, buffCount);
        List<String> obtainedBuffs = new ArrayList<>();
        boolean lifeBuffApplied = false;
        for (int i = 0; i < buffCount; i++) {
            String randomBuffKey = getRandomBuffKey(difficulty);
            int randomBuffLevel = getRandomBuffLevel(randomBuffKey, difficulty);
            String finalRandomBuffKey = randomBuffKey;
            while (obtainedBuffs.stream().anyMatch(buff -> buff.contains(finalRandomBuffKey))) {
                randomBuffKey = getRandomBuffKey(difficulty);
            }
            saveBuffToPlayerNBT(player, randomBuffKey, randomBuffLevel);
            if (!lifeBuffApplied && "life".equals(randomBuffKey)) {
                triggerLifeBuffEffect(player);
                lifeBuffApplied = true;
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
        CompoundNBT playerNBT = player.getPersistentData();
        CompoundNBT buffsData = playerNBT.getCompound(EnhanceCommand.BUFF_TAG);
        if (!playerNBT.getBoolean(FIRST_RED_GIFT_MARK)) {
            try {
                if (Enhance.EXTREME_REALM_TRIGGER != null) {
                    Enhance.EXTREME_REALM_TRIGGER.trigger(player);
                }
                int currentLevel = buffsData.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
                int newLevel = Math.min(currentLevel + 1, EnhanceCommand.MAX_ENHANCE_LEVEL);
                newLevel = Math.max(newLevel, 1); // 确保最低为1级
                buffsData.putInt(EnhanceCommand.ENHANCE_LEVEL_TAG, newLevel);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                player.sendMessage(
                        new StringTextComponent(TextFormatting.GREEN + I18n.format(
                                "command.enhance.level_up", newLevel
                        )),
                        player.getUniqueID()
                );
                playerNBT.putBoolean(FIRST_RED_GIFT_MARK, true);
                syncPlayerData(player);
            } catch (Exception e) {
            }
        }
    }
    private static void syncPlayerData(ServerPlayerEntity player) {
        player.refreshDisplayName();
        player.setHealth(player.getHealth());
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
        String message = I18n.format(RedGiftHandler.KEY_LIFE_TRIGGER);
        player.sendMessage(new StringTextComponent(message), player.getUniqueID());
    }
    private static String getRandomBuffKey(Difficulty difficulty) {
        List<String> targetBuffPool;
        if (isEasyOrNormal(difficulty)) {
            targetBuffPool = ConfigLoader.RED_GIFT_AVAILABLE_BUFFS;
        } else {
            List<String> monsterHardBuffPool = ConfigLoader.TIER_THREE_BUFFS_BY_DIFFICULTY.getOrDefault("hard", new ArrayList<>());
            targetBuffPool = new ArrayList<>();
            for (String buff : monsterHardBuffPool) {
                if (ConfigLoader.RED_GIFT_AVAILABLE_BUFFS.contains(buff)) {
                    targetBuffPool.add(buff);
                }
            }
        }
        if (targetBuffPool.isEmpty()) {
            return "life";
        }
        Random random = new Random();
        int randomIndex = random.nextInt(targetBuffPool.size());
        return targetBuffPool.get(randomIndex);
    }
    private static int getRandomBuffLevel(String buffKey, Difficulty difficulty) {
        Map<String, int[]> targetLevelRanges;
        int[] defaultRange = new int[]{1, 1};
        if (isEasyOrNormal(difficulty)) {
            targetLevelRanges = ConfigLoader.RED_GIFT_BUFF_RANGES;
        } else {
            if (!ConfigLoader.RED_GIFT_HARD_BUFF_RANGES.isEmpty()) {
                targetLevelRanges = ConfigLoader.RED_GIFT_HARD_BUFF_RANGES;
            } else {
                targetLevelRanges = new HashMap<>();
                loadDefaultRedGiftHardLevelRanges(targetLevelRanges);
            }
        }
        int[] levelRange = targetLevelRanges.getOrDefault(buffKey, defaultRange);
        int minLevel = Math.max(1, levelRange[0]);
        int maxLevel = Math.max(minLevel, levelRange[1]);
        if (minLevel == maxLevel) {
            return minLevel;
        } else {
            Random random = new Random();
            return minLevel + random.nextInt(maxLevel - minLevel + 1);
        }
    }
    private static void loadDefaultRedGiftHardLevelRanges(Map<String, int[]> targetMap) {
        targetMap.clear();
        targetMap.put("harmony", new int[]{15, 45});
        targetMap.put("unyielding", new int[]{5, 15});
        targetMap.put("thorns", new int[]{10, 25});
        targetMap.put("rob", new int[]{5, 15});
        targetMap.put("megaforce", new int[]{15, 40});
        targetMap.put("thunder", new int[]{20, 50});
        targetMap.put("life", new int[]{30, 60});
        targetMap.put("hunger", new int[]{20, 50});
        targetMap.put("phantom", new int[]{6, 8});
        targetMap.put("photosynthesis", new int[]{15, 30});
        targetMap.put("frost", new int[]{30, 60});
        targetMap.put("attack", new int[]{30, 60});
        targetMap.put("vampire", new int[]{12, 25});
        targetMap.put("curse", new int[]{10, 25});
        targetMap.put("death_bomb", new int[]{15, 40});
        targetMap.put("ricochet", new int[]{5, 5});
        targetMap.put("displacement", new int[]{5, 15});
    }
    private static boolean isEasyOrNormal(Difficulty difficulty) {
        return difficulty == Difficulty.EASY || difficulty == Difficulty.NORMAL;
    }
    private static void saveBuffToPlayerNBT(ServerPlayerEntity player, String buffKey, int level) {
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
}