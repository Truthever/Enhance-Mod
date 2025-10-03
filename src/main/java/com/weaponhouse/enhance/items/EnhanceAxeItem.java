package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.commands.EnhanceCommand;
import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.AxeItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
public class EnhanceAxeItem extends AxeItem {
    public static final String CHAIN_CUT_TAG = "enhance_axe_chain_enabled";
    public static final String LEAF_CHAIN_TAG = "enhance_axe_leaf_chain_enabled";
    private static final String INITIALIZED_TAG = "enhance_axe_initialized";
    public static final String BLEED_LEVEL_TAG = "enhance_axe_bleed_level";
    public EnhanceAxeItem(IItemTier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, -3.0F, builder
                .addToolType(ToolType.AXE, tier.getHarvestLevel())
                .defaultMaxDamage(1500));
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack axe = playerIn.getHeldItem(handIn);
        if (!worldIn.isRemote && isInitialized(axe)) {
            CompoundNBT nbt = axe.getOrCreateTag();
            boolean isChainOn = nbt.getBoolean(CHAIN_CUT_TAG);
            boolean isLeafChainOn = nbt.getBoolean(LEAF_CHAIN_TAG);
            ITextComponent message;
            if (!isChainOn && !isLeafChainOn) {
                nbt.putBoolean(CHAIN_CUT_TAG, true);
                nbt.putBoolean(LEAF_CHAIN_TAG, false);
                message = new TranslationTextComponent("message.enhance_axe.mode_2")
                        .mergeStyle(TextFormatting.YELLOW);
            } else if (isChainOn && !isLeafChainOn) {
                nbt.putBoolean(CHAIN_CUT_TAG, true);
                nbt.putBoolean(LEAF_CHAIN_TAG, true);
                message = new TranslationTextComponent("message.enhance_axe.mode_3")
                        .mergeStyle(TextFormatting.GREEN);
            } else {
                nbt.putBoolean(CHAIN_CUT_TAG, false);
                nbt.putBoolean(LEAF_CHAIN_TAG, false);
                message = new TranslationTextComponent("message.enhance_axe.mode_1")
                        .mergeStyle(TextFormatting.RED);
            }
            axe.setTag(nbt);
            playerIn.sendMessage(message, playerIn.getUniqueID());
            return new ActionResult<>(ActionResultType.SUCCESS, axe);
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
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
        nbt.putBoolean(CHAIN_CUT_TAG, false);
        nbt.putBoolean(LEAF_CHAIN_TAG, false);
        int playerEnhanceLevel = getPlayerEnhanceLevel(player);
        int bleedLevel = generateBleedLevel(playerEnhanceLevel);
        nbt.putInt(BLEED_LEVEL_TAG, bleedLevel);
        nbt.putBoolean(INITIALIZED_TAG, true);
        stack.setTag(nbt);
        int damagePerSecond = bleedLevel * 1;
        player.sendMessage(new TranslationTextComponent(
                "message.enhance_axe.bleed_init", bleedLevel, damagePerSecond
        ).mergeStyle(TextFormatting.GOLD), player.getUniqueID());
        player.sendMessage(new TranslationTextComponent(
                "message.enhance_axe.mode_hint"
        ).mergeStyle(TextFormatting.GRAY), player.getUniqueID());
    }
    private int generateBleedLevel(int playerEnhanceLevel) {
        if (playerEnhanceLevel <= 0) return 1;
        int clampedLevel = Math.min(playerEnhanceLevel, 6);
        Random random = new Random();
        switch (clampedLevel) {
            case 1: return random.nextInt(2) + 1;
            case 2: return random.nextInt(2) + 2;
            case 3: return random.nextInt(2) + 3;
            case 4: return random.nextInt(2) + 4;
            case 5: return random.nextInt(2) + 5;
            case 6: return random.nextInt(2) + 6;
            default: return 1;
        }
    }
    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!isInitialized(stack)) {
            return super.hitEntity(stack, target, attacker);
        }
        int bleedLevel = getBleedLevel(stack);
        if (bleedLevel <= 0) {
            return super.hitEntity(stack, target, attacker);
        }
        CompoundNBT targetNBT = target.getPersistentData();
        targetNBT.putInt(BLEED_LEVEL_TAG, bleedLevel);
        targetNBT.putInt("bleed_duration", 3 * 20);
        targetNBT.putLong("bleed_start_time", target.world.getGameTime());
        return super.hitEntity(stack, target, attacker);
    }
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("tooltip.enhance_axe.base_info").mergeStyle(TextFormatting.BLUE));
        if (isInitialized(stack)) {
            int bleedLevel = getBleedLevel(stack);
            int damagePerSecond = bleedLevel * 1;
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_axe.bleed_effect", bleedLevel, damagePerSecond
                    ).mergeStyle(TextFormatting.RED)
            );
            boolean isChainOn = isChainCutEnabled(stack);
            boolean isLeafChainOn = isLeafChainEnabled(stack);
            String modeKey;
            TextFormatting color;
            if (!isChainOn && !isLeafChainOn) {
                modeKey = "tooltip.enhance_axe.mode_1";
                color = TextFormatting.RED;
            } else if (isChainOn && !isLeafChainOn) {
                modeKey = "tooltip.enhance_axe.mode_2";
                color = TextFormatting.YELLOW;
            } else {
                modeKey = "tooltip.enhance_axe.mode_3";
                color = TextFormatting.GREEN;
            }
            tooltip.add(new TranslationTextComponent(modeKey).mergeStyle(color));
            tooltip.add(new TranslationTextComponent("tooltip.enhance_axe.mode_toggle").mergeStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance_axe.uninitialized").mergeStyle(TextFormatting.GRAY));
        }
    }
    private int getPlayerEnhanceLevel(PlayerEntity player) {
        CompoundNBT playerNBT = player.getPersistentData().getCompound(EnhanceCommand.BUFF_TAG);
        return playerNBT.getInt(EnhanceCommand.ENHANCE_LEVEL_TAG);
    }
    public static int getBleedLevel(ItemStack stack) {
        if (isInitialized(stack) && stack.hasTag()) {
            return stack.getTag().getInt(BLEED_LEVEL_TAG);
        }
        return 0;
    }
    public static boolean isChainCutEnabled(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getBoolean(CHAIN_CUT_TAG);
    }
    public static boolean isLeafChainEnabled(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getBoolean(LEAF_CHAIN_TAG);
    }
    public static boolean isInitialized(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_AXE_TIER.getRepairMaterial().test(repair);
    }
}