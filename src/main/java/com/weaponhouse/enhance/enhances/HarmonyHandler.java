package com.weaponhouse.enhance.enhances;

import net.minecraft.entity.LivingEntity;
import com.weaponhouse.enhance.effects.EffectRegistry;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Mod.EventBusSubscriber(modid = "enhance")
public class HarmonyHandler {
    private static final String BUFF_TAG = "WeaponHouseBuffs";
    private static final String HARMONY_TAG = "harmony";
    private static final Map<LivingEntity, Long> lastEffectTime = new ConcurrentHashMap<>();
    private static final Map<LivingEntity, Integer> particleCounter = new ConcurrentHashMap<>();
    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        World world = entity.world;
        if (world.isRemote())
            return;
        BlockPos pos = entity.getPosition();
        CompoundNBT data = entity.getPersistentData();
        if (!data.contains(BUFF_TAG, 10))
            return;
        CompoundNBT buffs = data.getCompound(BUFF_TAG);
        if (!buffs.contains(HARMONY_TAG, 3))
            return;
        int level = buffs.getInt(HARMONY_TAG);
        Biome biome = world.getBiome(pos);
        Biome.Category category = biome.getCategory();
        long currentTime = world.getGameTime();
        long lastTime = lastEffectTime.getOrDefault(entity, 0L);
        if (currentTime - lastTime >= 60) {
            if (category == Biome.Category.FOREST) {
                applyForestEffect(entity, level);
            }
            else if (category == Biome.Category.EXTREME_HILLS) {
                applyMountainEffect(entity, level);
            }
            else if (category == Biome.Category.DESERT) {
                applyDesertEffect(entity, level);
            }
            else if (category == Biome.Category.OCEAN) {
                applyOceanEffect(entity, level);
            }
            else if (category == Biome.Category.PLAINS) {
                applyPlainsEffect(entity, level);
            }
            else if (category == Biome.Category.NETHER) {
                applyNetherEffect(entity, level);
            }
            else if (category == Biome.Category.THEEND) {
                applyEndEffect(entity, level);
            }
            else if (category == Biome.Category.MUSHROOM) {
                applyMushroomEffect(entity, level);
            }
            else if (category == Biome.Category.TAIGA) {
                applyForestEffect(entity, level);
            }
            else if (category == Biome.Category.JUNGLE) {
                applyForestEffect(entity, level);
            }
            else if (category == Biome.Category.MESA) {
                applyMountainEffect(entity, level);
            }
            else if (category == Biome.Category.SAVANNA) {
                applyPlainsEffect(entity, level);
            }
            else if (category == Biome.Category.ICY) {
                applyIcyEffect(entity, level);
            }
            else if (category == Biome.Category.SWAMP) {
                applySwampEffect(entity, level);
            }
            lastEffectTime.put(entity, currentTime);
        }
        int counter = particleCounter.getOrDefault(entity, 0);
        particleCounter.put(entity, counter + 1);
        if (counter >= 15) {
            if (category == Biome.Category.FOREST) {
                spawnForestParticles(entity);
            }
            else if (category == Biome.Category.EXTREME_HILLS) {
                spawnMountainParticles(entity, level);
            }
            particleCounter.put(entity, 0);
        }
    }
    private static void applySwampEffect(LivingEntity entity, int level) {
        if (EffectRegistry.SWAMP != null) {
            int amplifier = 0;
            EffectInstance swampEffect = new EffectInstance(
                    EffectRegistry.SWAMP,
                    100,
                    amplifier,
                    false,
                    true
            );
            EffectInstance current = entity.getActivePotionEffect(EffectRegistry.SWAMP);
            if (current == null || current.getDuration() < 60) {
                entity.addPotionEffect(swampEffect);
            }
        }
        spawnSwampParticles(entity, level);
    }
    private static void spawnSwampParticles(LivingEntity entity, int level) {
        if (!entity.world.isRemote()) {
            ServerWorld serverWorld = (ServerWorld) entity.world;
            Vector3d pos = entity.getPositionVec();
            float entityHeight = entity.getHeight();
            int baseParticles = 4 * level;
            for (int i = 0; i < baseParticles; i++) {
                double angle = entity.ticksExisted * 0.04 + i * Math.PI / (baseParticles / 2.0);
                double radius = 0.8 + Math.sin(entity.ticksExisted * 0.12) * 0.2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                serverWorld.spawnParticle(
                        ParticleTypes.ITEM_SLIME,
                        pos.x + x,
                        pos.y + entityHeight * 0.4,
                        pos.z + z,
                        1,
                        0, 0.01, 0,
                        0.03
                );
            }
            for (int i = 0; i < baseParticles / 2; i++) {
                double x = pos.x + (serverWorld.rand.nextDouble() - 0.5) * 1.5;
                double y = pos.y + serverWorld.rand.nextDouble() * entityHeight;
                double z = pos.z + (serverWorld.rand.nextDouble() - 0.5) * 1.5;

                serverWorld.spawnParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        x, y, z,
                        1,
                        0.01, 0.03, 0.01,
                        0.04
                );
            }

        }
    }
    private static boolean isSlownessEffect(EffectInstance effect) {
        return effect.getPotion() == Effects.SLOWNESS || effect.getPotion() == EffectRegistry.FROST;
    }

    private static void applyIcyEffect(LivingEntity entity, int level) {
        int amplifier = level - 1;
        EffectInstance effect = new EffectInstance(
                EffectRegistry.ICY_BLESSING,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.ICY_BLESSING);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        spawnIcyParticles(entity, level);
        if (entity instanceof PlayerEntity) {
            for (EffectInstance activeEffect : entity.getActivePotionEffects()) {
                if (isSlownessEffect(activeEffect)) {
                    entity.removePotionEffect(activeEffect.getPotion());
                }
            }
        }
        int reducePerSecond = level;
        int triggerInterval = 3;
        int totalReduceTicks = reducePerSecond * triggerInterval * 20;
        for (EffectInstance activeEffect : new ArrayList<>(entity.getActivePotionEffects())) {
            if (activeEffect.getPotion() == EffectRegistry.FROST) {
                int currentDuration = activeEffect.getDuration();
                int newDuration = currentDuration - totalReduceTicks;
                if (newDuration <= 0) {
                    entity.removePotionEffect(EffectRegistry.FROST);
                } else {
                    entity.removePotionEffect(EffectRegistry.FROST);
                    entity.addPotionEffect(new EffectInstance(
                            EffectRegistry.FROST,
                            newDuration,
                            activeEffect.getAmplifier(),
                            false,
                            true
                    ));
                }
            }
        }
        spawnIcyParticles(entity, level);
    }
    private static void spawnIcyParticles(LivingEntity entity, int level) {
        if (!entity.world.isRemote()) {
            ServerWorld serverWorld = (ServerWorld) entity.world;
            Vector3d pos = entity.getPositionVec();
            float entityHeight = entity.getHeight();
            int baseParticles = 5 * level;
            for (int i = 0; i < baseParticles; i++) {
                double angle = entity.ticksExisted * 0.03 + i * Math.PI / (baseParticles / 3.0);
                double radius = 1.0 + Math.sin(entity.ticksExisted * 0.1) * 0.3;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                serverWorld.spawnParticle(
                        ParticleTypes.ITEM_SNOWBALL,
                        pos.x + x,
                        pos.y + entityHeight + 0.5,
                        pos.z + z,
                        1,
                        0, -0.05, 0,
                        0.01
                );
            }
            for (int i = 0; i < baseParticles / 2; i++) {
                double x = pos.x + (serverWorld.rand.nextDouble() - 0.5) * 1.2;
                double y = pos.y + serverWorld.rand.nextDouble() * entityHeight;
                double z = pos.z + (serverWorld.rand.nextDouble() - 0.5) * 1.2;

                serverWorld.spawnParticle(
                        ParticleTypes.ITEM_SNOWBALL,
                        x, y, z,
                        1,
                        0.02, 0.02, 0.02,
                        0.05
                );
            }
        }
    }
    private static void applyMushroomEffect(LivingEntity entity, int level) {
        int amplifier = level - 1;
        EffectInstance effect = new EffectInstance(
                EffectRegistry.MUSHROOMHEALTH,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.MUSHROOMHEALTH);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        spawnMushroomParticles(entity, level);
    }
    private static void spawnMushroomParticles(LivingEntity entity, int level) {
        if (!entity.world.isRemote()) {
            ServerWorld serverWorld = (ServerWorld) entity.world;
            Vector3d pos = entity.getPositionVec();
            float height = entity.getHeight();
            for (int i = 0; i < level * 2; i++) {
                double x = pos.x + (serverWorld.rand.nextDouble() - 0.5) * 1.2;
                double y = pos.y + height * 0.7 + serverWorld.rand.nextDouble() * 0.5;
                double z = pos.z + (serverWorld.rand.nextDouble() - 0.5) * 1.2;
                serverWorld.spawnParticle(
                        ParticleTypes.HEART,
                        x, y, z,
                        1,
                        0.02, 0.02, 0.02,
                        0.05
                );
            }
        }
    }
    private static void applyEndEffect(LivingEntity entity, int level) {
        int amplifier = level - 1;
        EffectInstance effect = new EffectInstance(
                EffectRegistry.END,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.END);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        spawnEndParticles(entity, level);
    }
    private static void spawnEndParticles(LivingEntity entity, int level) {
        if (!entity.world.isRemote()) {
            ServerWorld serverWorld = (ServerWorld) entity.world;
            Vector3d pos = entity.getPositionVec();
            float entityHeight = entity.getHeight();
            int baseParticles = 5 + level;
            for (int i = 0; i < baseParticles; i++) {
                double angle = entity.ticksExisted * 0.1 + i * Math.PI / (baseParticles / 2.0);
                double radius = 0.9 + Math.sin(entity.ticksExisted * 0.15) * 0.2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                serverWorld.spawnParticle(
                        ParticleTypes.PORTAL,
                        pos.x + x,
                        pos.y + entityHeight * 0.6,
                        pos.z + z,
                        1,
                        0, 0.02, 0,
                        0.02
                );
            }
            serverWorld.spawnParticle(
                    ParticleTypes.DRAGON_BREATH,
                    pos.x,
                    pos.y + entityHeight + 1.0,
                    pos.z,
                    baseParticles / 2,
                    0.5, 0.0, 0.5,
                    0.2
            );
        }
    }
    private static void applyDesertEffect(LivingEntity entity, int level) {
        CompoundNBT nbt = entity.getPersistentData();
        if (!nbt.contains("BaseAttackDamage")) {
            ModifiableAttributeInstance attackDamageAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamageAttr != null) {
                nbt.putDouble("BaseAttackDamage", attackDamageAttr.getBaseValue());
            }
        }
        int amplifier = level - 1;
        EffectInstance effect = new EffectInstance(
                EffectRegistry.DESERTATTACK,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.DESERTATTACK);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        spawnDesertParticles(entity, level);
    }
    private static void spawnDesertParticles(LivingEntity entity, int level) {
        if (entity.world.isRemote) {
            for (int i = 0; i < level * 5; i++) {
                double x = entity.getPosX() + (entity.world.rand.nextDouble() - 0.5) * 2.0;
                double y = entity.getPosY() + entity.getEyeHeight() + (entity.world.rand.nextDouble() - 0.5);
                double z = entity.getPosZ() + (entity.world.rand.nextDouble() - 0.5) * 2.0;
                entity.world.addParticle(
                        ParticleTypes.FLAME,
                        x, y, z,
                        0.0, 0.05, 0.0
                );
            }
        }
    }
    private static void applyOceanEffect(LivingEntity entity, int level) {
        int amplifier = level - 1;
        EffectInstance effect = new EffectInstance(
                EffectRegistry.OCEAN,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.OCEAN);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        spawnOceanParticles(entity, level);
    }
    private static void applyPlainsEffect(LivingEntity entity, int level) {
        int amplifier = level - 1;
        EffectInstance effect = new EffectInstance(
                EffectRegistry.PLAINS,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.PLAINS);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        spawnPlainsParticles(entity, level);
    }
    private static void spawnPlainsParticles(LivingEntity entity, int level) {
        if (entity.world.isRemote) {
            for (int i = 0; i < level * 5; i++) {
                double x = entity.getPosX() + (entity.world.rand.nextDouble() - 0.5) * 1.5;
                double y = entity.getPosY() + 0.1;
                double z = entity.getPosZ() + (entity.world.rand.nextDouble() - 0.5) * 1.5;
                entity.world.addParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        x, y, z,
                        0.0, 0.05, 0.0
                );
            }
        }
    }
    private static void spawnOceanParticles(LivingEntity entity, int level) {
        if (entity.world.isRemote) {
            for (int i = 0; i < level * 5; i++) {
                double x = entity.getPosX() + (entity.world.rand.nextDouble() - 0.5) * 2.0;
                double y = entity.getPosY() + entity.getEyeHeight() + (entity.world.rand.nextDouble() - 0.5);
                double z = entity.getPosZ() + (entity.world.rand.nextDouble() - 0.5) * 2.0;
                entity.world.addParticle(
                        ParticleTypes.BUBBLE,
                        x, y, z,
                        0.0, 0.1, 0.0
                );
            }
        }
    }
    private static void applyForestEffect(LivingEntity entity, int level) {
        boolean didHeal = false;
        if (entity.getHealth() < entity.getMaxHealth()) {
            float healAmount = 0.5f * level;
            entity.heal(healAmount);
            didHeal = true;
        }
        int amplifier = Math.min(level / 2, 8);
        EffectInstance effect = new EffectInstance(
                EffectRegistry.FORESTSPEED,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.FORESTSPEED);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        if (didHeal) {
            spawnHealingParticles(entity);
        }
    }
    private static void applyMountainEffect(LivingEntity entity, int level) {
        CompoundNBT nbt = entity.getPersistentData();
        if (!nbt.contains("naturalArmor")) {
            ModifiableAttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
            if (armorAttr != null) {
                nbt.putDouble("naturalArmor", armorAttr.getBaseValue());
            }
        }
        int amplifier = level - 1;
        EffectInstance effect = new EffectInstance(
                EffectRegistry.MOUNTAINDEFENSE,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.MOUNTAINDEFENSE);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        spawnMountainParticles(entity, level);
    }

    private static void spawnHealingParticles(LivingEntity entity) {
        if (!entity.world.isRemote()) {
            ServerWorld serverWorld = (ServerWorld) entity.world;
            Vector3d pos = entity.getPositionVec().add(0, entity.getHeight() * 0.8, 0);
            serverWorld.spawnParticle(
                    ParticleTypes.HEART,
                    pos.x,
                    pos.y,
                    pos.z,
                    1,
                    0.5, 0.8, 0.5,
                    0.1
            );
        }
    }
    private static void spawnForestParticles(LivingEntity entity) {
        if (!entity.world.isRemote()) {
            ServerWorld serverWorld = (ServerWorld) entity.world;
            Vector3d pos = entity.getPositionVec();
            float entityHeight = entity.getHeight();
            for (int i = 0; i < 6; i++) {
                double angle = entity.ticksExisted * 0.05 + i * Math.PI / 3;
                double radius = 0.7 + Math.sin(entity.ticksExisted * 0.1) * 0.1;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                serverWorld.spawnParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        pos.x + x,
                        pos.y + entityHeight * 0.3,
                        pos.z + z,
                        1,
                        0, 0, 0,
                        0
                );
            }
            for (int i = 0; i < 4; i++) {
                double offsetX = (entity.world.rand.nextDouble() - 0.5) * 2.5;
                double offsetY = entity.world.rand.nextDouble() * entityHeight;
                double offsetZ = (entity.world.rand.nextDouble() - 0.5) * 2.5;
                serverWorld.spawnParticle(
                        ParticleTypes.NOTE,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        0,
                        0, 0, 0,
                        0.5
                );
            }
            serverWorld.spawnParticle(
                    ParticleTypes.RAIN,
                    pos.x,
                    pos.y + entityHeight + 1.5,
                    pos.z,
                    5,
                    1.2, 0.0, 1.2,
                    0.01
            );
        }
    }
    private static void spawnMountainParticles(LivingEntity entity, int level) {
        if (!entity.world.isRemote()) {
            ServerWorld serverWorld = (ServerWorld) entity.world;
            Vector3d pos = entity.getPositionVec();
            float entityHeight = entity.getHeight();
            int baseParticles = 5 + level;
            for (int i = 0; i < baseParticles / 2; i++) {
                double angle = entity.ticksExisted * 0.05 + i * Math.PI / (baseParticles / 2.0);
                double radius = 0.8 + Math.sin(entity.ticksExisted * 0.1) * 0.1;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                serverWorld.spawnParticle(
                        ParticleTypes.CLOUD,
                        pos.x + x,
                        pos.y + entityHeight * 0.5,
                        pos.z + z,
                        1,
                        0, 0.02, 0,
                        0.02
                );
            }
        }
    }

    private static void applyNetherEffect(LivingEntity entity, int level) {
        int amplifier = level - 1;
        EffectInstance effect = new EffectInstance(
                EffectRegistry.NETHER,
                100,
                amplifier,
                false,
                true
        );
        EffectInstance current = entity.getActivePotionEffect(EffectRegistry.NETHER);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            entity.addPotionEffect(effect);
        }
        spawnNetherParticles(entity, level);
    }
    private static void spawnNetherParticles(LivingEntity entity, int level) {
        if (entity.world.isRemote) {
            for (int i = 0; i < level * 5; i++) {
                double x = entity.getPosX() + (entity.world.rand.nextDouble() - 0.5) * 2.0;
                double y = entity.getPosY() + entity.getEyeHeight() + (entity.world.rand.nextDouble() - 0.5);
                double z = entity.getPosZ() + (entity.world.rand.nextDouble() - 0.5) * 2.0;
                entity.world.addParticle(
                        ParticleTypes.FLAME,
                        x, y, z,
                        (entity.world.rand.nextDouble() - 0.5) * 0.1,
                        0.1,
                        (entity.world.rand.nextDouble() - 0.5) * 0.1
                );
                entity.world.addParticle(
                        ParticleTypes.LARGE_SMOKE,
                        x, y, z,
                        0.0, 0.05, 0.0
                );
            }
        }
    }

}