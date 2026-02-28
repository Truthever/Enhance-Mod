package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.data.PlayerDataManager;
import com.weaponhouse.enhance.invasion.InvasionHandler;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
@Mod.EventBusSubscriber(modid = "enhance")
public class AdvancementEventHandler {
    public static final String BUFF_TAG = "WeaponHouseBuffs";
    public static final String ENHANCE_LEVEL_TAG = "enhance_level";
    public static final int MAX_ENHANCE_LEVEL = 6;
    private static final ConcurrentHashMap<UUID, Long> lastProcessTime = new ConcurrentHashMap<>();
    private static final long PROCESS_COOLDOWN_MS = 1000L;
    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            ResourceLocation advancementId = event.getAdvancement().getId();
            if (advancementId.toString().equals("enhance:obtain_enhance_stone")) {
                CompoundNBT permanentData = getPermanentData(player);
                if (!permanentData.getBoolean("enhance_power_triggered")) {
                    permanentData.putBoolean("enhance_power_triggered", true);
                    savePermanentData(player, permanentData);
                    InvasionHandler.checkEnhancementUnlock(player);
                }
            }
        }
    }
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            ItemEntity itemEntity = event.getItem();
            ItemStack stack = itemEntity.getItem();
            ResourceLocation itemId = stack.getItem().getRegistryName();
            if (itemId != null && itemId.toString().equals("enhance:enhance_stone")) {
                UUID playerId = player.getUniqueID();
                long currentTime = System.currentTimeMillis();
                if (lastProcessTime.containsKey(playerId)) {
                    long lastTime = lastProcessTime.get(playerId);
                    if (currentTime - lastTime < PROCESS_COOLDOWN_MS) {
                        return;
                    }
                }
                lastProcessTime.put(playerId, currentTime);
                if (Enhance.OBTAIN_ENHANCE_STONE_TRIGGER != null) {
                    CompoundNBT permanentData = getPermanentData(player);
                    if (!permanentData.getBoolean("enhance_stone_triggered")) {
                        Enhance.OBTAIN_ENHANCE_STONE_TRIGGER.trigger(player);
                        permanentData.putBoolean("enhance_stone_triggered", true);
                        if (!permanentData.getBoolean("enhance_stone_level_increased")) {
                            increaseEnhanceLevel(player);
                            permanentData.putBoolean("enhance_stone_level_increased", true);
                        }
                        savePermanentData(player, permanentData);
                        InvasionHandler.checkEnhancementUnlock(player);
                    } else {
                        if (!permanentData.getBoolean("enhance_stone_level_increased")) {
                            increaseEnhanceLevel(player);
                            permanentData.putBoolean("enhance_stone_level_increased", true);
                            savePermanentData(player, permanentData);
                        }
                    }
                }
                cleanupOldEntries();
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
                UUID playerId = player.getUniqueID();
                long currentTime = System.currentTimeMillis();
                if (lastProcessTime.containsKey(playerId)) {
                    long lastTime = lastProcessTime.get(playerId);
                    if (currentTime - lastTime < PROCESS_COOLDOWN_MS) {
                        return;
                    }
                }
                lastProcessTime.put(playerId, currentTime);
                if (Enhance.OBTAIN_ENHANCE_STONE_TRIGGER != null) {
                    CompoundNBT permanentData = getPermanentData(player);
                    if (!permanentData.getBoolean("enhance_stone_triggered")) {
                        Enhance.OBTAIN_ENHANCE_STONE_TRIGGER.trigger(player);
                        permanentData.putBoolean("enhance_stone_triggered", true);
                        if (!permanentData.getBoolean("enhance_stone_level_increased")) {
                            increaseEnhanceLevel(player);
                            permanentData.putBoolean("enhance_stone_level_increased", true);
                        }
                        savePermanentData(player, permanentData);
                        //增幅入侵测试中，暂时不直接开放
                        //InvasionHandler.checkEnhancementUnlock(player);
                    }
                }
                cleanupOldEntries();
            }
        }
    }
    private static void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        lastProcessTime.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > PROCESS_COOLDOWN_MS * 5
        );
    }
    public static CompoundNBT getPermanentData(ServerPlayerEntity player) {
        CompoundNBT fullData = PlayerDataManager.getPermanentPlayerData(player);
        if (fullData.contains("PermanentData")) {
            return fullData.getCompound("PermanentData");
        }
        return new CompoundNBT();
    }
    public static void savePermanentData(ServerPlayerEntity player, CompoundNBT permanentData) {
        CompoundNBT fullData = PlayerDataManager.getPermanentPlayerData(player);
        fullData.put("PermanentData", permanentData);
        player.getPersistentData().put("EnhancePermanentData", permanentData);
        PlayerDataManager.savePermanentPlayerData(player, fullData);
    }
    private static void increaseEnhanceLevel(ServerPlayerEntity player) {
        CompoundNBT fullData = PlayerDataManager.getPermanentPlayerData(player);
        CompoundNBT buffs = fullData.contains("Buffs") ? fullData.getCompound("Buffs") : new CompoundNBT();
        int currentLevel = buffs.contains(ENHANCE_LEVEL_TAG) ? buffs.getInt(ENHANCE_LEVEL_TAG) : 0;
        if (currentLevel >= MAX_ENHANCE_LEVEL) {
            return;
        }
        int newLevel = Math.min(currentLevel + 1, MAX_ENHANCE_LEVEL);
        if (newLevel > currentLevel) {
            buffs.putInt(ENHANCE_LEVEL_TAG, newLevel);
            fullData.put("Buffs", buffs);
            PlayerDataManager.savePermanentPlayerData(player, fullData);
            player.getPersistentData().put(BUFF_TAG, buffs);
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
            if (messageArgs != null) {
                player.sendMessage(new TranslationTextComponent(messageKey, messageArgs), player.getUniqueID());
            }
        }
    }
}