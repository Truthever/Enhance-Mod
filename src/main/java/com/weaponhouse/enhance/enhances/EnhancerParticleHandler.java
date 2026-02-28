package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingEvent;
import java.util.HashMap;
import java.util.Map;

public class EnhancerParticleHandler {
    private static final Map<LivingEntity, Integer> particleCounter = new HashMap<>();
    private static final RedstoneParticleData GREEN_PARTICLE = new RedstoneParticleData(0.0F, 1.0F, 0.0F, 1.25F);
    private static final RedstoneParticleData BLUE_PARTICLE  = new RedstoneParticleData(0.0F, 0.0F, 1.0F, 1.25F);
    private static final RedstoneParticleData RED_PARTICLE   = new RedstoneParticleData(1.0F, 0.0F, 0.0F, 1.25F);
    public static void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!entity.getTags().contains("one_enhance") &&
                !entity.getTags().contains("two_enhance") &&
                !entity.getTags().contains("three_enhance")) {
            return;
        }
        int tier = 1;
        if (entity.getTags().contains("two_enhance")) tier = 2;
        else if (entity.getTags().contains("three_enhance")) tier = 3;
        int counter = particleCounter.getOrDefault(entity, 0);
        particleCounter.put(entity, counter + 1);
        if (counter >= 15) {
            spawnParticles(entity, tier);
            particleCounter.put(entity, 0);
        }
    }
    private static void spawnParticles(LivingEntity entity, int tier) {
        World world = entity.world;
        if (world.isRemote()) return;
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vector3d pos = entity.getPositionVec();
            float h = entity.getHeight();
            switch (tier) {
                case 1:
                    spawnTier1Particles(serverWorld, entity, pos, h);
                    break;
                case 2:
                    spawnTier2Particles(serverWorld, entity, pos, h);
                    break;
                case 3:
                    spawnTier3Particles(serverWorld, entity, pos, h);
                    break;
            }
        }
    }
    private static void spawnHelixRing(ServerWorld w, LivingEntity e, RedstoneParticleData p,
                                       Vector3d pos, float entityHeight,
                                       int points, double baseRadius, double radiusBreath,
                                       double yBaseRatio, double yWaveAmp, double yWaveSpeed,
                                       double angularSpeed, double phase) {
        double t = e.ticksExisted;
        double r = baseRadius + Math.sin(t * 0.12) * radiusBreath;
        double yBase = pos.y + entityHeight * yBaseRatio;
        for (int i = 0; i < points; i++) {
            double a = t * angularSpeed + phase + (Math.PI * 2.0) * (i / (double) points);
            double y = yBase + Math.sin(t * yWaveSpeed + i * 0.7) * yWaveAmp;
            double x = Math.cos(a) * r;
            double z = Math.sin(a) * r;
            double jx = (e.getRNG().nextDouble() - 0.5) * 0.01;
            double jy = (e.getRNG().nextDouble() - 0.5) * 0.01;
            double jz = (e.getRNG().nextDouble() - 0.5) * 0.01;
            w.spawnParticle(
                    p,
                    pos.x + x + jx,
                    y + jy,
                    pos.z + z + jz,
                    1,
                    0, 0, 0,
                    0.0
            );
        }
    }
    private static void spawnTier1Particles(ServerWorld w, LivingEntity e, Vector3d pos, float h) {
        spawnHelixRing(
                w, e, GREEN_PARTICLE, pos, h,
                10,
                0.58,
                0.06,
                0.55,
                0.03,
                0.10,
                0.12,
                0.0
        );
    }
    private static void spawnTier2Particles(ServerWorld w, LivingEntity e, Vector3d pos, float h) {
        spawnHelixRing(
                w, e, BLUE_PARTICLE, pos, h,
                12,
                0.60,
                0.07,
                0.58,
                0.035,
                0.11,
                0.14,
                0.0
        );
        spawnHelixRing(
                w, e, BLUE_PARTICLE, pos, h,
                12,
                0.52,
                0.06,
                0.40,
                0.03,
                0.12,
                -0.13,
                Math.PI / 12.0
        );
    }
    private static void spawnTier3Particles(ServerWorld w, LivingEntity e, Vector3d pos, float h) {
        spawnHelixRing(
                w, e, RED_PARTICLE, pos, h,
                14,
                0.62,
                0.08,
                0.60,
                0.04,
                0.12,
                0.16,
                0.0
        );
        spawnHelixRing(
                w, e, RED_PARTICLE, pos, h,
                14,
                0.54,
                0.07,
                0.42,
                0.04,
                0.13,
                -0.15,
                Math.PI / 10.0
        );
        spawnHelixRing(
                w, e, RED_PARTICLE, pos, h,
                10,
                0.45,
                0.04,
                0.28,
                0.02,
                0.14,
                0.10,
                Math.PI / 6.0
        );
    }
}
