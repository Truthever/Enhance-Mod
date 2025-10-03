package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.Enhance;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class AdvancementEventHandler {
    public static final String BUFF_TAG = "WeaponHouseBuffs";
    public static final String ENHANCE_LEVEL_TAG = "enhance_level";
    public static final int MAX_ENHANCE_LEVEL = 6;
    private static final String ACHIEVEMENT_DATA_TAG = "EnhanceAchievementData";
    private static final String TRIGGERED_ACHIEVEMENTS = "triggered_achievements";
    private static final String ENHANCE_STONE_ACHIEVEMENT_ID = "enhance:obtain_enhance_stone";
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            ItemEntity itemEntity = event.getItem();
            ItemStack stack = itemEntity.getItem();
            ResourceLocation itemId = stack.getItem().getRegistryName();
            if (itemId != null && itemId.toString().equals("enhance:enhance_stone")) {
                if (Enhance.OBTAIN_ENHANCE_STONE_TRIGGER != null) {
                    if (!hasAchievementTriggered(player, ENHANCE_STONE_ACHIEVEMENT_ID)) {
                        int currentLevel = getCurrentEnhanceLevel(player);
                        Enhance.OBTAIN_ENHANCE_STONE_TRIGGER.trigger(player);
                        markAchievementTriggered(player, ENHANCE_STONE_ACHIEVEMENT_ID);
                        checkAndIncreaseLevel(player, currentLevel);
                    } else if (Enhance.DEBUG_MODE) {
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            ItemStack stack = event.getCrafting();
            ResourceLocation itemId = stack.getItem().getRegistryName();
            if (itemId != null && itemId.toString().equals("enhance:enhance_stone")) {
                if (Enhance.OBTAIN_ENHANCE_STONE_TRIGGER != null) {
                    if (!hasAchievementTriggered(player, ENHANCE_STONE_ACHIEVEMENT_ID)) {
                        int currentLevel = getCurrentEnhanceLevel(player);
                        Enhance.OBTAIN_ENHANCE_STONE_TRIGGER.trigger(player);
                        markAchievementTriggered(player, ENHANCE_STONE_ACHIEVEMENT_ID);
                        checkAndIncreaseLevel(player, currentLevel);
                    } else if (Enhance.DEBUG_MODE) {
                    }
                }
            }
        }
    }
    private static boolean hasAchievementTriggered(ServerPlayerEntity player, String achievementId) {
        CompoundNBT playerData = player.getPersistentData();
        CompoundNBT achievementData = playerData.contains(ACHIEVEMENT_DATA_TAG)
                ? playerData.getCompound(ACHIEVEMENT_DATA_TAG)
                : new CompoundNBT();
        return achievementData.getBoolean(TRIGGERED_ACHIEVEMENTS + "_" + achievementId);
    }
    private static void markAchievementTriggered(ServerPlayerEntity player, String achievementId) {
        CompoundNBT playerData = player.getPersistentData();
        CompoundNBT achievementData = playerData.contains(ACHIEVEMENT_DATA_TAG)
                ? playerData.getCompound(ACHIEVEMENT_DATA_TAG)
                : new CompoundNBT();
        achievementData.putBoolean(TRIGGERED_ACHIEVEMENTS + "_" + achievementId, true);
        playerData.put(ACHIEVEMENT_DATA_TAG, achievementData);
    }
    private static void checkAndIncreaseLevel(ServerPlayerEntity player, int preTriggerLevel) {
        int currentLevel = getCurrentEnhanceLevel(player);
        if (currentLevel == preTriggerLevel) {
            increaseEnhanceLevel(player);
        }
    }
    private static int getCurrentEnhanceLevel(ServerPlayerEntity player) {
        CompoundNBT data = player.getPersistentData();
        CompoundNBT buffs = data.contains(BUFF_TAG) ? data.getCompound(BUFF_TAG) : new CompoundNBT();
        return buffs.contains(ENHANCE_LEVEL_TAG) ? buffs.getInt(ENHANCE_LEVEL_TAG) : 0;
    }
    private static void increaseEnhanceLevel(ServerPlayerEntity player) {
        CompoundNBT data = player.getPersistentData();
        CompoundNBT buffs = data.contains(BUFF_TAG) ? data.getCompound(BUFF_TAG) : new CompoundNBT();
        int currentLevel = buffs.contains(ENHANCE_LEVEL_TAG) ? buffs.getInt(ENHANCE_LEVEL_TAG) : 0;
        int newLevel = Math.min(currentLevel + 1, MAX_ENHANCE_LEVEL);
        if (newLevel > currentLevel) {
            buffs.putInt(ENHANCE_LEVEL_TAG, newLevel);
            data.put(BUFF_TAG, buffs);
            String messageKey;
            Object[] messageArgs = null;
            if (currentLevel == 0) {
                messageKey = "message.enhance.level_up.first";
            } else if (newLevel == MAX_ENHANCE_LEVEL) {
                messageKey = "message.enhance.level_up.max";
            } else {
                messageKey = "message.enhance.level_up";
                messageArgs = new Object[]{newLevel};
            }
            player.sendMessage(new TranslationTextComponent(messageKey, messageArgs), player.getUniqueID());
        }
    }
}
