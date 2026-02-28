package com.weaponhouse.enhance.items.armor;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.enchant.RadiationEnchantment;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
public class EnhanceChestplate extends ArmorItem {
    public static final String DEFENSE_REDUCE_LEVEL_TAG = "enhance_chestplate_reduce_level";
    private static final String INITIALIZED_TAG = "enhance_chestplate_initialized";
    private static final String CHARGE_TAG = "enhance_chestplate_charge";
    private static final int MAX_CHARGE = 1500;
    private static final int INITIAL_CHARGE = MAX_CHARGE / 2;
    private static final int CHARGE_COST_PER_USE = 20;
    private static final int CHARGE_PER_POWDER = 200;
    public EnhanceChestplate(IArmorMaterial materialIn, EquipmentSlotType slot, Properties builder) {
        super(materialIn, slot, builder);
    }
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || !(entityIn instanceof PlayerEntity) || !isSelected
                || isInitialized(stack) || ((PlayerEntity) entityIn).getHeldItemMainhand() != stack) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;
        CompoundNBT nbt = stack.getOrCreateTag();
        int playerEnhanceLevel = getPlayerEnhanceLevel(player);
        int reduceLevel = Math.max(1, Math.min(playerEnhanceLevel, 6));
        nbt.putInt(DEFENSE_REDUCE_LEVEL_TAG, reduceLevel);
        nbt.putBoolean(INITIALIZED_TAG, true);
        nbt.putInt(CHARGE_TAG, INITIAL_CHARGE);
        stack.setTag(nbt);
        float triggerChance = reduceLevel * 0.1F;
        float reduceRatio = reduceLevel * 0.1F;
        player.sendMessage(new TranslationTextComponent(
                "message.enhance_chestplate.init",
                reduceLevel,
                (int) (triggerChance * 100),
                (int) (reduceRatio * 100)
        ).mergeStyle(TextFormatting.GOLD), player.getUniqueID());
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("tooltip.enhance_chestplate.base_info").mergeStyle(TextFormatting.BLUE));
        if (isInitialized(stack)) {
            int level = getDefenseReduceLevel(stack);
            int currentCharge = getCharge(stack);
            float triggerChance = level * 0.1F;
            float reduceRatio = level * 0.1F;
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_chestplate.reduce_effect",
                            level,
                            (int) (triggerChance * 100),
                            (int) (reduceRatio * 100)
                    ).mergeStyle(TextFormatting.GREEN)
            );
            TextFormatting chargeColor = currentCharge > MAX_CHARGE * 0.5 ? TextFormatting.GREEN :
                    currentCharge > MAX_CHARGE * 0.2 ? TextFormatting.YELLOW : TextFormatting.RED;
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_chestplate.charge",
                            currentCharge, MAX_CHARGE
                    ).mergeStyle(chargeColor)
            );
            tooltip.add(new TranslationTextComponent("tooltip.enhance_chestplate.charge_cost", CHARGE_COST_PER_USE)
                    .mergeStyle(TextFormatting.GRAY));

            tooltip.add(new TranslationTextComponent("tooltip.enhance_chestplate.anvil_repair")
                    .mergeStyle(TextFormatting.DARK_GREEN));
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance_chestplate.uninitialized").mergeStyle(TextFormatting.GRAY));
        }
    }
    private int getPlayerEnhanceLevel(PlayerEntity player) {
        CompoundNBT playerNBT = player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
        return playerNBT.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
    }
    public static int getDefenseReduceLevel(ItemStack stack) {
        if (isInitialized(stack) && stack.hasTag()) {
            if (stack.getTag() != null) {
                return stack.getTag().getInt(DEFENSE_REDUCE_LEVEL_TAG);
            }
        }
        return 0;
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
        int radiationLevel = RadiationEnchantment.getRadiationLevel(stack);
        if (RadiationEnchantment.shouldConserveCharge(stack, radiationLevel)) {
            return true;
        }
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
        nbt.putInt(CHARGE_TAG, newCharge);
    }
    public static int getMaxCharge() {
        return MAX_CHARGE;
    }
    public static int getChargePerPowder() {
        return CHARGE_PER_POWDER;
    }
    public static boolean isInitialized(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
        }
        return false;
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_ARMOR_MATERIAL.getRepairMaterial().test(repair);
    }
}