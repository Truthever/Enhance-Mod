package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.blocks.EternalSacredFireBlock;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
public class PurpleGourdItem extends Item {
    private static final String SACRED_FIRE_COUNT = "SacredFireCount";
    private static final String MODE = "Mode";
    private static final ThreadLocal<Boolean> absorbingFire = ThreadLocal.withInitial(() -> false);
    public PurpleGourdItem() {
        super(new Properties()
                .group(Enhance.TAB)
                .maxStackSize(1)
        );
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            toggleMode(stack);
            if (world.isRemote) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f, 1.2f);
            }
            if (world.isRemote) {
                int mode = getMode(stack);
                TranslationTextComponent message = new TranslationTextComponent(
                        mode == 0 ? "item.enhance.purple_gourd.mode_absorb" : "item.enhance.purple_gourd.mode_release"
                );
                TranslationTextComponent fullMessage = (TranslationTextComponent) new TranslationTextComponent(
                        "item.enhance.purple_gourd.mode_switch",
                        message
                ).mergeStyle(TextFormatting.YELLOW);
                player.sendMessage(fullMessage, player.getUniqueID());
            }
            return ActionResult.resultSuccess(stack);
        } else {
            int mode = getMode(stack);
            if (mode == 0) {
                return tryAbsorbSacredFire(world, player, stack, hand);
            } else {
                return tryPlaceSacredFire(world, player, stack, hand);
            }
        }
    }
    private ActionResult<ItemStack> tryAbsorbSacredFire(World world, PlayerEntity player, ItemStack stack, Hand hand) {
        BlockRayTraceResult rayTrace = rayTrace(world, player, RayTraceContext.FluidMode.NONE);
        if (rayTrace.getType() == BlockRayTraceResult.Type.BLOCK) {
            BlockPos pos = rayTrace.getPos();
            if (world.getBlockState(pos).getBlock() == RegistryHandler.ETERNAL_SACRED_FIRE.get()) {
                absorbingFire.set(true);
                try {
                    List<BlockPos> adjacentFires = getAdjacentSacredFires(world, pos);
                    for (BlockPos adjacentPos : adjacentFires) {
                        EternalSacredFireBlock.addProtectedPosition(adjacentPos);
                    }
                    try {
                        if (!world.isRemote) {
                            world.removeBlock(pos, false);
                            addSacredFireCount(stack);
                            world.playSound(null, pos, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS, 1.0f, 0.8f);
                            world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0f, 0.8f);
                        }
                        if (!world.isRemote) {
                            ServerWorld serverWorld = (ServerWorld) world;
                            serverWorld.spawnParticle(net.minecraft.particles.ParticleTypes.SOUL_FIRE_FLAME,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    20, 0.3, 0.3, 0.3, 0.1);
                            serverWorld.spawnParticle(net.minecraft.particles.ParticleTypes.FLAME,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    10, 0.2, 0.2, 0.2, 0.05);
                        }
                        world.playSound(player, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.5f);
                        if (!world.isRemote) {
                            for (BlockPos adjacentPos : adjacentFires) {
                                world.setBlockState(adjacentPos,
                                        RegistryHandler.ETERNAL_SACRED_FIRE.get()
                                                .getDefaultState()
                                                .with(EternalSacredFireBlock.ACTIVE, true)
                                                .with(net.minecraft.block.FireBlock.AGE, 0),
                                        3);
                            }
                        }
                        player.swingArm(hand);
                        return ActionResult.resultSuccess(stack);
                    } finally {
                        for (BlockPos adjacentPos : adjacentFires) {
                            EternalSacredFireBlock.removeProtectedPosition(adjacentPos);
                        }
                    }
                } finally {
                    absorbingFire.set(false);
                }
            }
        }
        if (world.isRemote) {
            player.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
        }
        return ActionResult.resultPass(stack);
    }
    private List<BlockPos> getAdjacentSacredFires(World world, BlockPos centerPos) {
        List<BlockPos> adjacentFires = new ArrayList<>();
        for (net.minecraft.util.Direction direction : net.minecraft.util.Direction.values()) {
            BlockPos adjacentPos = centerPos.offset(direction);
            if (world.getBlockState(adjacentPos).getBlock() == RegistryHandler.ETERNAL_SACRED_FIRE.get()) {
                adjacentFires.add(adjacentPos);
            }
        }
        return adjacentFires;
    }
    private ActionResult<ItemStack> tryPlaceSacredFire(World world, PlayerEntity player, ItemStack stack, Hand hand) {
        if (getSacredFireCount(stack) <= 0) {
            if (world.isRemote) {
                player.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
                TranslationTextComponent message = (TranslationTextComponent) new TranslationTextComponent("item.enhance.purple_gourd.no_fire")
                        .mergeStyle(TextFormatting.RED);
                player.sendMessage(message, player.getUniqueID());
            }
            return ActionResult.resultFail(stack);
        }
        BlockRayTraceResult rayTrace = rayTrace(world, player, RayTraceContext.FluidMode.NONE);
        if (rayTrace.getType() == BlockRayTraceResult.Type.BLOCK) {
            BlockPos placePos = rayTrace.getPos().offset(rayTrace.getFace());
            if (world.isAirBlock(placePos) || world.getBlockState(placePos).getMaterial().isReplaceable()) {
                if (!world.isRemote) {
                    world.setBlockState(placePos,
                            RegistryHandler.ETERNAL_SACRED_FIRE.get()
                                    .getDefaultState()
                                    .with(EternalSacredFireBlock.ACTIVE, true)
                                    .with(net.minecraft.block.FireBlock.AGE, 0),
                            3);
                    consumeSacredFire(stack);
                    world.playSound(null, placePos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    world.playSound(null, placePos, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS, 0.5f, 1.2f);
                    ServerWorld serverWorld = (ServerWorld) world;
                    serverWorld.spawnParticle(net.minecraft.particles.ParticleTypes.FLAME,
                            placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5,
                            15, 0.3, 0.3, 0.3, 0.1);
                    serverWorld.spawnParticle(net.minecraft.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5,
                            5, 0.2, 0.2, 0.2, 0.05);
                }
                player.swingArm(hand);
                return ActionResult.resultSuccess(stack);
            }
        }
        return ActionResult.resultPass(stack);
    }
    private void toggleMode(ItemStack stack) {
        int currentMode = getMode(stack);
        int newMode = (currentMode + 1) % 2;
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putInt(MODE, newMode);
    }
    public int getSacredFireCount(ItemStack stack) {
        CompoundNBT nbt = stack.getTag();
        if (nbt != null && nbt.contains(SACRED_FIRE_COUNT)) {
            return nbt.getInt(SACRED_FIRE_COUNT);
        }
        return 0;
    }
    private void setSacredFireCount(ItemStack stack, int count) {
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putInt(SACRED_FIRE_COUNT, Math.max(0, count));
    }
    private void addSacredFireCount(ItemStack stack) {
        setSacredFireCount(stack, getSacredFireCount(stack) + 1);
    }
    private void consumeSacredFire(ItemStack stack) {
        setSacredFireCount(stack, getSacredFireCount(stack) - 1);
    }
    public int getMode(ItemStack stack) {
        CompoundNBT nbt = stack.getTag();
        if (nbt != null && nbt.contains(MODE)) {
            return nbt.getInt(MODE);
        }
        return 0;
    }
    public static boolean isAbsorbingFire() {
        return absorbingFire.get();
    }
    @Override
    public void onCreated(ItemStack stack, World worldIn, PlayerEntity playerIn) {
        super.onCreated(stack, worldIn, playerIn);
        if (!stack.hasTag()) {
            setSacredFireCount(stack, 1);
            CompoundNBT nbt = stack.getOrCreateTag();
            nbt.putInt(MODE, 0);
        } else {
            CompoundNBT nbt = stack.getOrCreateTag();
            if (!nbt.contains(SACRED_FIRE_COUNT)) {
                nbt.putInt(SACRED_FIRE_COUNT, 1);
            }
            if (!nbt.contains(MODE)) {
                nbt.putInt(MODE, 0);
            }
        }
    }
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        int fireCount = getSacredFireCount(stack);
        TranslationTextComponent fireCountText = new TranslationTextComponent(
                "item.enhance.purple_gourd.tooltip.fire_count",
                fireCount
        );
        tooltip.add(fireCountText);
        int mode = getMode(stack);
        TranslationTextComponent modeText = new TranslationTextComponent(
                mode == 0 ? "item.enhance.purple_gourd.tooltip.mode_absorb" : "item.enhance.purple_gourd.tooltip.mode_release"
        );
        TranslationTextComponent modeLabel = new TranslationTextComponent(
                "item.enhance.purple_gourd.tooltip.mode",
                modeText
        );
        tooltip.add(modeLabel);
        tooltip.add(new TranslationTextComponent("item.enhance.purple_gourd.tooltip.use"));
        tooltip.add(new TranslationTextComponent("item.enhance.purple_gourd.tooltip.sneak_use"));
        tooltip.add(new TranslationTextComponent("item.enhance.purple_gourd.tooltip.require"));
    }
    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        int fireCount = getSacredFireCount(stack);
        if (fireCount > 0) {
            return new TranslationTextComponent(
                    "item.enhance.purple_gourd.display_name_with_count",
                    fireCount
            );
        }
        return new TranslationTextComponent("item.enhance.purple_gourd.display_name");
    }
}