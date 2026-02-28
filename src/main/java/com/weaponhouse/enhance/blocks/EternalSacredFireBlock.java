package com.weaponhouse.enhance.blocks;

import com.weaponhouse.enhance.items.PurpleGourdItem;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public class EternalSacredFireBlock extends FireBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final ThreadLocal<Boolean> isReplacing = ThreadLocal.withInitial(() -> false);
    private static final Set<BlockPos> protectedPositions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final java.util.Map<java.util.UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    private static final long DAMAGE_COOLDOWN = 20;
    public EternalSacredFireBlock() {
        super(Properties.from(net.minecraft.block.Blocks.FIRE)
                .doesNotBlockMovement()
                .setLightLevel((state) -> state.get(ACTIVE) ? 15 : 0)
                .tickRandomly()
                .notSolid()
                .hardnessAndResistance(-1.0F, 3600000.0F)
                .noDrops()
        );
        this.setDefaultState(this.getDefaultState()
                .with(ACTIVE, true)
                .with(AGE, 0));
    }
    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(ACTIVE);
    }
    @Override
    protected BlockState getStateForPlacement(IBlockReader worldIn, BlockPos pos) {
        return this.getDefaultState().with(ACTIVE, true).with(AGE, 0);
    }
    @Override
    public int getFireSpreadSpeed(BlockState state, IBlockReader world, BlockPos pos, Direction face) {
        return 0;
    }
    @Override
    public int getFlammability(BlockState state, IBlockReader world, BlockPos pos, Direction face) {
        return 0;
    }
    @Override
    protected boolean canBurn(BlockState state) {
        return false;
    }
    @Override
    public boolean isReplaceable(BlockState state, net.minecraft.item.BlockItemUseContext useContext) {
        return false;
    }
    @Override
    public boolean canHarvestBlock(BlockState state, net.minecraft.world.IBlockReader world, BlockPos pos, PlayerEntity player) {
        return false;
    }
    @Override
    public boolean isToolEffective(BlockState state, net.minecraftforge.common.ToolType tool) {
        return false;
    }
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (PurpleGourdItem.isAbsorbingFire()) {
            super.onReplaced(state, world, pos, newState, isMoving);
            return;
        }
        if (newState.getBlock() != this) {
            if (isReplacing.get()) {
                super.onReplaced(state, world, pos, newState, isMoving);
                return;
            }
            isReplacing.set(true);
            try {
                world.setBlockState(pos, this.getDefaultState()
                        .with(ACTIVE, true)
                        .with(AGE, 0), 3);
                if (!world.isRemote) {
                    world.getPendingBlockTicks().scheduleTick(pos, this, 1);
                }
                if (!world.isRemote) {
                    world.playSound(null, pos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 0.5F, 1.0F);
                }
            } finally {
                isReplacing.set(false);
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
        if (isPositionProtected(pos)) {
            return;
        }
        if (!state.get(ACTIVE) || state.get(AGE) != 0) {
            world.setBlockState(pos, state.with(ACTIVE, true).with(AGE, 0), 3);
        }
        world.notifyBlockUpdate(pos, state, state, 3);
    }
    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
        world.getPendingBlockTicks().scheduleTick(pos, this, 1);
        if (isPositionProtected(pos)) {
            return;
        }
        if (!state.get(ACTIVE) || state.get(AGE) != 0) {
            world.setBlockState(pos, state.with(ACTIVE, true).with(AGE, 0), 3);
        }
        if (world.getGameTime() % 20 == 0) {
            destroyAllSurroundingElements(world, pos);
        }
        world.notifyBlockUpdate(pos, state, state, 3);
    }
    private void destroyAllSurroundingElements(ServerWorld world, BlockPos centerPos) {
        int range = 20;
        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();
        int destroyedCount = 0;
        int minY = Math.max(0, centerY - range);
        int maxY = Math.min(world.getHeight() - 1, centerY + range);
        for (int x = centerX - range; x <= centerX + range; x++) {
            for (int z = centerZ - range; z <= centerZ + range; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos targetPos = new BlockPos(x, y, z);
                    try {
                        BlockState targetState = world.getBlockState(targetPos);
                        Block targetBlock = targetState.getBlock();
                        if (shouldDestroyBlock(targetBlock, targetState)) {
                            world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), 3);
                            destroyedCount++;
                            if (destroyedCount % 5 == 0) {
                                world.spawnParticle(ParticleTypes.SMOKE,
                                        targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                                        2, 0.2, 0.2, 0.2, 0.02);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if (destroyedCount > 0) {
            world.playSound(null, centerPos, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.BLOCKS, 0.3F, 1.5F);
        }
    }
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (isPositionProtected(pos)) {
            return;
        }
        if (!world.isRemote && entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            long currentTime = world.getGameTime();
            java.util.UUID entityId = living.getUniqueID();
            Long lastDamage = lastDamageTime.get(entityId);
            if (lastDamage == null || (currentTime - lastDamage) >= DAMAGE_COOLDOWN) {
                lastDamageTime.put(entityId, currentTime);
                applyAbsoluteFireDamage(living);
            }
            if (lastDamage == null || (currentTime - lastDamage) >= DAMAGE_COOLDOWN) {
                net.minecraft.potion.EffectInstance fireResistance = living.getActivePotionEffect(net.minecraft.potion.Effects.FIRE_RESISTANCE);
                if (fireResistance != null) {
                    living.removePotionEffect(net.minecraft.potion.Effects.FIRE_RESISTANCE);
                }
                living.setFire(30);
                living.addPotionEffect(new net.minecraft.potion.EffectInstance(
                        net.minecraft.potion.Effects.GLOWING, 600, 0, false, false));
            }
        }
        super.onEntityCollision(state, world, pos, entity);
    }
    private void cleanupOldDamageRecords(World world) {
        long currentTime = world.getGameTime();
        if (currentTime % 1000 == 0) {
            lastDamageTime.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 6000);
        }
    }
    private void applyAbsoluteFireDamage(LivingEntity entity) {
        float currentHealth = entity.getHealth();
        java.util.Map<net.minecraft.potion.Effect, net.minecraft.potion.EffectInstance> savedEffects =
                new java.util.HashMap<>();
        if (entity.isPotionActive(net.minecraft.potion.Effects.FIRE_RESISTANCE)) {
            savedEffects.put(net.minecraft.potion.Effects.FIRE_RESISTANCE,
                    entity.getActivePotionEffect(net.minecraft.potion.Effects.FIRE_RESISTANCE));
            entity.removePotionEffect(net.minecraft.potion.Effects.FIRE_RESISTANCE);
        }
        if (entity.isPotionActive(net.minecraft.potion.Effects.RESISTANCE)) {
            savedEffects.put(net.minecraft.potion.Effects.RESISTANCE,
                    entity.getActivePotionEffect(net.minecraft.potion.Effects.RESISTANCE));
            entity.removePotionEffect(net.minecraft.potion.Effects.RESISTANCE);
        }
        float absorptionAmount = entity.getAbsorptionAmount();
        entity.setAbsorptionAmount(0);
        try {
            entity.attackEntityFrom(DamageSource.IN_FIRE, (float) 10.0);
            if (entity.isImmuneToFire()) {
                entity.attackEntityFrom(DamageSource.GENERIC, (float) 10.0);
            }
        } finally {
            for (java.util.Map.Entry<net.minecraft.potion.Effect, net.minecraft.potion.EffectInstance> entry : savedEffects.entrySet()) {
                entity.addPotionEffect(entry.getValue());
            }
            entity.setAbsorptionAmount(absorptionAmount);
        }
        if (Math.abs(entity.getHealth() - currentHealth) < 0.1) {
            entity.setHealth(Math.max(0.0F, currentHealth - (float) 10.0));
        }
    }
    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        if (isPositionProtected(pos)) {
            return;
        }
        if (entity instanceof FallingBlockEntity && !world.isRemote) {
            FallingBlockEntity fallingBlock = (FallingBlockEntity) entity;
            BlockState fallingState = fallingBlock.getBlockState();
            if (isConsumableBlock(fallingState)) {
                fallingBlock.remove();
                if (world instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld) world;
                    serverWorld.spawnParticle(ParticleTypes.FLAME,
                            entity.getPosX(), entity.getPosY(), entity.getPosZ(),
                            10, 0.5, 0.5, 0.5, 0.1);
                }
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        SoundCategory.BLOCKS, 0.8F, 0.5F);
            }
        }
        super.onEntityWalk(world, pos, entity);
    }
    private boolean isConsumableBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.GRAVEL ||
                block == Blocks.SAND ||
                block == Blocks.RED_SAND ||
                block == Blocks.DIRT ||
                block == Blocks.COBBLESTONE ||
                block == Blocks.STONE;
    }
    private boolean shouldDestroyBlock(Block block, BlockState state) {
        if (block == Blocks.WATER || state.getFluidState().getFluid() == Fluids.WATER ||
                state.getFluidState().getFluid() == Fluids.FLOWING_WATER) {
            return true;
        }
        if (block == Blocks.LAVA || state.getFluidState().getFluid() == Fluids.LAVA ||
                state.getFluidState().getFluid() == Fluids.FLOWING_LAVA) {
            return true;
        }
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE ||
                block == Blocks.FROSTED_ICE) {
            return true;
        }
        if (block == Blocks.SNOW || block == Blocks.SNOW_BLOCK) {
            return true;
        }
        return state.getMaterial() == Material.SNOW;
    }
    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        cleanupOldDamageRecords(world);
        if (isPositionProtected(pos)) {
            return;
        }
        if (world.getBlockState(fromPos).getMaterial() == Material.WATER) {
            if (world.isRemote) {
                spawnSteamParticles(world, pos);
            }
            if (!world.isRemote) {
                world.setBlockState(pos, state.with(ACTIVE, true).with(AGE, 0), 3);
            }
            return;
        }
        if (world.getBlockState(fromPos).getBlock() instanceof FireBlock) {
            if (!state.get(ACTIVE) || state.get(AGE) != 0) {
                world.setBlockState(pos, state.with(ACTIVE, true).with(AGE, 0), 3);
            }
            return;
        }
        if (!state.get(ACTIVE) || state.get(AGE) != 0) {
            world.setBlockState(pos, state.with(ACTIVE, true).with(AGE, 0), 3);
        }
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
    }
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
        cleanupOldDamageRecords(world);
        if (isPositionProtected(pos)) {
            return;
        }
        if (!state.get(ACTIVE) || state.get(AGE) != 0) {
            world.setBlockState(pos, state.with(ACTIVE, true).with(AGE, 0), 3);
        }
        if (!world.isRemote) {
            world.getPendingBlockTicks().scheduleTick(pos, this, 1);
        }
        if (!world.isRemote) {
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
    @Override
    public void animateTick(BlockState state, World world, BlockPos pos, Random rand) {
        if (isPositionProtected(pos)) {
            return;
        }
        if (!state.get(ACTIVE)) {
            for(int i = 0; i < 2; ++i) {
                double d0 = (double)pos.getX() + rand.nextDouble();
                double d1 = (double)pos.getY() + rand.nextDouble() * 0.5D + 0.5D;
                double d2 = (double)pos.getZ() + rand.nextDouble();
                world.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            }
            if (rand.nextInt(48) == 0) {
                world.playSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D,
                        SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS,
                        0.5F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.3F, false);
            }
            return;
        }
        if (rand.nextInt(24) == 0) {
            world.playSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D,
                    SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS,
                    1.0F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.3F, false);
        }
        for(int i = 0; i < 3; ++i) {
            double d0 = (double)pos.getX() + rand.nextDouble();
            double d1 = (double)pos.getY() + rand.nextDouble() * 0.5D + 0.5D;
            double d2 = (double)pos.getZ() + rand.nextDouble();
            world.addParticle(ParticleTypes.LARGE_SMOKE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }
        if (rand.nextInt(10) == 0) {
            world.addParticle(ParticleTypes.FLAME,
                    pos.getX() + rand.nextDouble(),
                    pos.getY() + 1.0D,
                    pos.getZ() + rand.nextDouble(),
                    0.0D, 0.1D, 0.0D);
        }
        if (rand.nextInt(5) == 0) {
            for(int i = 0; i < 2; ++i) {
                double sparkX = pos.getX() + 0.5D + (rand.nextDouble() - 0.5D) * 0.5D;
                double sparkY = pos.getY() + 0.8D + rand.nextDouble() * 0.5D;
                double sparkZ = pos.getZ() + 0.5D + (rand.nextDouble() - 0.5D) * 0.5D;
                world.addParticle(ParticleTypes.FLAME, sparkX, sparkY, sparkZ,
                        (rand.nextDouble() - 0.5D) * 0.05D,
                        rand.nextDouble() * 0.1D,
                        (rand.nextDouble() - 0.5D) * 0.05D);
            }
        }
    }
    private void spawnSteamParticles(World world, BlockPos pos) {
        Random rand = world.rand;
        for(int i = 0; i < 8; ++i) {
            double steamX = pos.getX() + rand.nextDouble();
            double steamY = pos.getY() + rand.nextDouble();
            double steamZ = pos.getZ() + rand.nextDouble();
            world.addParticle(ParticleTypes.CLOUD, steamX, steamY, steamZ,
                    0.0D, 0.1D, 0.0D);
        }
    }
    @Override
    public PushReaction getPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
    @Override
    public float getPlayerRelativeBlockHardness(BlockState state, PlayerEntity player, IBlockReader worldIn, BlockPos pos) {
        return 0.0F;
    }
    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (isPositionProtected(currentPos)) {
            return stateIn;
        }
        if (facingState.getBlock() instanceof FireBlock) {
            if (!stateIn.get(ACTIVE) || stateIn.get(AGE) != 0) {
                if (worldIn instanceof World) {
                    worldIn.setBlockState(currentPos, stateIn.with(ACTIVE, true).with(AGE, 0), 3);
                }
            }
            return stateIn;
        }
        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }
    public static void addProtectedPosition(BlockPos pos) {
        protectedPositions.add(pos.toImmutable());
    }
    public static void removeProtectedPosition(BlockPos pos) {
        protectedPositions.remove(pos);
    }
    public static boolean isPositionProtected(BlockPos pos) {
        return protectedPositions.contains(pos);
    }
}