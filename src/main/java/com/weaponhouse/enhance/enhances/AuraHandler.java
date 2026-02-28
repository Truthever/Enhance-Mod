package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.effects.AuraEffect;
import com.weaponhouse.enhance.effects.EffectRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
public class AuraHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String AURA_TAG = "aura";
    private static final int BASE_DURATION_TICKS = 20 * 10;
    private static final int DURATION_PER_LEVEL_TICKS = 20 * 2;
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (source instanceof ThornsHandler.ThornsDamageSource) {
            return;
        }
        if (!(event.getSource().getImmediateSource() instanceof LivingEntity)) return;
        if (!(event.getEntityLiving() instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
        LivingEntity target = event.getEntityLiving();
        int auraLevel = getAuraLevel(attacker);
        if (auraLevel > 0) {
            applyAuraEffect(target, auraLevel);
        }
    }
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        LivingEntity shooter = null;
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            shooter = (LivingEntity) arrow.getShooter();
        }
        else if (event.getEntity() instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) event.getEntity();
            shooter = (LivingEntity) throwable.getShooter();
        }
        if (shooter == null) return;
        RayTraceResult traceResult = event.getRayTraceResult();
        if (traceResult.getType() != RayTraceResult.Type.ENTITY) {
            return;
        }
        EntityRayTraceResult entityTrace = (EntityRayTraceResult) traceResult;
        Entity hitEntity = entityTrace.getEntity();
        if (!(hitEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity) hitEntity;
        int auraLevel = getAuraLevel(shooter);
        if (auraLevel > 0) {
            applyAuraEffect(target, auraLevel);
        }
    }
    private static int getAuraLevel(LivingEntity entity) {
        CompoundNBT entityData = entity.getPersistentData();
        if (entityData.contains(BUFF_TAG, Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT buffs = entityData.getCompound(BUFF_TAG);
            if (buffs.contains(AURA_TAG, Constants.NBT.TAG_INT)) {
                return buffs.getInt(AURA_TAG);
            }
        }
        return 0;
    }
    private static void applyAuraEffect(LivingEntity target, int level) {
        int duration = BASE_DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL_TICKS;
        target.removePotionEffect(EffectRegistry.AURA);
        target.addPotionEffect(new EffectInstance(
                EffectRegistry.AURA,
                duration,
                AuraEffect.AURA_AMPLIFIER,
                false,
                true
        ));
    }
}
