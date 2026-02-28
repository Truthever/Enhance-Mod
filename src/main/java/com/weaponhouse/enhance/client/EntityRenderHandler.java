package com.weaponhouse.enhance.client;

import com.weaponhouse.enhance.util.RegistryHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
@OnlyIn(Dist.CLIENT)
public class EntityRenderHandler {
    public static void registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(
                RegistryHandler.CHAOTIC_MERCHANT.get(),
                ChaoticMerchantRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                RegistryHandler.THROWN_DAGGER.get(),
                ThrownDaggerRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                RegistryHandler.FLOATING_PILL_ENTITY.get(),
                FloatingPillRenderer::new
        );
    }
}