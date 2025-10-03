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
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
@Mod.EventBusSubscriber(modid = "enhance")
public class BlueGiftHandler {
    private static final String BLUE_GIFT_ID = "enhance:blue_gift";
    public static final String KEY_SINGLE_BUFF_FEEDBACK = "message.enhance.blue_gift.single";
    public static final String KEY_MULTI_BUFF_FEEDBACK = "message.enhance.blue_gift.multi";
    public static final String KEY_LIFE_BUFF_TRIGGER = "message.enhance.blue_gift.life_trigger";
    private static final String FIRST_BLUE_GIFT_MARK = "first_blue_gift_opened";
    @SubscribeEvent
    public static void onBlueGiftRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) {
            return;
        }
        PlayerEntity player = event.getPlayer();
        Hand hand = event.getHand();
        ItemStack heldStack = player.getHeldItem(hand);
        if (!isBlueGiftItem(heldStack)) {
            return;
        }
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }
        executeBlueGiftFunction((ServerPlayerEntity) player, heldStack);
        event.setCancellationResult(ActionResultType.SUCCESS);
        event.setCanceled(true);
    }
    private static boolean isBlueGiftItem(ItemStack stack) {
        if (stack.getItem().getRegistryName() == null) {
            return false;
        }
        return stack.getItem().getRegistryName().toString().equals(BLUE_GIFT_ID);
    }
    private static void executeBlueGiftFunction(ServerPlayerEntity player, ItemStack giftStack) {
        int buffCount = Math.max(1, ConfigLoader.BLUE_GIFT_BUFF_COUNT);
        List<String> obtainedBuffs = new ArrayList<>();
        for (int i = 0; i < buffCount; i++) {
            String randomBuffKey = getRandomBuffKey();
            int randomBuffLevel = getRandomBuffLevel(randomBuffKey);
            String finalRandomBuffKey = randomBuffKey;
            while (obtainedBuffs.stream().anyMatch(buff -> buff.contains(finalRandomBuffKey))) {
                randomBuffKey = getRandomBuffKey();
            }
            saveBuffToPlayerNBT(player, randomBuffKey, randomBuffLevel);
            if (i == 0 && "life".equals(randomBuffKey)) {
                triggerLifeBuffEffect(player);
            }
            String localizedBuffName = getLocalizedBuffName(randomBuffKey);
            obtainedBuffs.add(String.format("%s Lv.%d", localizedBuffName, randomBuffLevel));
        }
        checkAndTriggerFirstBlueGiftRewards(player);
        GiftAchievementHelper.checkAndTrigger(player);
        playUpgradeSound(player);
        sendMultiBuffFeedbackToPlayer(player, obtainedBuffs);
        consumeBlueGiftItem(player, giftStack);
    }
    private static void checkAndTriggerFirstBlueGiftRewards(ServerPlayerEntity player) {
        CompoundNBT playerNBT = player.getPersistentData();
        CompoundNBT buffsData = playerNBT.getCompound(EnhanceCommand.BUFF_TAG);
        if (!playerNBT.getBoolean(FIRST_BLUE_GIFT_MARK)) {
            try {
                if (Enhance.ENHANCE_POWER_TRIGGER != null) {
                    Enhance.ENHANCE_POWER_TRIGGER.trigger(player);
                }
                int currentLevel = buffsData.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
                int newLevel = Math.min(currentLevel + 1, EnhanceCommand.MAX_ENHANCE_LEVEL);
                newLevel = Math.max(newLevel, 1);
                buffsData.putInt(EnhanceCommand.ENHANCE_LEVEL_TAG, newLevel);
                playerNBT.put(EnhanceCommand.BUFF_TAG, buffsData);
                player.sendMessage(
                        new StringTextComponent(TextFormatting.GREEN + I18n.format(
                                "command.enhance.level_up", newLevel
                        )),
                        player.getUniqueID()
                );
                playerNBT.putBoolean(FIRST_BLUE_GIFT_MARK, true);
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
    private static String getRandomBuffKey() {
        if (ConfigLoader.BLUE_GIFT_AVAILABLE_BUFFS.isEmpty()) {
            return "life";
        }
        Random random = new Random();
        int randomIndex = random.nextInt(ConfigLoader.BLUE_GIFT_AVAILABLE_BUFFS.size());
        String randomBuffKey = ConfigLoader.BLUE_GIFT_AVAILABLE_BUFFS.get(randomIndex);
        return randomBuffKey;
    }
    private static int getRandomBuffLevel(String buffKey) {
        int[] levelRange = ConfigLoader.BLUE_GIFT_BUFF_RANGES.getOrDefault(buffKey, new int[]{1, 1});
        int minLevel = levelRange[0];
        int maxLevel = levelRange[1];
        if (minLevel == maxLevel) {
            return minLevel;
        } else {
            Random random = new Random();
            int randomLevel = minLevel + random.nextInt(maxLevel - minLevel + 1);
            return randomLevel;
        }
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
        // 使用语言键获取本地化文本，替换占位符 %d（词条数）和 %s（词条列表）
        String message = I18n.format(
                BlueGiftHandler.KEY_MULTI_BUFF_FEEDBACK,
                obtainedBuffs.size(),
                buffsText.toString()
        );
        player.sendMessage(new StringTextComponent(message), player.getUniqueID());
    }
    private static void consumeBlueGiftItem(ServerPlayerEntity player, ItemStack giftStack) {
        if (player.abilities.isCreativeMode) {
            return;
        }
        giftStack.shrink(1);
    }
    private static void triggerLifeBuffEffect(ServerPlayerEntity player) {
        LifeHandler.applyLifeBuff(player);
        String message = I18n.format(BlueGiftHandler.KEY_LIFE_BUFF_TRIGGER);
        player.sendMessage(new StringTextComponent(message), player.getUniqueID());
    }
}