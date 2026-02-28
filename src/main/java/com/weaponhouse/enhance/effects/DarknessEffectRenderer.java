package com.weaponhouse.enhance.effects;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
public class DarknessEffectRenderer {
    private static final Lazy<Effect> DARKNESS_EFFECT = Lazy.of(() ->
            ForgeRegistries.POTIONS.getValue(new ResourceLocation("enhance:darkness"))
    );
    public static Effect getDarknessEffect() {
        return DARKNESS_EFFECT.get();
    }
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameSettings.hideGUI) {
            return;
        }
        Effect darknessEffect = getDarknessEffect();
        if (darknessEffect == null) {
            return;
        }
        EffectInstance effect = mc.player.getActivePotionEffect(darknessEffect);
        if (effect == null) {
            return;
        }
        int duration = effect.getDuration();
        int amplifier = effect.getAmplifier();
        int intervalSeconds = Math.max(1, amplifier);
        int intervalTicks = intervalSeconds * 20;
        int positionInInterval = duration % intervalTicks;
        boolean shouldRenderDarkness = positionInInterval < 20;
        if (shouldRenderDarkness) {
            renderDarkScreen(event.getMatrixStack());
        }
    }
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderLiving(RenderLivingEvent.Post<LivingEntity, ?> event) {
        LivingEntity entity = event.getEntity();
        Effect darknessEffect = getDarknessEffect();
        if (darknessEffect == null) {
            return;
        }
        EffectInstance effect = entity.getActivePotionEffect(darknessEffect);
        if (effect == null) {
            return;
        }
        int duration = effect.getDuration();
        int amplifier = effect.getAmplifier();
        int intervalSeconds = Math.max(1, amplifier);
        int intervalTicks = intervalSeconds * 20;
        int positionInInterval = duration % intervalTicks;
        boolean shouldShowEffect = positionInInterval < 20;
        if (shouldShowEffect && !(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
            if (entity.world.getGameTime() % 10 == 0) {
                renderEntityDarknessEffect(event.getMatrixStack(), entity);
            }
        }
    }
    @OnlyIn(Dist.CLIENT)
    private static void renderDarkScreen(MatrixStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getMainWindow().getScaledWidth();
        int screenHeight = mc.getMainWindow().getScaledHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(0f, 0f, 0f, 1f);
        AbstractGui.fill(matrixStack, 0, 0, screenWidth, screenHeight, 0xFF000000);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
    }
    @OnlyIn(Dist.CLIENT)
    private static void renderEntityDarknessEffect(MatrixStack matrixStack, LivingEntity entity) {
        double x = entity.getPosX() + (entity.getRNG().nextDouble() - 0.5) * entity.getWidth();
        double y = entity.getPosY() + entity.getRNG().nextDouble() * entity.getHeight();
        double z = entity.getPosZ() + (entity.getRNG().nextDouble() - 0.5) * entity.getWidth();
        entity.world.addParticle(net.minecraft.particles.ParticleTypes.SMOKE,
                x, y, z, 0, 0.1, 0);
    }
}