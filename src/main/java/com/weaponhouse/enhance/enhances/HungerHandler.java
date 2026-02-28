package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
public class HungerHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String HUNGER_TAG = "hunger";
    private static final int BASE_DURATION_SEC = 10;
    private static final int ADD_DURATION_PER_LEVEL = 3;
    private static final Effect ORIGINAL_HUNGER_EFFECT = Effects.HUNGER;
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (source instanceof ThornsHandler.ThornsDamageSource) {
            return;
        }
        if (event.getEntity().world.isRemote) return;
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) return;
        if (!(event.getEntityLiving() instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        LivingEntity target = event.getEntityLiving();
        if (hasHungerBuff(attacker)) {
            int hungerLevel = getHungerBuffLevel(attacker);
            applyOriginalHungerEffect(target, hungerLevel);
        }
    }
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity().world.isRemote) return;
        LivingEntity shooter = null;
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            shooter = (LivingEntity) arrow.getShooter();
        }
        else if (event.getEntity() instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) event.getEntity();
            shooter = (LivingEntity) throwable.getShooter();
        }
        if (shooter == null || !hasHungerBuff(shooter)) return;
        RayTraceResult traceResult = event.getRayTraceResult();
        if (traceResult.getType() != RayTraceResult.Type.ENTITY) return;
        EntityRayTraceResult entityTrace = (EntityRayTraceResult) traceResult;
        Entity hitEntity = entityTrace.getEntity();
        if (!(hitEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity) hitEntity;
        int hungerLevel = getHungerBuffLevel(shooter);
        applyOriginalHungerEffect(target, hungerLevel);
    }
    private static boolean hasHungerBuff(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        return entityData.contains(BUFF_TAG, Constants.NBT.TAG_COMPOUND)
                && entityData.getCompound(BUFF_TAG).contains(HUNGER_TAG, Constants.NBT.TAG_INT);
    }
    private static int getHungerBuffLevel(LivingEntity entity) {
        CompoundNBT buffData = entity.getPersistentData().getCompound(BUFF_TAG);
        return Math.max(1, buffData.getInt(HUNGER_TAG));
    }
    private static void applyOriginalHungerEffect(LivingEntity target, int buffLevel) {
        if (ORIGINAL_HUNGER_EFFECT == null) {
            return;
        }
        int durationTick = (BASE_DURATION_SEC + (buffLevel - 1) * ADD_DURATION_PER_LEVEL) * 20;
        int effectAmplifier = getHungerEffectAmplifier(buffLevel);
        target.removePotionEffect(ORIGINAL_HUNGER_EFFECT);
        target.addPotionEffect(new EffectInstance(
                ORIGINAL_HUNGER_EFFECT,
                durationTick,
                effectAmplifier,
                false,
                true
        ));
    }
    private static int getHungerEffectAmplifier(int buffLevel) {
        if (buffLevel >= 1 && buffLevel <= 5) {
            return 0;
        } else if (buffLevel >= 6 && buffLevel <= 10) {
            return 1;
        } else {
            return 2;
        }
    }
}