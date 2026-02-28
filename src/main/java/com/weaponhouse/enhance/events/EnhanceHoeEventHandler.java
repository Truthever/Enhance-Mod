package com.weaponhouse.enhance.events;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;
import java.util.Random;
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnhanceHoeEventHandler {
    private static final Random RANDOM = new Random();
    private static final int MAX_ENHANCE_LEVEL = 6;
    private static final String WHEAT_BLOCK_REGISTRY_NAME = "minecraft:wheat";
    private static final Item WHEAT_ITEM = net.minecraft.item.Items.WHEAT;
    private static final Item WHEAT_SEED_ITEM = net.minecraft.item.Items.WHEAT_SEEDS;
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        PlayerEntity player = event.getPlayer();
        World world = (World) event.getWorld();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();
        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.getItem() != RegistryHandler.ENHANCE_HOE.get()) {
            return;
        }
        if (!(block instanceof CropsBlock) || !isCropMature((CropsBlock) block, state)) {
            return;
        }
        int playerEnhanceLevel = getPlayerEnhanceLevel(player);
        int effectiveLevel = Math.min(playerEnhanceLevel, MAX_ENHANCE_LEVEL);
        event.setCanceled(true);
        world.destroyBlock(pos, false);
        String blockRegistryName = Objects.requireNonNull(block.getRegistryName()).toString();
        if (WHEAT_BLOCK_REGISTRY_NAME.equals(blockRegistryName)) {
            handleWheatDrop(world, pos, effectiveLevel);
        } else {
            handleNormalCropDrop(world, pos, block.asItem(), effectiveLevel);
        }
        heldItem.damageItem(1, player, p -> p.sendBreakAnimation(player.getActiveHand()));
        sendYieldMessage(player, effectiveLevel);
    }
    private static boolean isCropMature(CropsBlock cropBlock, BlockState state) {
        int maxAge = cropBlock.getMaxAge();
        int currentAge = state.get(cropBlock.getAgeProperty());
        return currentAge >= maxAge;
    }
    private static int getPlayerEnhanceLevel(PlayerEntity player) {
        return player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG)
                .getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
    }
    private static void handleWheatDrop(World world, BlockPos pos, int bonusLevel) {
        int wheatCount = 1 + bonusLevel;
        Block.spawnAsEntity(world, pos, new ItemStack(WHEAT_ITEM, wheatCount));
        int baseSeedCount = RANDOM.nextInt(4);
        int totalSeedCount = baseSeedCount + bonusLevel;
        if (totalSeedCount > 0) {
            Block.spawnAsEntity(world, pos, new ItemStack(WHEAT_SEED_ITEM, totalSeedCount));
        }
    }
    private static void handleNormalCropDrop(World world, BlockPos pos, Item cropItem, int bonusLevel) {
        int cropCount = 1 + bonusLevel;
        Block.spawnAsEntity(world, pos, new ItemStack(cropItem, cropCount));
    }
    private static void sendYieldMessage(PlayerEntity player, int effectiveLevel) {
        if (effectiveLevel > 0) {
            player.sendMessage(new TranslationTextComponent(
                    "message.enhance_hoe.yield_bonus", effectiveLevel, 1 + effectiveLevel
            ).mergeStyle(TextFormatting.GREEN), player.getUniqueID());
        }
    }
}