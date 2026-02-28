package com.weaponhouse.enhance.client;

import com.weaponhouse.enhance.world.dimension.DimensionRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "enhance", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeBusEventHandler {
    private static long lastFogUpdate = 0;
    private static float fogColorPhase = 0.0f;
    @SubscribeEvent
    public static void onFogColor(EntityViewRenderEvent.FogColors event) {
        event.getInfo().getRenderViewEntity();
        if (event.getInfo().getRenderViewEntity().world == null) {
            return;
        }
        if (event.getInfo().getRenderViewEntity().world.getDimensionKey() == DimensionRegistry.ENHANCE_DIMENSION) {
            updateFogColorPhase();
            float[] fogColor = getCurrentUnifiedFogColor();
            event.setRed(fogColor[0]);
            event.setGreen(fogColor[1]);
            event.setBlue(fogColor[2]);
        }
    }
    private static void updateFogColorPhase() {
        long currentTime = System.currentTimeMillis();
        if (lastFogUpdate == 0) {
            lastFogUpdate = currentTime;
        }
        long deltaTime = currentTime - lastFogUpdate;
        lastFogUpdate = currentTime;
        fogColorPhase += deltaTime * 0.00005f;
        if (fogColorPhase > 1.0f) {
            fogColorPhase -= 1.0f;
        }
    }
    private static float[] getCurrentUnifiedFogColor() {
        float[][] colorSequence = {
                {0.3f, 0.6f, 1.0f, 1.0f},
                {0.6f, 0.4f, 0.9f, 1.0f},
                {0.8f, 0.3f, 1.0f, 1.0f},
                {0.9f, 0.3f, 0.7f, 1.0f},
                {1.0f, 0.3f, 0.4f, 1.0f},
                {1.0f, 0.5f, 0.3f, 1.0f},
                {1.0f, 0.7f, 0.3f, 1.0f},
                {0.9f, 0.8f, 0.3f, 1.0f},
                {0.7f, 0.9f, 0.3f, 1.0f},
                {0.4f, 1.0f, 0.3f, 1.0f},
                {0.3f, 0.9f, 0.5f, 1.0f},
                {0.3f, 0.8f, 0.8f, 1.0f}
        };
        float sequencePosition = fogColorPhase * colorSequence.length;
        int colorIndex1 = (int) Math.floor(sequencePosition) % colorSequence.length;
        int colorIndex2 = (colorIndex1 + 1) % colorSequence.length;
        float blendFactor = sequencePosition - colorIndex1;
        float smoothBlend = smoothStep(blendFactor);
        return interpolateColors(colorSequence[colorIndex1], colorSequence[colorIndex2], smoothBlend);
    }
    private static float smoothStep(float x) {
        return x * x * (3 - 2 * x);
    }
    private static float[] interpolateColors(float[] color1, float[] color2, float factor) {
        return new float[]{
                color1[0] + (color2[0] - color1[0]) * factor,
                color1[1] + (color2[1] - color1[1]) * factor,
                color1[2] + (color2[2] - color1[2]) * factor,
                color1[3] + (color2[3] - color1[3]) * factor
        };
    }
}