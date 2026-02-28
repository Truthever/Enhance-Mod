package com.weaponhouse.enhance.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.weaponhouse.enhance.entity.ChaoticMerchantEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.IHasArm;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
public class ChaoticMerchantModel extends EntityModel<ChaoticMerchantEntity> implements IHasArm {
    private final ModelRenderer body;
    private final ModelRenderer head;
    private final ModelRenderer arms;
    private final ModelRenderer leg0;
    private final ModelRenderer leg1;
    private final ModelRenderer bb_main;
    private final ModelRenderer cube_r1;
    public ChaoticMerchantModel() {
        textureWidth = 64;
        textureHeight = 64;
        body = new ModelRenderer(this);
        body.setRotationPoint(0.0F, 24.0F, 0.0F);
        body.setTextureOffset(16, 20).addBox(-4.0F, -24.0F, -3.0F, 8.0F, 12.0F, 6.0F, 0.0F, false);
        body.setTextureOffset(0, 38).addBox(-4.0F, -24.0F, -3.0F, 8.0F, 18.0F, 6.0F, 0.5F, false);
        head = new ModelRenderer(this);
        head.setRotationPoint(0.0F, -24.0F, 0.0F);
        body.addChild(head);
        head.setTextureOffset(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, 0.0F, false);
        ModelRenderer helmet = new ModelRenderer(this);
        helmet.setRotationPoint(0.0F, 0.0F, 0.0F);
        head.addChild(helmet);
        helmet.setTextureOffset(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, 0.5F, false);
        ModelRenderer brim = new ModelRenderer(this);
        brim.setRotationPoint(0.0F, 0.0F, 0.0F);
        head.addChild(brim);
        arms = new ModelRenderer(this);
        arms.setRotationPoint(0.0F, -21.0F, -1.0F);
        arms.rotateAngleX = -0.75F;
        body.addChild(arms);
        arms.setTextureOffset(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F, 0.0F, false);
        arms.setTextureOffset(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, 0.0F, false);
        arms.setTextureOffset(44, 22).addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, 0.0F, true);
        ModelRenderer held_item = new ModelRenderer(this);
        held_item.setRotationPoint(0.0F, 22.0F, 0.0F);
        arms.addChild(held_item);
        leg0 = new ModelRenderer(this);
        leg0.setRotationPoint(-2.0F, -12.0F, 0.0F);
        body.addChild(leg0);
        leg0.setTextureOffset(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);
        leg1 = new ModelRenderer(this);
        leg1.setRotationPoint(2.0F, -12.0F, 0.0F);
        body.addChild(leg1);
        leg1.setTextureOffset(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, true);
        bb_main = new ModelRenderer(this);
        bb_main.setRotationPoint(0.0F, 24.0F, 0.0F);
        cube_r1 = new ModelRenderer(this);
        cube_r1.setRotationPoint(8.0F, -7.0F, 7.0F);
        cube_r1.rotateAngleX = 0.1309F;
        cube_r1.rotateAngleY = 0.0F;
        cube_r1.rotateAngleZ = 0.0F;
        bb_main.addChild(cube_r1);
        cube_r1.setTextureOffset(28, 46).addBox(-15.0F, -18.0F, -1.0F, 14.0F, 18.0F, 0.0F, 0.0F, false);
    }
    @Override
    public void setRotationAngles(ChaoticMerchantEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.rotateAngleY = netHeadYaw * ((float)Math.PI / 180F);
        this.head.rotateAngleX = headPitch * ((float)Math.PI / 180F);
        this.arms.rotateAngleX = -0.75F + MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leg0.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        float cloakSwing = MathHelper.cos(limbSwing * 0.6662F) * 0.3F * limbSwingAmount;
        this.cube_r1.rotateAngleX = 0.1309F + cloakSwing;
        this.arms.rotateAngleZ = 0.0F;
    }
    @Override
    public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        body.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bb_main.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
    @Override
    public void translateHand(HandSide sideIn, MatrixStack matrixStackIn) {}
}