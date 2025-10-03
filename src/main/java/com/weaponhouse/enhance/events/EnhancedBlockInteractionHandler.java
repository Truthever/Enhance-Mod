package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.client.gui.ReplaceScreen;
import com.weaponhouse.enhance.client.gui.SacrificeScreen;
import com.weaponhouse.enhance.client.gui.SpeedReducerScreen;
import com.weaponhouse.enhance.network.RequestBuffPacket;
import com.weaponhouse.enhance.network.RequestSessionPacket;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "enhance")
public class EnhancedBlockInteractionHandler {

    /**
     * 检查是否为增强模组的自定义方块
     */
    private static boolean isEnhanceBlock(Block block) {
        if (block == null) return false;
        ResourceLocation blockId = block.getRegistryName();
        return blockId != null && "enhance".equals(blockId.getNamespace()) &&
                (blockId.getPath().equals("sacrifice_block") ||
                        blockId.getPath().equals("replace_block") ||
                        blockId.getPath().equals("speed_reducer"));
    }

    /**
     * 主事件处理器 - 处理右键点击方块事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        PlayerEntity player = event.getPlayer();
        ItemStack heldItem = event.getItemStack();

        Block clickedBlock = world.getBlockState(pos).getBlock();

        if (isEnhanceBlock(clickedBlock)) {
            // 立即取消事件，阻止任何后续处理（包括方块放置）
            event.setCanceled(true);

            // 如果手持的是方块，需要额外处理
            if (heldItem.getItem() instanceof BlockItem) {
                // 设置手部动画，让交互看起来更自然
                if (world.isRemote) {
                    player.swingArm(event.getHand());
                }
            }

            // 根据方块类型处理交互逻辑
            if (world.isRemote) {
                handleClientInteraction(clickedBlock, pos, player);
            } else {
                handleServerInteraction(clickedBlock, pos, player);
            }
        }
    }

    /**
     * 客户端交互处理
     */
    private static void handleClientInteraction(Block block, BlockPos pos, PlayerEntity player) {
        ResourceLocation blockId = block.getRegistryName();
        if (blockId == null) return;

        String path = blockId.getPath();

        switch (path) {
            case "sacrifice_block":
                // 献祭方块逻辑
                Enhance.sendToServer(new RequestBuffPacket());
                Minecraft.getInstance().enqueue(() -> {
                    if (Minecraft.getInstance().currentScreen == null) {
                        Minecraft.getInstance().displayGuiScreen(new SacrificeScreen());
                    }
                });
                break;

            case "replace_block":
                // 替换方块逻辑
                Enhance.sendToServer(new RequestSessionPacket(pos, player.getUniqueID()));
                Minecraft.getInstance().enqueue(() -> {
                    if (Minecraft.getInstance().currentScreen == null) {
                        Minecraft.getInstance().displayGuiScreen(new ReplaceScreen(pos));
                    }
                });
                break;

            case "speed_reducer":
                // 减速器方块逻辑
                Minecraft.getInstance().enqueue(() -> {
                    if (Minecraft.getInstance().currentScreen == null) {
                        Minecraft.getInstance().displayGuiScreen(new SpeedReducerScreen());
                    }
                });
                break;
        }
    }

    /**
     * 服务端交互处理（可以添加验证逻辑）
     */
    private static void handleServerInteraction(Block block, BlockPos pos, PlayerEntity player) {
        // 服务端可以在这里添加验证逻辑
        // 例如：检查玩家权限、冷却时间、距离验证等
        ResourceLocation blockId = block.getRegistryName();
        if (blockId != null) {
            switch (blockId.getPath()) {
                case "sacrifice_block":
                    // 献祭方块的服务端逻辑
                    break;
                case "replace_block":
                    // 替换方块的服务端逻辑
                    break;
                case "speed_reducer":
                    // 减速器方块的服务端逻辑
                    break;
            }
        }
    }

    /**
     * 额外的防护：监听物品使用事件，防止在看向增强方块时使用物品
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        World world = event.getWorld();
        PlayerEntity player = event.getPlayer();
        ItemStack heldItem = event.getItemStack();

        // 只在客户端处理，避免服务端重复处理
        if (world.isRemote && heldItem.getItem() instanceof BlockItem) {
            // 使用射线检测判断玩家是否在看向增强方块
            BlockPos lookingAt = getTargetedBlockPos(player);
            if (lookingAt != null) {
                Block targetBlock = world.getBlockState(lookingAt).getBlock();
                if (isEnhanceBlock(targetBlock)) {
                    // 取消物品使用事件，防止方块放置
                    event.setCanceled(true);
                    player.swingArm(event.getHand());
                }
            }
        }
    }

    /**
     * 获取玩家视线方向上的方块位置
     */
    private static BlockPos getTargetedBlockPos(PlayerEntity player) {
        try {
            // 使用射线追踪获取玩家正在看的方块
            BlockRayTraceResult rayTrace = (BlockRayTraceResult) player.pick(5.0D, 1.0F, false);
            return rayTrace.getPos();
        } catch (Exception e) {
            // 如果射线追踪失败，返回null
            return null;
        }
    }

    /**
     * 工具方法：检查字符串是否为增强方块ID
     */
    private static boolean isEnhanceBlock(String blockId) {
        return blockId != null &&
                (blockId.equals("enhance:sacrifice_block") ||
                        blockId.equals("enhance:replace_block") ||
                        blockId.equals("enhance:speed_reducer"));
    }
}