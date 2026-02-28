package com.weaponhouse.enhance.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.ISkyRenderHandler;
import org.lwjgl.opengl.GL11;
public class ChaoslandsSkyRenderer implements ISkyRenderHandler {
    private long lastTime = 0;
    private float colorPhase = 0.0f;
    @Override
    public void render(int ticks, float partialTicks, MatrixStack matrixStack, ClientWorld world, Minecraft mc) {
        completeSkyTakeover();
        updateColorPhase();
        Matrix4f projectionMatrix = matrixStack.getLast().getMatrix();
        renderUnifiedColorSky(matrixStack, projectionMatrix);
        restoreRenderingState();
    }
    private void completeSkyTakeover() {
        RenderSystem.pushMatrix();
        RenderSystem.pushLightingAttributes();
        RenderSystem.disableTexture();
        RenderSystem.disableFog();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableLighting();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
    }
    private void restoreRenderingState() {
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableLighting();
        RenderSystem.enableFog();
        RenderSystem.enableTexture();
        RenderSystem.popAttributes();
        RenderSystem.popMatrix();
    }
    private void updateColorPhase() {
        long currentTime = System.currentTimeMillis();
        if (lastTime == 0) {
            lastTime = currentTime;
        }
        long deltaTime = currentTime - lastTime;
        lastTime = currentTime;
        colorPhase += deltaTime * 0.00005f;
        if (colorPhase > 1.0f) {
            colorPhase -= 1.0f;
        }
    }
    private void renderUnifiedColorSky(MatrixStack matrixStack, Matrix4f projectionMatrix) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        RenderSystem.disableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        float[] currentSkyColor = getCurrentUnifiedColor();
        matrixStack.push();
        matrixStack.rotate(Vector3f.XP.rotationDegrees(90.0F));
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        double radius = 300.0D;
        int horizontalSegments = 24;
        int verticalSegments = 12;
        for (int i = 0; i < horizontalSegments; i++) {
            float longitude1 = (float) (i * 2 * Math.PI / horizontalSegments);
            float longitude2 = (float) ((i + 1) * 2 * Math.PI / horizontalSegments);
            for (int j = 0; j < verticalSegments; j++) {
                float latitude1 = (float) (j * Math.PI / verticalSegments - Math.PI / 2);
                float latitude2 = (float) ((j + 1) * Math.PI / verticalSegments - Math.PI / 2);
                double x1 = Math.cos(latitude1) * Math.cos(longitude1) * radius;
                double y1 = Math.cos(latitude1) * Math.sin(longitude1) * radius;
                double z1 = Math.sin(latitude1) * radius;
                double x2 = Math.cos(latitude1) * Math.cos(longitude2) * radius;
                double y2 = Math.cos(latitude1) * Math.sin(longitude2) * radius;
                double z2 = Math.sin(latitude1) * radius;
                double x3 = Math.cos(latitude2) * Math.cos(longitude2) * radius;
                double y3 = Math.cos(latitude2) * Math.sin(longitude2) * radius;
                double z3 = Math.sin(latitude2) * radius;
                double x4 = Math.cos(latitude2) * Math.cos(longitude1) * radius;
                double y4 = Math.cos(latitude2) * Math.sin(longitude1) * radius;
                double z4 = Math.sin(latitude2) * radius;
                buffer.pos(projectionMatrix, (float)x1, (float)z1, (float)y1)
                        .color(currentSkyColor[0], currentSkyColor[1], currentSkyColor[2], currentSkyColor[3]).endVertex();
                buffer.pos(projectionMatrix, (float)x2, (float)z2, (float)y2)
                        .color(currentSkyColor[0], currentSkyColor[1], currentSkyColor[2], currentSkyColor[3]).endVertex();
                buffer.pos(projectionMatrix, (float)x3, (float)z3, (float)y3)
                        .color(currentSkyColor[0], currentSkyColor[1], currentSkyColor[2], currentSkyColor[3]).endVertex();
                buffer.pos(projectionMatrix, (float)x4, (float)z4, (float)y4)
                        .color(currentSkyColor[0], currentSkyColor[1], currentSkyColor[2], currentSkyColor[3]).endVertex();
            }
        }
        tessellator.draw();
        matrixStack.pop();
        RenderSystem.shadeModel(GL11.GL_FLAT);
    }
    private float[] getCurrentUnifiedColor() {
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
        float sequencePosition = colorPhase * colorSequence.length;
        int colorIndex1 = (int) Math.floor(sequencePosition) % colorSequence.length;
        int colorIndex2 = (colorIndex1 + 1) % colorSequence.length;
        float blendFactor = sequencePosition - colorIndex1;
        float smoothBlend = smoothStep(blendFactor);
        return interpolateColors(colorSequence[colorIndex1], colorSequence[colorIndex2], smoothBlend);
    }
    private float smoothStep(float x) {
        return x * x * (3 - 2 * x);
    }
    private float[] interpolateColors(float[] color1, float[] color2, float factor) {
        return new float[]{
                color1[0] + (color2[0] - color1[0]) * factor,
                color1[1] + (color2[1] - color1[1]) * factor,
                color1[2] + (color2[2] - color1[2]) * factor,
                color1[3] + (color2[3] - color1[3]) * factor
        };
    }
}