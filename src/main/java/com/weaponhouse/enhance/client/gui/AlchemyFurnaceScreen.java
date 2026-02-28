package com.weaponhouse.enhance.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
@OnlyIn(Dist.CLIENT)
public class AlchemyFurnaceScreen extends ContainerScreen<AlchemyFurnaceContainer> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("enhance", "textures/gui/alchemy_furnace.png");
    public AlchemyFurnaceScreen(AlchemyFurnaceContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.xSize = 176;
        this.ySize = 166;
    }
    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        if (this.minecraft != null) {
            this.minecraft.getTextureManager().bindTexture(TEXTURE);
        }
        int i = this.guiLeft;
        int j = this.guiTop;
        this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize);
        AlchemyFurnaceTileEntity tileEntity = this.container.getTileEntity();
        if (tileEntity.isCrafting()) {
            int progress = tileEntity.getCraftingProgress();
            int totalTime = tileEntity.getCraftingTotalTime();
            int progressWidth = (int) (24.0f * progress / totalTime);
            this.blit(matrixStack, i + 89, j + 34, 176, 0, progressWidth, 17);
            float successChance = tileEntity.getSuccessChance();
            String chanceText = String.format("%.1f%%", successChance * 100);
            this.font.drawString(matrixStack, chanceText, i + 85, j + 20, 0xFFFFFF);
        }
    }
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
        if (isPointInRegion(85, 20, 30, 10, mouseX, mouseY)) {
            AlchemyFurnaceTileEntity tileEntity = this.container.getTileEntity();
            if (tileEntity.isCrafting()) {
                float successChance = tileEntity.getSuccessChance();
                this.renderTooltip(matrixStack,
                        new StringTextComponent("成功概率: " + String.format("%.1f%%", successChance * 100)),
                        mouseX, mouseY);
            }
        }
    }
}