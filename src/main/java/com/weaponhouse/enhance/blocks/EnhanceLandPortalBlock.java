package com.weaponhouse.enhance.blocks;

import com.weaponhouse.enhance.world.dimension.DimensionRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Objects;
import java.util.Random;
public class EnhanceLandPortalBlock extends Block {
    public static final IntegerProperty ANIMATION_FRAME = IntegerProperty.create("animation_frame", 0, 27);
    public EnhanceLandPortalBlock() {
        super(Block.Properties.create(Material.PORTAL)
                .doesNotBlockMovement()
                .hardnessAndResistance(-1.0F)
                .setLightLevel(state -> 11)
                .noDrops()
                .tickRandomly());
        this.setDefaultState(this.stateContainer.getBaseState().with(ANIMATION_FRAME, 0));
    }
    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(ANIMATION_FRAME);
    }
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isRemote &&
                !entity.isPassenger() &&
                !entity.isBeingRidden() &&
                entity.canChangeDimension() &&
                entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            teleportToChaoslands(player);
        }
    }
    private void teleportToChaoslands(ServerPlayerEntity player) {
        ServerWorld chaosWorld = Objects.requireNonNull(player.getServer()).getWorld(DimensionRegistry.ENHANCE_DIMENSION);
        if (chaosWorld == null) return;
        BlockPos safePos = findRandomSafeLandingPosition(chaosWorld);
        playTeleportSound(player.world, player.getPosition());
        teleportPlayer(player, chaosWorld, safePos);
        playTeleportSound(chaosWorld, safePos);
    }
    private void teleportPlayer(ServerPlayerEntity player, ServerWorld targetWorld, BlockPos targetPos) {
        player.stopRiding();
        if (player.world == targetWorld) {
            player.connection.setPlayerLocation(
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    player.rotationYaw,
                    player.rotationPitch
            );
        } else {
            player.teleport(
                    targetWorld,
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    player.rotationYaw,
                    player.rotationPitch
            );
        }
    }
    private BlockPos findRandomSafeLandingPosition(ServerWorld chaosWorld) {
        Random random = new Random();
        for (int attempt = 0; attempt < 30; attempt++) {
            int x = random.nextInt(4000) - 2000;
            int z = random.nextInt(4000) - 2000;
            int surfaceY = chaosWorld.getHeight(net.minecraft.world.gen.Heightmap.Type.WORLD_SURFACE, x, z);
            if (surfaceY >= 20 && surfaceY <= 200) {
                BlockPos pos = new BlockPos(x, surfaceY + 1, z);
                if (isSafePosition(chaosWorld, pos)) {
                    return pos;
                }
            }
        }
        return generateSafePlatform(chaosWorld);
    }
    private BlockPos generateSafePlatform(ServerWorld chaosWorld) {
        Random random = new Random();
        int x = random.nextInt(4000) - 2000;
        int z = random.nextInt(4000) - 2000;
        int y = Math.max(80, 20);
        BlockPos centerPos = new BlockPos(x, y, z);
        generateStonePlatform(chaosWorld, centerPos);
        return centerPos.up();
    }
    private void generateStonePlatform(ServerWorld world, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos platformPos = center.add(dx, 0, dz);
                world.setBlockState(platformPos, Blocks.STONE.getDefaultState(), 3);
            }
        }
    }
    private boolean isSafePosition(World world, BlockPos pos) {
        BlockState currentState = world.getBlockState(pos);
        BlockState belowState = world.getBlockState(pos.down());
        return !currentState.getMaterial().isLiquid() &&
                !currentState.getMaterial().blocksMovement() &&
                belowState.getMaterial().isSolid() &&
                !belowState.getMaterial().isLiquid();
    }
    private void playTeleportSound(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new EnhanceLandPortalTileEntity();
    }
}