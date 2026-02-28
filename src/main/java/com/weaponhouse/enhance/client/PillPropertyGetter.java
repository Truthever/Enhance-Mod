package com.weaponhouse.enhance.client;

import com.weaponhouse.enhance.common.PillBuffGenerator;
import com.weaponhouse.enhance.common.PillTextureType;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid = "enhance", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PillPropertyGetter {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Item enhancePill = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation("enhance", "enhance_pill")
            );
            if (enhancePill != null) {
                net.minecraft.item.ItemModelsProperties.registerProperty(
                        enhancePill,
                        new ResourceLocation("enhance", "pill_type"),
                        (stack, world, entity) -> {
                            PillTextureType textureType = PillBuffGenerator.getTextureTypeFromPill(stack);
                            switch (textureType) {
                                case ATTACK: return 1.0F;
                                case LIFE: return 2.0F;
                                case DEFENSE: return 3.0F;
                                case SPEED: return 4.0F;
                                default: return 0.0F;
                            }
                        }
                );
            }
        });
    }
}