package com.weaponhouse.enhance.enhances;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
@Mod.EventBusSubscriber(modid = "enhance")
public class DeathBombHandler {
    private static final float DEFAULT_MAX_BREAKABLE_HARDNESS = 1.2f;
    private static final float ADVANCED_MAX_BREAKABLE_HARDNESS = 2.0f;
    @SubscribeEvent
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
        return 4.0f + (level - 1) * 3.0f;
    }
    private static float calculateRadius(int level) {
        if (level <= 5) {
            return 3.0f;
        } else if (level <= 10) {
            return 5.0f;
        } else {
            return 7.0f;
        }
    }
    private static void executeDeathBomb(LivingEntity entity, float damage, float radius, int deathBombLevel) {
        World world = entity.world;
        Vector3d pos = entity.getPositionVec();
        int centerX = (int) Math.floor(pos.x);
        int centerY = (int) Math.floor(pos.y);
        int centerZ = (int) Math.floor(pos.z);
        float maxBreakableHardness = deathBombLevel > 10 ?
                ADVANCED_MAX_BREAKABLE_HARDNESS : DEFAULT_MAX_BREAKABLE_HARDNESS;
        AxisAlignedBB area = new AxisAlignedBB(
                pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius
        );
        List<Entity> allEntities = world.getEntitiesWithinAABB(Entity.class, area);
        for (Entity target : allEntities) {
            if (target == entity) continue;
            if (target instanceof LivingEntity && !(target instanceof ItemEntity)) {
                LivingEntity livingTarget = (LivingEntity) target;
                double distance = target.getDistanceSq(pos.x, pos.y, pos.z);
                double maxDistanceSq = radius * radius;
                if (distance <= maxDistanceSq) {
                    float distanceFactor = 1.0f - (float)Math.sqrt(distance) / radius;
                    float actualDamage = damage * distanceFactor;
                    livingTarget.attackEntityFrom(DamageSource.causeExplosionDamage(entity), actualDamage);
                    Vector3d knockback = target.getPositionVec().subtract(pos).normalize()
                            .mul(distanceFactor * 2.0, distanceFactor * 2.0, distanceFactor * 2.0);
                    target.setMotion(target.getMotion().add(knockback));
                }
            }
        }
        if (!world.isRemote) {
            for (int x = centerX - (int) radius; x <= centerX + (int) radius; x++) {
                for (int y = centerY - (int) radius; y <= centerY + (int) radius; y++) {
                    for (int z = centerZ - (int) radius; z <= centerZ + (int) radius; z++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState blockState = world.getBlockState(blockPos);
                        Block block = blockState.getBlock();
                        if (blockState.isAir()) {
                            continue;
                        }
                        double distance = Math.sqrt(
                                Math.pow(x - pos.x, 2) +
                                        Math.pow(y - pos.y, 2) +
                                        Math.pow(z - pos.z, 2)
                        );
                        if (distance <= radius) {
                            float hardness = block.getDefaultState().getBlockHardness(world, blockPos);
                            if (hardness >= 0 && hardness < maxBreakableHardness) {
                                world.destroyBlock(blockPos, true);
                            }
                        }
                    }
                }
            }
        }
        if (world.isRemote) {
            for (int i = 0; i < 20; ++i) {
                double d0 = world.rand.nextGaussian() * 0.02D;
                double d1 = world.rand.nextGaussian() * 0.02D;
                double d2 = world.rand.nextGaussian() * 0.02D;
                world.addParticle(net.minecraft.particles.ParticleTypes.EXPLOSION,
                        pos.x + (world.rand.nextFloat() * radius * 2.0F) - radius,
                        pos.y + (world.rand.nextFloat() * radius * 2.0F) - radius,
                        pos.z + (world.rand.nextFloat() * radius * 2.0F) - radius,
                        d0, d1, d2);
            }
        }
        world.playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.util.SoundEvents.ENTITY_GENERIC_EXPLODE,
                net.minecraft.util.SoundCategory.BLOCKS,
                4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
    }
}