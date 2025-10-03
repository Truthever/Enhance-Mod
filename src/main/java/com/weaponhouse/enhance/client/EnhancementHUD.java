package com.weaponhouse.enhance.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber (modid = "enhance", value = Dist.CLIENT)
public class EnhancementHUD {
    private static final Map<UUID, BossData> bossDataMap = new HashMap<>();
    private static final int MAX_DISPLAYED_BOSSES = 3;
    private static long animationTimer = 0;
    private static final float GLOBAL_TEXT_SCALE = 0.7F;
    private static final Map<String, String> MOD_LOCALIZATION = new HashMap<>();
    private static final String IRON_GOLEM_ID = "entity.minecraft.iron_golem";
    static {
        MOD_LOCALIZATION.put("frost", "buff.enhance.frost");
        MOD_LOCALIZATION.put("life", "buff.enhance.life");
        MOD_LOCALIZATION.put("attack", "buff.enhance.attack");
        MOD_LOCALIZATION.put("megaforce", "buff.enhance.megaforce");
        MOD_LOCALIZATION.put("vampire", "buff.enhance.vampire");
        MOD_LOCALIZATION.put("rob", "buff.enhance.rob");
        MOD_LOCALIZATION.put("thunder", "buff.enhance.thunder");
        MOD_LOCALIZATION.put("ricochet", "buff.enhance.ricochet");
        MOD_LOCALIZATION.put("harmony", "buff.enhance.harmony");
        MOD_LOCALIZATION.put("curse", "buff.enhance.curse");
        MOD_LOCALIZATION.put("thorns", "buff.enhance.thorns");
        MOD_LOCALIZATION.put("aura", "buff.enhance.aura");
        MOD_LOCALIZATION.put("hunger", "buff.enhance.hunger");
        MOD_LOCALIZATION.put("displacement", "buff.enhance.displacement");
        MOD_LOCALIZATION.put("death_bomb", "buff.enhance.death_bomb");
        MOD_LOCALIZATION.put("tracking", "buff.enhance.tracking");
        MOD_LOCALIZATION.put("unyielding", "buff.enhance.unyielding");
        MOD_LOCALIZATION.put("summon", "buff.enhance.summon");
        MOD_LOCALIZATION.put("phantom", "buff.enhance.phantom");
        MOD_LOCALIZATION.put("photosynthesis", "buff.enhance.photosynthesis");
        MOD_LOCALIZATION.put("fasting", "buff.enhance.fasting");
    }
    private static final int BOSS_BAR_WIDTH = 180;
    private static final int BOSS_BAR_HEIGHT = 6;
    private static final int BAR_BORDER_WIDTH = 1;
    private static final int BAR_PADDING = 1;
    private static final int GLOW_SIZE = 2;
    private static final int PADDING_NAME_TO_BAR = 0;
    private static final int PADDING_BAR_TO_HEALTH = -5;
    private static final int PADDING_HEALTH_TO_MODS = 1;
    private static final int PADDING_MOD_LINE = 6;
    private static final int PADDING_MOD_ITEM = 4;
    private static final int MAX_MODS_PER_LINE = 5;
    private static final String DISTANCE_FORMAT = "[%.1fm]";
    private static final TextFormatting DISTANCE_COLOR = TextFormatting.YELLOW;
    private static final int FIRST_BOSS_Y = 5;
    private static final int SECOND_BOSS_Y = 37;
    private static final int THIRD_BOSS_Y = 72;
    private static final int COLOR_IRON_BASE = 0xFFC8C8C8;
    private static final int COLOR_IRON_LIGHT = 0xFFE8E8E8;
    private static final int COLOR_IRON_GLOW = 0x30C8C8C8;
    private static final int COLOR_IRON_INNER_GLOW = 0x50C8C8C8;
    private static final int COLOR_IRON_HIGHLIGHT = 0x20E8E8E8;
    private static final int COLOR_TIER1_BASE = 0xFF33CC33;
    private static final int COLOR_TIER2_BASE = 0xFF3333CC;
    private static final int COLOR_TIER3_BASE = 0xFFCC3333;
    private static final int COLOR_DEFAULT = 0xFF9900FF;
    private static final int COLOR_TIER1_LIGHT = 0xFF88FF88;
    private static final int COLOR_TIER2_LIGHT = 0xFF8888FF;
    private static final int COLOR_TIER3_LIGHT = 0xFFFF8888;
    private static final int COLOR_BORDER = 0xFF222222;
    private static final int COLOR_BACKGROUND = 0x20000000;
    private static final int COLOR_GLOW_TIER1 = 0x4033CC33;
    private static final int COLOR_GLOW_TIER2 = 0x403333CC;
    private static final int COLOR_GLOW_TIER3 = 0x40CC3333;
    public static void setBossData(UUID entityId, BossData data) {
        bossDataMap.put(entityId, data);
    }
    public static void removeBossData(UUID entityId) {
        bossDataMap.remove(entityId);
    }
    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!ClientConfigCache.isBossbarEnabled()) {
            return;
        }
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null || bossDataMap.isEmpty()) {
            return;
        }
        animationTimer++;
        int screenWidth = event.getWindow().getScaledWidth();
        List<BossData> sortedBossData = new ArrayList<>(bossDataMap.values());
        sortedBossData.sort(Comparator.comparingDouble(BossData::getDistance));
        int[] bossYPositions = {FIRST_BOSS_Y, SECOND_BOSS_Y, THIRD_BOSS_Y};
        for (int i = 0; i < Math.min(sortedBossData.size(), MAX_DISPLAYED_BOSSES); i++) {
            BossData data = sortedBossData.get(i);
            renderSingleBossPanel(event.getMatrixStack(), data, mc, screenWidth, bossYPositions[i]);
        }
    }
    private static void renderSingleBossPanel(MatrixStack matrixStack, BossData data, Minecraft mc, int screenWidth, int startY) {
        FontRenderer fontRenderer = mc.fontRenderer;
        if (fontRenderer == null) {
            return;
        }
        ITextComponent tierComponent = getEnhancementTitle(data.tier, data.entityType);
        ITextComponent entityComponent = new TranslationTextComponent(data.entityType);
        String distanceText = String.format(DISTANCE_FORMAT, data.getDistance());
        String title = DISTANCE_COLOR + distanceText + TextFormatting.RESET + " "
                + tierComponent.getString() + " " + entityComponent.getString();
        float pulse = (float) (0.8 + 0.2 * Math.sin(animationTimer * 0.1));
        int titleColor = (int) (pulse * 255) << 24 | 0xFFFFFF;
        int titleWidth = fontRenderer.getStringWidth(title);
        int titleX = (screenWidth - titleWidth) / 2;
        int titleY = startY;
        matrixStack.push();
        matrixStack.scale(GLOBAL_TEXT_SCALE, GLOBAL_TEXT_SCALE, 1.0F);
        float scaledTitleX = titleX / GLOBAL_TEXT_SCALE;
        float scaledTitleY = titleY / GLOBAL_TEXT_SCALE;
        fontRenderer.drawStringWithShadow(matrixStack, title, scaledTitleX, scaledTitleY, titleColor);
        matrixStack.pop();
        int barX = (screenWidth - BOSS_BAR_WIDTH) / 2;
        int scaledTitleHeight = (int) (fontRenderer.FONT_HEIGHT * GLOBAL_TEXT_SCALE);
        int barY = titleY + scaledTitleHeight + PADDING_NAME_TO_BAR;
        int glowColor = isIronGolem(data.entityType) ? COLOR_IRON_GLOW : getGlowColor(data.tier);
        drawGlow(matrixStack, barX, barY, BOSS_BAR_WIDTH, BOSS_BAR_HEIGHT, glowColor);
        int bgX1 = barX + BAR_BORDER_WIDTH + BAR_PADDING;
        int bgY1 = barY + BAR_BORDER_WIDTH + BAR_PADDING;
        int bgX2 = barX + BOSS_BAR_WIDTH - BAR_BORDER_WIDTH - BAR_PADDING;
        int bgY2 = barY + BOSS_BAR_HEIGHT - BAR_BORDER_WIDTH - BAR_PADDING;
        fill(matrixStack, bgX1, bgY1, bgX2, bgY2, COLOR_BACKGROUND);
        float healthPercent = Math.max(0.0F, Math.min(1.0F, data.currentHealth / data.maxHealth));
        int progressWidth = (int) ((bgX2 - bgX1) * healthPercent);
        if (progressWidth > 0) {
            int[] gradientColors = isIronGolem(data.entityType)
                    ? new int[]{COLOR_IRON_LIGHT, COLOR_IRON_BASE}
                    : getGradientColors(data.tier);
            drawGradientProgress(matrixStack, bgX1, bgY1, bgX1 + progressWidth, bgY2, gradientColors);
            drawAnimatedHighlight(matrixStack, bgX1, bgY1, bgX1 + progressWidth, bgY2, data.tier, data.entityType);
        }
        drawBorderWithInnerGlow(matrixStack, barX, barY, BOSS_BAR_WIDTH, BOSS_BAR_HEIGHT, data.tier, data.entityType);
        String healthText = String.format("%.1f/%.1f", data.currentHealth, data.maxHealth);
        int healthY = barY + BOSS_BAR_HEIGHT + PADDING_BAR_TO_HEALTH;
        matrixStack.push();
        matrixStack.scale(GLOBAL_TEXT_SCALE, GLOBAL_TEXT_SCALE, 1.0F);
        int healthTextWidth = fontRenderer.getStringWidth(healthText);
        int healthX = (screenWidth - healthTextWidth) / 2;
        float scaledHealthX = healthX / GLOBAL_TEXT_SCALE;
        float scaledHealthY = healthY / GLOBAL_TEXT_SCALE;
        fontRenderer.drawStringWithShadow(matrixStack, healthText, scaledHealthX, scaledHealthY, 0xFFFFFF);
        matrixStack.pop();
        int scaledHealthHeight = (int) (fontRenderer.FONT_HEIGHT * GLOBAL_TEXT_SCALE);
        int modY = healthY + scaledHealthHeight + PADDING_HEALTH_TO_MODS;
        renderModifiers(matrixStack, data.modNames, mc, screenWidth, modY);
    }
    private static boolean isIronGolem(String entityType) {
        return entityType != null && entityType.toLowerCase(Locale.ROOT).equals(IRON_GOLEM_ID);
    }
    private static ITextComponent getEnhancementTitle(int tier, String entityType) {
        TextFormatting color = isIronGolem(entityType) ? TextFormatting.GRAY :
                (tier == 1 ? TextFormatting.GREEN : (tier == 2 ? TextFormatting.BLUE : TextFormatting.RED));
        switch (tier) {
            case 1:
                return new StringTextComponent("[I]").mergeStyle(color);
            case 2:
                return new StringTextComponent("[II]").mergeStyle(color);
            case 3:
                return new StringTextComponent("[III]").mergeStyle(color);
            default:
                return new StringTextComponent("[0]").mergeStyle(color);
        }
    }
    private static void drawBorderWithInnerGlow(MatrixStack matrixStack, int x, int y, int width, int height, int tier, String entityType) {
        fill(matrixStack, x, y, x + width, y + BAR_BORDER_WIDTH, COLOR_BORDER);
        fill(matrixStack, x, y + height - BAR_BORDER_WIDTH, x + width, y + height, COLOR_BORDER);
        fill(matrixStack, x, y + BAR_BORDER_WIDTH, x + BAR_BORDER_WIDTH, y + height - BAR_BORDER_WIDTH, COLOR_BORDER);
        fill(matrixStack, x + width - BAR_BORDER_WIDTH, y + BAR_BORDER_WIDTH, x + width, y + height - BAR_BORDER_WIDTH, COLOR_BORDER);
        int innerGlowColor = isIronGolem(entityType) ? COLOR_IRON_INNER_GLOW :
                (tier == 1 ? 0x6033CC33 : (tier == 2 ? 0x603333CC : 0x60CC3333));
        int innerX = x + BAR_BORDER_WIDTH;
        int innerY = y + BAR_BORDER_WIDTH;
        int innerWidth = width - 2 * BAR_BORDER_WIDTH;
        int innerHeight = height - 2 * BAR_BORDER_WIDTH;
        fill(matrixStack, innerX, innerY, innerX + innerWidth, innerY + 1, innerGlowColor);
        fill(matrixStack, innerX, innerY + innerHeight - 1, innerX + innerWidth, innerY + innerHeight, innerGlowColor);
        fill(matrixStack, innerX, innerY + 1, innerX + 1, innerY + innerHeight - 1, innerGlowColor);
        fill(matrixStack, innerX + innerWidth - 1, innerY + 1, innerX + innerWidth, innerY + innerHeight - 1, innerGlowColor);
    }
    private static void drawAnimatedHighlight(MatrixStack matrixStack, int x1, int y1, int x2, int y2, int tier, String entityType) {
        float offset = (animationTimer % 100) / 100.0f;
        int highlightWidth = (int) (x2 - x1) / 5;
        int highlightX = (int) (x1 + (x2 - x1 - highlightWidth) * offset);
        int highlightColor = isIronGolem(entityType) ? COLOR_IRON_HIGHLIGHT : 0x30FFFFFF;
        fill(matrixStack, highlightX, y1, Math.min(highlightX + highlightWidth, x2), y2, highlightColor);
    }
    private static void drawGlow(MatrixStack matrixStack, int x, int y, int width, int height, int color) {
        int outerGlowColor = (color & 0x00FFFFFF) | 0x10000000;
        fill(matrixStack, x - GLOW_SIZE, y - GLOW_SIZE, x + width + GLOW_SIZE, y + height + GLOW_SIZE, outerGlowColor);
        int innerGlowColor = (color & 0x00FFFFFF) | 0x20000000;
        fill(matrixStack, x - GLOW_SIZE / 2, y - GLOW_SIZE / 2, x + width + GLOW_SIZE / 2, y + height + GLOW_SIZE / 2, innerGlowColor);
    }
    private static void drawGradientProgress(MatrixStack matrixStack, int x1, int y1, int x2, int y2, int[] colors) {
        int height = y2 - y1;
        for (int i = 0; i < height; i++) {
            float ratio = (float) i / height;
            int mixedColor = mixColors(colors[0], colors[1], 1 - ratio, ratio);
            fill(matrixStack, x1, y1 + i, x2, y1 + i + 1, mixedColor);
        }
    }
    private static int mixColors(int color1, int color2, float ratio1, float ratio2) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a = (int) (a1 * ratio1 + a2 * ratio2);
        int r = (int) (r1 * ratio1 + r2 * ratio2);
        int g = (int) (g1 * ratio1 + g2 * ratio2);
        int b = (int) (b1 * ratio1 + b2 * ratio2);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    private static int[] getGradientColors(int tier) {
        switch (tier) {
            case 1:
                return new int[]{COLOR_TIER1_LIGHT, COLOR_TIER1_BASE};
            case 2:
                return new int[]{COLOR_TIER2_LIGHT, COLOR_TIER2_BASE};
            case 3:
                return new int[]{COLOR_TIER3_LIGHT, COLOR_TIER3_BASE};
            default:
                return new int[]{COLOR_DEFAULT, COLOR_DEFAULT};
        }
    }
    private static int getGlowColor(int tier) {
        switch (tier) {
            case 1:
                return COLOR_GLOW_TIER1;
            case 2:
                return COLOR_GLOW_TIER2;
            case 3:
                return COLOR_GLOW_TIER3;
            default:
                return 0x409900FF;
        }
    }
    private static void renderModifiers(MatrixStack matrixStack, List<String> modNames, Minecraft mc, int screenWidth, int yOffset) {
        FontRenderer fontRenderer = mc.fontRenderer;
        if (fontRenderer == null || modNames.isEmpty()) {
            return;
        }
        int totalMods = modNames.size();
        int modsInFirstLine = Math.min(totalMods, MAX_MODS_PER_LINE);
        int modsInSecondLine = totalMods > MAX_MODS_PER_LINE ? totalMods - MAX_MODS_PER_LINE : 0;
        if (modsInFirstLine > 0) {
            renderModifierLine(matrixStack, modNames.subList(0, modsInFirstLine), mc, screenWidth, yOffset);
        }
        if (modsInSecondLine > 0) {
            renderModifierLine(matrixStack, modNames.subList(MAX_MODS_PER_LINE, totalMods), mc, screenWidth, yOffset + PADDING_MOD_LINE);
        }
    }
    private static void renderModifierLine(MatrixStack matrixStack, List<String> modNames, Minecraft mc, int screenWidth, int yOffset) {
        FontRenderer fontRenderer = mc.fontRenderer;
        if (fontRenderer == null || modNames.isEmpty()) {
            return;
        }
        int totalWidth = 0;
        List<String> texts = new ArrayList<>();
        boolean isBlindMode = ClientConfigCache.isBlindModeEnabled();
        for (String modNameWithLevel : modNames) {
            String[] parts = modNameWithLevel.split(":");
            String modKey = parts[0];
            String level = parts.length > 1 ? parts[1] : "1";
            String displayName = isBlindMode ? "??" : new TranslationTextComponent(MOD_LOCALIZATION.getOrDefault(modKey, modKey)).getString();
            String fullText = displayName + "Lv." + level;
            texts.add(fullText);
            totalWidth += fontRenderer.getStringWidth(fullText) + PADDING_MOD_ITEM;
        }
        if (!texts.isEmpty()) {
            totalWidth -= PADDING_MOD_ITEM;
        }
        int xOffset = (screenWidth - totalWidth) / 2;
        matrixStack.push();
        matrixStack.scale(GLOBAL_TEXT_SCALE, GLOBAL_TEXT_SCALE, 1.0F);
        for (String fullText : texts) {
            float scaledX = xOffset / GLOBAL_TEXT_SCALE;
            float scaledY = yOffset / GLOBAL_TEXT_SCALE;
            fontRenderer.drawStringWithShadow(matrixStack, fullText, scaledX, scaledY, 0xFFFFFF);
            xOffset += fontRenderer.getStringWidth(fullText) + PADDING_MOD_ITEM;
        }
        matrixStack.pop();
    }
    private static void fill(MatrixStack matrixStack, int x1, int y1, int x2, int y2, int color) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        net.minecraft.client.gui.AbstractGui.fill(matrixStack, minX, minY, maxX, maxY, color);
    }
    public static class BossData {
        public List<String> modNames;
        public int tier;
        public String entityType;
        public float currentHealth;
        public float maxHealth;
        public double distance;
        public BossData(List<String> modNames, int tier, String entityType, float currentHealth, float maxHealth, double distance) {
            this.modNames = modNames;
            this.tier = tier;
            this.entityType = entityType;
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
            this.distance = distance;
        }
        public double getDistance() {
            return distance;
        }
    }
}