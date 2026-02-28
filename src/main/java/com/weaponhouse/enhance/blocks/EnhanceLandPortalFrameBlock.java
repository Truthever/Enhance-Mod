package com.weaponhouse.enhance.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

import static net.minecraft.block.TwistingVinesBlock.SHAPE;
public class EnhanceLandPortalFrameBlock extends Block {
    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");
    public static final EnumProperty<PillType> PILL_TYPE = EnumProperty.create("pill_type", PillType.class);
    public EnhanceLandPortalFrameBlock() {
        super(Properties.create(Material.ROCK, MaterialColor.BLACK)
                .hardnessAndResistance(50.0f, 1200.0f)
                .sound(SoundType.STONE)
                .harvestLevel(3)
                .harvestTool(ToolType.PICKAXE)
                .setRequiresTool()
                .setLightLevel(state -> 15)
                .notSolid()
        );
        this.setDefaultState(this.getStateContainer().getBaseState()
                .with(ACTIVATED, false)
                .with(PILL_TYPE, PillType.DEFAULT));
    }
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return SHAPE;
    }
    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return SHAPE;
    }
    @Override
    public boolean isTransparent(BlockState state) {
        return true;
    }
    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }
    @Override
    public float getAmbientOcclusionLightValue(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return 1.0F;
    }
    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED, PILL_TYPE);
    }
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new EnhanceLandPortalFrameTileEntity();
    }
    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof EnhanceLandPortalFrameTileEntity)) {
            return ActionResultType.PASS;
        }
        EnhanceLandPortalFrameTileEntity frameTileEntity = (EnhanceLandPortalFrameTileEntity) tileEntity;
        ItemStack heldItem = player.getHeldItem(hand);
        if (!frameTileEntity.hasPill() && heldItem.getItem() instanceof com.weaponhouse.enhance.items.EnhancePillItem) {
            ItemStack pillStack = heldItem.copy();
            pillStack.setCount(1);
            frameTileEntity.setPillStack(pillStack);
            if (!player.abilities.isCreativeMode) {
                heldItem.shrink(1);
            }
            com.weaponhouse.enhance.common.PillTextureType pillTextureType =
                    com.weaponhouse.enhance.common.PillBuffGenerator.getTextureTypeFromPill(pillStack);
            PillType pillType = PillType.fromTextureType(pillTextureType);
            world.setBlockState(pos, state.with(ACTIVATED, true).with(PILL_TYPE, pillType), 3);
            world.playSound(null, pos, net.minecraft.util.SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                    net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
            checkForPortalLines(world, pos);
            return ActionResultType.SUCCESS;
        }
        else if (frameTileEntity.hasPill() && heldItem.isEmpty()) {
            ItemStack pillStack = frameTileEntity.removePill();
            if (!player.inventory.addItemStackToInventory(pillStack)) {
                ItemEntity itemEntity = new ItemEntity(world, player.getPosX(), player.getPosY(), player.getPosZ(), pillStack);
                world.addEntity(itemEntity);
            }
            world.setBlockState(pos, state.with(ACTIVATED, false).with(PILL_TYPE, PillType.DEFAULT), 3);
            world.playSound(null, pos, net.minecraft.util.SoundEvents.BLOCK_STONE_PLACE,
                    net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
            checkForPortalLines(world, pos);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }
    public enum PillType implements IStringSerializable {
        DEFAULT("default"),
        ATTACK("attack"),
        LIFE("life"),
        DEFENSE("defense"),
        SPEED("speed");
        private final String name;
        PillType(String name) {
            this.name = name;
        }
        @Override
        public String getString() {
            return name;
        }
        public static PillType fromTextureType(com.weaponhouse.enhance.common.PillTextureType textureType) {
            switch (textureType) {
                case ATTACK: return ATTACK;
                case LIFE: return LIFE;
                case DEFENSE: return DEFENSE;
                case SPEED: return SPEED;
                default: return DEFAULT;
            }
        }
    }
    private void checkForPortalLines(World world, BlockPos framePos) {
        Set<BlockPos> processedCenters = new HashSet<>();
        for (BlockPos relativePos : PortalActivator.PORTAL_FRAME_POSITIONS) {
            BlockPos center = framePos.subtract(relativePos);
            if (isValidPortalCenter(world, center) && processedCenters.add(center)) {
                PortalActivator.checkAndUpdateLines(world, center);
            }
        }
    }
    private boolean isValidPortalCenter(World world, BlockPos center) {
        int validFrameCount = 0;
        for (BlockPos relativePos : PortalActivator.PORTAL_FRAME_POSITIONS) {
            BlockPos framePos = center.add(relativePos);
            BlockState state = world.getBlockState(framePos);
            if (state.getBlock() instanceof EnhanceLandPortalFrameBlock) {
                validFrameCount++;
            }
        }
        return validFrameCount >= 4;
    }
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof EnhanceLandPortalFrameTileEntity) {
                EnhanceLandPortalFrameTileEntity frameTileEntity = (EnhanceLandPortalFrameTileEntity) tileEntity;
                if (frameTileEntity.hasPill()) {
                    ItemStack pillStack = frameTileEntity.getPillStack();
                    ItemEntity itemEntity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, pillStack);
                    world.addEntity(itemEntity);
                }
                removeRelatedLines(world, pos);
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }
    private void removeRelatedLines(World world, BlockPos framePos) {
        Set<BlockPos> processedCenters = new HashSet<>();
        for (BlockPos relativePos : PortalActivator.PORTAL_FRAME_POSITIONS) {
            BlockPos center = framePos.subtract(relativePos);
            if (processedCenters.add(center)) {
                PortalActivator.removeAllLines(world, center);
            }
        }
    }
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onBlockAdded(state, world, pos, oldState, isMoving);
        if (!world.isRemote && state.get(ACTIVATED)) {
            checkForPortalLines(world, pos);
        }
    }
}