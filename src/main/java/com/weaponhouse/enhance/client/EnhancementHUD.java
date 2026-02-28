package com.weaponhouse.enhance.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;
@OnlyIn(Dist.CLIENT)
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
        MOD_LOCALIZATION.put("chaos", "buff.enhance.chaos");
        MOD_LOCALIZATION.put("inspiration", "buff.enhance.inspiration");
        MOD_LOCALIZATION.put("annihilation", "buff.enhance.annihilation");
        MOD_LOCALIZATION.put("spirit_shield", "buff.enhance.spirit_shield");
        MOD_LOCALIZATION.put("corrosion", "buff.enhance.corrosion");
        MOD_LOCALIZATION.put("combo", "buff.enhance.combo");
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
    private static final int COLOR_IRON_BASE_OPAQUE = 0xFFC8C8C8;
    private static final int COLOR_IRON_LIGHT_OPAQUE = 0xFFE8E8E8;
    private static final int COLOR_IRON_GLOW_OPAQUE = 0x30C8C8C8;
    private static final int COLOR_IRON_INNER_GLOW_OPAQUE = 0x50C8C8C8;
    private static final int COLOR_IRON_HIGHLIGHT_OPAQUE = 0x20E8E8E8;
    private static final int COLOR_TIER1_BASE_OPAQUE = 0xFF33CC33;
    private static final int COLOR_TIER2_BASE_OPAQUE = 0xFF3333CC;
    private static final int COLOR_TIER3_BASE_OPAQUE = 0xFFCC3333;
    private static final int COLOR_DEFAULT_OPAQUE = 0xFF9900FF;
    private static final int COLOR_TIER1_LIGHT_OPAQUE = 0xFF88FF88;
    private static final int COLOR_TIER2_LIGHT_OPAQUE = 0xFF8888FF;
    private static final int COLOR_TIER3_LIGHT_OPAQUE = 0xFFFF8888;
    private static final int COLOR_BORDER_OPAQUE = 0xFF222222;
    private static final int COLOR_IRON_BASE_TRANSLUCENT = 0x80C8C8C8;
    private static final int COLOR_IRON_LIGHT_TRANSLUCENT = 0x80E8E8E8;
    private static final int COLOR_IRON_GLOW_TRANSLUCENT = 0x30C8C8C8;
    private static final int COLOR_IRON_INNER_GLOW_TRANSLUCENT = 0x50C8C8C8;
    private static final int COLOR_IRON_HIGHLIGHT_TRANSLUCENT = 0x20E8E8E8;
    private static final int COLOR_TIER1_BASE_TRANSLUCENT = 0x8033CC33;
    private static final int COLOR_TIER2_BASE_TRANSLUCENT = 0x803333CC;
    private static final int COLOR_TIER3_BASE_TRANSLUCENT = 0x80CC3333;
    private static final int COLOR_DEFAULT_TRANSLUCENT = 0x809900FF;
    private static final int COLOR_TIER1_LIGHT_TRANSLUCENT = 0x8088FF88;
    private static final int COLOR_TIER2_LIGHT_TRANSLUCENT = 0x808888FF;
    private static final int COLOR_TIER3_LIGHT_TRANSLUCENT = 0x80FF8888;
    private static final int COLOR_BORDER_TRANSLUCENT = 0x80222222;
    private static final int COLOR_DRAGON_WITHER_BASE_OPAQUE = 0xFFFF69B4;
    private static final int COLOR_DRAGON_WITHER_LIGHT_OPAQUE = 0xFFFFB6D9;
    private static final int COLOR_DRAGON_WITHER_GLOW_OPAQUE = 0x40FF69B4;
    private static final int COLOR_DRAGON_WITHER_BASE_TRANSLUCENT = 0x80FF69B4;
    private static final int COLOR_DRAGON_WITHER_LIGHT_TRANSLUCENT = 0x80FFB6D9;
    private static final int COLOR_DRAGON_WITHER_GLOW_TRANSLUCENT = 0x40FF69B4;
    private static final int COLOR_BACKGROUND = 0x20000000;
    private static final int COLOR_GLOW_TIER1 = 0x4033CC33;
    private static final int COLOR_GLOW_TIER2 = 0x403333CC;
    private static final int COLOR_GLOW_TIER3 = 0x40CC3333;
    private static float targetSpiritShield = 0;
    private static float targetMaxSpiritShield = 0;
    private static float displaySpiritShield = 0;
    private static float displayMaxSpiritShield = 0;
    private static int spiritShieldHoldTicks = 0;
    private static final int SPIRIT_SHIELD_HOLD = 60;
    public static void setBossData(UUID entityId, BossData data) {
        bossDataMap.put(entityId, data);
    }
    public static void removeBossData(UUID entityId) {
        bossDataMap.remove(entityId);
    }
    public static void setSpiritShieldData(float current, float max) {
        targetSpiritShield = Math.max(0, current);
        targetMaxSpiritShield = Math.max(0, max);
        spiritShieldHoldTicks = SPIRIT_SHIELD_HOLD;
    }
    public static void clearAllClientCache() {
        bossDataMap.clear();
        animationTimer = 0;
        targetSpiritShield = 0;
        targetMaxSpiritShield = 0;
        displaySpiritShield = 0;
        displayMaxSpiritShield = 0;
        spiritShieldHoldTicks = 0;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null) return;
        MatrixStack matrixStack = event.getMatrixStack();
        int screenWidth = event.getWindow().getScaledWidth();
        int screenHeight = event.getWindow().getScaledHeight();
        renderSpiritShieldBar(matrixStack, mc, screenWidth, screenHeight);
        if (!ClientConfigCache.isBossbarEnabled()) return;
        if (bossDataMap.isEmpty()) return;
        animationTimer++;
        List<BossData> prioritized = new ArrayList<>();
        List<BossData> others = new ArrayList<>();
        for (BossData data : bossDataMap.values()) {
            if (isEnderDragonOrWither(data.entityType)) prioritized.add(data);
            else others.add(data);
        }
        others.sort(Comparator.comparingDouble(BossData::getDistance));
        List<BossData> allBosses = new ArrayList<>(prioritized.size() + others.size());
        allBosses.addAll(prioritized);
        allBosses.addAll(others);
        int[] bossYPositions = {FIRST_BOSS_Y, SECOND_BOSS_Y, THIRD_BOSS_Y};
        for (int i = 0; i < Math.min(allBosses.size(), MAX_DISPLAYED_BOSSES); i++) {
            renderSingleBossPanel(matrixStack, allBosses.get(i), mc, screenWidth, bossYPositions[i]);
        }
    }
    private static boolean isEnderDragonOrWither(String entityType) {
        return ("entity.minecraft.ender_dragon".equals(entityType) || "entity.minecraft.wither".equals(entityType));
    }
    private static boolean isIronGolem(String entityType) {
        return entityType != null && entityType.toLowerCase(Locale.ROOT).equals(IRON_GOLEM_ID);
    }
    private static int pickColor(int opaqueColor, int translucentColor) {
        return ClientConfigCache.isTranslucentModeEnabled() ? translucentColor : opaqueColor;
    }
    private static int getIronBaseColor() { return pickColor(COLOR_IRON_BASE_OPAQUE, COLOR_IRON_BASE_TRANSLUCENT); }
    private static int getIronLightColor() { return pickColor(COLOR_IRON_LIGHT_OPAQUE, COLOR_IRON_LIGHT_TRANSLUCENT); }
    private static int getIronGlowColor() { return pickColor(COLOR_IRON_GLOW_OPAQUE, COLOR_IRON_GLOW_TRANSLUCENT); }
    private static int getIronInnerGlowColor() { return pickColor(COLOR_IRON_INNER_GLOW_OPAQUE, COLOR_IRON_INNER_GLOW_TRANSLUCENT); }
    private static int getIronHighlightColor() { return pickColor(COLOR_IRON_HIGHLIGHT_OPAQUE, COLOR_IRON_HIGHLIGHT_TRANSLUCENT); }
    private static int getDragonWitherBaseColor() { return pickColor(COLOR_DRAGON_WITHER_BASE_OPAQUE, COLOR_DRAGON_WITHER_BASE_TRANSLUCENT); }
    private static int getDragonWitherLightColor() { return pickColor(COLOR_DRAGON_WITHER_LIGHT_OPAQUE, COLOR_DRAGON_WITHER_LIGHT_TRANSLUCENT); }
    private static int getDragonWitherGlowColor() { return pickColor(COLOR_DRAGON_WITHER_GLOW_OPAQUE, COLOR_DRAGON_WITHER_GLOW_TRANSLUCENT); }
    private static int getTierBaseColor(int tier) {
        switch (tier) {
            case 1: return pickColor(COLOR_TIER1_BASE_OPAQUE, COLOR_TIER1_BASE_TRANSLUCENT);
            case 2: return pickColor(COLOR_TIER2_BASE_OPAQUE, COLOR_TIER2_BASE_TRANSLUCENT);
            case 3: return pickColor(COLOR_TIER3_BASE_OPAQUE, COLOR_TIER3_BASE_TRANSLUCENT);
            default: return pickColor(COLOR_DEFAULT_OPAQUE, COLOR_DEFAULT_TRANSLUCENT);
        }
    }
    private static int getTierLightColor(int tier) {
        switch (tier) {
            case 1: return pickColor(COLOR_TIER1_LIGHT_OPAQUE, COLOR_TIER1_LIGHT_TRANSLUCENT);
            case 2: return pickColor(COLOR_TIER2_LIGHT_OPAQUE, COLOR_TIER2_LIGHT_TRANSLUCENT);
            case 3: return pickColor(COLOR_TIER3_LIGHT_OPAQUE, COLOR_TIER3_LIGHT_TRANSLUCENT);
            default: return pickColor(COLOR_DEFAULT_OPAQUE, COLOR_DEFAULT_TRANSLUCENT);
        }
    }
    private static int getBorderColor() {
        return pickColor(COLOR_BORDER_OPAQUE, COLOR_BORDER_TRANSLUCENT);
    }
    private static int getGlowColor(int tier) {
        switch (tier) {
            case 1: return COLOR_GLOW_TIER1;
            case 2: return COLOR_GLOW_TIER2;
            case 3: return COLOR_GLOW_TIER3;
            default: return 0x409900FF;
        }
    }
    private static void renderSpiritShieldBar(MatrixStack ms, Minecraft mc, int screenWidth, int screenHeight) {
        boolean hasAny = (targetMaxSpiritShield > 0) || (displayMaxSpiritShield > 0.01f) || (spiritShieldHoldTicks > 0);
        if (!hasAny) return;
        float lerp = 0.20f;
        displayMaxSpiritShield += (targetMaxSpiritShield - displayMaxSpiritShield) * lerp;
        displaySpiritShield += (targetSpiritShield - displaySpiritShield) * lerp;
        if (displayMaxSpiritShield < 0.01f) displayMaxSpiritShield = 0;
        if (displaySpiritShield < 0.01f) displaySpiritShield = 0;
        if (displayMaxSpiritShield > 0 && displaySpiritShield > displayMaxSpiritShield) displaySpiritShield = displayMaxSpiritShield;
        if (spiritShieldHoldTicks > 0) spiritShieldHoldTicks--;
        boolean translucent = ClientConfigCache.isTranslucentModeEnabled();
        float fade = 1.0f;
        if (targetSpiritShield <= 0.01f) {
            fade = Math.min(1.0f, spiritShieldHoldTicks / (float) SPIRIT_SHIELD_HOLD);
        }
        int baseA = translucent ? (int)(fade * 140) : (int)(fade * 230);
        if (baseA <= 0) return;
        int left = screenWidth / 2 - 91;
        int baseY = screenHeight - 39;
        int iconX = left - 14;
        int iconY = baseY - 10;
        int iconW = 10;
        int iconH = 10;
        int goldDark   = (baseA << 24) | 0xB8860B;
        int goldBright = (baseA << 24) | 0xFFD700;
        int rune       = (baseA << 24) | 0xFFF2B0;
        int rim        = (Math.max(0, baseA - 80) << 24) | 0x5A3A00;
        fill(ms, iconX + 3, iconY, iconX + 7, iconY + 1, rim);
        fill(ms, iconX + 2, iconY + 1, iconX + 3, iconY + 7, rim);
        fill(ms, iconX + 7, iconY + 1, iconX + 8, iconY + 7, rim);
        fill(ms, iconX + 3, iconY + 7, iconX + 7, iconY + 9, rim);
        fill(ms, iconX + 4, iconY + 9, iconX + 6, iconY + 10, rim);
        fill(ms, iconX + 3, iconY + 1, iconX + 7, iconY + 2, goldDark);
        fill(ms, iconX + 2, iconY + 2, iconX + 3, iconY + 6, goldDark);
        fill(ms, iconX + 7, iconY + 2, iconX + 8, iconY + 6, goldDark);
        fill(ms, iconX + 3, iconY + 6, iconX + 7, iconY + 8, goldDark);
        fill(ms, iconX + 4, iconY + 8, iconX + 6, iconY + 9, goldDark);
        fill(ms, iconX + 5, iconY + 2, iconX + 6, iconY + 7, goldBright);
        fill(ms, iconX + 4, iconY + 3, iconX + 7, iconY + 4, (Math.max(0, baseA - 30) << 24) | 0xFFD700);
        fill(ms, iconX + 3, iconY + 3, iconX + 4, iconY + 4, rune);
        fill(ms, iconX + 6, iconY + 3, iconX + 7, iconY + 4, rune);
        if (displayMaxSpiritShield > 0.01f) {
            FontRenderer fr = mc.fontRenderer;
            String txt = String.format("%.0f/%.0f", displaySpiritShield, displayMaxSpiritShield);
            ms.push();
            float s = 0.5f;
            ms.scale(s, s, 1f);
            int textW = fr.getStringWidth(txt);
            int textX = (int)((iconX + iconW / 2f) / s - textW / 2f);
            int textY = (int)((iconY + iconH + 1) / s);
            int textColor = translucent ? 0x80FFF2B0 : 0xFFFFF2B0;
            fr.drawStringWithShadow(ms, txt, textX, textY, textColor);
            ms.pop();
        }
    }

    private static void renderSingleBossPanel(MatrixStack matrixStack, BossData data, Minecraft mc, int screenWidth, int startY) {
        FontRenderer font = mc.fontRenderer;
        if (font == null) return;

        ITextComponent tierComponent = getEnhancementTitle(data.tier, data.entityType);
        ITextComponent entityComponent = new TranslationTextComponent(data.entityType);

        String distanceText = String.format(DISTANCE_FORMAT, data.getDistance());
        String title = DISTANCE_COLOR + distanceText + TextFormatting.RESET + " "
                + tierComponent.getString() + " " + entityComponent.getString();

        float pulse = (float) (0.8 + 0.2 * Math.sin(animationTimer * 0.1));
        boolean translucent = ClientConfigCache.isTranslucentModeEnabled();
        int alpha = translucent ? (int) (pulse * 128) : (int) (pulse * 255);
        int titleColor = (alpha << 24) | 0xFFFFFF;
        int titleWidth = font.getStringWidth(title);
        int titleX = (screenWidth - titleWidth) / 2;
        matrixStack.push();
        matrixStack.scale(GLOBAL_TEXT_SCALE, GLOBAL_TEXT_SCALE, 1.0F);
        float scaledTitleX = titleX / GLOBAL_TEXT_SCALE;
        float scaledTitleY = startY / GLOBAL_TEXT_SCALE;
        drawTextCompat(font, matrixStack, title, scaledTitleX, scaledTitleY, titleColor);
        matrixStack.pop();
        int barX = (screenWidth - BOSS_BAR_WIDTH) / 2;
        int scaledTitleHeight = (int) (font.FONT_HEIGHT * GLOBAL_TEXT_SCALE);
        int barY = startY + scaledTitleHeight + PADDING_NAME_TO_BAR;
        int glowColor;
        drawOuterFrameDarkMagic(matrixStack, barX, barY);
        if (isIronGolem(data.entityType)) {
            glowColor = getIronGlowColor();
        } else if (isEnderDragonOrWither(data.entityType)) {
            glowColor = getDragonWitherGlowColor();
        } else {
            glowColor = getGlowColor(data.tier);
        }
        drawGlow(matrixStack, barX, barY, glowColor);
        int bgX1 = barX + BAR_BORDER_WIDTH + BAR_PADDING;
        int bgY1 = barY + BAR_BORDER_WIDTH + BAR_PADDING;
        int bgX2 = barX + BOSS_BAR_WIDTH - BAR_BORDER_WIDTH - BAR_PADDING;
        int bgY2 = barY + BOSS_BAR_HEIGHT - BAR_BORDER_WIDTH - BAR_PADDING;
        fill(matrixStack, bgX1, bgY1, bgX2, bgY2, COLOR_BACKGROUND);
        float healthPercent = Math.max(0.0F, Math.min(1.0F, data.currentHealth / data.maxHealth));
        int progressWidth = (int) ((bgX2 - bgX1) * healthPercent);
        if (progressWidth > 0) {
            int[] gradientColors;
            if (isIronGolem(data.entityType)) {
                gradientColors = new int[]{getIronLightColor(), getIronBaseColor()};
            } else if (isEnderDragonOrWither(data.entityType)) {
                gradientColors = new int[]{getDragonWitherLightColor(), getDragonWitherBaseColor()};
            } else {
                gradientColors = new int[]{getTierLightColor(data.tier), getTierBaseColor(data.tier)};
            }
            drawGradientProgress(matrixStack, bgX1, bgY1, bgX1 + progressWidth, bgY2, gradientColors);
            drawAnimatedHighlight(matrixStack, bgX1, bgY1, bgX1 + progressWidth, bgY2, data.tier, data.entityType);
        }
        drawBorderWithInnerGlow(matrixStack, barX, barY, data.tier, data.entityType);
        String healthText = String.format("%.1f/%.1f", data.currentHealth, data.maxHealth);
        int healthY = barY + BOSS_BAR_HEIGHT + PADDING_BAR_TO_HEALTH;
        matrixStack.push();
        matrixStack.scale(GLOBAL_TEXT_SCALE, GLOBAL_TEXT_SCALE, 1.0F);
        int healthTextWidth = font.getStringWidth(healthText);
        int healthX = (screenWidth - healthTextWidth) / 2;
        float scaledHealthX = healthX / GLOBAL_TEXT_SCALE;
        float scaledHealthY = healthY / GLOBAL_TEXT_SCALE;
        int healthTextColor = translucent ? 0x80FFFFFF : 0xFFFFFFFF;
        drawTextCompat(font, matrixStack, healthText, scaledHealthX, scaledHealthY, healthTextColor);
        matrixStack.pop();
        int scaledHealthHeight = (int) (font.FONT_HEIGHT * GLOBAL_TEXT_SCALE);
        int modY = healthY + scaledHealthHeight + PADDING_HEALTH_TO_MODS;
        renderModifiers(matrixStack, data.modNames, mc, screenWidth, modY);
    }
    private static ITextComponent getEnhancementTitle(int tier, String entityType) {
        TextFormatting color = isIronGolem(entityType) ? TextFormatting.GRAY
                : (tier == 1 ? TextFormatting.GREEN
                : (tier == 2 ? TextFormatting.BLUE
                : (tier == 3 ? TextFormatting.RED : TextFormatting.LIGHT_PURPLE))); // tier=0 紫
        switch (tier) {
            case 1: return new StringTextComponent("[I]").mergeStyle(color);
            case 2: return new StringTextComponent("[II]").mergeStyle(color);
            case 3: return new StringTextComponent("[III]").mergeStyle(color);
            default: return new StringTextComponent("[0]").mergeStyle(color);
        }
    }
    private static void drawBorderWithInnerGlow(MatrixStack matrixStack, int x, int y, int tier, String entityType) {
        int borderColor = getBorderColor();
        fill(matrixStack, x, y, x + EnhancementHUD.BOSS_BAR_WIDTH, y + BAR_BORDER_WIDTH, borderColor);
        fill(matrixStack, x, y + EnhancementHUD.BOSS_BAR_HEIGHT - BAR_BORDER_WIDTH, x + EnhancementHUD.BOSS_BAR_WIDTH, y + EnhancementHUD.BOSS_BAR_HEIGHT, borderColor);
        fill(matrixStack, x, y + BAR_BORDER_WIDTH, x + BAR_BORDER_WIDTH, y + EnhancementHUD.BOSS_BAR_HEIGHT - BAR_BORDER_WIDTH, borderColor);
        fill(matrixStack, x + EnhancementHUD.BOSS_BAR_WIDTH - BAR_BORDER_WIDTH, y + BAR_BORDER_WIDTH, x + EnhancementHUD.BOSS_BAR_WIDTH, y + EnhancementHUD.BOSS_BAR_HEIGHT - BAR_BORDER_WIDTH, borderColor);
        int innerGlowColor;
        if (isIronGolem(entityType)) {
            innerGlowColor = getIronInnerGlowColor();
        } else if (isEnderDragonOrWither(entityType)) {
            innerGlowColor = ClientConfigCache.isTranslucentModeEnabled() ? 0x60FF69B4 : 0x90FF69B4;
        } else {
            if (tier == 1) innerGlowColor = 0x6033CC33;
            else if (tier == 2) innerGlowColor = 0x603333CC;
            else if (tier == 3) innerGlowColor = 0x60CC3333;
            else innerGlowColor = 0x609900FF;
        }
        int innerX = x + BAR_BORDER_WIDTH;
        int innerY = y + BAR_BORDER_WIDTH;
        int innerWidth = EnhancementHUD.BOSS_BAR_WIDTH - 2 * BAR_BORDER_WIDTH;
        int innerHeight = EnhancementHUD.BOSS_BAR_HEIGHT - 2 * BAR_BORDER_WIDTH;
        fill(matrixStack, innerX, innerY, innerX + innerWidth, innerY + 1, innerGlowColor);
        fill(matrixStack, innerX, innerY + innerHeight - 1, innerX + innerWidth, innerY + innerHeight, innerGlowColor);
        fill(matrixStack, innerX, innerY + 1, innerX + 1, innerY + innerHeight - 1, innerGlowColor);
        fill(matrixStack, innerX + innerWidth - 1, innerY + 1, innerX + innerWidth, innerY + innerHeight - 1, innerGlowColor);
    }
    private static void drawOuterFrameDarkMagic(MatrixStack ms, int x, int y) {
        boolean translucent = ClientConfigCache.isTranslucentModeEnabled();
        int fogOuter   = translucent ? 0x10000000 : 0x18000000;
        int fogInner   = translucent ? 0x18000000 : 0x24000000;
        int obsidian   = translucent ? 0x80202020 : 0xFF151515;
        int cursedEdge = translucent ? 0x50300040 : 0x80300040;
        int runeBright = translucent ? 0x60AA33FF : 0xB0AA33FF;
        int runeDim    = translucent ? 0x30300040 : 0x50300040;
        fill(ms, x - 2, y - 2, x + EnhancementHUD.BOSS_BAR_WIDTH + 2, y + EnhancementHUD.BOSS_BAR_HEIGHT + 2, fogOuter);
        fill(ms, x - 1, y - 1, x + EnhancementHUD.BOSS_BAR_WIDTH + 1, y + EnhancementHUD.BOSS_BAR_HEIGHT + 1, fogInner);
        fill(ms, x, y, x + EnhancementHUD.BOSS_BAR_WIDTH, y + 1, obsidian);
        fill(ms, x, y + EnhancementHUD.BOSS_BAR_HEIGHT - 1, x + EnhancementHUD.BOSS_BAR_WIDTH, y + EnhancementHUD.BOSS_BAR_HEIGHT, obsidian);
        fill(ms, x, y, x + 1, y + EnhancementHUD.BOSS_BAR_HEIGHT, obsidian);
        fill(ms, x + EnhancementHUD.BOSS_BAR_WIDTH - 1, y, x + EnhancementHUD.BOSS_BAR_WIDTH, y + EnhancementHUD.BOSS_BAR_HEIGHT, obsidian);
        fill(ms, x + 1, y + 1, x + EnhancementHUD.BOSS_BAR_WIDTH - 1, y + 2, cursedEdge);
        fill(ms, x + 1, y + EnhancementHUD.BOSS_BAR_HEIGHT - 2, x + EnhancementHUD.BOSS_BAR_WIDTH - 1, y + EnhancementHUD.BOSS_BAR_HEIGHT - 1, cursedEdge);
        fill(ms, x + 1, y + 1, x + 2, y + EnhancementHUD.BOSS_BAR_HEIGHT - 1, cursedEdge);
        fill(ms, x + EnhancementHUD.BOSS_BAR_WIDTH - 2, y + 1, x + EnhancementHUD.BOSS_BAR_WIDTH - 1, y + EnhancementHUD.BOSS_BAR_HEIGHT - 1, cursedEdge);
        fill(ms, x + 2, y + 2, x + 5, y + 3, runeDim);
        fill(ms, x + 2, y + 2, x + 3, y + 5, runeDim);
        fill(ms, x + EnhancementHUD.BOSS_BAR_WIDTH - 5, y + 2, x + EnhancementHUD.BOSS_BAR_WIDTH - 2, y + 3, runeDim);
        fill(ms, x + EnhancementHUD.BOSS_BAR_WIDTH - 3, y + 2, x + EnhancementHUD.BOSS_BAR_WIDTH - 2, y + 5, runeDim);
        fill(ms, x + 2, y + EnhancementHUD.BOSS_BAR_HEIGHT - 3, x + 5, y + EnhancementHUD.BOSS_BAR_HEIGHT - 2, runeDim);
        fill(ms, x + 2, y + EnhancementHUD.BOSS_BAR_HEIGHT - 5, x + 3, y + EnhancementHUD.BOSS_BAR_HEIGHT - 2, runeDim);
        fill(ms, x + EnhancementHUD.BOSS_BAR_WIDTH - 5, y + EnhancementHUD.BOSS_BAR_HEIGHT - 3, x + EnhancementHUD.BOSS_BAR_WIDTH - 2, y + EnhancementHUD.BOSS_BAR_HEIGHT - 2, runeDim);
        fill(ms, x + EnhancementHUD.BOSS_BAR_WIDTH - 3, y + EnhancementHUD.BOSS_BAR_HEIGHT - 5, x + EnhancementHUD.BOSS_BAR_WIDTH - 2, y + EnhancementHUD.BOSS_BAR_HEIGHT - 2, runeDim);
        int pulseLen = Math.max(8, EnhancementHUD.BOSS_BAR_WIDTH / 10);
        int t = (int)(animationTimer % (EnhancementHUD.BOSS_BAR_WIDTH + EnhancementHUD.BOSS_BAR_HEIGHT + EnhancementHUD.BOSS_BAR_WIDTH + EnhancementHUD.BOSS_BAR_HEIGHT));
        int px = x, py = y;
        if (t < EnhancementHUD.BOSS_BAR_WIDTH) { px = x + t;
        }
        else if (t < EnhancementHUD.BOSS_BAR_WIDTH + EnhancementHUD.BOSS_BAR_HEIGHT) { px = x + EnhancementHUD.BOSS_BAR_WIDTH - 1; py = y + (t - EnhancementHUD.BOSS_BAR_WIDTH); }
        else if (t < EnhancementHUD.BOSS_BAR_WIDTH + EnhancementHUD.BOSS_BAR_HEIGHT + EnhancementHUD.BOSS_BAR_WIDTH) { px = x + (EnhancementHUD.BOSS_BAR_WIDTH - 1) - (t - (EnhancementHUD.BOSS_BAR_WIDTH + EnhancementHUD.BOSS_BAR_HEIGHT)); py = y + EnhancementHUD.BOSS_BAR_HEIGHT - 1; }
        else {
            py = y + (EnhancementHUD.BOSS_BAR_HEIGHT - 1) - (t - (EnhancementHUD.BOSS_BAR_WIDTH + EnhancementHUD.BOSS_BAR_HEIGHT + EnhancementHUD.BOSS_BAR_WIDTH)); }
        if (py == y || py == y + EnhancementHUD.BOSS_BAR_HEIGHT - 1) {
            int x1 = Math.max(x + 1, px - pulseLen / 2);
            int x2 = Math.min(x + EnhancementHUD.BOSS_BAR_WIDTH - 1, x1 + pulseLen);
            fill(ms, x1, py, x2, py + 1, runeBright);
        } else {
            int y1 = Math.max(y + 1, py - pulseLen / 2);
            int y2 = Math.min(y + EnhancementHUD.BOSS_BAR_HEIGHT - 1, y1 + pulseLen);
            fill(ms, px, y1, px + 1, y2, runeBright);
        }
    }
    private static void drawAnimatedHighlight(MatrixStack matrixStack, int x1, int y1, int x2, int y2, int tier, String entityType) {
        float offset = (animationTimer % 100) / 100.0f;
        int highlightWidth = (x2 - x1) / 5;
        int highlightX = (int) (x1 + (x2 - x1 - highlightWidth) * offset);

        int highlightColor = isIronGolem(entityType) ? getIronHighlightColor() : 0x30FFFFFF;
        fill(matrixStack, highlightX, y1, Math.min(highlightX + highlightWidth, x2), y2, highlightColor);
    }
    private static void drawGlow(MatrixStack matrixStack, int x, int y, int color) {
        int outerGlowColor = (color & 0x00FFFFFF) | 0x10000000;
        fill(matrixStack, x - GLOW_SIZE, y - GLOW_SIZE, x + EnhancementHUD.BOSS_BAR_WIDTH + GLOW_SIZE, y + EnhancementHUD.BOSS_BAR_HEIGHT + GLOW_SIZE, outerGlowColor);
        int innerGlowColor = (color & 0x00FFFFFF) | 0x20000000;
        fill(matrixStack, x - GLOW_SIZE / 2, y - GLOW_SIZE / 2, x + EnhancementHUD.BOSS_BAR_WIDTH + GLOW_SIZE / 2, y + EnhancementHUD.BOSS_BAR_HEIGHT + GLOW_SIZE / 2, innerGlowColor);
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
    private static void renderModifiers(MatrixStack matrixStack, List<String> modNames, Minecraft mc, int screenWidth, int yOffset) {
        FontRenderer font = mc.fontRenderer;
        if (font == null || modNames == null || modNames.isEmpty()) return;
        int totalMods = modNames.size();
        int modsInFirstLine = Math.min(totalMods, MAX_MODS_PER_LINE);
        int modsInSecondLine = totalMods > MAX_MODS_PER_LINE ? totalMods - MAX_MODS_PER_LINE : 0;
        renderModifierLine(matrixStack, modNames.subList(0, modsInFirstLine), mc, screenWidth, yOffset);
        if (modsInSecondLine > 0) {
            renderModifierLine(matrixStack, modNames.subList(MAX_MODS_PER_LINE, totalMods), mc, screenWidth, yOffset + PADDING_MOD_LINE);
        }
    }
    private static void renderModifierLine(MatrixStack matrixStack, List<String> modNames, Minecraft mc, int screenWidth, int yOffset) {
        FontRenderer font = mc.fontRenderer;
        if (font == null || modNames == null || modNames.isEmpty()) return;
        int totalWidth = 0;
        List<String> texts = new ArrayList<>();
        boolean isBlindMode = ClientConfigCache.isBlindModeEnabled();
        for (String modNameWithLevel : modNames) {
            String[] parts = modNameWithLevel.split(":");
            String modKey = parts[0];
            String level = parts.length > 1 ? parts[1] : "1";
            String displayName = isBlindMode
                    ? "??"
                    : new TranslationTextComponent(MOD_LOCALIZATION.getOrDefault(modKey, modKey)).getString();
            String fullText = displayName + "Lv." + level;
            texts.add(fullText);
            totalWidth += font.getStringWidth(fullText) + PADDING_MOD_ITEM;
        }
        totalWidth -= PADDING_MOD_ITEM;
        int xOffset = (screenWidth - totalWidth) / 2;
        matrixStack.push();
        matrixStack.scale(GLOBAL_TEXT_SCALE, GLOBAL_TEXT_SCALE, 1.0F);
        int modTextColor = ClientConfigCache.isTranslucentModeEnabled() ? 0x80FFFFFF : 0xFFFFFFFF;
        for (String fullText : texts) {
            float scaledX = xOffset / GLOBAL_TEXT_SCALE;
            float scaledY = yOffset / GLOBAL_TEXT_SCALE;
            drawTextCompat(font, matrixStack, fullText, scaledX, scaledY, modTextColor);
            xOffset += font.getStringWidth(fullText) + PADDING_MOD_ITEM;
        }
        matrixStack.pop();
    }
    private static void drawTextCompat(FontRenderer font, MatrixStack ms, String text, float x, float y, int color) {
        if (ClientConfigCache.isTranslucentModeEnabled()) {
            font.drawString(ms, text, x, y, color);
        } else {
            font.drawStringWithShadow(ms, text, x, y, color);
        }
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
