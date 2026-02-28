package com.weaponhouse.enhance.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
@Mod.EventBusSubscriber(modid = "enhance", value = Dist.CLIENT)
public class BlockLinkRenderer {
    private static final Map<String, LinkInfo> activeLinks = new HashMap<>();
    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");
    private static class LinkInfo {
        public final BlockPos pos1;
        public final BlockPos pos2;
        public final int color;
        public final String key;
        public LinkInfo(BlockPos pos1, BlockPos pos2, int color) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.color = color;
            this.key = generateKey(pos1, pos2);
        }
    }
    private static String generateKey(BlockPos pos1, BlockPos pos2) {
        BlockPos minPos = pos1.compareTo(pos2) < 0 ? pos1 : pos2;
        BlockPos maxPos = pos1.compareTo(pos2) < 0 ? pos2 : pos1;
        return minPos.getX() + "," + minPos.getY() + "," + minPos.getZ() + ":" +
                maxPos.getX() + "," + maxPos.getY() + "," + maxPos.getZ();
    }
    public static void addLink(BlockPos pos1, BlockPos pos2, int color) {
        String key = generateKey(pos1, pos2);
        activeLinks.put(key, new LinkInfo(pos1, pos2, color));
    }
    public static void removeLink(BlockPos pos1, BlockPos pos2) {
        String key = generateKey(pos1, pos2);
        activeLinks.remove(key);
    }
    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (activeLinks.isEmpty()) return;
        MatrixStack matrixStack = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        matrixStack.push();
        Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        for (LinkInfo link : activeLinks.values()) {
            renderThinConnection(matrixStack, buffer, link.pos1, link.pos2, link.color, event.getPartialTicks());
        }
        matrixStack.pop();
        buffer.finish();
    }
    private static void renderThinConnection(MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                             BlockPos pos1, BlockPos pos2, int color, float partialTicks) {
        try {
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            Vector3d start = new Vector3d(pos1.getX() + 0.5, pos1.getY() + 0.5, pos1.getZ() + 0.5);
            Vector3d end = new Vector3d(pos2.getX() + 0.5, pos2.getY() + 0.5, pos2.getZ() + 0.5);
            long worldTime = 0;
            if (Minecraft.getInstance().world != null) {
                worldTime = Minecraft.getInstance().world.getGameTime();
            }
            renderThinBeam(matrixStack, buffer, start, end, r, g, b, worldTime, partialTicks);
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }
    private static void renderThinBeam(MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                       Vector3d start, Vector3d end, float r, float g, float b,
                                       long worldTime, float partialTicks) {
        Vector3d direction = end.subtract(start);
        double distance = direction.length();
        Vector3d up = new Vector3d(0, 1, 0);
        Vector3d right = direction.crossProduct(up).normalize();
        if (right.lengthSquared() < 0.001) {
            right = direction.crossProduct(new Vector3d(1, 0, 0)).normalize();
        }
        Vector3d forward = direction.normalize();
        up = right.crossProduct(forward).normalize();
        float beamWidth = 0.1f;
        float animation = (float)((worldTime + partialTicks) * 0.1) % 1.0f;
        matrixStack.push();
        IVertexBuilder builder = buffer.getBuffer(RenderType.getBeaconBeam(BEAM_TEXTURE, false));
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        Matrix3f normal = matrixStack.getLast().getNormal();
        Vector3d rightOffset = right.scale(beamWidth);
        Vector3d upOffset = up.scale(beamWidth);
        Vector3d p1 = start.add(rightOffset).add(upOffset);
        Vector3d p2 = start.add(rightOffset).subtract(upOffset);
        Vector3d p3 = start.subtract(rightOffset).subtract(upOffset);
        Vector3d p4 = start.subtract(rightOffset).add(upOffset);
        Vector3d p5 = end.add(rightOffset).add(upOffset);
        Vector3d p6 = end.add(rightOffset).subtract(upOffset);
        Vector3d p7 = end.subtract(rightOffset).subtract(upOffset);
        Vector3d p8 = end.subtract(rightOffset).add(upOffset);
        renderBeamFace(matrix, normal, builder, r, g, b, p1, p2, p6, p5, animation, distance);
        renderBeamFace(matrix, normal, builder, r, g, b, p2, p3, p7, p6, animation, distance);
        renderBeamFace(matrix, normal, builder, r, g, b, p3, p4, p8, p7, animation, distance);
        renderBeamFace(matrix, normal, builder, r, g, b, p4, p1, p5, p8, animation, distance);
        matrixStack.pop();
    }
    private static void renderBeamFace(Matrix4f matrix, Matrix3f normal, IVertexBuilder builder,
                                       float r, float g, float b, Vector3d p1, Vector3d p2,
                                       Vector3d p3, Vector3d p4, float animation, double distance) {
        float texEnd = animation + (float)(distance * 0.1);
        addBeamVertex(matrix, normal, builder, r, g, b, p1, animation, 0);
        addBeamVertex(matrix, normal, builder, r, g, b, p2, animation, 1);
        addBeamVertex(matrix, normal, builder, r, g, b, p3, texEnd, 1);
        addBeamVertex(matrix, normal, builder, r, g, b, p4, texEnd, 0);
    }
    private static void addBeamVertex(Matrix4f matrix, Matrix3f normal, IVertexBuilder builder,
                                      float r, float g, float b, Vector3d pos, float u, float v) {
        builder.pos(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                .color(r, g, b, 1.0f)
                .tex(u, v)
                .overlay(OverlayTexture.NO_OVERLAY)
                .lightmap(15728880)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}