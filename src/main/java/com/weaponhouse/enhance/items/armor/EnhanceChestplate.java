package com.weaponhouse.enhance.items.armor;

import com.weaponhouse.enhance.commands.EnhanceCommand;
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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;
public class EnhanceChestplate extends ArmorItem {
    public static final String DEFENSE_REDUCE_LEVEL_TAG = "enhance_chestplate_reduce_level";
    private static final String INITIALIZED_TAG = "enhance_chestplate_initialized";
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
        stack.setTag(nbt);
        float triggerChance = reduceLevel * 0.1F; // 10%-60%
        float reduceRatio = reduceLevel * 0.1F;   // 10%-60%
        player.sendMessage(new TranslationTextComponent(
                "message.enhance_chestplate.init",
                reduceLevel,
                (int) (triggerChance * 100),
                (int) (reduceRatio * 100)
        ).mergeStyle(TextFormatting.GOLD), player.getUniqueID());
    }
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("tooltip.enhance_chestplate.base_info").mergeStyle(TextFormatting.BLUE));
        if (isInitialized(stack)) {
            int level = getDefenseReduceLevel(stack);
            float triggerChance = level * 0.1F;
            float reduceRatio = level * 0.1F;
            tooltip.add(new TranslationTextComponent(
                            "tooltip.enhance_chestplate.reduce_effect",
                            level,
                            (int) (triggerChance * 100),
                            (int) (reduceRatio * 100)
                    ).mergeStyle(TextFormatting.GREEN)
            );
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
            return stack.getTag().getInt(DEFENSE_REDUCE_LEVEL_TAG);
        }
        return 0;
    }
    public static boolean isInitialized(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getBoolean(INITIALIZED_TAG);
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_ARMOR_MATERIAL.getRepairMaterial().test(repair);
    }
}