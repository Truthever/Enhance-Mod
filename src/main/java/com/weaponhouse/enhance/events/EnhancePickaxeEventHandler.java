package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.items.EnhancePickaxeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance")
public class EnhancePickaxeEventHandler {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = (World) event.getWorld();
        PlayerEntity player = event.getPlayer();
        if (world.isRemote || player == null) {
            return;
        }
        ItemStack heldItem = player.getHeldItemMainhand();
        if (!(heldItem.getItem() instanceof EnhancePickaxeItem)) {
            return;
        }
        if (!EnhancePickaxeItem.isAreaMiningEnabled(heldItem)) {
            return;
        }
        BlockPos centerPos = event.getPos();
        BlockState centerState = world.getBlockState(centerPos);
        if (!heldItem.canHarvestBlock(centerState)) {
            return;
        }
        int totalBroken = 0;
        int range = EnhancePickaxeItem.MINING_RANGE;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos targetPos = centerPos.add(x, y, z);
                    BlockState targetState = world.getBlockState(targetPos);
                    if (heldItem.canHarvestBlock(targetState)
                            && !targetState.isAir(world, targetPos)
                            && targetState.getBlockHardness(world, targetPos) >= 0) {
                        world.destroyBlock(targetPos, true, player);
                        totalBroken++;
                    }
                }
            }
        }
        if (totalBroken > 0) {
            int damageToTake = (totalBroken + 2) / 3;
            heldItem.damageItem(damageToTake, player, (p) -> {
                p.sendBreakAnimation(player.getActiveHand());
            });
        }
    }
}