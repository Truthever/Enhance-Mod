package com.weaponhouse.enhance.items.armor;

import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
public class EnhanceHelmet extends ArmorItem {
    public EnhanceHelmet(IArmorMaterial material, EquipmentSlotType slot, Properties properties) {
        super(material, slot, properties);
    }
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return RegistryHandler.ENHANCE_ARMOR_MATERIAL.getRepairMaterial().test(repair);
    }
}