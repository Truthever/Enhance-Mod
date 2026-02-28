package com.weaponhouse.enhance.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
public class FloatingPillRenderer extends EntityRenderer<FloatingPillEntity> {
    public FloatingPillRenderer(EntityRendererManager renderManager) {
        super(renderManager);
    }
    @Override
    public void render(FloatingPillEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        if (!entity.isAlive()) {
            return;
        }
        matrixStack.push();
        try {
            ItemStack pillStack = entity.getItemStack();
            if (pillStack.isEmpty()) {
                matrixStack.pop();
                return;
            }
            float hover = (float) Math.sin((entity.getAge() + partialTicks) * 0.1) * 0.1f;
            matrixStack.translate(0.0D, hover, 0.0D);
            float rotation = (entity.getAge() + partialTicks) * 2.0f;
            matrixStack.rotate(Vector3f.YP.rotationDegrees(rotation));
            matrixStack.scale(0.4f, 0.4f, 0.4f);
            Minecraft.getInstance().getItemRenderer().renderItem(
                    pillStack,
                    ItemCameraTransforms.TransformType.FIXED,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    matrixStack,
                    buffer
            );
        } catch (Exception e) {
            e.fillInStackTrace();
        } finally {
            matrixStack.pop();
        }
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }
    @Override
    public ResourceLocation getEntityTexture(FloatingPillEntity entity) {
        return null;
    }
}