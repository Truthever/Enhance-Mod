package com.weaponhouse.enhance.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.weaponhouse.enhance.Enhance;
import com.weaponhouse.enhance.network.SetSpeedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@OnlyIn (Dist.CLIENT)
public class SpeedReducerScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("enhance", "textures/gui/speed_reducer.png");
    private static final int DISPLAY_WIDTH = 169;
    private static final int DISPLAY_HEIGHT = 48;
    private static final int[][] LEFT_DISPLAY_BOXES = {
            {11, 20, 19, 35},
            {29, 20, 37, 35},
            {43, 20, 51, 35},
            {57, 20, 65, 35}
    };
    private static final int[][] RIGHT_INPUT_BOXES = {
            {98, 20, 106, 35},
            {116, 20, 124, 35},
            {130, 20, 138, 35},
            {144, 20, 152, 35}
    };
    private static final int[] ARROW_AREA = {71, 21, 92, 34};
    private static final int[] DARK_ARROW_UV = {0, 48, 21, 61};
    private static final int[] BRIGHT_ARROW_UV = {71, 21, 92, 34};
    private final List<TextFieldWidget> rightInputFields = new ArrayList<>();
    private float currentSpeed;
    private int guiLeft;
    private int guiTop;
    private int successMessageTimer = 0;
    private String successMessage = "";
    @OnlyIn (Dist.CLIENT)
    public SpeedReducerScreen() {
        super(new StringTextComponent("Speed Reducer"));
    }
    @Override
    protected void init() {
        super.init();
        guiLeft = (this.width - DISPLAY_WIDTH) / 2;
        guiTop = (this.height - DISPLAY_HEIGHT) / 2;
        updateCurrentSpeedFromPlayer();
        rightInputFields.clear();
        for (int i = 0; i < RIGHT_INPUT_BOXES.length; i++) {
            int[] coords = RIGHT_INPUT_BOXES[i];
            int x = guiLeft + coords[0];
            int y = guiTop + coords[1];
            int width = coords[2] - coords[0];
            int height = coords[3] - coords[1];
            TextFieldWidget textField = new TextFieldWidget(
                    this.font, x, y, width, height, new StringTextComponent("")
            );
            textField.setMaxStringLength(1);
            textField.setTextColor(0xFFFFFF);
            textField.setEnableBackgroundDrawing(false);
            final int index = i;
            textField.setResponder(s -> validateInput(index));
            rightInputFields.add(textField);
            this.addButton(textField);
        }
        populateDefaultValues();
        successMessageTimer = 0;
        successMessage = "";
    }
    private void updateCurrentSpeedFromPlayer() {
        PlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            if (player.getPersistentData().contains("BaseMovementSpeed")) {
                currentSpeed = player.getPersistentData().getFloat("BaseMovementSpeed");
            } else {
                currentSpeed = (float) Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue();
            }
            currentSpeed = Math.max(0.0F, Math.min(1.0F, currentSpeed));
        } else {
            currentSpeed = 0.1F;
        }
    }
    public void updateCurrentSpeed(float speed) {
        this.currentSpeed = Math.max(0.0F, Math.min(1.0F, speed));
    }
    private void populateDefaultValues() {
        String speedStr = String.format("%.3f", currentSpeed);
        String[] parts = speedStr.split("\\.");
        rightInputFields.get(0).setText(parts.length > 0 ? parts[0] : "0");
        String decimalPart = parts.length > 1 ? parts[1] : "000";
        for (int i = 0; i < 3; i++) {
            rightInputFields.get(i + 1).setText(
                    i < decimalPart.length() ? String.valueOf(decimalPart.charAt(i)) : "0"
            );
        }
    }
    private void validateInput(int index) {
        TextFieldWidget field = rightInputFields.get(index);
        String text = field.getText();
        if (!text.isEmpty() && !Character.isDigit(text.charAt(0))) {
            field.setText("");
        }
        if (index == 0 && !text.isEmpty()) {
            try {
                int value = Integer.parseInt(text);
                if (value < 0 || value > 1) {
                    field.setText("0");
                }
            } catch (NumberFormatException e) {
                field.setText("0");
            }
        }
    }
    @Override
    public void tick() {
        super.tick();
        if (successMessageTimer > 0) {
            successMessageTimer--;
            if (successMessageTimer == 0) {
                successMessage = "";
            }
        }
    }
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
        this.blit(matrixStack, guiLeft, guiTop, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        String tipText = I18n.format("enhance.speed_reducer.tip");
        drawCenteredString(matrixStack, font, tipText, this.width / 2, guiTop - 15, 0xFFFFFF);
        if (!successMessage.isEmpty()) {
            drawCenteredString(matrixStack, font, successMessage, this.width / 2, guiTop - 30, 0x00FF00);
        }
        String currentLabel = I18n.format("enhance.speed_reducer.current");
        String targetLabel = I18n.format("enhance.speed_reducer.target");
        drawString(matrixStack, font, currentLabel,
                guiLeft + LEFT_DISPLAY_BOXES[0][0],
                guiTop + LEFT_DISPLAY_BOXES[0][1] - 10, 0xFFFFFF);
        drawString(matrixStack, font, targetLabel,
                guiLeft + RIGHT_INPUT_BOXES[0][0],
                guiTop + RIGHT_INPUT_BOXES[0][1] - 10, 0xFFFFFF);
        renderCurrentSpeed(matrixStack);
        renderArrow(matrixStack, mouseX, mouseY);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
    private void renderCurrentSpeed(MatrixStack matrixStack) {
        String speedStr = String.format("%.3f", currentSpeed);
        String[] parts = speedStr.split("\\.");
        String integerPart = parts.length > 0 ? parts[0] : "0";
        drawCenteredString(matrixStack, font, integerPart,
                guiLeft + (LEFT_DISPLAY_BOXES[0][0] + LEFT_DISPLAY_BOXES[0][2]) / 2,
                guiTop + LEFT_DISPLAY_BOXES[0][1] + 3, 0xFFFFFF);
        StringBuilder decimalPart = new StringBuilder(parts.length > 1 ? parts[1] : "000");
        while (decimalPart.length() < 3) decimalPart.append("0");
        for (int i = 0; i < 3; i++) {
            drawCenteredString(matrixStack, font,
                    String.valueOf(decimalPart.charAt(i)),
                    guiLeft + (LEFT_DISPLAY_BOXES[i + 1][0] + LEFT_DISPLAY_BOXES[i + 1][2]) / 2,
                    guiTop + LEFT_DISPLAY_BOXES[i + 1][1] + 3, 0xFFFFFF);
        }
    }
    private void renderArrow(MatrixStack matrixStack, int mouseX, int mouseY) {
        Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
        boolean conditionsMet = areConditionsMet();
        int[] arrowUV = conditionsMet ? BRIGHT_ARROW_UV : DARK_ARROW_UV;
        int arrowWidth = arrowUV[2] - arrowUV[0];
        int arrowHeight = arrowUV[3] - arrowUV[1];
        this.blit(
                matrixStack,
                guiLeft + ARROW_AREA[0],
                guiTop + ARROW_AREA[1],
                arrowUV[0],
                arrowUV[1],
                arrowWidth,
                arrowHeight
        );
    }
    private boolean isMouseOverArrow(double mouseX, double mouseY) {
        int x1 = guiLeft + ARROW_AREA[0];
        int y1 = guiTop + ARROW_AREA[1];
        int x2 = guiLeft + ARROW_AREA[2];
        int y2 = guiTop + ARROW_AREA[3];
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }
    private boolean areConditionsMet() {
        for (TextFieldWidget field : rightInputFields) {
            if (field.getText().isEmpty()) return false;
        }
        float targetSpeed = getTargetSpeed();
        return targetSpeed < currentSpeed && targetSpeed >= 0.0f && targetSpeed <= 1.0f;
    }
    private float getTargetSpeed() {
        try {
            String speedStr = String.format("%s.%s%s%s",
                    rightInputFields.get(0).getText(),
                    rightInputFields.get(1).getText(),
                    rightInputFields.get(2).getText(),
                    rightInputFields.get(3).getText());
            return Float.parseFloat(speedStr);
        } catch (NumberFormatException e) {
            return currentSpeed;
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inputClicked = super.mouseClicked(mouseX, mouseY, button);
        if (!inputClicked && isMouseOverArrow(mouseX, mouseY) && areConditionsMet()) {
            Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            float targetSpeed = getTargetSpeed();
            Enhance.sendToServer(new SetSpeedPacket(targetSpeed));
            successMessage = I18n.format("enhance.speed_reducer.success", String.format("%.3f", targetSpeed));
            successMessageTimer = 100;
            currentSpeed = targetSpeed;
            return true;
        }
        return inputClicked;
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.minecraft != null) {
                this.minecraft.displayGuiScreen(null);
            }
            return true;
        }
        for (TextFieldWidget field : rightInputFields) {
            if (field.isFocused() && field.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (TextFieldWidget field : rightInputFields) {
            if (field.isFocused()) {
                if (Character.isDigit(codePoint)) {
                    return field.charTyped(codePoint, modifiers);
                }
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}