package com.weaponhouse.enhance.world.dimension;

import com.weaponhouse.enhance.Enhance;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
public class DimensionRegistry {
    public static final ResourceLocation ENHANCE_DIMENSION_ID = new ResourceLocation(Enhance.MOD_ID, "chaoslands");
    public static final RegistryKey<World> ENHANCE_DIMENSION = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, ENHANCE_DIMENSION_ID);
}