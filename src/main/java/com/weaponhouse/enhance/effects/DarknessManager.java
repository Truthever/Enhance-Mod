package com.weaponhouse.enhance.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
@Mod.EventBusSubscriber
public class DarknessManager {
    private static final Map<UUID, DarknessData> darknessEffects = new ConcurrentHashMap<>();
    private static Field attackTargetField = null;
    private static Field revengeTargetField = null;
    private static boolean reflectionInitialized = false;
    private static int processedThisTick = 0;
    private static final int MAX_PROCESS_PER_TICK = 10;
    static {
        initializeReflection();
    }
    private static class DarknessData {
        public final long endTime;
        public final int amplifier;
        public DarknessData(long endTime, int amplifier) {
            this.endTime = endTime;
            this.amplifier = amplifier;
        }
    }
    private static void initializeReflection() {
        if (reflectionInitialized) return;
        try {
            attackTargetField = MobEntity.class.getDeclaredField("field_70699_by");
            attackTargetField.setAccessible(true);
            revengeTargetField = MobEntity.class.getDeclaredField("field_70708_bq");
            revengeTargetField.setAccessible(true);
            reflectionInitialized = true;
        } catch (Exception ignored) {}
    }
    public static void addDarknessEffect(MobEntity entity, int amplifier) {
        long endTime = entity.world.getGameTime() + 20;
        darknessEffects.put(entity.getUniqueID(), new DarknessData(endTime, amplifier));
        clearTargetSafely(entity);
    }
    public static boolean hasDarknessEffect(LivingEntity entity) {
        UUID entityId = entity.getUniqueID();
        if (!darknessEffects.containsKey(entityId)) {
            return false;
        }
        DarknessData data = darknessEffects.get(entityId);
        if (entity.world.getGameTime() > data.endTime) {
            darknessEffects.remove(entityId);
            return false;
        }
        return true;
    }
    private static void clearTargetSafely(MobEntity mob) {
        if (!reflectionInitialized) {
            scheduleTargetClear(mob);
            return;
        }
        try {
            attackTargetField.set(mob, null);
            revengeTargetField.set(mob, null);
            mob.getNavigator().clearPath();
        } catch (Exception e) {
            scheduleTargetClear(mob);
        }
    }
    private static void scheduleTargetClear(MobEntity mob) {
        if (mob.getServer() != null) {
            mob.getServer().deferTask(() -> {
                if (mob.isAlive()) {
                    try {
                        mob.setAttackTarget(null);
                        mob.setRevengeTarget(null);
                        mob.getNavigator().clearPath();
                    } catch (Exception ignored) {}
                }
            });
        }
    }
    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.world.isRemote) return;
        if (processedThisTick >= MAX_PROCESS_PER_TICK) {
            return;
        }
        if (entity instanceof MobEntity && hasDarknessEffect(entity)) {
            MobEntity mob = (MobEntity) entity;
            clearTargetSafely(mob);
            if (mob.world.getGameTime() % 5 == 0) {
                enhanceDarknessEffect(mob, getDarknessLevel(entity));
            }
            processedThisTick++;
        }
    }
    @SubscribeEvent
    public static void onLivingSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (event.getEntityLiving() == null || event.getEntityLiving().world.isRemote) {
            return;
        }
        if (event.getEntityLiving() instanceof MobEntity && hasDarknessEffect(event.getEntityLiving())) {
            MobEntity mob = (MobEntity) event.getEntityLiving();
            clearTargetSafely(mob);
        }
    }
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntityLiving();
        darknessEffects.remove(entity.getUniqueID());
    }
    @SubscribeEvent
    public static void onWorldTick(net.minecraftforge.event.TickEvent.WorldTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.world.isRemote()) {
            return;
        }
        World world = event.world;
        long currentTime = world.getGameTime();
        darknessEffects.entrySet().removeIf(entry -> currentTime > entry.getValue().endTime);
        processedThisTick = 0;
    }
    private static int getDarknessLevel(LivingEntity entity) {
        UUID entityId = entity.getUniqueID();
        if (darknessEffects.containsKey(entityId)) {
            return darknessEffects.get(entityId).amplifier;
        }
        return 0;
    }
    private static void enhanceDarknessEffect(MobEntity mob, int level) {
        if (level >= 3) {
            if (mob.getRNG().nextInt(10) == 0) {
                double strength = level >= 5 ? 1.0 : 0.5;
                mob.setMotion(
                        (mob.getRNG().nextDouble() - 0.5) * strength,
                        mob.getMotion().y,
                        (mob.getRNG().nextDouble() - 0.5) * strength
                );
            }
        }
        if (mob.world.getGameTime() % 20 == 0) {
            mob.getPersistentData().putLong("LastDarknessTime", mob.world.getGameTime());
        }
    }
}