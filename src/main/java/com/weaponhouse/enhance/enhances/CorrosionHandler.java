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
public class CorrosionHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String CORROSION_TAG = "corrosion";
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getImmediateSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
            tryApplyCorrosion(attacker, event.getEntityLiving());
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
        if (event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY) {
            Entity target = ((EntityRayTraceResult)event.getRayTraceResult()).getEntity();
            if (target instanceof LivingEntity) {
                tryApplyCorrosion(shooter, (LivingEntity)target);
            }
        }
    }
    private static void tryApplyCorrosion(LivingEntity attacker, LivingEntity target) {
        if (attacker.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = attacker.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains(CORROSION_TAG)) {
                int level = buffs.getInt(CORROSION_TAG);
                applyCorrosion(target, level);
            }
        }
    }
    private static void applyCorrosion(LivingEntity target, int level) {
        int effectLevel = Math.min(level - 1, 9);
        int durationTicks = 25 * 20;
        target.addPotionEffect(new EffectInstance(
                EffectRegistry.CORROSION,
                durationTicks,
                effectLevel,
                false,
                true
        ));
    }
}