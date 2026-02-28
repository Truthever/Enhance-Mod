package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Random;
public class UnyieldingHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String UNYIELDING_TAG = "unyielding";
    private static final int BASE_INVINCIBLE_SECONDS = 3;
    private static final int SECONDS_PER_LEVEL = 2;
    private static final int MAX_INVINCIBLE_SECONDS = 30;
    private static final String INVINCIBLE_TICK_TAG = "UnyieldingInvincibleTicks";
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        World world = entity.world;
        if (!world.isRemote && isInUnyieldingInvincible(entity)) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            return;
        }
        if (world.isRemote || hasUnyieldingBuff(entity)) {
            return;
        }
        float currentHealth = entity.getHealth();
        float damageAmount = event.getAmount();
        if (damageAmount < currentHealth) {
            return;
        }
        triggerUnyieldingEffect(entity, event);
    }
    private static void triggerUnyieldingEffect(LivingEntity entity, LivingHurtEvent event) {
        event.setAmount(0.0F);
        event.setCanceled(true);
        if (entity.getHealth() <= 0) {
            entity.setHealth(1.0F);
        }
        int unyieldingLevel = getUnyieldingLevel(entity);
        int invincibleSeconds = BASE_INVINCIBLE_SECONDS + (SECONDS_PER_LEVEL * unyieldingLevel);
        invincibleSeconds = Math.min(invincibleSeconds, MAX_INVINCIBLE_SECONDS);
        int invincibleTicks = invincibleSeconds * 20;
        CompoundNBT entityData = entity.getPersistentData();
        entityData.putInt(INVINCIBLE_TICK_TAG, invincibleTicks);
        removeUnyieldingBuff(entity);

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            player.sendMessage(
                    new TranslationTextComponent("enhance.buff.unyielding.triggered", invincibleSeconds)
                            .mergeStyle(TextFormatting.GOLD),
                    player.getUniqueID()
            );
        }
        spawnUnyieldingParticles(entity);
    }
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        World world = entity.world;

        if (world.isRemote || !entity.getPersistentData().contains(INVINCIBLE_TICK_TAG) || entity.getPersistentData().getInt(INVINCIBLE_TICK_TAG) <= 0) {
            if (!world.isRemote && entity.getPersistentData().contains(INVINCIBLE_TICK_TAG)) {
                entity.getPersistentData().remove(INVINCIBLE_TICK_TAG);
            }
            return;
        }
        CompoundNBT entityData = entity.getPersistentData();
        int remainingTicks = entityData.getInt(INVINCIBLE_TICK_TAG) - 1;
        entityData.putInt(INVINCIBLE_TICK_TAG, remainingTicks);
        if (remainingTicks % 5 == 0) {
            spawnUnyieldingParticles(entity);
        }
    }
    public static boolean hasUnyieldingBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        return !entityData.contains(BUFF_TAG) ||
                !entityData.getCompound(BUFF_TAG).contains(UNYIELDING_TAG) ||
                entityData.getCompound(BUFF_TAG).getInt(UNYIELDING_TAG) <= 0;
    }
    public static int getUnyieldingLevel(LivingEntity entity) {
        if (hasUnyieldingBuff(entity)) {
            return 0;
        }
        CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
        return buffs.getInt(UNYIELDING_TAG);
    }
    public static void removeUnyieldingBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG)) {
            CompoundNBT buffs = entityData.getCompound(BUFF_TAG);
            buffs.remove(UNYIELDING_TAG);
            entityData.put(BUFF_TAG, buffs);
            BossBarHandler.createOrUpdateBossBar(entity);
        }
    }
    public static boolean isInUnyieldingInvincible(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        return entityData.contains(INVINCIBLE_TICK_TAG) &&
                entityData.getInt(INVINCIBLE_TICK_TAG) > 0;
    }
    private static void spawnUnyieldingParticles(LivingEntity entity) {
        World world = entity.world;
        if (world.isRemote) {
            Random rand = world.rand;
            for (int i = 0; i < 8 + rand.nextInt(4); i++) {
                world.addParticle(
                        ParticleTypes.HEART,
                        entity.getPosX() + (rand.nextDouble() - 0.5) * entity.getWidth(),
                        entity.getPosY() + rand.nextDouble() * entity.getHeight(),
                        entity.getPosZ() + (rand.nextDouble() - 0.5) * entity.getWidth(),
                        0.0D, 0.1D, 0.0D
                );
                world.addParticle(
                        ParticleTypes.ENTITY_EFFECT,
                        entity.getPosX() + (rand.nextDouble() - 0.5) * entity.getWidth(),
                        entity.getPosY() + entity.getHeight() + 0.2D,
                        entity.getPosZ() + (rand.nextDouble() - 0.5) * entity.getWidth(),
                        0.0D, 0.0D, 0.0D
                );
            }
        }
    }
}
