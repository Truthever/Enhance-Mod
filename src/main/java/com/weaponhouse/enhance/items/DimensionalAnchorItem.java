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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import javax.annotation.Nullable;
import java.util.List;
@Mod.EventBusSubscriber
public class DimensionalAnchorItem extends Item {
    private static final String CHARGE_TAG = "dimensional_anchor_charge";
    private static final String INITIALIZED_TAG = "dimensional_anchor_initialized";
    private static final String LAST_SET_DIMENSION_TAG = "last_set_dimension";
    private static final String LAST_SET_POS_TAG = "last_set_pos";
    private static final int MAX_CHARGE = 6000;
    private static final int USE_CHARGE_COST = 1200;
    private static final int INITIAL_CHARGE = MAX_CHARGE;
    private static final int CHARGE_PER_POWDER = 200;
    public DimensionalAnchorItem(Properties properties) {
        super(properties
                .maxStackSize(1)
                .defaultMaxDamage(0)
        );
    }
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof PlayerEntity &&
                event.getSlot().getSlotType() == net.minecraft.inventory.EquipmentSlotType.Group.HAND) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            ItemStack newStack = event.getTo();
            if (newStack.getItem() instanceof DimensionalAnchorItem) {
                if (!isInitialized(newStack)) {
                    initAnchor(newStack);
                    if (!player.world.isRemote) {
                        player.sendMessage(
                                new TranslationTextComponent("item.enhance.dimensional_anchor.initialized")
                                        .mergeStyle(TextFormatting.GREEN),
                                player.getUniqueID()
                        );
                    }
                }
            }
        }
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack anchor = playerIn.getHeldItem(handIn);
        if (worldIn.isRemote) {
            return new ActionResult<>(ActionResultType.SUCCESS, anchor);
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) playerIn;
        if (!isInitialized(anchor)) {
            initAnchor(anchor);
        }
        if (getCharge(anchor) < USE_CHARGE_COST) {
            playerIn.sendMessage(
                    new TranslationTextComponent("item.enhance.dimensional_anchor.insufficient_charge")
                            .mergeStyle(TextFormatting.RED),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.FAIL, anchor);
        }
        try {
            consumeCharge(anchor, USE_CHARGE_COST);
            double x = playerIn.getPosX();
            double y = playerIn.getPosY();
            double z = playerIn.getPosZ();
            String dimension = ((ServerPlayerEntity) playerIn).getServerWorld().getDimensionKey().getLocation().toString();
            CompoundNBT playerData = serverPlayer.getPersistentData();
            CompoundNBT spawnData = new CompoundNBT();
            spawnData.putInt("SpawnX", (int) x);
            spawnData.putInt("SpawnY", (int) y);
            spawnData.putInt("SpawnZ", (int) z);
            spawnData.putString("SpawnDimension", dimension);
            playerData.put("PlayerSpawn", spawnData);
            updateLastSetPosition(anchor, dimension, x, y, z);
            playerIn.sendMessage(
                    new TranslationTextComponent("item.enhance.dimensional_anchor.success")
                            .mergeStyle(TextFormatting.GREEN)
                            .appendSibling(
                                    new TranslationTextComponent(" " + dimension)
                                            .mergeStyle(TextFormatting.YELLOW)
                            )
                            .appendSibling(
                                    new TranslationTextComponent(" (" + (int)x + ", " + (int)y + ", " + (int)z + ")")
                                            .mergeStyle(TextFormatting.GREEN)
                            ),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.SUCCESS, anchor);
        } catch (Exception e) {
            playerIn.sendMessage(
                    new TranslationTextComponent("item.enhance.dimensional_anchor.failure", e.getMessage())
                            .mergeStyle(TextFormatting.RED),
                    playerIn.getUniqueID()
            );
            return new ActionResult<>(ActionResultType.FAIL, anchor);
        }
    }
    public static void initAnchor(ItemStack stack) {
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putInt(CHARGE_TAG, INITIAL_CHARGE);
        nbt.putBoolean(INITIALIZED_TAG, true);
    }
    private void updateLastSetPosition(ItemStack stack, String dimension, double x, double y, double z) {
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString(LAST_SET_DIMENSION_TAG, dimension);
        CompoundNBT posData = new CompoundNBT();
        posData.putDouble("x", x);
        posData.putDouble("y", y);
        posData.putDouble("z", z);
        nbt.put(LAST_SET_POS_TAG, posData);
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("item.enhance.dimensional_anchor")
                .mergeStyle(TextFormatting.AQUA));
        tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.description")
                .mergeStyle(TextFormatting.GRAY));
        if (isInitialized(stack)) {
            int currentCharge = getCharge(stack);
            int usesRemaining = currentCharge / USE_CHARGE_COST;
            TextFormatting chargeColor = getChargeColor(currentCharge);
            tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.charge",
                    currentCharge, MAX_CHARGE)
                    .mergeStyle(chargeColor));
            tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.uses_remaining", usesRemaining)
                    .mergeStyle(TextFormatting.GRAY));
            tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.charge_cost", USE_CHARGE_COST)
                    .mergeStyle(TextFormatting.GRAY));
            if (hasLastSetPosition(stack)) {
                String lastDimension = getLastSetDimension(stack);
                CompoundNBT posData = null;
                if (stack.getTag() != null) {
                    posData = stack.getTag().getCompound(LAST_SET_POS_TAG);
                }
                int x = 0;
                if (posData != null) {
                    x = (int) posData.getDouble("x");
                }
                int y = 0;
                if (posData != null) {
                    y = (int) posData.getDouble("y");
                }
                int z = 0;
                if (posData != null) {
                    z = (int) posData.getDouble("z");
                }
                tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.last_set")
                        .mergeStyle(TextFormatting.GOLD));
                tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.position",
                        lastDimension, x, y, z)
                        .mergeStyle(TextFormatting.GRAY));
            } else {
                tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.not_set")
                        .mergeStyle(TextFormatting.GRAY));
            }
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }

        tooltip.add(new TranslationTextComponent("tooltip.enhance.dimensional_anchor.anvil_repair")
                .mergeStyle(TextFormatting.DARK_GREEN));
    }
    private TextFormatting getChargeColor(int currentCharge) {
        if (currentCharge >= USE_CHARGE_COST * 3) {
            return TextFormatting.GREEN;
        } else if (currentCharge >= USE_CHARGE_COST) {
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
        int currentCharge = nbt.getInt(CHARGE_TAG);
        int newCharge = Math.min(MAX_CHARGE, currentCharge + amount);
        nbt.putInt(CHARGE_TAG, newCharge);
    }
    public static boolean hasLastSetPosition(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().contains(LAST_SET_DIMENSION_TAG);
        }
        return false;
    }
    public static String getLastSetDimension(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return "";
        if (stack.getTag() != null) {
            return stack.getTag().getString(LAST_SET_DIMENSION_TAG);
        }
        return "";
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