package com.weaponhouse.enhance.effects;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Objects;
public class DarknessEffectRenderer {
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        EffectInstance effect = mc.player.getActivePotionEffect(
                Objects.requireNonNull(ForgeRegistries.POTIONS.getValue(new ResourceLocation("enhance:darkness")))
        );
        if (effect == null) {
            return;
        }
        int duration = effect.getDuration();
        int amplifier = effect.getAmplifier();
        int interval = Math.max(1, 20 - (amplifier + 1));
        int intervalTicks = interval * 20;
        if ((duration % intervalTicks) < 20) {
            renderDarkScreen(event.getMatrixStack());
        }
    }
    private static void renderDarkScreen(MatrixStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getMainWindow().getScaledWidth();
        int screenHeight = mc.getMainWindow().getScaledHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(0f, 0f, 0f, 1f); // 纯黑色
        AbstractGui.fill(matrixStack, 0, 0, screenWidth, screenHeight, 0xFF000000);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }
}
