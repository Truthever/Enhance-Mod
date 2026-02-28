package com.weaponhouse.enhance.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.common.EnhanceCommonRules;
import com.weaponhouse.enhance.network.ExchangeBuffPacket;
import com.weaponhouse.enhance.network.PlayerInteractionStatePacket;
import com.weaponhouse.enhance.network.RequestBuffPacket;
import com.weaponhouse.enhance.network.RequestOtherBuffPacket;
import com.weaponhouse.enhance.session.BlockSession;
import com.weaponhouse.enhance.session.ClientBlockSession;
import com.weaponhouse.enhance.session.ClientSessionManager;
import com.weaponhouse.enhance.session.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
@OnlyIn (Dist.CLIENT)
public class ReplaceScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("enhance", "textures/gui/replace.png");
    private static final int DISPLAY_WIDTH = 197;
    private static final int DISPLAY_HEIGHT = 167;
    private final BlockPos blockPos;
    private BlockSession currentSession;
    private int currentPage = 1;
    private int totalBuffs = 0;
    private int otherCurrentPage = 1;
    private int totalOtherBuffs = 0;
    private static final int PAGE_SIZE = 5;
    private static final int[][] MID_LEFT_BUTTONS = {
            {81, 35, 97, 46}, {81, 59, 97, 70}, {81, 82, 97, 93}, {81, 107, 97, 118}, {81, 130, 97, 141}
    };
    private static final int[][] MID_RIGHT_BUTTONS = {
            {101, 35, 117, 46}, {101, 59, 117, 70}, {101, 82, 117, 93}, {101, 107, 117, 118}, {101, 130, 117, 141}
    };
    private final boolean[][] isExchangeBtnEnabled = new boolean[5][2];
    private static final int[] CURRENT_LEFT_ARROW_SCREEN = {7, 150, 28, 164};
    private static final int[] CURRENT_RIGHT_ARROW_SCREEN = {33, 150, 56, 164};
    private static final int[] OTHER_LEFT_ARROW_SCREEN = {145, 150, 166, 164};
    private static final int[] OTHER_RIGHT_ARROW_SCREEN = {171, 150, 192, 164};
    private static final int[] LEFT_ARROW_BRIGHT_UV = {0, 171, 21, 185};
    private static final int[] RIGHT_ARROW_BRIGHT_UV = {22, 171, 43, 185};
    private static final int LEFT_TITLE_X_OFFSET = 7;
    private static final int LEFT_TITLE_FIRST_LINE_Y = 10;
    private static final int LEFT_TITLE_SECOND_LINE_Y = 20;
    private static final int RIGHT_TITLE_X_OFFSET = 120;
    private static final int RIGHT_TITLE_FIRST_LINE_Y = 10;
    private static final int RIGHT_TITLE_SECOND_LINE_Y = 20;
    private static final float TITLE_SCALE = 0.7f;
    private static final int CURRENT_TITLE_COLOR = 0xFFFFAA00;
    private static final int OTHER_TITLE_COLOR = 0xAAAAFF;
    private boolean isCurrentLeftArrowEnabled = false;
    private boolean isCurrentRightArrowEnabled = false;
    private boolean isOtherLeftArrowEnabled = false;
    private boolean isOtherRightArrowEnabled = false;
    private static final int[][] LEFT_TEXT_BOX_COORDINATES = {
            {7, 29, 78, 50}, {7, 53, 78, 74}, {7, 77, 78, 98}, {7, 101, 78, 122}, {7, 125, 78, 146}
    };
    private static final int[][] RIGHT_TEXT_BOX_COORDINATES = {
            {120, 29, 191, 50}, {120, 53, 191, 74}, {120, 77, 191, 98}, {120, 101, 191, 122}, {120, 125, 191, 146}
    };
    private static final int[] EXCHANGE_LEFT_BTN_UV = {44, 171, 60, 182};
    private static final int[] EXCHANGE_RIGHT_BTN_UV = {61, 171, 77, 182};
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
    private final List<BuffInfo> currentBuffInfos = new ArrayList<>();
    private final List<BuffInfo> otherBuffInfos = new ArrayList<>();
    private final PlayerEntity currentPlayer;
    private UUID otherPlayerUUID = null;
    private boolean isClosed = false;
    private Thread otherBuffSyncThread = null;
    private final SessionData sessionData = new SessionData();
    @OnlyIn (Dist.CLIENT)
    public ReplaceScreen(BlockPos blockPos) {
        super(new TranslationTextComponent("gui.enhance.replace.title"));
        this.currentPlayer = Minecraft.getInstance().player;
        this.blockPos = blockPos;
        this.currentSession = SessionManager.getInstance().getSession(blockPos);
        Enhance.sendToServer(new RequestBuffPacket());
        updateSessionPlayers();
        startOtherBuffSyncTask();
    }
    @Override
    protected void init() {
        super.init();
        Enhance.sendToServer(new PlayerInteractionStatePacket(blockPos, true));
    }
    private void startOtherBuffSyncTask() {
        if (currentPlayer == null || blockPos == null) return;
        Minecraft.getInstance().enqueue(() -> {
            otherBuffSyncThread = new Thread(() -> {
                while (!isClosed && !Thread.currentThread().isInterrupted()) {
                    if (otherPlayerUUID != null) {
                        requestOtherBuff();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "OtherBuffSyncThread");
            otherBuffSyncThread.start();
        });
    }
    private void requestOtherBuff() {
        if (blockPos != null && currentPlayer != null) {
            Enhance.sendToServer(new RequestOtherBuffPacket(blockPos, currentPlayer.getUniqueID()));
        }
    }
    public void updateOtherBuffs(List<String> buffs, UUID targetPlayerUUID) {
        this.otherPlayerUUID = targetPlayerUUID;
        otherBuffInfos.clear();
        totalOtherBuffs = 0;
        for (String buffString : buffs) {
            String originalName = getOriginalBuffName(buffString);
            if ("enhance_level".equals(originalName)) continue;
            String[] parsed = parseBuff(buffString);
            int color = getBuffColor(originalName, parsed[1]);
            otherBuffInfos.add(new BuffInfo(parsed[0], originalName, parsed[1], color));
            totalOtherBuffs++;
        }
        if (totalOtherBuffs == 0 && otherPlayerUUID != null) {
            otherBuffInfos.add(new BuffInfo(
                    new TranslationTextComponent("gui.enhance.replace.no_buff").getString(),
                    "", "", 0xFFFFFF
            ));
        }
        int totalOtherPages = (totalOtherBuffs + PAGE_SIZE - 1) / PAGE_SIZE;
        if (otherCurrentPage > totalOtherPages) {
            otherCurrentPage = Math.max(1, totalOtherPages);
        }
        isOtherLeftArrowEnabled = otherCurrentPage > 1;
        isOtherRightArrowEnabled = otherCurrentPage < totalOtherPages;
    }
    public void updateCurrentBuffs(List<String> buffs) {
        currentBuffInfos.clear();
        totalBuffs = 0;
        for (String buffString : buffs) {
            String originalName = getOriginalBuffName(buffString);
            if ("enhance_level".equals(originalName)) continue;
            String[] parsed = parseBuff(buffString);
            int color = getBuffColor(originalName, parsed[1]);
            currentBuffInfos.add(new BuffInfo(parsed[0], originalName, parsed[1], color));
            totalBuffs++;
        }
        if (totalBuffs == 0) {
            currentBuffInfos.add(new BuffInfo(
                    new TranslationTextComponent("gui.enhance.replace.no_buff").getString(),
                    "", "", 0xFFFFFF
            ));
        }
        int totalPages = (totalBuffs + PAGE_SIZE - 1) / PAGE_SIZE;
        if (currentPage > totalPages) {
            currentPage = Math.max(1, totalPages);
        }
        isCurrentLeftArrowEnabled = currentPage > 1;
        isCurrentRightArrowEnabled = currentPage < totalPages;
    }
    private void updateSessionPlayers() {
        if (blockPos != null && currentPlayer != null) {
            ClientBlockSession session = ClientSessionManager.getInstance().getSession(blockPos);
            if (session != null) {
                Set<UUID> allPlayers = session.getPlayers();
                UUID newOtherUUID = allPlayers.stream()
                        .filter(uuid -> !uuid.equals(currentPlayer.getUniqueID()))
                        .findFirst()
                        .orElse(null);
                if (!Objects.equals(newOtherUUID, otherPlayerUUID)) {
                    otherPlayerUUID = newOtherUUID;
                    otherBuffInfos.clear();
                    totalOtherBuffs = 0;
                    otherCurrentPage = 1;
                }
            }
        }
    }
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        int guiLeft = (this.width - DISPLAY_WIDTH) / 2;
        int guiTop = (this.height - DISPLAY_HEIGHT) / 2;
        if (this.minecraft != null) {
            this.minecraft.getTextureManager().bindTexture(TEXTURE);
        }
        this.blit(matrixStack, guiLeft, guiTop, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        updateSessionPlayers();
        updateExchangeBtnStates();
        renderLeftTitle(matrixStack, guiLeft, guiTop);
        renderRightTitle(matrixStack, guiLeft, guiTop);
        renderBothSideBuffs(matrixStack, guiLeft, guiTop);
        renderBothSideArrows(matrixStack, guiLeft, guiTop);
        renderExchangeButtons(matrixStack, guiLeft, guiTop);
    }
    private void renderLeftTitle(MatrixStack matrixStack, int guiLeft, int guiTop) {
        if (currentPlayer == null) return;
        ITextComponent firstLineComp = new StringTextComponent("[" + currentPlayer.getName().getString() + "]");
        ITextComponent secondLineComp = new TranslationTextComponent("gui.enhance.replace.left_title_line2");
        int titleScreenX = guiLeft + LEFT_TITLE_X_OFFSET;
        int firstLineScreenY = guiTop + LEFT_TITLE_FIRST_LINE_Y;
        int secondLineScreenY = guiTop + LEFT_TITLE_SECOND_LINE_Y;
        matrixStack.push();
        matrixStack.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        float scaledFirstLineY = firstLineScreenY / TITLE_SCALE;
        float scaledSecondLineY = secondLineScreenY / TITLE_SCALE;
        int firstLineX = (int) (titleScreenX / TITLE_SCALE);
        int secondLineX = (int) (titleScreenX / TITLE_SCALE);
        this.font.drawString(matrixStack, firstLineComp.getString(), firstLineX, scaledFirstLineY, CURRENT_TITLE_COLOR);
        this.font.drawString(matrixStack, secondLineComp.getString(), secondLineX, scaledSecondLineY, CURRENT_TITLE_COLOR);
        matrixStack.pop();
    }
    private void renderRightTitle(MatrixStack matrixStack, int guiLeft, int guiTop) {
        String playerName;
        if (otherPlayerUUID != null && Minecraft.getInstance().world != null) {
            PlayerEntity otherPlayer = Minecraft.getInstance().world.getPlayerByUuid(otherPlayerUUID);
            playerName = otherPlayer != null ? otherPlayer.getName().getString() :
                    new TranslationTextComponent("gui.enhance.display.unknown_player").getString();
        } else {
            playerName = new TranslationTextComponent("gui.enhance.display.unknown_player").getString();
        }
        ITextComponent firstLineComp = new StringTextComponent("[" + playerName + "]");
        ITextComponent secondLineComp = new TranslationTextComponent("gui.enhance.replace.right_title_line2");
        int titleScreenX = guiLeft + RIGHT_TITLE_X_OFFSET;
        int firstLineScreenY = guiTop + RIGHT_TITLE_FIRST_LINE_Y;
        int secondLineScreenY = guiTop + RIGHT_TITLE_SECOND_LINE_Y;
        matrixStack.push();
        matrixStack.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        float scaledFirstLineY = firstLineScreenY / TITLE_SCALE;
        float scaledSecondLineY = secondLineScreenY / TITLE_SCALE;
        int firstLineX = (int) (titleScreenX / TITLE_SCALE);
        int secondLineX = (int) (titleScreenX / TITLE_SCALE);
        this.font.drawString(matrixStack, firstLineComp.getString(), firstLineX, scaledFirstLineY, OTHER_TITLE_COLOR);
        this.font.drawString(matrixStack, secondLineComp.getString(), secondLineX, scaledSecondLineY, OTHER_TITLE_COLOR);
        matrixStack.pop();
    }
    private void renderExchangeButtons(MatrixStack matrixStack, int guiLeft, int guiTop) {
        if (this.minecraft != null) {
            this.minecraft.getTextureManager().bindTexture(TEXTURE);
        }
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (isExchangeBtnEnabled[i][0]) {
                int[] btnCoords = MID_LEFT_BUTTONS[i];
                int screenX = guiLeft + btnCoords[0];
                int screenY = guiTop + btnCoords[1];
                int btnWidth = btnCoords[2] - btnCoords[0];
                int btnHeight = btnCoords[3] - btnCoords[1];
                this.blit(matrixStack, screenX, screenY,
                        EXCHANGE_LEFT_BTN_UV[0], EXCHANGE_LEFT_BTN_UV[1],
                        btnWidth, btnHeight);
            }
            if (isExchangeBtnEnabled[i][1]) {
                int[] btnCoords = MID_RIGHT_BUTTONS[i];
                int screenX = guiLeft + btnCoords[0];
                int screenY = guiTop + btnCoords[1];
                int btnWidth = btnCoords[2] - btnCoords[0];
                int btnHeight = btnCoords[3] - btnCoords[1];
                this.blit(matrixStack, screenX, screenY,
                        EXCHANGE_RIGHT_BTN_UV[0], EXCHANGE_RIGHT_BTN_UV[1],
                        btnWidth, btnHeight);
            }
        }
    }
    private void renderBothSideBuffs(MatrixStack matrixStack, int guiLeft, int guiTop) {
        renderSingleSideBuffs(matrixStack, guiLeft, guiTop,
                currentBuffInfos, totalBuffs, currentPage,
                LEFT_TEXT_BOX_COORDINATES);
        if (otherPlayerUUID != null) {
            renderSingleSideBuffs(matrixStack, guiLeft, guiTop,
                    otherBuffInfos, totalOtherBuffs, otherCurrentPage,
                    RIGHT_TEXT_BOX_COORDINATES);
        } else {
            renderWaitingForPlayer(matrixStack, guiLeft, guiTop);
        }
    }
    private void renderWaitingForPlayer(MatrixStack matrixStack, int guiLeft, int guiTop) {
        int[] coords = RIGHT_TEXT_BOX_COORDINATES[0];
        int x1 = coords[0];
        int y1 = coords[1];
        int width = coords[2] - coords[0];
        int height = coords[3] - coords[1];
        int buffAreaLeft = guiLeft + x1;
        int buffAreaTop = guiTop + y1;
        matrixStack.push();
        matrixStack.scale(0.8f, 0.8f, 0.8f);
        float scaledLeft = (buffAreaLeft + (float) width / 2) / 0.8f;
        float scaledTop = buffAreaTop / 0.8f;
        ITextComponent waitingTextComp;
        if (totalOtherBuffs > 0) {
            waitingTextComp = new TranslationTextComponent("gui.enhance.replace.player_left");
        } else {
            waitingTextComp = new TranslationTextComponent("gui.enhance.replace.waiting_for_player");
        }
        int textColor = totalOtherBuffs > 0 ? 0xFF555555 : 0xAAAAAA; // 退出：浅红，等待：浅灰
        int textWidth = this.font.getStringWidth(waitingTextComp.getString());
        int textX = (int) (scaledLeft - (textWidth * 0.8f) / 2);
        int textY = (int) (scaledTop + ((float) height / 2 - this.font.FONT_HEIGHT * 0.8f));
        this.font.drawString(matrixStack, waitingTextComp.getString(), textX, textY, textColor);
        matrixStack.pop();
    }
    private void renderSingleSideBuffs(MatrixStack matrixStack, int guiLeft, int guiTop,
                                       List<BuffInfo> buffList, int total, int currentPage,
                                       int[][] textBoxes) {
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            BuffInfo buffInfo;
            int targetIndex = startIndex + i;
            if (targetIndex < total) {
                buffInfo = buffList.get(targetIndex);
            } else {
                buffInfo = new BuffInfo("", "", "", 0xFFFFFF);
            }
            int[] coords = textBoxes[i];
            int x1 = coords[0];
            int y1 = coords[1];
            int x2 = coords[2];
            int y2 = coords[3];
            int width = x2 - x1;
            int height = y2 - y1;
            renderSingleBuff(matrixStack, guiLeft, guiTop,
                    buffInfo.name, buffInfo.level,
                    x1, y1, width, height, buffInfo.color);
        }
    }
    private void renderSingleBuff(MatrixStack matrixStack, int guiLeft, int guiTop, String name,
                                  String level, int xOffset, int yOffset, int width, int height, int color) {
        int buffAreaLeft = guiLeft + xOffset;
        int buffAreaTop = guiTop + yOffset;
        matrixStack.push();
        matrixStack.scale(0.8f, 0.8f, 0.8f);
        float scaledLeft = (buffAreaLeft + (float) width / 2) / 0.8f;
        float scaledTop = buffAreaTop / 0.8f;
        int nameWidth = this.font.getStringWidth(name);
        int nameX = (int) (scaledLeft - (nameWidth * 0.8f) / 2);
        int nameY = (int) (scaledTop + ((float) height / 2 - this.font.FONT_HEIGHT * 0.8f));
        this.font.drawString(matrixStack, name, nameX, nameY, color);
        if (!level.isEmpty()) {
            ITextComponent levelComp = new TranslationTextComponent("gui.enhance.replace.level_prefix", level);
            int levelWidth = this.font.getStringWidth(levelComp.getString());
            int levelX = (int) (scaledLeft - (levelWidth * 0.8f) / 2);
            int levelY = nameY + this.font.FONT_HEIGHT + 2;
            this.font.drawString(matrixStack, levelComp.getString(), levelX, levelY, color);
        }
        matrixStack.pop();
    }
    private void renderBothSideArrows(MatrixStack matrixStack, int guiLeft, int guiTop) {
        renderSingleSideArrows(matrixStack, guiLeft, guiTop,
                totalBuffs, isCurrentLeftArrowEnabled, isCurrentRightArrowEnabled,
                CURRENT_LEFT_ARROW_SCREEN, CURRENT_RIGHT_ARROW_SCREEN);
        renderSingleSideArrows(matrixStack, guiLeft, guiTop,
                totalOtherBuffs, isOtherLeftArrowEnabled, isOtherRightArrowEnabled,
                OTHER_LEFT_ARROW_SCREEN, OTHER_RIGHT_ARROW_SCREEN);
    }
    private void renderSingleSideArrows(MatrixStack matrixStack, int guiLeft, int guiTop,
                                        int total, boolean leftEnabled, boolean rightEnabled,
                                        int[] leftScreen, int[] rightScreen) {
        if (total <= PAGE_SIZE) return;
        if (this.minecraft != null) {
            this.minecraft.getTextureManager().bindTexture(TEXTURE);
        }
        if (leftEnabled) {
            int screenX = guiLeft + leftScreen[0];
            int screenY = guiTop + leftScreen[1];
            int width = leftScreen[2] - leftScreen[0];
            int height = leftScreen[3] - leftScreen[1];
            this.blit(matrixStack, screenX, screenY,
                    LEFT_ARROW_BRIGHT_UV[0], LEFT_ARROW_BRIGHT_UV[1],
                    width, height);
        }
        if (rightEnabled) {
            int screenX = guiLeft + rightScreen[0];
            int screenY = guiTop + rightScreen[1];
            int width = rightScreen[2] - rightScreen[0];
            int height = rightScreen[3] - rightScreen[1];
            this.blit(matrixStack, screenX, screenY,
                    RIGHT_ARROW_BRIGHT_UV[0], RIGHT_ARROW_BRIGHT_UV[1],
                    width, height);
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - DISPLAY_WIDTH) / 2;
        int guiTop = (this.height - DISPLAY_HEIGHT) / 2;
        if (handleExchangeBtnClick(mouseX, mouseY, guiLeft, guiTop)) {
            return true;
        }
        if (checkSingleSideArrowClick(mouseX, mouseY, guiLeft, guiTop,
                totalBuffs, isCurrentLeftArrowEnabled, isCurrentRightArrowEnabled,
                CURRENT_LEFT_ARROW_SCREEN, CURRENT_RIGHT_ARROW_SCREEN, false)) {
            return true;
        }
        if (checkSingleSideArrowClick(mouseX, mouseY, guiLeft, guiTop,
                totalOtherBuffs, isOtherLeftArrowEnabled, isOtherRightArrowEnabled,
                OTHER_LEFT_ARROW_SCREEN, OTHER_RIGHT_ARROW_SCREEN, true)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private boolean handleExchangeBtnClick(double mouseX, double mouseY, int guiLeft, int guiTop) {
        if (currentPlayer == null || otherPlayerUUID == null) return false;
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (isExchangeBtnEnabled[i][0]) {
                int[] btnCoords = MID_LEFT_BUTTONS[i];
                int x1 = guiLeft + btnCoords[0];
                int y1 = guiTop + btnCoords[1];
                int x2 = guiLeft + btnCoords[2];
                int y2 = guiTop + btnCoords[3];
                if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                    playClickSound();
                    sendExchangeRequest(i, ExchangeBuffPacket.ExchangeDirection.RIGHT_TO_LEFT);
                    return true;
                }
            }
            if (isExchangeBtnEnabled[i][1]) {
                int[] btnCoords = MID_RIGHT_BUTTONS[i];
                int x1 = guiLeft + btnCoords[0];
                int y1 = guiTop + btnCoords[1];
                int x2 = guiLeft + btnCoords[2];
                int y2 = guiTop + btnCoords[3];
                if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                    playClickSound();
                    sendExchangeRequest(i, ExchangeBuffPacket.ExchangeDirection.LEFT_TO_RIGHT);
                    return true;
                }
            }
        }
        return false;
    }
    private void sendExchangeRequest(int rowIndex, ExchangeBuffPacket.ExchangeDirection direction) {
        int currentBuffIdx = (currentPage - 1) * PAGE_SIZE + rowIndex;
        int otherBuffIdx = (otherCurrentPage - 1) * PAGE_SIZE + rowIndex;
        if (currentBuffIdx >= currentBuffInfos.size() || otherBuffIdx >= otherBuffInfos.size()) return;
        BuffInfo currentBuff = currentBuffInfos.get(currentBuffIdx);
        BuffInfo otherBuff = otherBuffInfos.get(otherBuffIdx);
        if (currentBuff.originalName.isEmpty() || otherBuff.originalName.isEmpty()) return;
        Enhance.sendToServer(new ExchangeBuffPacket(
                blockPos,
                currentPlayer.getUniqueID(),
                otherPlayerUUID,
                rowIndex,
                direction,
                currentBuff.originalName,
                otherBuff.originalName
        ));
    }
    private boolean checkSingleSideArrowClick(double mouseX, double mouseY, int guiLeft, int guiTop,
                                              int total, boolean leftEnabled, boolean rightEnabled,
                                              int[] leftScreen, int[] rightScreen, boolean isOther) {
        if (total <= PAGE_SIZE) return false;
        if (leftEnabled) {
            int x1 = guiLeft + leftScreen[0];
            int y1 = guiTop + leftScreen[1];
            int x2 = guiLeft + leftScreen[2];
            int y2 = guiTop + leftScreen[3];
            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                if (isOther) otherCurrentPage--;
                else currentPage--;
                playClickSound();
                requestBuffUpdate(isOther);
                return true;
            }
        }
        if (rightEnabled) {
            int x1 = guiLeft + rightScreen[0];
            int y1 = guiTop + rightScreen[1];
            int x2 = guiLeft + rightScreen[2];
            int y2 = guiTop + rightScreen[3];
            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                if (isOther) otherCurrentPage++;
                else currentPage++;
                playClickSound();
                requestBuffUpdate(isOther);
                return true;
            }
        }
        return false;
    }
    private void playClickSound() {
        Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
    private void requestBuffUpdate(boolean isOther) {
        if (isOther) {
            if (otherPlayerUUID != null) requestOtherBuff();
        } else {
            Enhance.sendToServer(new RequestBuffPacket());
        }
    }
    private String getOriginalBuffName(String buffString) {
        if (buffString == null || buffString.isEmpty()) return "";
        String[] parts = buffString.split("Lv.");
        return parts.length > 0 ? parts[0].trim() : "";
    }
    private String[] parseBuff(String buffString) {
        String[] parts = buffString.split("Lv.");
        String originalName;
        String level = "";
        if (parts.length >= 1) {
            originalName = parts[0].trim();
            level = parts.length > 1 ? parts[1].trim() : "";
        } else {
            originalName = buffString.trim();
        }
        ITextComponent translatedComp = new TranslationTextComponent("buff.enhance." + originalName);
        String translatedName = translatedComp.getString().equals("buff.enhance." + originalName)
                ? originalName : translatedComp.getString();
        return new String[]{translatedName, level};
    }
    private int getBuffColor(String buffName, String levelStr) {
        try {
            int level = Integer.parseInt(levelStr);
            if (isGreenBuffLevel(buffName, level)) return 0x4FFF00;
            if (isBlueBuffLevel(buffName, level)) return 0x0000FF;
            if (isRedBuffLevel(buffName, level)) return 0xFF0000;
        } catch (NumberFormatException ignored) {}
        return 0xFFFFFF;
    }
    private boolean isGreenBuffLevel(String buffName, int level) {
        int[] range = EnhanceCommonRules.GREEN_GIFT_BUFF_RANGES.get(buffName);
        return range != null && level >= range[0] && level <= range[1];
    }
    private boolean isBlueBuffLevel(String buffName, int level) {
        int[] range = EnhanceCommonRules.BLUE_GIFT_BUFF_RANGES.get(buffName);
        return range != null && level >= range[0] && level <= range[1];
    }
    private boolean isRedBuffLevel(String buffName, int level) {
        int[] range = EnhanceCommonRules.RED_GIFT_BUFF_RANGES.get(buffName);
        return range != null && level >= range[0] && level <= range[1];
    }
    @Override
    public void onClose() {
        if (currentPlayer != null && blockPos != null && !isClosed) {
            SessionManager.getInstance().leaveSession(blockPos, currentPlayer.getUniqueID());
            isClosed = true;
        }
        if (otherBuffSyncThread != null && !otherBuffSyncThread.isInterrupted()) {
            otherBuffSyncThread.interrupt();
        }
        Enhance.sendToServer(new PlayerInteractionStatePacket(blockPos, false));
        super.onClose();
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    private void updateExchangeBtnStates() {
        if (otherPlayerUUID == null) {
            for (int i = 0; i < PAGE_SIZE; i++) {
                isExchangeBtnEnabled[i][0] = false;
                isExchangeBtnEnabled[i][1] = false;
            }
            return;
        }
        boolean bothInteracting = sessionData.isBothInteracting();
        if (!bothInteracting) {
            for (int i = 0; i < PAGE_SIZE; i++) {
                isExchangeBtnEnabled[i][0] = false;
                isExchangeBtnEnabled[i][1] = false;
            }
            return;
        }
        int currentStartIdx = (currentPage - 1) * PAGE_SIZE;
        int otherStartIdx = (otherCurrentPage - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            boolean currentHasValidBuff = false;
            int currentBuffIdx = currentStartIdx + i;
            if (currentBuffIdx < currentBuffInfos.size()) {
                BuffInfo currentBuff = currentBuffInfos.get(currentBuffIdx);
                currentHasValidBuff = !currentBuff.name.isEmpty()
                        && !currentBuff.name.equals(new TranslationTextComponent("gui.enhance.replace.no_buff").getString())
                        && !currentBuff.originalName.isEmpty();
            }
            boolean otherHasValidBuff = false;
            int otherBuffIdx = otherStartIdx + i;
            if (otherBuffIdx < otherBuffInfos.size()) {
                BuffInfo otherBuff = otherBuffInfos.get(otherBuffIdx);
                otherHasValidBuff = !otherBuff.name.isEmpty()
                        && !otherBuff.name.equals(new TranslationTextComponent("gui.enhance.replace.no_buff").getString())
                        && !otherBuff.originalName.isEmpty();
            }
            isExchangeBtnEnabled[i][0] = currentHasValidBuff || otherHasValidBuff;
            isExchangeBtnEnabled[i][1] = currentHasValidBuff || otherHasValidBuff;
        }
    }
    public void handleSessionStateUpdate(Map<UUID, Boolean> interactionStates, UUID disconnectedPlayer) {
        sessionData.updateInteractionStates(interactionStates);
        if (disconnectedPlayer != null) {
            if (disconnectedPlayer.equals(otherPlayerUUID)) {
                ClientSessionManager.getInstance().removePlayerFromSession(blockPos, disconnectedPlayer);
                otherBuffInfos.clear();
                totalOtherBuffs = 0;
                otherCurrentPage = 1;
                isOtherLeftArrowEnabled = false;
                isOtherRightArrowEnabled = false;
                otherPlayerUUID = null;
                Minecraft.getInstance().enqueue(() -> {
                    if (otherBuffSyncThread != null && !otherBuffSyncThread.isInterrupted()) {
                        otherBuffSyncThread.interrupt();
                    }
                });
            }
            else if (currentPlayer != null && disconnectedPlayer.equals(currentPlayer.getUniqueID())) {
                this.onClose();
            }
            updateExchangeBtnStates();
        }
    }
    private static class SessionData {
        private final Map<UUID, Boolean> interactionStates = new HashMap<>();
        public void updateInteractionStates(Map<UUID, Boolean> newStates) {
            interactionStates.clear();
            interactionStates.putAll(newStates);
        }
        public boolean isBothInteracting() {
            if (interactionStates.size() != 2) return false;
            boolean[] states = new boolean[2];
            int index = 0;
            for (boolean state : interactionStates.values()) {
                states[index++] = state;
            }
            return states[0] && states[1];
        }
    }
}