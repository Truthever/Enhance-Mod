package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
@Mod.EventBusSubscriber(modid = "enhance")
public class LifeHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    public static final String LIFE_TAG = "life";
    public static final String INTERNAL_ORIGINAL_MAX_HEALTH = "Enhance_Internal_OriginalMaxHealth";
    public static final UUID LIFE_MODIFIER_UUID = UUID.fromString("d6d8b9d7-1a1b-4c8c-9f9a-0e7e6d5c4b3a");
    public static final float HEALTH_BONUS_PER_LEVEL = 0.05f;
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            applyLifeBuff((LivingEntity) event.getEntity());
        }
    }
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        applyLifeBuff(event.getPlayer());
    }
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        applyLifeBuff(event.getPlayer());
    }
    public static void applyLifeBuff(LivingEntity entity) {
        if (entity.world.isRemote) return;
        int lifeLevel = getLifeLevel(entity);
        ModifiableAttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (lifeLevel <= 0 || maxHealthAttr == null) {
            restoreOriginalMaxHealth(entity, maxHealthAttr);
            return;
        }
        CompoundNBT entityData = entity.getPersistentData();
        double originalBaseHealth;
        if (!entityData.contains(INTERNAL_ORIGINAL_MAX_HEALTH)) {
            originalBaseHealth = maxHealthAttr.getBaseValue();
            entityData.putDouble(INTERNAL_ORIGINAL_MAX_HEALTH, originalBaseHealth);
        } else {
            originalBaseHealth = entityData.getDouble(INTERNAL_ORIGINAL_MAX_HEALTH);
        }
        double totalHealthWithBonus = originalBaseHealth * (1 + lifeLevel * HEALTH_BONUS_PER_LEVEL);
        double adjustment = totalHealthWithBonus - maxHealthAttr.getBaseValue();
        AttributeModifier existing = maxHealthAttr.getModifier(LIFE_MODIFIER_UUID);
        if (existing != null) {
            maxHealthAttr.removeModifier(existing);
        }
        AttributeModifier newModifier = new AttributeModifier(
                LIFE_MODIFIER_UUID,
                "LifeBuffModifier",
                adjustment,
                AttributeModifier.Operation.ADDITION
        );
        maxHealthAttr.applyPersistentModifier(newModifier);
        float newMaxHealth = (float) totalHealthWithBonus;
        if (entity.getHealth() > newMaxHealth) {
            entity.setHealth(newMaxHealth);
        }
    }
    public static void restoreOriginalMaxHealth(LivingEntity entity, ModifiableAttributeInstance maxHealthAttr) {
        if (maxHealthAttr == null || entity.world.isRemote) return;
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(INTERNAL_ORIGINAL_MAX_HEALTH)) {
            double originalBaseHealth = entityData.getDouble(INTERNAL_ORIGINAL_MAX_HEALTH);
            maxHealthAttr.setBaseValue(originalBaseHealth);
            AttributeModifier existing = maxHealthAttr.getModifier(LIFE_MODIFIER_UUID);
            if (existing != null) {
                maxHealthAttr.removeModifier(existing);
            }
            entityData.remove(INTERNAL_ORIGINAL_MAX_HEALTH);
            if (entity.getHealth() > originalBaseHealth) {
                entity.setHealth((float) originalBaseHealth);
            }
        }
    }
    private static int getLifeLevel(LivingEntity entity) {
        CompoundNBT data = entity.getPersistentData();
        if (data.contains(BUFF_TAG)) {
            CompoundNBT buffs = data.getCompound(BUFF_TAG);
            if (buffs.contains(LIFE_TAG)) {
                return buffs.getInt(LIFE_TAG);
            }
        }
        return 0;
    }
    public static void syncLifeBuff(LivingEntity entity) {
        restoreOriginalMaxHealth(entity, entity.getAttribute(Attributes.MAX_HEALTH));
        applyLifeBuff(entity);
    }
}