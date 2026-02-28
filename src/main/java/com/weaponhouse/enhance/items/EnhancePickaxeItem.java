package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.enchant.RadiationEnchantment;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
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
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;
public class EnhancePickaxeItem extends PickaxeItem {
    private static final Random RANDOM = new Random();
    private static final String INITIALIZED_TAG = "hammer_initialized";
    public static final String HAMMER_STRIKE_TAG = "hammer_strike_level";
    public static final String AREA_MINING_TAG = "enhance_pickaxe_area_enabled";
    private static final String CHARGE_TAG = "enhance_pickaxe_charge";
    private static final int MAX_CHARGE = 1200;
    private static final int INITIAL_CHARGE = MAX_CHARGE / 2;
    private static final int AREA_MINING_CHARGE_COST = 8;
    private static final int CHARGE_PER_POWDER = 200;
    private static final float BASE_ATTACK_DAMAGE = 4.0F;
    public static final int MINING_RANGE = 1;
    public static final UUID PICKAXE_ATTACK_BOOST_UUID = UUID.fromString("f8a9b0c1-d2e3-4f56-7a8b-9c0d1e2f3a4b");
    public EnhancePickaxeItem() {
        super(
                RegistryHandler.ENHANCE_PICKAXE_TIER,
                1,
                -2.8F,
                new Properties()
                        .group(Enhance.TAB)
                        .addToolType(ToolType.PICKAXE, 6)
                        .defaultMaxDamage(1800)
        );
    }
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || !(entityIn instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;
        boolean isMainHand = player.getHeldItemMainhand() == stack && isSelected;
        if (!isInitialized(stack) && isMainHand) {
            initPickaxeNBT(stack, player);
        }
        ModifiableAttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr == null) return;
        AttributeModifier existing = attackAttr.getModifier(PICKAXE_ATTACK_BOOST_UUID);
        if (existing != null) {
            attackAttr.removeModifier(existing);
        }
        if (isMainHand && isInitialized(stack)) {
            int strikeLevel = getHammerStrikeLevel(stack);
            double attackBoost = strikeLevel * 2.0;
            AttributeModifier modifier = new AttributeModifier(
                    PICKAXE_ATTACK_BOOST_UUID,
                    "EnhancePickaxe_AttackBoost",
                    attackBoost,
                    AttributeModifier.Operation.ADDITION
            );
            attackAttr.applyPersistentModifier(modifier);
        }
    }
    private void initPickaxeNBT(ItemStack stack, PlayerEntity player) {
        CompoundNBT nbt = stack.getOrCreateTag();
        int playerEnhanceLevel = getPlayerEnhanceLevel(player);
        int minLevel = 1;
        int maxLevel = 3 + playerEnhanceLevel;
        maxLevel = Math.min(maxLevel, 9);
        int hammerStrikeLevel = RANDOM.nextInt(maxLevel - minLevel + 1) + minLevel;
        nbt.putInt(HAMMER_STRIKE_TAG, hammerStrikeLevel);
        nbt.putInt(CHARGE_TAG, INITIAL_CHARGE);
        nbt.putBoolean(INITIALIZED_TAG, true);
        stack.setTag(nbt);
        player.sendMessage(new TranslationTextComponent(
                "message.enhance_pickaxe.initialized",
                hammerStrikeLevel,
                hammerStrikeLevel * 2,
                INITIAL_CHARGE,
                MAX_CHARGE
        ).mergeStyle(TextFormatting.GOLD), player.getUniqueID());
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack pickaxe = playerIn.getHeldItem(handIn);
        if (!worldIn.isRemote && isInitialized(pickaxe)) {
            CompoundNBT nbt = pickaxe.getOrCreateTag();
            boolean newState = !nbt.getBoolean(AREA_MINING_TAG);
            nbt.putBoolean(AREA_MINING_TAG, newState);
            playerIn.sendMessage(new TranslationTextComponent(
                    newState ? "message.enhance_pickaxe.enabled" : "message.enhance_pickaxe.disabled"
            ).mergeStyle(newState ? TextFormatting.GREEN : TextFormatting.RED), playerIn.getUniqueID());
        }
        return new ActionResult<>(ActionResultType.SUCCESS, pickaxe);
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("tooltip.enhance_pickaxe.base_info")
                .mergeStyle(TextFormatting.BLUE));
        if (isInitialized(stack)) {
            int strikeLevel = getHammerStrikeLevel(stack);
            int attackBoost = strikeLevel * 2;
            int currentCharge = getCharge(stack);
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_pickaxe.attack_boost",
                    attackBoost
            ).mergeStyle(TextFormatting.DARK_GREEN));
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_pickaxe.hammer_strike",
                    strikeLevel, attackBoost
            ).mergeStyle(TextFormatting.LIGHT_PURPLE));
            TextFormatting chargeColor = currentCharge > MAX_CHARGE * 0.5 ? TextFormatting.GREEN :
                    currentCharge > MAX_CHARGE * 0.2 ? TextFormatting.YELLOW : TextFormatting.RED;

            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_pickaxe.charge",
                    currentCharge, MAX_CHARGE
            ).mergeStyle(chargeColor));
            tooltip.add(new TranslationTextComponent("tooltip.enhance_pickaxe.area_mining_cost", AREA_MINING_CHARGE_COST)
                    .mergeStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_pickaxe.base_damage",
                    BASE_ATTACK_DAMAGE
            ).mergeStyle(TextFormatting.DARK_GREEN));
            tooltip.add(new TranslationTextComponent("tooltip.enhance_pickaxe.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }
        boolean isMiningEnabled = isAreaMiningEnabled(stack);
        tooltip.add(new TranslationTextComponent(
                isMiningEnabled ? "tooltip.enhance_pickaxe.state_enabled" : "tooltip.enhance_pickaxe.state_disabled"
        ).mergeStyle(isMiningEnabled ? TextFormatting.GREEN : TextFormatting.RED));

        tooltip.add(new TranslationTextComponent("tooltip.enhance_pickaxe.toggle_hint")
                .mergeStyle(TextFormatting.GRAY));

        tooltip.add(new TranslationTextComponent("tooltip.enhance_pickaxe.anvil_repair")
                .mergeStyle(TextFormatting.DARK_GREEN));
    }
    private int getPlayerEnhanceLevel(PlayerEntity player) {
        if (player == null) return 0;
        CompoundNBT playerBuffNBT = player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
        return playerBuffNBT.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
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
    public static int getAreaMiningChargeCost() {
        return AREA_MINING_CHARGE_COST;
    }
    public static boolean isInitialized(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
        }
        return false;
    }
    public static int getHammerStrikeLevel(ItemStack stack) {
        if (!isInitialized(stack)) return 0;
        if (stack.getTag() != null) {
            return stack.getTag().getInt(HAMMER_STRIKE_TAG);
        }
        return 0;
    }
    public static boolean isAreaMiningEnabled(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(AREA_MINING_TAG);
        }
        return false;
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_PICKAXE_TIER.getRepairMaterial().test(repair);
    }
    @Override
    public float getAttackDamage() {
        return BASE_ATTACK_DAMAGE;
    }
}