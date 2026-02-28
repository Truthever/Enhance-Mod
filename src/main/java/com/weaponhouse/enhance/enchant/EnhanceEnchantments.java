package com.weaponhouse.enhance.enchant;

import com.weaponhouse.enhance.Enhance;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
public class EnhanceEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Enhance.MOD_ID);
    public static final RegistryObject<Enchantment> RADIATION =
            ENCHANTMENTS.register("radiation", RadiationEnchantment::new);
}