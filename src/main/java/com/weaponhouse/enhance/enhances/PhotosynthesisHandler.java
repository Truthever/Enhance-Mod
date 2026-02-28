package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
public class PhotosynthesisHandler {
    private static final Random RANDOM = new Random();
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String PHOTOSYNTHESIS_TAG = "photosynthesis";
    private static final int CHECK_INTERVAL = 20;
    private static final int MIN_LIGHT_LEVEL = 9;
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PlayerEntity player = event.player;
        World world = player.world;
        if (world.isRemote) return;
        if (player.ticksExisted % CHECK_INTERVAL != 0) return;
        if (hasPhotosynthesisBuff(player)) return;
        int photosynthesisLevel = getPhotosynthesisLevel(player);
        if (getLightLevel(player) >= MIN_LIGHT_LEVEL) {
            float healAmount = 0.5f * photosynthesisLevel;
            player.heal(healAmount);
            spawnSunlightParticles((ServerWorld) world, player);
        }
    }
    public static void onEntityTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;
        ServerWorld world = (ServerWorld) event.world;
        if (world.getGameTime() % CHECK_INTERVAL != 0) return;
        List<LivingEntity> entities = world.getEntities()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .collect(Collectors.toList());
        for (LivingEntity entity : entities) {
            if (entity instanceof PlayerEntity) continue;
            if (hasPhotosynthesisBuff(entity)) continue;
            int photosynthesisLevel = getPhotosynthesisLevel(entity);
            if (getLightLevel(entity) >= MIN_LIGHT_LEVEL) {
                float healAmount = 0.5f * photosynthesisLevel;
                entity.heal(healAmount);
                spawnSunlightParticles(world, entity);
            }
        }
    }
    private static int getLightLevel(LivingEntity entity) {
        World world = entity.world;
        return Math.max(
                world.getLightFor(LightType.SKY, entity.getPosition()),
                world.getLightFor(LightType.BLOCK, entity.getPosition())
        );
    }
    private static boolean hasPhotosynthesisBuff(LivingEntity entity) {
        if (!entity.getPersistentData().contains(BUFF_TAG)) {
            return true;
        }
        return !entity.getPersistentData().getCompound(BUFF_TAG).contains(PHOTOSYNTHESIS_TAG);
    }
    private static int getPhotosynthesisLevel(LivingEntity entity) {
        if (entity.getPersistentData().contains(BUFF_TAG)) {
            return entity.getPersistentData().getCompound(BUFF_TAG).getInt(PHOTOSYNTHESIS_TAG);
        }
        return 0;
    }
    private static void spawnSunlightParticles(ServerWorld world, LivingEntity entity) {
        Vector3d pos = entity.getPositionVec();
        for (int i = 0; i < 5; i++) {
            double x = pos.x + (RANDOM.nextDouble() - 0.5) * 2.0;
            double y = pos.y + RANDOM.nextDouble() * entity.getHeight();
            double z = pos.z + (RANDOM.nextDouble() - 0.5) * 2.0;

            world.spawnParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
        if (RANDOM.nextInt(5) == 0) {
            double beamX = pos.x;
            double beamY = pos.y + entity.getHeight() + 2.0;
            double beamZ = pos.z;
            for (int i = 0; i < 3; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * 0.5;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.5;
                world.spawnParticle(
                        ParticleTypes.END_ROD,
                        beamX + offsetX, beamY, beamZ + offsetZ,
                        1,
                        0.0, -0.1, 0.0,
                        0.05
                );
            }
        }
    }
}
