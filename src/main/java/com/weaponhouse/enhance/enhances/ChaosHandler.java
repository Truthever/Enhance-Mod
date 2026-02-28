package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class ChaosHandler {
    private static final Random RANDOM = new Random();
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final int COOLDOWN_TICKS = 100;
    private static final Map<UUID, Long> cooldownMap = new ConcurrentHashMap<>();
    private static List<Effect> availableEffects = null;
    private static final Set<Effect> EXCLUDED_EFFECTS = new HashSet<>();
    static {
        EXCLUDED_EFFECTS.add(Effects.RESISTANCE);
        EXCLUDED_EFFECTS.add(Effects.INSTANT_HEALTH);
        EXCLUDED_EFFECTS.add(Effects.INSTANT_DAMAGE);
    }
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getImmediateSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
            LivingEntity target = (LivingEntity) event.getEntity();
            applyChaosEffect(attacker, target);
        }
    }
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            if (arrow.getShooter() instanceof LivingEntity &&
                    event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
                EntityRayTraceResult entityResult = (EntityRayTraceResult) event.getRayTraceResult();
                if (entityResult.getEntity() instanceof LivingEntity) {
                    LivingEntity shooter = (LivingEntity) arrow.getShooter();
                    LivingEntity target = (LivingEntity) entityResult.getEntity();
                    applyChaosEffect(shooter, target);
                }
            }
        }
        if (event.getEntity() instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) event.getEntity();
            if (throwable.getShooter() instanceof LivingEntity &&
                    event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
                EntityRayTraceResult entityResult = (EntityRayTraceResult) event.getRayTraceResult();
                if (entityResult.getEntity() instanceof LivingEntity) {
                    LivingEntity shooter = (LivingEntity) throwable.getShooter();
                    LivingEntity target = (LivingEntity) entityResult.getEntity();
                    applyChaosEffect(shooter, target);
                }
            }
        }
    }
    private static void applyChaosEffect(LivingEntity attacker, LivingEntity target) {
        if (attacker.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = attacker.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains("chaos")) {
                int chaosLevel = buffs.getInt("chaos");
                if (!isOnCooldown(attacker)) {
                    Effect randomEffect = getRandomEffect();
                    if (randomEffect != null) {
                        int duration = chaosLevel * 20;
                        int effectLevel = (chaosLevel - 1) / 10;
                        target.addPotionEffect(new EffectInstance(randomEffect, duration, effectLevel));
                        setCooldown(attacker);
                    }
                }
            }
        }
    }
    private static boolean isOnCooldown(LivingEntity entity) {
        UUID entityId = entity.getUniqueID();
        Long lastTriggerTime = cooldownMap.get(entityId);
        if (lastTriggerTime == null) {
            return false;
        }
        long currentTime = entity.world.getGameTime();
        long timeSinceLastTrigger = currentTime - lastTriggerTime;
        return timeSinceLastTrigger < COOLDOWN_TICKS;
    }
    private static void setCooldown(LivingEntity entity) {
        cooldownMap.put(entity.getUniqueID(), entity.world.getGameTime());
    }
    private static Effect getRandomEffect() {
        if (availableEffects == null) {
            initializeAvailableEffects();
        }
        if (availableEffects.isEmpty()) {
            return null;
        }
        return availableEffects.get(RANDOM.nextInt(availableEffects.size()));
    }
    private static void initializeAvailableEffects() {
        availableEffects = new ArrayList<>();

        for (Effect effect : ForgeRegistries.POTIONS) {
            if (isEffectAllowed(effect)) {
                availableEffects.add(effect);
            }
        }
    }
    private static boolean isEffectAllowed(Effect effect) {
        String effectName = effect.getName();
        if (effectName.contains("debug") || effectName.contains("test")) {
            return false;
        }
        return !EXCLUDED_EFFECTS.contains(effect);
    }
    public static List<net.minecraft.potion.Effect> getAvailableEffects() {
        if (availableEffects == null) {
            initializeAvailableEffects();
        }
        return new ArrayList<>(availableEffects);
    }
}