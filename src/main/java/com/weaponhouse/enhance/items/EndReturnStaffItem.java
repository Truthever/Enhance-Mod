package com.weaponhouse.enhance.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import java.util.Collection;
import java.util.List;
public class EndReturnStaffItem extends Item {
    private static final String END_DIMENSION = "minecraft:the_end";
    private static final String OVERWORLD_DIMENSION = "minecraft:overworld";
    public EndReturnStaffItem(Properties properties) {
        super(properties
                .maxStackSize(1)
                .maxDamage(9)
        );
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack staff = playerIn.getHeldItem(handIn);
        if (worldIn.isRemote) {
            return new ActionResult<>(ActionResultType.SUCCESS, staff);
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) playerIn;
        String currentDimension = serverPlayer.getServerWorld().getDimensionKey().getLocation().toString();
        if (!currentDimension.equals(END_DIMENSION)) {
            playerIn.sendMessage(
                    new TranslationTextComponent("item.end_return_staff.cannot_use")
                            .mergeStyle(TextFormatting.RED),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.FAIL, staff);
        }
        try {
            ServerWorld overworld = getOverworld();
            if (overworld == null) {
                playerIn.sendMessage(
                        new TranslationTextComponent("item.end_return_staff.overworld_not_found")
                                .mergeStyle(TextFormatting.RED),
                        playerIn.getUniqueID()
                );
                return new ActionResult<>(ActionResultType.FAIL, staff);
            }
            serverPlayer.teleport(
                    overworld,
                    overworld.getSpawnPoint().getX(),
                    overworld.getSpawnPoint().getY(),
                    overworld.getSpawnPoint().getZ(),
                    serverPlayer.rotationYaw,
                    serverPlayer.rotationPitch
            );
            staff.damageItem(1, playerIn, p -> p.sendBreakAnimation(handIn));
            playerIn.sendMessage(
                    new TranslationTextComponent("item.end_return_staff.success")
                            .mergeStyle(TextFormatting.GREEN),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.SUCCESS, staff);
        } catch (Exception e) {
            playerIn.sendMessage(
                    new TranslationTextComponent("item.end_return_staff.failure", e.getMessage())
                            .mergeStyle(TextFormatting.RED),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.FAIL, staff);
        }
    }
    private ServerWorld getOverworld() {
        Collection<ServerWorld> worlds = (Collection<ServerWorld>) ServerLifecycleHooks.getCurrentServer().getWorlds();
        for (ServerWorld world : worlds) {
            if (world.getDimensionKey().getLocation().toString().equals(OVERWORLD_DIMENSION)) {
                return world;
            }
        }
        return null;
    }
    @Override
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("item.end_return_staff.name")
                .mergeStyle(TextFormatting.BLUE));
        tooltip.add(new TranslationTextComponent("item.end_return_staff.description")
                .mergeStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.end_return_staff.uses_remaining",
                (stack.getMaxDamage() - stack.getDamage() + 1))
                .mergeStyle(TextFormatting.GRAY));
    }
}
