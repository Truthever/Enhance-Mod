package com.weaponhouse.enhance.client;

import net.minecraft.client.world.DimensionRenderInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import java.lang.reflect.Field;
import java.util.Map;
@Mod.EventBusSubscriber(modid = "enhance", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {
    private static ChaoslandsSkyRenderer skyRenderer;
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            skyRenderer = new ChaoslandsSkyRenderer();
            try {
                Field field = DimensionRenderInfo.class.getDeclaredField("field_239208_a_");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<ResourceLocation, DimensionRenderInfo> dimensionRenderInfoMap =
                        (Map<ResourceLocation, DimensionRenderInfo>) field.get(null);
                DimensionRenderInfo chaoslandsRenderInfo = new DimensionRenderInfo(128.0F, false,
                        DimensionRenderInfo.FogType.NONE, false, false) {
                    @Override
                    public net.minecraft.util.math.vector.Vector3d func_230494_a_(
                            net.minecraft.util.math.vector.Vector3d color, float partialTicks) {
                        return new net.minecraft.util.math.vector.Vector3d(0.1, 0.1, 0.1);
                    }
                    @Override
                    public boolean func_230493_a_(int x, int y) {
                        return false;
                    }
                };
                chaoslandsRenderInfo.setSkyRenderHandler(skyRenderer);
                ResourceLocation chaoslandsEffects = new ResourceLocation("enhance", "chaoslands_sky");
                dimensionRenderInfoMap.put(chaoslandsEffects, chaoslandsRenderInfo);
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        });
    }
}