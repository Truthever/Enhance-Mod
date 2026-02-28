package com.weaponhouse.enhance.enchant;

import com.weaponhouse.enhance.items.*;
import com.weaponhouse.enhance.items.armor.EnhanceBoots;
import com.weaponhouse.enhance.items.armor.EnhanceChestplate;
import com.weaponhouse.enhance.items.armor.EnhanceHelmet;
import com.weaponhouse.enhance.items.armor.EnhanceLeggings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;

import java.util.Random;
public class RadiationEnchantment extends Enchantment {
    private static final Random RANDOM = new Random();
    private static final float PROBABILITY_PER_LEVEL = 0.15F;
    public static final EnchantmentType ENHANCE_GEAR = EnchantmentType.create("ENHANCE_GEAR", item ->
            item instanceof EnhanceHelmet ||
                    item instanceof EnhanceChestplate ||
                    item instanceof EnhanceLeggings ||
                    item instanceof EnhanceBoots ||
                    item instanceof EnhanceSwordItem ||
                    item instanceof EnhanceAxeItem ||
                    item instanceof EnhancePickaxeItem ||
                    item instanceof EnhanceShovelItem ||
                    item instanceof EnhanceHoeItem
    );
    public RadiationEnchantment() {
        super(Rarity.RARE, ENHANCE_GEAR,
                new EquipmentSlotType[]{
                        EquipmentSlotType.HEAD,
                        EquipmentSlotType.CHEST,
                        EquipmentSlotType.LEGS,
                        EquipmentSlotType.FEET,
                        EquipmentSlotType.MAINHAND
                });
    }
    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return 15 + (enchantmentLevel - 1) * 9;
    }
    @Override
    public int getMaxEnchantability(int enchantmentLevel) {
        return super.getMinEnchantability(enchantmentLevel) + 50;
    }
    @Override
    public int getMaxLevel() {
        return 3;
    }
    public static boolean shouldConserveCharge(ItemStack stack, int enchantmentLevel) {
        if (enchantmentLevel <= 0) return false;

        float probability = enchantmentLevel * PROBABILITY_PER_LEVEL;
        return RANDOM.nextFloat() < probability;
    }
    public static int getRadiationLevel(ItemStack stack) {
        if (stack == null || !stack.isEnchanted()) return 0;
        return EnchantmentHelper.getEnchantmentLevel(EnhanceEnchantments.RADIATION.get(), stack);
    }
    @Override
    public boolean canApply(ItemStack stack) {
        return this.type.canEnchantItem(stack.getItem());
    }
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return stack.getItem().isEnchantable(stack) && this.type.canEnchantItem(stack.getItem());
    }
}