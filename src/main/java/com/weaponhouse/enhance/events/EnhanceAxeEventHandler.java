package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.items.EnhanceAxeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
@Mod.EventBusSubscriber(modid = "enhance")
public class EnhanceAxeEventHandler {
    private static final int HORIZONTAL_RANGE = 4; // 横向最大距离
    private static final int VERTICAL_RANGE = 30;  // 纵向总范围
    @SubscribeEvent
    public static void onAxeBlockBreak(BlockEvent.BreakEvent event) {
        World world = (World) event.getWorld();
        PlayerEntity player = event.getPlayer();
        if (world.isRemote || player == null) return;
        ItemStack heldAxe = player.getHeldItemMainhand();
        if (!(heldAxe.getItem() instanceof EnhanceAxeItem)
                || !EnhanceAxeItem.isInitialized(heldAxe)
                || !EnhanceAxeItem.isChainCutEnabled(heldAxe)) {
            return;
        }
        BlockPos breakPos = event.getPos();
        BlockState breakState = world.getBlockState(breakPos);
        Material breakMaterial = breakState.getMaterial();
        if ((breakMaterial != Material.WOOD && breakMaterial != Material.LEAVES)
                || breakState.getBlockHardness(world, breakPos) < 0) {
            return;
        }
        Block targetWood = breakMaterial == Material.WOOD ? breakState.getBlock() : null;
        boolean isLeafMode = breakMaterial == Material.LEAVES;
        boolean chainLeaves = EnhanceAxeItem.isLeafChainEnabled(heldAxe);
        List<BlockPos> allBlockPos = new ArrayList<>();
        Queue<BlockPos> queue = new LinkedList<>();
        List<BlockPos> processed = new ArrayList<>();
        queue.add(breakPos);
        processed.add(breakPos);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos nextPos = current.add(dx, dy, dz);
                        if (processed.contains(nextPos) || !isWithinRange(breakPos, nextPos)) {
                            continue;
                        }
                        BlockState nextState = world.getBlockState(nextPos);
                        Material nextMaterial = nextState.getMaterial();
                        boolean isTarget = false;
                        if (nextMaterial == Material.WOOD) {
                            isTarget = targetWood != null && nextState.getBlock() == targetWood;
                        } else if (chainLeaves && nextMaterial == Material.LEAVES) {
                            isTarget = true;
                        }
                        if (isTarget) {
                            allBlockPos.add(nextPos);
                            processed.add(nextPos);
                            queue.add(nextPos);
                        }
                    }
                }
            }
        }
        int totalChained = 0;
        for (BlockPos pos : allBlockPos) {
            if (world.destroyBlock(pos, true, player)) {
                totalChained++;
            }
        }
        if (totalChained > 0) {
            int damage = 1;
            for (BlockPos pos : allBlockPos) {
                Material mat = world.getBlockState(pos).getMaterial();
                damage += mat == Material.WOOD ? 1 : 0.5;
            }
            heldAxe.damageItem((int) Math.ceil(damage), player, p -> p.sendBreakAnimation(player.getActiveHand()));
        }
    }
    private static boolean isWithinRange(BlockPos center, BlockPos target) {
        int dx = Math.abs(target.getX() - center.getX());
        int dy = Math.abs(target.getY() - center.getY());
        int dz = Math.abs(target.getZ() - center.getZ());
        return dx <= HORIZONTAL_RANGE
                && dy <= VERTICAL_RANGE
                && dz <= HORIZONTAL_RANGE;
    }
}
