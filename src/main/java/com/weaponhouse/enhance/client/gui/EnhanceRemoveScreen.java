package com.weaponhouse.enhance.client.gui;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.common.EnhanceCommonRules;
import com.weaponhouse.enhance.network.RemoveBuffPacket;
import com.weaponhouse.enhance.network.RequestBuffPacket;
import com.weaponhouse.enhance.network.SacrificeBuffPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
@Mod.EventBusSubscriber (modid = "enhance", value = Dist.CLIENT)
@OnlyIn (Dist.CLIENT)
public class EnhanceRemoveScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("enhance", "textures/gui/enhance_remove.png");
    private static final int DISPLAY_WIDTH = 200;
    private static final int DISPLAY_HEIGHT = 171;
    private int currentPage = 1;
    private int totalBuffs = 0;
    private static final int PAGE_SIZE = 10;
    private static final int[] LEFT_ARROW_SCREEN = {77, 150, 98, 164};
    private static final int[] LEFT_ARROW_BRIGHT_UV = {23, 171, 44, 185};
    private static final int[] RIGHT_ARROW_SCREEN = {103, 150, 124, 164};
    private static final int[] RIGHT_ARROW_BRIGHT_UV = {46, 171, 67, 185};
    private boolean isLeftArrowEnabled = false;
    private boolean isRightArrowEnabled = false;
    private static final int TEXT_X1 = 5;
    private static final int TEXT_Y1 = 4;
    private static final int TEXT_X2 = 193;
    private static final int TEXT_Y2 = 21;
    private static final int TEXT_WIDTH = TEXT_X2 - TEXT_X1;
    private static final int TEXT_HEIGHT = TEXT_Y2 - TEXT_Y1;
    private static final int[][][] BUFF_AREA_COORDINATES = {
            {{8, 28}, {70, 47}}, {{105, 28}, {167, 47}},
            {{8, 54}, {70, 73}}, {{105, 54}, {167, 73}},
            {{8, 78}, {70, 97}}, {{105, 78}, {167, 97}},
            {{8, 102}, {70, 121}}, {{105, 102}, {167, 121}},
            {{8, 126}, {70, 145}}, {{105, 126}, {167, 145}}
    };
    private static final int[][] BUTTON_COORDINATES = {
            {77, 27, 98, 48}, {172, 27, 193, 48},
            {77, 53, 98, 74}, {172, 53, 193, 74},
            {77, 77, 98, 98}, {172, 77, 193, 98},
            {77, 101, 98, 122}, {172, 101, 193, 122},
            {77, 125, 98, 146}, {172, 125, 193, 146}
    };
    private static final int HOVER_TEXTURE_X1 = 0;
    private static final int HOVER_TEXTURE_Y1 = 171;
    private boolean[] isButtonHovered = new boolean[10];
    private static class BuffInfo {
        public String name;
        public String originalName;
        public String level;
        public int color;
        public BuffInfo(String name, String originalName, String level, int color) {
            this.name = name;
            this.originalName = originalName;
            this.level = level;
            this.color = color;
        }
    }
    public static final String KEY_SCREEN_TITLE_LINE1 = "gui.enhance.remove.title.line1";
    public static final String KEY_SCREEN_TITLE_LINE2 = "gui.enhance.remove.title.line2";
    public static final String KEY_NO_BUFF = "gui.enhance.remove.no_buff";
    public static final String KEY_UNKNOWN_PLAYER = "gui.enhance.remove.unknown_player";
    private List<BuffInfo> buffInfos = new ArrayList<>();
    public EnhanceRemoveScreen() {
        super(new StringTextComponent("Enhance Remove"));
        for (int i = 0; i < isButtonHovered.length; i++) {
            isButtonHovered[i] = false;
        }
    }
    public void updateBuffs(List<String> buffs) {
        buffInfos.clear();
        totalBuffs = buffs.size();
        for (String buffString : buffs) {
            String originalName = getOriginalBuffName(buffString);
            String[] parsed = parseBuff(buffString);
            int color = getBuffColor(originalName, parsed[1]);
            buffInfos.add(new BuffInfo(parsed[0], originalName, parsed[1], color));
        }
        if (totalBuffs == 0) {
            buffInfos.add(new BuffInfo(I18n.format(KEY_NO_BUFF), "", "", 0xFFFFFF));
        }
        int totalPages = (totalBuffs + PAGE_SIZE - 1) / PAGE_SIZE;
        if (currentPage > totalPages) {
            currentPage = Math.max(1, totalPages);
        }
        isLeftArrowEnabled = currentPage > 1;
        isRightArrowEnabled = currentPage < totalPages;
    }
    private void renderPageArrows(MatrixStack matrixStack, int guiLeft, int guiTop) {
        if (totalBuffs <= PAGE_SIZE) return;
        Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
        if (isLeftArrowEnabled) {
            int screenX = guiLeft + LEFT_ARROW_SCREEN[0];
            int screenY = guiTop + LEFT_ARROW_SCREEN[1];
            int width = LEFT_ARROW_BRIGHT_UV[2] - LEFT_ARROW_BRIGHT_UV[0];
            int height = LEFT_ARROW_BRIGHT_UV[3] - LEFT_ARROW_BRIGHT_UV[1];
            this.blit(matrixStack, screenX, screenY, LEFT_ARROW_BRIGHT_UV[0], LEFT_ARROW_BRIGHT_UV[1], width, height);
        }
        if (isRightArrowEnabled) {
            int screenX = guiLeft + RIGHT_ARROW_SCREEN[0];
            int screenY = guiTop + RIGHT_ARROW_SCREEN[1];
            int width = RIGHT_ARROW_BRIGHT_UV[2] - RIGHT_ARROW_BRIGHT_UV[0];
            int height = RIGHT_ARROW_BRIGHT_UV[3] - RIGHT_ARROW_BRIGHT_UV[1];
            this.blit(matrixStack, screenX, screenY, RIGHT_ARROW_BRIGHT_UV[0], RIGHT_ARROW_BRIGHT_UV[1], width, height);
        }
    }
    private String[] parseBuff(String buffString) {
        String[] parts = buffString.split("Lv.");
        String originalName = "";
        String level = "";
        if (parts.length >= 1) {
            originalName = parts[0].trim();
            level = parts.length > 1 ? parts[1].trim() : "";
        } else {
            originalName = buffString.trim();
        }
        String localizedName = I18n.format("buff.enhance." + originalName, originalName);
        return new String[]{localizedName, level};
    }
    private int getBuffColor(String buffName, String levelStr) {
        try {
            int level = Integer.parseInt(levelStr);
            if (EnhanceCommonRules.GREEN_GIFT_BUFF_RANGES.containsKey(buffName)) {
                int[] range = EnhanceCommonRules.GREEN_GIFT_BUFF_RANGES.get(buffName);
                if (level >= range[0] && level <= range[1]) return 0x4FFF00;
            }
            if (EnhanceCommonRules.BLUE_GIFT_BUFF_RANGES.containsKey(buffName)) {
                int[] range = EnhanceCommonRules.BLUE_GIFT_BUFF_RANGES.get(buffName);
                if (level >= range[0] && level <= range[1]) return 0x0000FF;
            }
            if (EnhanceCommonRules.RED_GIFT_BUFF_RANGES.containsKey(buffName)) {
                int[] range = EnhanceCommonRules.RED_GIFT_BUFF_RANGES.get(buffName);
                if (level >= range[0] && level <= range[1]) return 0xFF0000;
            }
        } catch (NumberFormatException e) {
        }
        return 0xFFFFFF;
    }
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        int guiLeft = (this.width - DISPLAY_WIDTH) / 2;
        int guiTop = (this.height - DISPLAY_HEIGHT) / 2;
        Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
        this.blit(matrixStack, guiLeft, guiTop, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        renderText(matrixStack, guiLeft, guiTop);
        renderBuffInfo(matrixStack, guiLeft, guiTop);
        renderPageArrows(matrixStack, guiLeft, guiTop);
        updateAllButtonHoverStates(mouseX, mouseY, guiLeft, guiTop);
        renderAllButtonHoverTextures(matrixStack, guiLeft, guiTop);
    }
    private void renderText(MatrixStack matrixStack, int guiLeft, int guiTop) {
        PlayerEntity player = Minecraft.getInstance().player;
        String playerName = player != null ? player.getName().getString() : I18n.format(KEY_UNKNOWN_PLAYER);
        String line1 = I18n.format(KEY_SCREEN_TITLE_LINE1, playerName);
        String line2 = I18n.format(KEY_SCREEN_TITLE_LINE2);
        int textAreaLeft = guiLeft + TEXT_X1;
        int textAreaTop = guiTop + TEXT_Y1;
        int line1Width = this.font.getStringWidth(line1);
        int line2Width = this.font.getStringWidth(line2);
        int line1X = textAreaLeft + (TEXT_WIDTH - line1Width) / 2;
        int line1Y = textAreaTop + (TEXT_HEIGHT / 2 - this.font.FONT_HEIGHT);
        int line2X = textAreaLeft + (TEXT_WIDTH - line2Width) / 2;
        int line2Y = textAreaTop + (TEXT_HEIGHT / 2);
        this.font.drawString(matrixStack, line1, line1X, line1Y, 0XCA20FF);
        this.font.drawString(matrixStack, line2, line2X, line2Y, 0xFF0000);
    }
    private void renderBuffInfo(MatrixStack matrixStack, int guiLeft, int guiTop) {
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        for (int i = 0; i < BUFF_AREA_COORDINATES.length; i++) {
            BuffInfo buffInfo;
            int targetBuffIndex = startIndex + i;
            if (targetBuffIndex < totalBuffs) {
                buffInfo = buffInfos.get(targetBuffIndex);
            } else {
                buffInfo = new BuffInfo("", "", "", 0xFFFFFF);
            }
            if (totalBuffs == 0 && i == 0) {
                buffInfo = new BuffInfo(I18n.format(KEY_NO_BUFF), "", "", 0xFFFFFF);
            }
            int[][] coords = BUFF_AREA_COORDINATES[i];
            int x1 = coords[0][0];
            int y1 = coords[0][1];
            int x2 = coords[1][0];
            int y2 = coords[1][1];
            renderSingleBuff(matrixStack, guiLeft, guiTop, buffInfo.name, buffInfo.level, x1, y1, x2 - x1, y2 - y1, buffInfo.color);
        }
    }
    private void renderSingleBuff(MatrixStack matrixStack, int guiLeft, int guiTop, String name, String level, int xOffset, int yOffset, int width, int height, int color) {
        int buffAreaLeft = guiLeft + xOffset;
        int buffAreaTop = guiTop + yOffset;
        matrixStack.push();
        matrixStack.scale(0.8f, 0.8f, 0.8f);
        float scaledLeft = (buffAreaLeft + width / 2) / 0.8f;
        float scaledTop = buffAreaTop / 0.8f;
        int nameWidth = this.font.getStringWidth(name);
        int nameX = (int) (scaledLeft - nameWidth * 0.8f / 2);
        int nameY = (int) (scaledTop + (height / 2 - this.font.FONT_HEIGHT * 0.8f));
        this.font.drawString(matrixStack, name, nameX, nameY, color);
        if (!level.isEmpty()) {
            String levelText = "Lv." + level;
            int levelWidth = this.font.getStringWidth(levelText);
            int levelX = (int) (scaledLeft - levelWidth * 0.8f / 2);
            int levelY = (int) (scaledTop + (height / 2 + 4));
            this.font.drawString(matrixStack, levelText, levelX, levelY, color);
        }
        matrixStack.pop();
    }
    private boolean checkArrowClick(double mouseX, double mouseY, int guiLeft, int guiTop) {
        if (totalBuffs <= PAGE_SIZE) return false;
        if (isLeftArrowEnabled) {
            int x1 = guiLeft + LEFT_ARROW_SCREEN[0];
            int y1 = guiTop + LEFT_ARROW_SCREEN[1];
            int x2 = guiLeft + LEFT_ARROW_SCREEN[2];
            int y2 = guiTop + LEFT_ARROW_SCREEN[3];
            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                currentPage--;
                playClickSound();
                requestBuffUpdate();
                return true;
            }
        }
        if (isRightArrowEnabled) {
            int x1 = guiLeft + RIGHT_ARROW_SCREEN[0];
            int y1 = guiTop + RIGHT_ARROW_SCREEN[1];
            int x2 = guiLeft + RIGHT_ARROW_SCREEN[2];
            int y2 = guiTop + RIGHT_ARROW_SCREEN[3];
            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                currentPage++;
                playClickSound();
                requestBuffUpdate();
                return true;
            }
        }
        return false;
    }
    private void playClickSound() {
        Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
    private String getOriginalBuffName(String buffString) {
        if (buffString == null || buffString.isEmpty()) return "";
        String[] parts = buffString.split("Lv.");
        return parts.length > 0 ? parts[0].trim() : "";
    }
    private void updateAllButtonHoverStates(double mouseX, double mouseY, int guiLeft, int guiTop) {
        for (int i = 0; i < BUTTON_COORDINATES.length; i++) {
            isButtonHovered[i] = isMouseOverButton(i, mouseX, mouseY, guiLeft, guiTop);
        }
    }
    private void renderAllButtonHoverTextures(MatrixStack matrixStack, int guiLeft, int guiTop) {
        Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
        for (int i = 0; i < BUTTON_COORDINATES.length; i++) {
            if (isButtonHovered[i] && hasBuffAtPosition(i)) {
                int[] btnCoords = BUTTON_COORDINATES[i];
                int btnX1 = btnCoords[0];
                int btnY1 = btnCoords[1];
                int btnWidth = btnCoords[2] - btnCoords[0];
                int btnHeight = btnCoords[3] - btnCoords[1];
                this.blit(matrixStack,
                        guiLeft + btnX1, guiTop + btnY1,
                        HOVER_TEXTURE_X1, HOVER_TEXTURE_Y1,
                        btnWidth, btnHeight);
            }
        }
    }
    private boolean hasBuffAtPosition(int index) {
        if (index < 0 || index >= buffInfos.size()) return false;
        BuffInfo info = buffInfos.get(index);
        return !info.originalName.isEmpty() && !info.name.equals(I18n.format(KEY_NO_BUFF));
    }
    private boolean isMouseOverButton(int buttonIndex, double mouseX, double mouseY, int guiLeft, int guiTop) {
        if (buttonIndex < 0 || buttonIndex >= BUTTON_COORDINATES.length) return false;
        if (!hasBuffAtPosition(buttonIndex)) return false;
        int[] btnCoords = BUTTON_COORDINATES[buttonIndex];
        int btnX1 = guiLeft + btnCoords[0];
        int btnY1 = guiTop + btnCoords[1];
        int btnX2 = guiLeft + btnCoords[2];
        int btnY2 = guiTop + btnCoords[3];

        return mouseX >= btnX1 && mouseX <= btnX2 && mouseY >= btnY1 && mouseY <= btnY2;
    }
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int guiLeft = (this.width - DISPLAY_WIDTH) / 2;
        int guiTop = (this.height - DISPLAY_HEIGHT) / 2;
        updateAllButtonHoverStates(mouseX, mouseY, guiLeft, guiTop);
        super.mouseMoved(mouseX, mouseY);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - DISPLAY_WIDTH) / 2;
        int guiTop = (this.height - DISPLAY_HEIGHT) / 2;
        if (checkArrowClick(mouseX, mouseY, guiLeft, guiTop)) {
            return true;
        }
        for (int i = 0; i < BUTTON_COORDINATES.length; i++) {
            if (isMouseOverButton(i, mouseX, mouseY, guiLeft, guiTop)) {
                playClickSound();
                removeBuffAtPosition(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void removeBuffAtPosition(int index) {
        if (index < 0 || index >= buffInfos.size()) return;
        BuffInfo info = buffInfos.get(index);
        if (info.originalName.isEmpty() || info.name.equals("无")) return;
        PlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        try {
            int buffLevel = info.level.isEmpty() ? 1 : Integer.parseInt(info.level);
            String buffId = info.originalName;
            if (EnhanceCommonRules.isSacrificeableBuff(buffId)) {
                Enhance.sendToServer(new SacrificeBuffPacket(buffId, buffLevel));
            } else {
                Enhance.sendToServer(new RemoveBuffPacket(buffId));
            }
        } catch (NumberFormatException e) {
            Enhance.sendToServer(new RemoveBuffPacket(info.originalName));
            player.sendMessage(
                    new StringTextComponent(TextFormatting.RED + "词条等级异常，已普通移除"),
                    player.getUniqueID()
            );
        }
    }

    private void requestBuffUpdate() {
        Enhance.sendToServer(new RequestBuffPacket());
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}