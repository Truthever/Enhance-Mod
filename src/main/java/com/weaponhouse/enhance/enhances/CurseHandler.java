package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
public class CurseHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String CURSE_TAG = "curse";
    private static final float DAMAGE_BONUS_PER_LEVEL = 0.10f;
    private static final float ARROW_SPEED_BONUS_PER_LEVEL = 0.05f;
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getImmediateSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
            applyMeleeBonus(attacker, event);
        }
    }
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (source.isExplosion()) {
            LivingEntity explosionSource = getExplosionSource(source);
            if (explosionSource != null) {
                int level = getCurseLevel(explosionSource);
                if (level > 0) {
                    float originalDamage = event.getAmount();
                    float newDamage = originalDamage * (1 + level * DAMAGE_BONUS_PER_LEVEL);
                    event.setAmount(newDamage);
                }
            }
        }
        if (source.getImmediateSource() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) source.getImmediateSource();
            if (arrow.getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) arrow.getShooter();
                int level = getCurseLevel(shooter);
                if (level > 0) {
                    float originalDamage = event.getAmount();
                    float newDamage = originalDamage * (1 + level * DAMAGE_BONUS_PER_LEVEL);
                    event.setAmount(newDamage);
                }
            }
        }
        else if (source.getImmediateSource() instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) source.getImmediateSource();
            if (throwable.getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) throwable.getShooter();
                int level = getCurseLevel(shooter);
                if (level > 0) {
                    float originalDamage = event.getAmount();
                    float newDamage = originalDamage * (1 + level * DAMAGE_BONUS_PER_LEVEL);
                    event.setAmount(newDamage);
                }
            }
        }
    }
    public static void onArrowShoot(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            if (arrow.getShooter() instanceof LivingEntity) {
                applyArrowSpeedBonus((LivingEntity) arrow.getShooter(), arrow);
            }
        }
    }
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
            if (arrow.getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) arrow.getShooter();
                int level = getCurseLevel(shooter);
                if (level > 0 && event.getRayTraceResult().getType() == net.minecraft.util.math.RayTraceResult.Type.ENTITY) {
                    net.minecraft.util.math.EntityRayTraceResult entityResult =
                            (net.minecraft.util.math.EntityRayTraceResult) event.getRayTraceResult();
                    if (entityResult.getEntity() instanceof LivingEntity) {
                        applyArrowSpeedBonus(shooter, arrow);
                    }
                }
            }
        }
        else if (event.getEntity() instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) event.getEntity();
            if (throwable.getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) throwable.getShooter();
                int level = getCurseLevel(shooter);
                if (level > 0) {
                    event.getRayTraceResult().getType();
                }
            }
        }
    }
    private static void applyMeleeBonus(LivingEntity attacker, LivingDamageEvent event) {
        int level = getCurseLevel(attacker);
        if (level > 0) {
            float newDamage = event.getAmount() * (1 + level * DAMAGE_BONUS_PER_LEVEL);
            event.setAmount(newDamage);
        }
    }
    private static void applyArrowSpeedBonus(LivingEntity shooter, AbstractArrowEntity arrow) {
        int level = getCurseLevel(shooter);
        if (level > 0) {
            arrow.setMotion(
                    arrow.getMotion().x * (1 + level * ARROW_SPEED_BONUS_PER_LEVEL),
                    arrow.getMotion().y * (1 + level * ARROW_SPEED_BONUS_PER_LEVEL),
                    arrow.getMotion().z * (1 + level * ARROW_SPEED_BONUS_PER_LEVEL)
            );
        }
    }
    private static LivingEntity getExplosionSource(DamageSource source) {
        if (source.getTrueSource() instanceof LivingEntity) {
            return (LivingEntity) source.getTrueSource();
        }
        if (source.getImmediateSource() instanceof LivingEntity) {
            return (LivingEntity) source.getImmediateSource();
        }

        return null;
    }
    private static int getCurseLevel(LivingEntity entity) {
        if (entity.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains(CURSE_TAG)) {
                return buffs.getInt(CURSE_TAG);
            }
        }
        return 0;
    }
}