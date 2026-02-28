package com.weaponhouse.enhance.items;

import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
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
import java.util.Objects;

public class EnhanceShovelItem extends ShovelItem {
    private static final String INITIALIZED_TAG = "enhance_shovel_initialized";
    public static final String AUTO_SMELT_TAG = "enhance_shovel_auto_smelt";
    private static final Enchantment KNOCKBACK_ENCHANT = Enchantments.KNOCKBACK;
    private static final int KNOCKBACK_LEVEL = 2;
    public EnhanceShovelItem(IItemTier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder
                .addToolType(ToolType.SHOVEL, tier.getHarvestLevel())
                .defaultMaxDamage(tier.getMaxUses()));
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack shovel = playerIn.getHeldItem(handIn);
        if (!worldIn.isRemote && isInitialized(shovel)) {
            CompoundNBT nbt = shovel.getOrCreateTag();
            boolean newState = !nbt.getBoolean(AUTO_SMELT_TAG);
            nbt.putBoolean(AUTO_SMELT_TAG, newState);
            shovel.setTag(nbt);
            ITextComponent message = new TranslationTextComponent(
                    newState ? "message.enhance_shovel.smelt_enabled" : "message.enhance_shovel.smelt_disabled"
            ).mergeStyle(newState ? TextFormatting.GREEN : TextFormatting.RED);
            playerIn.sendMessage(message, playerIn.getUniqueID());
        }
        return new ActionResult<>(ActionResultType.SUCCESS, shovel);
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
        addKnockbackEnchant(stack);
        nbt.putBoolean(AUTO_SMELT_TAG, false);
        nbt.putBoolean(INITIALIZED_TAG, true);
        stack.setTag(nbt);
        player.sendMessage(new TranslationTextComponent(
                "message.enhance_shovel.initialized_knockback"
        ).mergeStyle(TextFormatting.GREEN), player.getUniqueID());
    }
    private void addKnockbackEnchant(ItemStack stack) {
        CompoundNBT stackNBT = stack.getOrCreateTag();
        if (!stackNBT.contains("Enchantments", 9)) {
            stackNBT.put("Enchantments", new ListNBT());
        }
        ListNBT enchantList = stackNBT.getList("Enchantments", 10);
        boolean hasKnockback = false;
        for (int i = 0; i < enchantList.size(); i++) {
            CompoundNBT enchantNBT = enchantList.getCompound(i);
            String enchantId = enchantNBT.getString("id");
            if (enchantId.equals(Objects.requireNonNull(KNOCKBACK_ENCHANT.getRegistryName()).toString())) {
                enchantNBT.putInt("lvl", KNOCKBACK_LEVEL);
                hasKnockback = true;
                break;
            }
        }
        if (!hasKnockback) {
            CompoundNBT newEnchantNBT = new CompoundNBT();
            newEnchantNBT.putString("id", Objects.requireNonNull(KNOCKBACK_ENCHANT.getRegistryName()).toString());
            newEnchantNBT.putInt("lvl", KNOCKBACK_LEVEL);
            enchantList.add(newEnchantNBT);
        }
        stack.setTag(stackNBT);
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("tooltip.enhance_shovel.base_info")
                .mergeStyle(TextFormatting.BLUE));
        if (stack.isEnchanted()) {
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_shovel.knockback_effect", KNOCKBACK_LEVEL
                    ).mergeStyle(TextFormatting.LIGHT_PURPLE)
            );
        }
        if (isInitialized(stack)) {
            boolean isSmeltOn = isAutoSmeltEnabled(stack);
            tooltip.add(new TranslationTextComponent(
                            isSmeltOn ? "tooltip.enhance_shovel.smelt_enabled" : "tooltip.enhance_shovel.smelt_disabled"
                    ).mergeStyle(isSmeltOn ? TextFormatting.GREEN : TextFormatting.RED)
            );
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.enhance_shovel.uninitialized")
                    .mergeStyle(TextFormatting.GRAY));
        }
        tooltip.add(new TranslationTextComponent("tooltip.enhance_shovel.toggle_hint")
                .mergeStyle(TextFormatting.GRAY));
    }
    public static boolean isInitialized(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
        }
        return false;
    }
    public static boolean isAutoSmeltEnabled(ItemStack stack) {
        if (stack.getTag() != null) {
            return stack.hasTag() && stack.getTag().getBoolean(AUTO_SMELT_TAG);
        }
        return false;
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_SHOVEL_TIER.getRepairMaterial().test(repair);
    }
}