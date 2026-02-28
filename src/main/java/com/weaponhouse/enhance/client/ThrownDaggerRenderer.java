package com.weaponhouse.enhance.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.weaponhouse.enhance.items.ThrownDaggerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
@OnlyIn(Dist.CLIENT)
public class ThrownDaggerRenderer extends EntityRenderer<ThrownDaggerEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("enhance", "textures/entity/dagger_side_1.png");
    private final DaggerModel<ThrownDaggerEntity> model;
    public ThrownDaggerRenderer(EntityRendererManager renderManager) {
        super(renderManager);
        this.model = new DaggerModel<>();
    }
    @Override
    public void render(ThrownDaggerEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        if (!entity.isAlive()) {
            return;
        }
        matrixStack.push();
        try {
            matrixStack.translate(0.0D, 0.15D, 0.0D);
            matrixStack.rotate(Vector3f.YP.rotationDegrees(entity.rotationYaw - 90.0F));
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(entity.rotationPitch + 90.0F));
            float spin = (entity.ticksExisted + partialTicks) * 20F;
            matrixStack.rotate(Vector3f.XP.rotationDegrees(spin));
            matrixStack.scale(0.5F, 0.5F, 0.5F);
            IVertexBuilder vertexBuilder = buffer.getBuffer(this.model.getRenderType(this.getEntityTexture(entity)));
            this.model.setRotationAngles(entity, 0, 0, entity.ticksExisted + partialTicks, 0, 0);
            this.model.render(matrixStack, vertexBuilder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } catch (Exception e) {
            e.fillInStackTrace();
        } finally {
            matrixStack.pop();
        }
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }
    @Override
    public ResourceLocation getEntityTexture(ThrownDaggerEntity entity) {
        return TEXTURE;
    }
}