package com.weaponhouse.enhance.client;
import com.weaponhouse.enhance.entity.ChaoticMerchantEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.util.ResourceLocation;
public class ChaoticMerchantRenderer extends MobRenderer<ChaoticMerchantEntity, ChaoticMerchantModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("enhance", "textures/entity/chaotic_merchant.png");
    public ChaoticMerchantRenderer(EntityRendererManager renderManagerIn) {
        super(renderManagerIn, new ChaoticMerchantModel(), 0.7F);
        this.addLayer(new HeldItemLayer<ChaoticMerchantEntity, ChaoticMerchantModel>(this) {
            @Override
            public void render(com.mojang.blaze3d.matrix.MatrixStack matrixStack, net.minecraft.client.renderer.IRenderTypeBuffer buffer, int packedLight, ChaoticMerchantEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
                entity.getHeldItemMainhand();
                if (!entity.getHeldItemMainhand().isEmpty()) {
                    super.render(matrixStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                }
            }
        });
    }
    @Override
    public ResourceLocation getEntityTexture(ChaoticMerchantEntity entity) {
        return TEXTURE;
    }
    @Override
    protected void preRenderCallback(ChaoticMerchantEntity entitylivingbaseIn, com.mojang.blaze3d.matrix.MatrixStack matrixStackIn, float partialTickTime) {
        float scale = 1.0F;
        matrixStackIn.scale(scale, scale, scale);
    }
}