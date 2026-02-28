package com.weaponhouse.enhance.items;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
public class EndReturnStaffItem extends Item {
    private static final String OVERWORLD_DIMENSION = "minecraft:overworld";
    private static final String CHARGE_TAG = "end_return_staff_charge";
    private static final String INITIALIZED_TAG = "end_return_staff_initialized";
    private static final int MAX_CHARGE = 4000;
    private static final int TELEPORT_CHARGE_COST = 400;
    private static final int INITIAL_CHARGE = MAX_CHARGE;
    private static final int CHARGE_PER_POWDER = 200;
    public EndReturnStaffItem(Properties properties) {
        super(properties
                .maxStackSize(1)
                .defaultMaxDamage(0)
        );
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack staff = playerIn.getHeldItem(handIn);
        if (worldIn.isRemote) {
            return new ActionResult<>(ActionResultType.SUCCESS, staff);
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) playerIn;
        if (!isInitialized(staff)) {
            initStaff(staff);
        }
        if (getCharge(staff) < TELEPORT_CHARGE_COST) {
            playerIn.sendMessage(
                    new TranslationTextComponent("item.enhance.end_return_staff.insufficient_charge")
                            .mergeStyle(TextFormatting.RED),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.FAIL, staff);
        }
        try {
            ServerWorld overworld = getOverworld();
            if (overworld == null) {
                playerIn.sendMessage(
                        new TranslationTextComponent("item.enhance.end_return_staff.overworld_not_found")
                                .mergeStyle(TextFormatting.RED),
                        playerIn.getUniqueID()
                );
                return new ActionResult<>(ActionResultType.FAIL, staff);
            }
            String currentDimension = serverPlayer.getServerWorld().getDimensionKey().getLocation().toString();
            if (currentDimension.equals(OVERWORLD_DIMENSION)) {
                playerIn.sendMessage(
                        new TranslationTextComponent("item.enhance.end_return_staff.already_in_overworld")
                                .mergeStyle(TextFormatting.YELLOW),
                        playerIn.getUniqueID()
                );
                return new ActionResult<>(ActionResultType.FAIL, staff);
            }
            consumeCharge(staff, TELEPORT_CHARGE_COST);
            serverPlayer.teleport(
                    overworld,
                    overworld.getSpawnPoint().getX(),
                    overworld.getSpawnPoint().getY() + 1,
                    overworld.getSpawnPoint().getZ(),
                    serverPlayer.rotationYaw,
                    serverPlayer.rotationPitch
            );
            playerIn.sendMessage(
                    new TranslationTextComponent("item.enhance.end_return_staff.success")
                            .mergeStyle(TextFormatting.GREEN),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.SUCCESS, staff);
        } catch (Exception e) {
            playerIn.sendMessage(
                    new TranslationTextComponent("item.enhance.end_return_staff.failure", e.getMessage())
                            .mergeStyle(TextFormatting.RED),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.FAIL, staff);
        }
    }
    private void initStaff(ItemStack stack) {
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putInt(CHARGE_TAG, INITIAL_CHARGE);
        nbt.putBoolean(INITIALIZED_TAG, true);
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
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("item.enhance.end_return_staff")
                .mergeStyle(TextFormatting.BLUE));
        tooltip.add(new TranslationTextComponent("tooltip.enhance.end_return_staff.description")
                .mergeStyle(TextFormatting.GRAY));
        if (isInitialized(stack)) {
            int currentCharge = getCharge(stack);
            int usesRemaining = currentCharge / TELEPORT_CHARGE_COST;
            TextFormatting chargeColor = getChargeColor(currentCharge);
            tooltip.add(new TranslationTextComponent("tooltip.enhance.end_return_staff.charge",
                    currentCharge, MAX_CHARGE)
                    .mergeStyle(chargeColor));
            tooltip.add(new TranslationTextComponent("tooltip.enhance.end_return_staff.uses_remaining", usesRemaining)
                    .mergeStyle(TextFormatting.GRAY));
            tooltip.add(new TranslationTextComponent("tooltip.enhance.end_return_staff.charge_cost", TELEPORT_CHARGE_COST)
                    .mergeStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance.end_return_staff.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }
        tooltip.add(new TranslationTextComponent("tooltip.enhance.end_return_staff.works_in_any_dimension")
                .mergeStyle(TextFormatting.GOLD));
        tooltip.add(new TranslationTextComponent("tooltip.enhance.end_return_staff.anvil_repair")
                .mergeStyle(TextFormatting.DARK_GREEN));
    }
    private TextFormatting getChargeColor(int currentCharge) {
        if (currentCharge >= TELEPORT_CHARGE_COST * 3) {
            return TextFormatting.GREEN;
        } else if (currentCharge >= TELEPORT_CHARGE_COST) {
            return TextFormatting.YELLOW;
        } else {
            return TextFormatting.RED;
        }
    }
    public static boolean isInitialized(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
        }
        return false;
    }
    public static int getCharge(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return 0;
        if (stack.getTag() != null) {
            return stack.getTag().getInt(CHARGE_TAG);
        }
        return 0;
    }

    public static boolean consumeCharge(ItemStack stack, int amount) {
        if (stack == null || !stack.hasTag()) return false;
        CompoundNBT nbt = stack.getTag();
        int currentCharge = 0;
        if (nbt != null) {
            currentCharge = nbt.getInt(CHARGE_TAG);
        }
        if (currentCharge < amount) {
            return false;
        }
        int newCharge = Math.max(0, currentCharge - amount);
        if (nbt != null) {
            nbt.putInt(CHARGE_TAG, newCharge);
        }
        return true;
    }
    public static void addCharge(ItemStack stack, int amount) {
        if (stack == null || !stack.hasTag()) return;
        CompoundNBT nbt = stack.getTag();
        int currentCharge = 0;
        if (nbt != null) {
            currentCharge = nbt.getInt(CHARGE_TAG);
        }
        int newCharge = Math.min(MAX_CHARGE, currentCharge + amount);
        if (nbt != null) {
            nbt.putInt(CHARGE_TAG, newCharge);
        }
    }
    public static int getMaxCharge() {
        return MAX_CHARGE;
    }
    public static int getChargePerPowder() {
        return CHARGE_PER_POWDER;
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == com.weaponhouse.enhance.util.RegistryHandler.ENHANCE_DUST.get();
    }
}