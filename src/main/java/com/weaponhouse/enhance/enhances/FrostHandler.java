package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.effects.EffectRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
public class FrostHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String FROST_TAG = "frost";
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getImmediateSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
            tryApplyFrost(attacker, event.getEntityLiving());
        }
    }
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        LivingEntity shooter = null;
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            shooter = (LivingEntity) arrow.getShooter();
        } else if (event.getEntity() instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) event.getEntity();
            shooter = (LivingEntity) throwable.getShooter();
        }
        if (shooter == null) return;
        if (event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
            Entity target = ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
            if (target instanceof LivingEntity) {
                tryApplyFrost(shooter, (LivingEntity) target);
            }
        }
    }
    private static void tryApplyFrost(LivingEntity attacker, LivingEntity target) {
        if (attacker.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = attacker.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains(FROST_TAG)) {
                int level = buffs.getInt(FROST_TAG);
                applySlowness(target, level);
            }
        }
    }
    private static void applySlowness(LivingEntity target, int level) {
        int effectLevel;
        if (level <= 10) {
            effectLevel = 0;
        } else if (level <= 20) {
            effectLevel = 1;
        } else if (level <= 30) {
            effectLevel = 2;
        } else {
            effectLevel = 3;
        }
        int durationTicks = 20 * 2 * level;
        target.addPotionEffect(new EffectInstance(
                EffectRegistry.FROST,
                durationTicks,
                effectLevel,
                false,
                true
        ));
    }
}