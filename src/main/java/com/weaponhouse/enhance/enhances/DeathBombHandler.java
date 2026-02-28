package com.weaponhouse.enhance.enhances;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
public class DeathBombHandler {
    private static final float DEFAULT_MAX_BREAKABLE_HARDNESS = 1.2f;
    private static final float ADVANCED_MAX_BREAKABLE_HARDNESS = 2.0f;
    private static final Random random = new Random();
    private static final float DAMAGE_MULTIPLIER_PER_LEVEL = 0.1f;
    private static final float MAX_DAMAGE_MULTIPLIER = 2.5f;
    private static final float RADIUS_MULTIPLIER_PER_LEVEL = 0.05f;
    private static final float MAX_RADIUS_MULTIPLIER = 2.0f;
    private static final Set<String> PROTECTED_ITEMS = new HashSet<>(Arrays.asList(
            "enhance:enhance_stone",
            "enhance:enhance_block",
            "enhance:blue_gift",
            "enhance:green_gift",
            "enhance:red_gift",
            "enhance:purple_gift",
            "enhance:alchemy_furnace"
    ));
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntityLiving();
        World world = entity.world;
        if (world.isRemote) return;
        if (!hasDeathBomb(entity)) return;
        int deathBombLevel = getDeathBombLevel(entity);
        float damage = calculateDamage(deathBombLevel);
        float radius = calculateRadius(deathBombLevel);
        executeDeathBomb(entity, damage, radius, deathBombLevel);
    }
    private static boolean hasDeathBomb(LivingEntity entity) {
        if (!entity.getPersistentData().contains("WeaponHouseBuffs")) {
            return false;
        }
        return entity.getPersistentData().getCompound("WeaponHouseBuffs").contains("death_bomb");
    }
    private static int getDeathBombLevel(LivingEntity entity) {
        return entity.getPersistentData().getCompound("WeaponHouseBuffs").getInt("death_bomb");
    }
    private static float calculateDamage(int level) {
        float baseDamage = 4.0f + (level - 1) * 3.0f;
        float damageMultiplier = 1.0f + Math.min(level * DAMAGE_MULTIPLIER_PER_LEVEL, MAX_DAMAGE_MULTIPLIER - 1.0f);
        return baseDamage * damageMultiplier;
    }
    private static float calculateRadius(int level) {
        float baseRadius;
        if (level <= 5) {
            baseRadius = 3.0f;
        } else if (level <= 15) {
            baseRadius = 4.0f;
        } else {
            baseRadius = 6.0f;
        }
        float radiusMultiplier = 1.0f + Math.min(level * RADIUS_MULTIPLIER_PER_LEVEL, MAX_RADIUS_MULTIPLIER - 1.0f);
        return baseRadius * radiusMultiplier;
    }
    private static void executeDeathBomb(LivingEntity entity, float damage, float radius, int deathBombLevel) {
        World world = entity.world;
        Vector3d pos = entity.getPositionVec();
        float maxBreakableHardness = deathBombLevel > 10 ?
                ADVANCED_MAX_BREAKABLE_HARDNESS : DEFAULT_MAX_BREAKABLE_HARDNESS;
        float volume = 4.0f + Math.min(deathBombLevel * 0.1f, 2.0f);
        world.playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.util.SoundEvents.ENTITY_GENERIC_EXPLODE,
                net.minecraft.util.SoundCategory.BLOCKS,
                volume, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
        List<ItemStack> generatedDrops = handleBlocks(world, pos, radius, maxBreakableHardness, deathBombLevel);
        handleEntities(world, pos, damage, radius, entity, generatedDrops);
        spawnExplosionEffects(world, pos, radius, deathBombLevel);
    }
    private static List<ItemStack> handleBlocks(World world, Vector3d centerPos, float radius, float maxBreakableHardness, int deathBombLevel) {
        java.util.ArrayList<ItemStack> generatedDrops = new java.util.ArrayList<>();
        int centerX = (int) Math.floor(centerPos.x);
        int centerY = (int) Math.floor(centerPos.y);
        int centerZ = (int) Math.floor(centerPos.z);
        int radiusInt = (int) Math.ceil(radius);
        float hardnessLimit = maxBreakableHardness;
        if (deathBombLevel > 20) {
            hardnessLimit *= 1.2f;
        }
        for (int x = centerX - radiusInt; x <= centerX + radiusInt; x++) {
            for (int y = centerY - radiusInt; y <= centerY + radiusInt; y++) {
                for (int z = centerZ - radiusInt; z <= centerZ + radiusInt; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    double distance = Math.sqrt(
                            Math.pow(x - centerPos.x, 2) +
                                    Math.pow(y - centerPos.y, 2) +
                                    Math.pow(z - centerPos.z, 2)
                    );
                    if (distance <= radius) {
                        BlockState blockState = world.getBlockState(blockPos);
                        Block block = blockState.getBlock();
                        if (blockState.isAir() || isProtectedBlock(block) || isUnbreakableBlock(block)) {
                            continue;
                        }
                        float hardness = block.getDefaultState().getBlockHardness(world, blockPos);
                        if (hardness >= 0 && hardness < hardnessLimit) {
                            float distanceFactor = 1.0f - (float)(distance / radius);
                            boolean shouldDropItems = shouldDropItems(deathBombLevel, distanceFactor);
                            if (shouldDropItems) {
                                List<ItemStack> drops = Block.getDrops(blockState, (ServerWorld) world, blockPos, null);
                                if (!drops.isEmpty()) {
                                    generatedDrops.addAll(drops);
                                }
                                world.destroyBlock(blockPos, false);
                            } else {
                                world.removeBlock(blockPos, false);
                                world.playEvent(null, 2001, blockPos, Block.getStateId(blockState));
                            }
                        }
                    }
                }
            }
        }
        return generatedDrops;
    }
    private static void handleEntities(World world, Vector3d centerPos, float damage, float radius, LivingEntity sourceEntity, List<ItemStack> newlyGeneratedDrops) {
        AxisAlignedBB area = new AxisAlignedBB(
                centerPos.x - radius, centerPos.y - radius, centerPos.z - radius,
                centerPos.x + radius, centerPos.y + radius, centerPos.z + radius
        );
        List<Entity> allEntities = world.getEntitiesWithinAABB(Entity.class, area);
        java.util.Set<Entity> entitiesToRemove = new java.util.HashSet<>();
        for (Entity target : allEntities) {
            if (target == sourceEntity) continue;
            double distance = target.getDistanceSq(centerPos.x, centerPos.y, centerPos.z);
            double maxDistanceSq = radius * radius;
            if (distance <= maxDistanceSq) {
                float distanceFactor = 1.0f - (float)Math.sqrt(distance) / radius;
                if (target instanceof LivingEntity) {
                    LivingEntity livingTarget = (LivingEntity) target;
                    float baseDamage = damage * distanceFactor;
                    float levelDamageBonus = 0.0f;
                    if (damage > 50) {
                        levelDamageBonus = damage * 0.2f;
                    }
                    float actualDamage = baseDamage + levelDamageBonus;
                    livingTarget.attackEntityFrom(DamageSource.causeExplosionDamage(sourceEntity), actualDamage);
                    float knockbackMultiplier = 2.0f + Math.min(damage * 0.01f, 1.0f);
                    Vector3d knockback = target.getPositionVec().subtract(centerPos).normalize()
                            .mul(distanceFactor * knockbackMultiplier,
                                    distanceFactor * knockbackMultiplier,
                                    distanceFactor * knockbackMultiplier);
                    target.setMotion(target.getMotion().add(knockback));
                }
                else if (target instanceof ItemEntity) {
                    ItemEntity itemEntity = (ItemEntity) target;
                    ItemStack itemStack = itemEntity.getItem();
                    if (isProtectedItem(itemStack)) {
                        float destroyChance = distanceFactor * 0.8f;
                        if (damage > 30) {
                            destroyChance += 0.2f;
                        }
                        destroyChance = Math.min(destroyChance, 0.95f);
                        if (world.rand.nextFloat() < destroyChance) {
                            entitiesToRemove.add(target);
                        }
                    }
                }
            }
        }
        for (Entity entityToRemove : entitiesToRemove) {
            entityToRemove.remove();
        }
        for (ItemStack dropStack : newlyGeneratedDrops) {
            if (isProtectedItem(dropStack)) {
                float keepChance = 0.3f;
                if (damage > 40) {
                    keepChance += 0.2f;
                }
                keepChance = Math.min(keepChance, 0.6f);
                boolean keepItem = random.nextFloat() < keepChance;
                if (keepItem) {
                    ItemEntity itemEntity = new ItemEntity(world,
                            centerPos.x + (random.nextDouble() - 0.5) * radius * 0.5,
                            centerPos.y + random.nextDouble() * radius * 0.5,
                            centerPos.z + (random.nextDouble() - 0.5) * radius * 0.5,
                            dropStack);
                    world.addEntity(itemEntity);
                }
            }
        }
    }
    private static boolean shouldDropItems(int deathBombLevel, float distanceFactor) {
        float baseDropRate;
        if (deathBombLevel <= 5) {
            baseDropRate = 0.8f;
        } else if (deathBombLevel <= 15) {
            baseDropRate = 0.6f;
        } else {
            baseDropRate = 0.4f;
        }
        float distanceDropModifier = distanceFactor * 0.3f;
        float levelDropReduction = Math.min(deathBombLevel * 0.02f, 0.3f);
        float finalDropRate = Math.max(baseDropRate - distanceDropModifier - levelDropReduction, 0.05f);
        return random.nextFloat() < finalDropRate;
    }
    private static void spawnExplosionEffects(World world, Vector3d pos, float radius, int deathBombLevel) {
        if (!world.isRemote) {
            return;
        }
        int particleCount = 20 + Math.min(deathBombLevel, 30);
        for (int i = 0; i < particleCount; ++i) {
            double d0 = random.nextGaussian() * 0.02D;
            double d1 = random.nextGaussian() * 0.02D;
            double d2 = random.nextGaussian() * 0.02D;
            double particleScale = 1.0 + Math.min(deathBombLevel * 0.01, 0.5);
            world.addParticle(net.minecraft.particles.ParticleTypes.EXPLOSION,
                    pos.x + (random.nextFloat() * radius * 2.0F) - radius,
                    pos.y + (random.nextFloat() * radius * 2.0F) - radius,
                    pos.z + (random.nextFloat() * radius * 2.0F) - radius,
                    d0 * particleScale, d1 * particleScale, d2 * particleScale);
        }
        int smokeCount = 15 + Math.min(deathBombLevel / 2, 20);
        for (int i = 0; i < smokeCount; ++i) {
            double offsetX = (random.nextDouble() - 0.5) * radius * 1.5;
            double offsetY = random.nextDouble() * radius * 1.5;
            double offsetZ = (random.nextDouble() - 0.5) * radius * 1.5;
            double smokeScale = 0.1 + Math.min(deathBombLevel * 0.002, 0.1);
            world.addParticle(net.minecraft.particles.ParticleTypes.SMOKE,
                    pos.x, pos.y, pos.z,
                    offsetX * smokeScale, offsetY * smokeScale, offsetZ * smokeScale);
        }
        if (deathBombLevel > 25) {
            for (int i = 0; i < 10; ++i) {
                double flameOffsetX = (random.nextDouble() - 0.5) * radius * 2.0;
                double flameOffsetY = random.nextDouble() * radius * 1.5;
                double flameOffsetZ = (random.nextDouble() - 0.5) * radius * 2.0;
                world.addParticle(net.minecraft.particles.ParticleTypes.FLAME,
                        pos.x + flameOffsetX,
                        pos.y + flameOffsetY,
                        pos.z + flameOffsetZ,
                        0, 0.02, 0);
            }
        }
    }
    private static boolean isProtectedItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return true;
        }
        Item item = itemStack.getItem();
        ResourceLocation itemId = item.getRegistryName();
        if (itemId == null) {
            return true;
        }
        return !PROTECTED_ITEMS.contains(itemId.toString());
    }
    private static boolean isProtectedBlock(Block block) {
        ResourceLocation blockId = block.getRegistryName();
        if (blockId == null) {
            return false;
        }
        return PROTECTED_ITEMS.contains(blockId.toString());
    }
    private static boolean isUnbreakableBlock(Block block) {
        return block == net.minecraft.block.Blocks.BEDROCK ||
                block == net.minecraft.block.Blocks.END_PORTAL_FRAME ||
                block == net.minecraft.block.Blocks.END_PORTAL ||
                block == net.minecraft.block.Blocks.NETHER_PORTAL ||
                block == net.minecraft.block.Blocks.OBSIDIAN ||
                block == net.minecraft.block.Blocks.ANCIENT_DEBRIS ||
                block == net.minecraft.block.Blocks.RESPAWN_ANCHOR;
    }
}