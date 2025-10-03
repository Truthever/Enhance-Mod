package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
@Mod.EventBusSubscriber(modid = "enhance")
public class EnhancerParticleHandler {
    private static final Map<LivingEntity, Integer> particleCounter = new HashMap<>();
    private static final RedstoneParticleData GREEN_PARTICLE = new RedstoneParticleData(0.0F, 1.0F, 0.0F, 2.0F);
    private static final RedstoneParticleData BLUE_PARTICLE = new RedstoneParticleData(0.0F, 0.0F, 1.0F, 2.0F);
    private static final RedstoneParticleData RED_PARTICLE = new RedstoneParticleData(1.0F, 0.0F, 0.0F, 2.0F);
    @SubscribeEvent
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
            float entityHeight = entity.getHeight();
            switch (tier) {
                case 1:
                    spawnTier1Particles(serverWorld, entity, pos, entityHeight);
                    break;
                case 2:
                    spawnTier2Particles(serverWorld, entity, pos, entityHeight);
                    break;
                case 3:
                    spawnTier3Particles(serverWorld, entity, pos, entityHeight);
                    break;
            }
        }
    }
    private static void spawnTier1Particles(ServerWorld serverWorld, LivingEntity entity, Vector3d pos, float entityHeight) {
        int baseParticles = 8;
        for (int i = 0; i < baseParticles; i++) {
            double angle = entity.ticksExisted * 0.1 + i * Math.PI / (baseParticles / 2.0);
            double radius = 0.9 + Math.sin(entity.ticksExisted * 0.15) * 0.2;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            serverWorld.spawnParticle(
                    GREEN_PARTICLE,
                    pos.x + x,
                    pos.y + entityHeight * 0.6,
                    pos.z + z,
                    1,
                    0, 0.02, 0,
                    0.02
            );
        }
    }
    private static void spawnTier2Particles(ServerWorld serverWorld, LivingEntity entity, Vector3d pos, float entityHeight) {
        int baseParticles = 10;
        for (int i = 0; i < baseParticles; i++) {
            double angle = entity.ticksExisted * 0.1 + i * Math.PI / (baseParticles / 2.0);
            double radius = 0.9 + Math.sin(entity.ticksExisted * 0.15) * 0.2;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            serverWorld.spawnParticle(
                    BLUE_PARTICLE,
                    pos.x + x,
                    pos.y + entityHeight * 0.6,
                    pos.z + z,
                    1,
                    0, 0.02, 0,
                    0.02
            );
        }
    }
    private static void spawnTier3Particles(ServerWorld serverWorld, LivingEntity entity, Vector3d pos, float entityHeight) {
        int baseParticles = 12;
        for (int i = 0; i < baseParticles; i++) {
            double angle = entity.ticksExisted * 0.1 + i * Math.PI / (baseParticles / 2.0);
            double radius = 0.9 + Math.sin(entity.ticksExisted * 0.15) * 0.2;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            serverWorld.spawnParticle(
                    RED_PARTICLE,
                    pos.x + x,
                    pos.y + entityHeight * 0.6,
                    pos.z + z,
                    1,
                    0, 0.02, 0,
                    0.02
            );
        }
        for (int i = 0; i < 6; i++) {
            double angle = entity.ticksExisted * 0.05 + i * Math.PI / 3;
            double radius = 0.7;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            serverWorld.spawnParticle(
                    RED_PARTICLE,
                    pos.x + x,
                    pos.y + entityHeight * 0.3,
                    pos.z + z,
                    1,
                    0, 0.02, 0,
                    0.02
            );
        }
    }
}