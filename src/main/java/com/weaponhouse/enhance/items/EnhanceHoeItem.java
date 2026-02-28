package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.enchant.RadiationEnchantment;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;
public class EnhanceHoeItem extends HoeItem {
    private static final String INITIALIZED_TAG = "enhance_hoe_initialized";
    private static final String ENHANCE_CROP_BOOST_TAG = "enhance_crop_boost_level";
    private static final String CHARGE_TAG = "enhance_hoe_charge";
    private static final int MAX_CHARGE = 1000;
    private static final int INITIAL_CHARGE = MAX_CHARGE / 2;
    private static final int BOOST_CHARGE_COST = 15;
    private static final int CHARGE_PER_POWDER = 150;
    private static final int MAX_ENHANCE_LEVEL = 6;
    private static final String LAST_BOOST_TIME_TAG = "enhance_hoe_last_boost_time";
    private static final int BOOST_INTERVAL = 400;
    private int getBoostRange(int level) {
        return 2 + level;
    }
    private int getBoostStep(int level) {
        return 1 + (level / 2);
    }
    public EnhanceHoeItem(IItemTier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder
                .addToolType(ToolType.HOE, tier.getHarvestLevel())
                .defaultMaxDamage(1200));
    }
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || !(entityIn instanceof PlayerEntity) || !isSelected
                || ((PlayerEntity) entityIn).getHeldItemMainhand() != stack) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entityIn;
        CompoundNBT nbt = stack.getOrCreateTag();

        if (!nbt.getBoolean(INITIALIZED_TAG)) {
            initializeHoe(stack, player, nbt);
            return;
        }
        int currentCharge = getCharge(stack);
        if (currentCharge <= 0) {
            return;
        }
        long currentGameTime = worldIn.getGameTime();
        long lastBoostTime = nbt.getLong(LAST_BOOST_TIME_TAG);
        int boostLevel = nbt.getInt(ENHANCE_CROP_BOOST_TAG);
        if (currentGameTime - lastBoostTime >= BOOST_INTERVAL) {
            if (!hasGrowableCropsInRange(worldIn, player.getPosition(), boostLevel)) {
                return;
            }
            if (!consumeCharge(stack, BOOST_CHARGE_COST)) {
                return;
            }
            int boostedCount = boostCropsInRange(worldIn, player.getPosition(), boostLevel);
            if (boostedCount > 0) {
                nbt.putLong(LAST_BOOST_TIME_TAG, currentGameTime);
                stack.setTag(nbt);

                player.sendMessage(new TranslationTextComponent(
                        "message.enhance_hoe.boost_success", boostedCount
                ).mergeStyle(TextFormatting.YELLOW), player.getUniqueID());
            }
        }
    }
    private void initializeHoe(ItemStack stack, PlayerEntity player, CompoundNBT nbt) {
        int playerEnhanceLevel = getPlayerEnhanceLevel(player);
        int finalBoostLevel = Math.min(playerEnhanceLevel, MAX_ENHANCE_LEVEL);
        nbt.putInt(ENHANCE_CROP_BOOST_TAG, finalBoostLevel);
        nbt.putInt(CHARGE_TAG, INITIAL_CHARGE);
        nbt.putBoolean(INITIALIZED_TAG, true);
        stack.setTag(nbt);
        player.sendMessage(new TranslationTextComponent(
                "message.enhance_hoe.initialized",
                finalBoostLevel,
                getBoostRange(finalBoostLevel),
                getBoostStep(finalBoostLevel),
                INITIAL_CHARGE,
                MAX_CHARGE
        ).mergeStyle(TextFormatting.GOLD), player.getUniqueID());
    }
    private int getPlayerEnhanceLevel(PlayerEntity player) {
        if (player == null) return 0;
        CompoundNBT playerBuffNBT = player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
        return playerBuffNBT.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
    }
    private boolean hasGrowableCropsInRange(World world, BlockPos centerPos, int boostLevel) {
        int range = getBoostRange(boostLevel);
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos targetPos = centerPos.add(x, y, z);
                    BlockState state = world.getBlockState(targetPos);
                    if (state.getBlock() instanceof CropsBlock) {
                        CropsBlock crop = (CropsBlock) state.getBlock();
                        int currentAge = state.get(crop.getAgeProperty());
                        int maxAge = crop.getMaxAge();
                        if (currentAge < maxAge) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    private int boostCropsInRange(World world, BlockPos centerPos, int boostLevel) {
        int boostedCount = 0;
        int range = getBoostRange(boostLevel);
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos targetPos = centerPos.add(x, y, z);
                    BlockState state = world.getBlockState(targetPos);
                    if (state.getBlock() instanceof CropsBlock) {
                        CropsBlock crop = (CropsBlock) state.getBlock();
                        int currentAge = state.get(crop.getAgeProperty());
                        int maxAge = crop.getMaxAge();
                        if (currentAge < maxAge) {
                            BlockState newState = crop.getDefaultState().with(crop.getAgeProperty(), maxAge);
                            world.setBlockState(targetPos, newState);
                            boostedCount++;
                        }
                    }
                }
            }
        }
        return boostedCount;
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        CompoundNBT nbt = stack.getOrCreateTag();
        tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.base_info")
                .mergeStyle(TextFormatting.BLUE));
        if (nbt.getBoolean(INITIALIZED_TAG)) {
            int boostLevel = nbt.getInt(ENHANCE_CROP_BOOST_TAG);
            int currentRange = getBoostRange(boostLevel);
            int currentStep = getBoostStep(boostLevel);
            int currentCharge = getCharge(stack);
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_hoe.crop_boost_perk",
                    boostLevel, currentRange, currentStep
            ).mergeStyle(TextFormatting.LIGHT_PURPLE));
            TextFormatting chargeColor = currentCharge > MAX_CHARGE * 0.5 ? TextFormatting.GREEN :
                    currentCharge > MAX_CHARGE * 0.2 ? TextFormatting.YELLOW : TextFormatting.RED;
            tooltip.add(new TranslationTextComponent(
                    "tooltip.enhance_hoe.charge",
                    currentCharge, MAX_CHARGE
            ).mergeStyle(chargeColor));
            tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.boost_charge_cost", BOOST_CHARGE_COST)
                    .mergeStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }
        tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.yield_rule")
                .mergeStyle(TextFormatting.LIGHT_PURPLE));
        tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.boost_rule")
                .mergeStyle(TextFormatting.GOLD));
        tooltip.add(new TranslationTextComponent("tooltip.enhance_hoe.anvil_repair")
                .mergeStyle(TextFormatting.DARK_GREEN));
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
    public static boolean isInitialized(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
        }
        return false;
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_HOE_TIER.getRepairMaterial().test(repair);
    }
}
