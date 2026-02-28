package com.weaponhouse.enhance.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
public class DaggerModel<T extends Entity> extends EntityModel<T> {
    private final ModelRenderer bone;
    public DaggerModel() {
        textureWidth = 16;
        textureHeight = 16;
        bone = new ModelRenderer(this);
        bone.setRotationPoint(-1.4426F, 19.2987F, 0.0F);
        bone.setTextureOffset(0, 0).addBox(-9.4142F, -8.0F, 0.0F, 16.0F, 16.0F, 0.0F, 0.0F, false);
        bone.rotateAngleZ = -2.3562F;
    }
    @Override
    public void setRotationAngles(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity instanceof com.weaponhouse.enhance.items.ThrownDaggerEntity) {
            bone.rotateAngleZ = ageInTicks * 0.5F;
        }
    }
    @Override
    public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bone.render(matrixStack, buffer, packedLight, packedOverlay);
    }
}