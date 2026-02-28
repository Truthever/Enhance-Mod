package com.weaponhouse.enhance.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class EnhanceLandPortalTileEntity extends TileEntity implements ITickableTileEntity {
    public EnhanceLandPortalTileEntity() {
        super(com.weaponhouse.enhance.util.RegistryHandler.ENHANCE_LAND_PORTAL_TE.get());
    }
    @Override
    public void tick() {
        if (world != null && !world.isRemote && pos != null) {
            updateAnimationFrame();
        }
    }
    private void updateAnimationFrame() {
        World world = getWorld();
        BlockPos pos = getPos();
        if (world != null) {
            BlockState currentState = world.getBlockState(pos);
            if (currentState.getBlock() instanceof EnhanceLandPortalBlock) {
                int currentFrame = currentState.get(EnhanceLandPortalBlock.ANIMATION_FRAME);
                int nextFrame = (currentFrame + 1) % 28;
                if (currentFrame != nextFrame) {
                    world.setBlockState(pos, currentState.with(EnhanceLandPortalBlock.ANIMATION_FRAME, nextFrame), 3);
                }
            }
        }
    }
}