package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
public class InspirationBuffExpiryHandler {
    private static final String INSPIRATION_MARKER_TAG = "InspirationMarker";
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.world instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) event.world;
        if (world.getGameTime() % 20 != 0) return;
        checkAllEntitiesWithInspirationMarkers(world);
    }
    private static void checkAllEntitiesWithInspirationMarkers(ServerWorld world) {
        world.getEntities().forEach(entity -> {
            if (entity instanceof LivingEntity) {
                checkAndRemoveExpiredInspiration((LivingEntity) entity);
            }
        });
    }
    private static void checkAndRemoveExpiredInspiration(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (!entityData.contains(INSPIRATION_MARKER_TAG)) {
            return;
        }
        CompoundNBT marker = entityData.getCompound(INSPIRATION_MARKER_TAG);
        if (!marker.getBoolean("active")) {
            return;
        }
        long currentTime = entity.world.getGameTime();
        long expireTime = marker.getLong("expireTime");
        if (currentTime >= expireTime) {
            removeExpiredInspiration(entity, marker);
        }
    }
    private static void removeExpiredInspiration(LivingEntity entity, CompoundNBT marker) {
        String buffName = marker.getString("buffName");
        int buffLevel = marker.getInt("buffLevel");
        removeTemporaryBuffFromMainNBT(entity, buffName);
        clearInspirationMarker(entity);
        BossBarHandler.createOrUpdateBossBar(entity);
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            player.sendMessage(new TranslationTextComponent(
                    "message.enhance_inspiration.buff_expired",
                    getBuffDisplayName(buffName),
                    buffLevel
            ).mergeStyle(TextFormatting.GRAY), player.getUniqueID());
        }
    }
    private static void removeTemporaryBuffFromMainNBT(LivingEntity entity, String buffName) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG)) {
            CompoundNBT mainBuffs = entityData.getCompound(BUFF_TAG);
            if (mainBuffs.contains(buffName)) {
                mainBuffs.remove(buffName);
                entityData.put(BUFF_TAG, mainBuffs);
                if ("life".equals(buffName)) {
                    LifeHandler.restoreOriginalMaxHealth(entity, entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH));
                }
                if ("attack".equals(buffName)) {
                    AttackHandler.restoreOriginalAttack(entity, entity.getAttribute(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                }
            }
        }
    }
    private static void clearInspirationMarker(LivingEntity entity) {
        CompoundNBT marker = new CompoundNBT();
        marker.putBoolean("active", false);
        entity.getPersistentData().put(INSPIRATION_MARKER_TAG, marker);
    }
    private static String getBuffDisplayName(String buffKey) {
        return new TranslationTextComponent("buff.enhance." + buffKey).getString();
    }
}