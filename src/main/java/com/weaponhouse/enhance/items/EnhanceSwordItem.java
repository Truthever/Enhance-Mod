package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.enchant.RadiationEnchantment;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
public class EnhanceSwordItem extends SwordItem {
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("fa5c5c33-8a7a-4b8a-9b2a-2b3a9b4c5d6e");
    public static final String ENHANCE_STRIKE_TAG = "enhance_strike";
    private static final String INITIALIZED_TAG = "enhance_sword_initialized";
    private static final String FIXED_ATTACK_SPEED_TAG = "fixed_attack_speed_bonus";
    private static final String CHARGE_TAG = "enhance_sword_charge";
    private static final int MAX_CHARGE = 1000;
    private static final int INITIAL_CHARGE = MAX_CHARGE / 2;
    private static final int STRIKE_CHARGE_COST = 12;
    private static final int CHARGE_PER_POWDER = 200;
    private static final int MAX_LEVEL = 6;
    public EnhanceSwordItem(IItemTier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder
                .defaultMaxDamage(1500));
    }
    @Override
    public int getMaxDamage(ItemStack stack) {
        return 1500;
    }
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || !isSelected || !(entityIn instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;
        if (player.getHeldItemMainhand() != stack) {
            return;
        }
        CompoundNBT swordNBT = stack.getOrCreateTag();
        if (swordNBT.getBoolean(INITIALIZED_TAG)) {
            applyFixedAttackSpeed(stack, player, swordNBT);
            return;
        }
        initializeSword(stack, player, swordNBT);
    }
    public static boolean hasEnoughCharge(ItemStack stack, int amount) {
        if (stack == null || !stack.hasTag()) return false;
        int radiationLevel = RadiationEnchantment.getRadiationLevel(stack);
        if (RadiationEnchantment.shouldConserveCharge(stack, radiationLevel)) {
            return true;
        }
        int currentCharge = 0;
        if (stack.getTag() != null) {
            currentCharge = stack.getTag().getInt(CHARGE_TAG);
        }
        return currentCharge >= amount;
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
    private void initializeSword(ItemStack stack, PlayerEntity player, CompoundNBT swordNBT) {
        int playerEnhanceLevel = getPlayerEnhanceLevel(player);
        if (playerEnhanceLevel <= 0) {
            return;
        }
        int finalLevel = Math.min(playerEnhanceLevel, MAX_LEVEL);
        swordNBT.putInt(ENHANCE_STRIKE_TAG, finalLevel);
        float fixedAttackSpeed = calculateAttackSpeedBonus(finalLevel);
        swordNBT.putFloat(FIXED_ATTACK_SPEED_TAG, fixedAttackSpeed);
        swordNBT.putInt(CHARGE_TAG, INITIAL_CHARGE);
        swordNBT.putBoolean(INITIALIZED_TAG, true);
        stack.setTag(swordNBT);
        applyFixedAttackSpeed(stack, player, swordNBT);
    }
    private void applyFixedAttackSpeed(ItemStack stack, PlayerEntity player, CompoundNBT swordNBT) {
        float fixedSpeed = swordNBT.getFloat(FIXED_ATTACK_SPEED_TAG);
        AttributeModifier existingModifier = Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_SPEED)).getModifier(ATTACK_SPEED_MODIFIER_ID);
        if (existingModifier != null) {
            Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_SPEED)).removeModifier(existingModifier);
        }
        AttributeModifier speedModifier = new AttributeModifier(
                ATTACK_SPEED_MODIFIER_ID,
                "Enhance sword fixed attack speed",
                fixedSpeed,
                AttributeModifier.Operation.ADDITION
        );
        Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_SPEED)).applyPersistentModifier(speedModifier);
    }
    private float calculateAttackSpeedBonus(int level) {
        switch (level) {
            case 1: return 0.5F;
            case 2: return 1.0F;
            case 3: return 1.5F;
            case 4: return 2.0F;
            case 5: return 2.5F;
            case 6: return 10.0F;
            default: return 0.0F;
        }
    }
    private int getPlayerEnhanceLevel(PlayerEntity player) {
        if (player == null) return 0;
        CompoundNBT playerBuffNBT = player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
        return playerBuffNBT.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        CompoundNBT swordNBT = stack.getOrCreateTag();
        if (swordNBT.getBoolean(INITIALIZED_TAG)) {
            int strikeLevel = swordNBT.getInt(ENHANCE_STRIKE_TAG);
            float fixedSpeed = swordNBT.getFloat(FIXED_ATTACK_SPEED_TAG);
            int currentCharge = getCharge(stack);
            int triggerChance = 3 * strikeLevel;
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_sword.enhance_strike_fixed",
                    strikeLevel, triggerChance
            ).mergeStyle(TextFormatting.LIGHT_PURPLE));
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_sword.fixed_attack_speed",
                    fixedSpeed
            ).mergeStyle(TextFormatting.GREEN));
            TextFormatting chargeColor = currentCharge > MAX_CHARGE * 0.5 ? TextFormatting.GREEN :
                    currentCharge > MAX_CHARGE * 0.2 ? TextFormatting.YELLOW : TextFormatting.RED;
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_sword.charge",
                    currentCharge, MAX_CHARGE
            ).mergeStyle(chargeColor));
            tooltip.add(new TranslationTextComponent("tooltip.enhance_sword.strike_charge_cost", STRIKE_CHARGE_COST)
                    .mergeStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance_sword.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }
        tooltip.add(new TranslationTextComponent("tooltip.enhance_sword.anvil_repair")
                .mergeStyle(TextFormatting.DARK_GREEN));
    }
    public static int getCharge(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return 0;
        if (stack.getTag() != null) {
            return stack.getTag().getInt(CHARGE_TAG);
        }
        return 0;
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
    public static int getStrikeChargeCost() {
        return STRIKE_CHARGE_COST;
    }
    public static boolean isInitialized(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
        }
        return false;
    }
    public static int getEnhanceStrikeLevel(ItemStack stack) {
        if (stack != null && stack.hasTag()) {
            CompoundNBT nbt = stack.getTag();
            if (nbt != null && nbt.getBoolean(INITIALIZED_TAG)) {
                return nbt.getInt(ENHANCE_STRIKE_TAG);
            }
        }
        return 0;
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_TIER.getRepairMaterial().test(repair);
    }
}