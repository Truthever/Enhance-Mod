package com.weaponhouse.enhance.util;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.commands.EnhanceCommand;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
public class GiftAchievementHelper {
    private static final String TRIGGERED_MARK = "first_gift_enhancement_triggered";
    private static final String BUFF_TAG = EnhanceCommand.BUFF_TAG;
    private static final String ENHANCE_LEVEL_TAG = EnhanceCommand.ENHANCE_LEVEL_TAG;
    private static final int MAX_ENHANCE_LEVEL = EnhanceCommand.MAX_ENHANCE_LEVEL;
    public static void checkAndTrigger(ServerPlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.getBoolean(TRIGGERED_MARK)) {
            return;
        }
        if (!hasValidBuffs(playerData)) {
            return;
        }
        triggerAchievementAndLevelUp(player);
    }
    private static boolean hasValidBuffs(CompoundNBT playerData) {
        if (!playerData.contains(BUFF_TAG)) {
            return false;
        }
        CompoundNBT buffs = playerData.getCompound(BUFF_TAG);
        for (String key : buffs.keySet()) {
            if (!key.equals(ENHANCE_LEVEL_TAG)) {
                return true;
            }
        }
        return false;
    }
    private static void triggerAchievementAndLevelUp(ServerPlayerEntity player) {
        try {
            CompoundNBT playerData = player.getPersistentData();
            CompoundNBT buffs = playerData.getCompound(BUFF_TAG);
            if (Enhance.FIRST_ENHANCEMENT_TRIGGER != null) {
                String firstBuff = null;
                for (String key : buffs.keySet()) {
                    if (!key.equals(ENHANCE_LEVEL_TAG)) {
                        firstBuff = key;
                        break;
                    }
                }
                if (firstBuff != null) {
                    Enhance.FIRST_ENHANCEMENT_TRIGGER.trigger(player, firstBuff);
                }
            }
            int currentLevel = buffs.getInt(ENHANCE_LEVEL_TAG);
            int newLevel = Math.min(currentLevel + 1, MAX_ENHANCE_LEVEL);
            newLevel = Math.max(newLevel, 1);
            buffs.putInt(ENHANCE_LEVEL_TAG, newLevel);
            playerData.put(BUFF_TAG, buffs);
            player.sendMessage(
                    new TranslationTextComponent("command.enhance.level_up", newLevel)
                            .mergeStyle(TextFormatting.GREEN),
                    player.getUniqueID()
            );
            playerData.putBoolean(TRIGGERED_MARK, true);
        } catch (Exception e) {}
    }
}