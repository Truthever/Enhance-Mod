package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
public class ThunderHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final float BASE_CHANCE = 0.05f;
    private static final float CHANCE_PER_LEVEL = 0.03f;
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getImmediateSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getImmediateSource();
            LivingEntity target = (LivingEntity) event.getEntity();
            trySpawnLightning(attacker, target.getPosX(), target.getPosY(), target.getPosZ());
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
                    trySpawnLightning(shooter, target.getPosX(), target.getPosY(), target.getPosZ());
                    trySpawnLightning(shooter, shooter.getPosX(), shooter.getPosY(), shooter.getPosZ());
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
                    trySpawnLightning(shooter, target.getPosX(), target.getPosY(), target.getPosZ());
                    trySpawnLightning(shooter, shooter.getPosX(), shooter.getPosY(), shooter.getPosZ());
                }
            }
        }
    }
    private static void trySpawnLightning(LivingEntity entity, double x, double y, double z) {
        if (entity.getPersistentData().contains(BUFF_TAG)) {
            CompoundNBT buffs = entity.getPersistentData().getCompound(BUFF_TAG);
            if (buffs.contains("thunder")) {
                int level = buffs.getInt("thunder");
                if (entity.getEntityWorld().getRandom().nextFloat() < BASE_CHANCE + CHANCE_PER_LEVEL * level) {
                    spawnLightning(entity.getEntityWorld(), x, y + 1, z);
                }
            }
        }
    }
    private static void spawnLightning(World world, double x, double y, double z) {
        if (!world.isRemote) {
            LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.moveForced(x, y, z);
            }
            if (lightning != null) {
                world.addEntity(lightning);
            }
        }
    }
}