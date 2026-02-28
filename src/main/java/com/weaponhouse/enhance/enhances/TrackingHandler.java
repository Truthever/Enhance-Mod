package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class TrackingHandler {
    private static final Map<UUID, TrackedTarget> trackingTargets = new HashMap<>();
    private static class TrackedTarget {
        public final UUID targetId;
        public final long endTime;
        public final int level;
        public TrackedTarget(UUID targetId, long endTime, int level) {
            this.targetId = targetId;
            this.endTime = endTime;
            this.level = level;
        }
    }
    public static void onEntityDamage(LivingDamageEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (event.getSource().getTrueSource() instanceof MobEntity) {
            MobEntity attacker = (MobEntity) event.getSource().getTrueSource();
            LivingEntity target = event.getEntityLiving();
            if (hasTrackingBuff(attacker)) {
                int trackingLevel = getTrackingLevel(attacker);
                setTrackingTarget(attacker, target, trackingLevel);
            }
        }
    }
    public static void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.world.isRemote) return;
        if (entity instanceof MobEntity && trackingTargets.containsKey(entity.getUniqueID())) {
            MobEntity mob = (MobEntity) entity;
            TrackedTarget trackedTarget = trackingTargets.get(entity.getUniqueID());
            if (entity.world.getGameTime() > trackedTarget.endTime) {
                trackingTargets.remove(entity.getUniqueID());
                return;
            }
            LivingEntity target = getTargetEntity(entity.world, trackedTarget.targetId);
            if (target != null && target.isAlive()) {
                mob.setAttackTarget(target);
                enhanceTracking(mob, target, trackedTarget.level);
            } else {
                trackingTargets.remove(entity.getUniqueID());
            }
        }
    }
    private static boolean hasTrackingBuff(LivingEntity entity) {
        if (!entity.getPersistentData().contains("WeaponHouseBuffs")) {
            return false;
        }
        return entity.getPersistentData().getCompound("WeaponHouseBuffs").contains("tracking");
    }
    private static int getTrackingLevel(LivingEntity entity) {
        return entity.getPersistentData().getCompound("WeaponHouseBuffs").getInt("tracking");
    }
    private static void setTrackingTarget(MobEntity attacker, LivingEntity target, int level) {
        long endTime = attacker.world.getGameTime() + 20000;
        trackingTargets.put(attacker.getUniqueID(), new TrackedTarget(target.getUniqueID(), endTime, level));
        attacker.setAttackTarget(target);
    }
    private static LivingEntity getTargetEntity(World world, UUID targetId) {
        for (LivingEntity entity : world.getEntitiesWithinAABB(LivingEntity.class,
                new AxisAlignedBB(-1, -1, -1, 1, 1, 1).grow(1000))) {
            if (entity.getUniqueID().equals(targetId)) {
                return entity;
            }
        }
        return null;
    }
    private static void enhanceTracking(MobEntity mob, LivingEntity target, int level) {
        switch (level) {
            case 1:
            case 2:
                break;
            case 3:
            case 4:
                if (mob.getDistanceSq(target) < 1024) {
                    mob.getNavigator().tryMoveToEntityLiving(target, 1.0);
                }
                break;
            case 5:
            case 6:
                if (mob.getDistanceSq(target) < 4096) {
                    mob.getNavigator().tryMoveToEntityLiving(target, 1.2);
                    mob.getNavigator().setCanSwim(true);
                }
                break;
        }
        mob.getPersistentData().putLong("LastTargetTime", mob.world.getGameTime());
    }
}