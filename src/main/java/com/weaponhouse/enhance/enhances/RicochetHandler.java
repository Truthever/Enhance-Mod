package com.weaponhouse.enhance.enhances;

import com.weaponhouse.enhance.items.ThrownDaggerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
public class RicochetHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String RICOCHET_TAG = "ricochet";
    private static final float SPEED_DECAY = 0.8f;
    private static final float SEARCH_RANGE = 10.0f;
    private static final ThreadLocal<Boolean> isProcessingRicochet = ThreadLocal.withInitial(() -> false);
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (isProcessingRicochet.get()) {
            return;
        }
        try {
            isProcessingRicochet.set(true);
            Entity projectile = event.getEntity();
            if (projectile.getPersistentData().getBoolean("IsRicochetCopy")) {
                return;
            }
            LivingEntity shooter = getShooter(projectile);
            if (shooter == null) return;
            if (!shooter.getPersistentData().contains(BUFF_TAG)) return;
            CompoundNBT buffs = shooter.getPersistentData().getCompound(BUFF_TAG);
            if (!buffs.contains(RICOCHET_TAG)) return;
            int maxBounces = buffs.getInt(RICOCHET_TAG);
            int currentBounces = projectile.getPersistentData().getInt("RicochetCount");
            if (currentBounces >= maxBounces) return;
            RayTraceResult hit = event.getRayTraceResult();
            Vector3d hitPos = hit.getHitVec();
            Entity hitEntity;
            if (hit.getType() == RayTraceResult.Type.ENTITY) {
                Entity tempEntity = ((EntityRayTraceResult) hit).getEntity();
                if (tempEntity instanceof LivingEntity) {
                    hitEntity = tempEntity;
                } else {
                    return;
                }
            } else {
                return;
            }
            World world = projectile.world;
            final LivingEntity finalShooter = shooter;
            List<Integer> hitEntities = getHitEntities(projectile);
            hitEntities.add(hitEntity.getEntityId());
            List<LivingEntity> targets = world.getEntitiesWithinAABB(
                            LivingEntity.class,
                            new AxisAlignedBB(hitPos, hitPos).grow(SEARCH_RANGE),
                            e -> e != finalShooter &&
                                    !hitEntities.contains(e.getEntityId()) &&
                                    e.isAlive()
                    ).stream()
                    .sorted(Comparator.comparingDouble(e -> e.getDistanceSq(hitPos)))
                    .collect(Collectors.toList());
            if (!targets.isEmpty()) {
                int remainingBounces = maxBounces - currentBounces;
                int projectilesToCreate = Math.min(remainingBounces, targets.size());
                for (int i = 0; i < projectilesToCreate; i++) {
                    LivingEntity newTarget = targets.get(i);
                    spawnRicochetProjectile(projectile, hitPos, newTarget, currentBounces + 1, hitEntities);
                }
                if (!world.isRemote) {
                    projectile.remove();
                }
            }
        } finally {
            isProcessingRicochet.set(false);
        }
    }
    private static LivingEntity getShooter(Entity projectile) {
        if (projectile instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) projectile;
            if (arrow.getShooter() instanceof LivingEntity) {
                return (LivingEntity) arrow.getShooter();
            }
        } else if (projectile instanceof ThrowableEntity) {
            ThrowableEntity throwable = (ThrowableEntity) projectile;
            if (throwable.getShooter() instanceof LivingEntity) {
                return (LivingEntity) throwable.getShooter();
            }
        } else if (projectile instanceof ThrownDaggerEntity) {
            ThrownDaggerEntity dagger = (ThrownDaggerEntity) projectile;
            if (dagger.getShooter() instanceof LivingEntity) {
                return (LivingEntity) dagger.getShooter();
            }
        }
        return null;
    }
    private static List<Integer> getHitEntities(Entity projectile) {
        CompoundNBT data = projectile.getPersistentData();
        if (data.contains("HitEntities")) {
            int[] hitArray = data.getIntArray("HitEntities");
            List<Integer> hitList = new ArrayList<>();
            for (int id : hitArray) {
                hitList.add(id);
            }
            return hitList;
        }
        return new ArrayList<>();
    }
    private static void setHitEntities(Entity projectile, List<Integer> hitEntities) {
        CompoundNBT data = projectile.getPersistentData();
        int[] hitEntitiesArray = new int[hitEntities.size()];
        for (int i = 0; i < hitEntities.size(); i++) {
            hitEntitiesArray[i] = hitEntities.get(i);
        }
        data.putIntArray("HitEntities", hitEntitiesArray);
    }
    private static void spawnRicochetProjectile(Entity original, Vector3d originPos, LivingEntity target, int bounceCount, List<Integer> hitEntities) {
        World world = original.world;
        Vector3d targetPos = target.getPositionVec();
        Vector3d direction = targetPos.subtract(originPos).normalize()
                .scale(original.getMotion().length() * SPEED_DECAY);
        Entity newProjectile = null;
        if (original instanceof AbstractArrowEntity) {
            AbstractArrowEntity originalArrow = (AbstractArrowEntity) original;
            newProjectile = createRicochetArrow(originalArrow, world, originPos, direction, bounceCount, hitEntities);
        } else if (original instanceof ThrowableEntity) {
            ThrowableEntity originalThrowable = (ThrowableEntity) original;
            newProjectile = createRicochetThrowable(originalThrowable, world, originPos, direction, bounceCount, hitEntities);
        } else if (original instanceof ThrownDaggerEntity) {
            ThrownDaggerEntity originalDagger = (ThrownDaggerEntity) original;
            newProjectile = createRicochetDagger(originalDagger, world, originPos, direction, bounceCount, hitEntities);
        }
        if (newProjectile != null) {
            world.addEntity(newProjectile);
        }
    }
    private static AbstractArrowEntity createRicochetArrow(AbstractArrowEntity original, World world, Vector3d pos, Vector3d direction, int bounceCount, List<Integer> hitEntities) {
        AbstractArrowEntity newArrow = new net.minecraft.entity.projectile.ArrowEntity(world, pos.x, pos.y, pos.z);
        newArrow.setShooter(original.getShooter());
        newArrow.setMotion(direction);
        newArrow.setDamage(original.getDamage() * SPEED_DECAY);
        CompoundNBT persistentData = newArrow.getPersistentData();
        persistentData.putInt("RicochetCount", bounceCount);
        persistentData.putBoolean("IsRicochetCopy", true);
        setHitEntities(newArrow, hitEntities);
        return newArrow;
    }
    private static ThrowableEntity createRicochetThrowable(ThrowableEntity original, World world, Vector3d pos, Vector3d direction, int bounceCount, List<Integer> hitEntities) {
        try {
            ThrowableEntity newThrowable = original.getClass().getConstructor(World.class, LivingEntity.class)
                    .newInstance(world, (LivingEntity) original.getShooter());
            newThrowable.setPosition(pos.x, pos.y, pos.z);
            newThrowable.setMotion(direction);
            CompoundNBT persistentData = newThrowable.getPersistentData();
            persistentData.putInt("RicochetCount", bounceCount);
            persistentData.putBoolean("IsRicochetCopy", true);
            setHitEntities(newThrowable, hitEntities);
            return newThrowable;
        } catch (Exception e) {
            return null;
        }
    }
    private static ThrownDaggerEntity createRicochetDagger(ThrownDaggerEntity original, World world, Vector3d pos, Vector3d direction, int bounceCount, List<Integer> hitEntities) {
        ThrownDaggerEntity newDagger = new ThrownDaggerEntity(world, (LivingEntity) original.getShooter());
        newDagger.setPosition(pos.x, pos.y, pos.z);
        newDagger.setMotion(direction);
        CompoundNBT persistentData = newDagger.getPersistentData();
        persistentData.putInt("RicochetCount", bounceCount);
        persistentData.putBoolean("IsRicochetCopy", true);
        setHitEntities(newDagger, hitEntities);
        return newDagger;
    }
}