package com.amnedev.p3rmenu.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared foreground shell for menu destinations that still need Minecraft's
 * native list widgets and actions. The replaceable title texture stays a
 * background layer; every branded shape is rendered independently above it.
 */
public final class P3RScreenShell {
    public static final int SPACE_MICRO = 4;
    public static final int SPACE_TIGHT = 8;
    public static final int SPACE_RELATED = 12;
    public static final int SPACE_CONTROL = 16;
    public static final int SPACE_GROUP = 24;
    public static final int SPACE_SECTION = 32;
    public static final int SPACE_MAJOR = 48;
    public static final int SPACE_HERO = 64;

    public static final int LAYER_BACKGROUND = 0;
    public static final int LAYER_SHELL = 10;
    public static final int LAYER_CONTENT = 30;
    public static final int LAYER_FOREGROUND = 60;

    private static final int BLUE = 0xFF080CB5;
    private static final int COBALT = 0xFF0647E6;
    private static final int DEEP_BLUE = 0xFF061343;
    private static final int CYAN = 0xFF58E7FF;
    private static final int WHITE = 0xFFF7FAFF;

    private P3RScreenShell() {
    }

    public static float uiScale(int width, int height) {
        float proportional = Math.min(width / 960.0F, height / 540.0F);
        return MathHelper.clamp(proportional, 0.72F, 1.55F);
    }

    public static float entrance(long startedAt) {
        float raw = (Util.getMeasuringTimeMs() - startedAt) / 520.0F;
        return easeOutExpo(MathHelper.clamp(raw, 0.0F, 1.0F));
    }

    public static float contentSlide(long startedAt, int width) {
        return (1.0F - entrance(startedAt)) * Math.min(96.0F, width * 0.12F);
    }

    public static void renderBackground(DrawContext context, int width, int height,
            long startedAt) {
        float intro = entrance(startedAt);
        float uiScale = uiScale(width, height);
        int slide = Math.round((1.0F - intro) * SPACE_HERO * uiScale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(WallpaperManager.getBackgroundTexture(), 0, 0,
                LAYER_BACKGROUND, 0.0F, 0.0F,
                width, height, width, height);

        // A restrained tint stabilizes legibility over any user-selected image.
        context.fill(0, 0, width, height, 0x76020A24);

        int leftPanelWidth = Math.round(width * 0.345F);
        int leftSkew = Math.max(SPACE_SECTION, Math.round(56.0F * uiScale));
        P3RHelper.drawSkewedRect(context, -leftSkew - slide, 0,
                leftPanelWidth + leftSkew, height, leftSkew, 0xD906123D);
        P3RHelper.drawSkewedRect(context, -leftSkew - slide, 0,
                Math.round(width * 0.105F) + leftSkew, height, leftSkew, 0xC9080CB5);

        int contentLeft = Math.round(width * 0.365F + slide);
        int contentTop = Math.max(SPACE_MAJOR, Math.round(52.0F * uiScale));
        int contentBottom = height - Math.max(SPACE_HERO, Math.round(68.0F * uiScale));
        int contentRight = width - Math.max(SPACE_CONTROL, Math.round(20.0F * uiScale));
        int contentSkew = Math.max(SPACE_RELATED, Math.round(22.0F * uiScale));
        P3RHelper.drawSkewedRect(context, contentLeft - contentSkew, contentTop,
                contentRight - contentLeft + contentSkew, Math.max(1, contentBottom - contentTop),
                contentSkew, 0xB306174F);

        int topRuleY = Math.max(SPACE_TIGHT, Math.round(12.0F * uiScale));
        context.fill(0, topRuleY, Math.round(width * 0.29F * intro),
                topRuleY + Math.max(2, Math.round(3.0F * uiScale)), CYAN);
        context.fill(0, height - Math.max(SPACE_TIGHT, Math.round(10.0F * uiScale)),
                Math.round(width * 0.34F * intro), height, BLUE);
        RenderSystem.disableBlend();
    }

    public static void renderFallbackBackground(DrawContext context, int width, int height) {
        float uiScale = uiScale(width, height);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(WallpaperManager.getBackgroundTexture(), 0, 0,
                LAYER_BACKGROUND, 0.0F, 0.0F,
                width, height, width, height);
        context.fill(0, 0, width, height, 0xB0040D2E);

        int accentWidth = Math.round(width * 0.13F);
        int skew = Math.max(SPACE_GROUP, Math.round(SPACE_MAJOR * uiScale));
        P3RHelper.drawSkewedRect(context, -skew, 0,
                accentWidth + skew, height, skew, 0xA9080CB5);
        context.fill(0, Math.max(SPACE_TIGHT, Math.round(12.0F * uiScale)),
                Math.round(width * 0.28F),
                Math.max(SPACE_RELATED, Math.round(15.0F * uiScale)), CYAN);
        RenderSystem.disableBlend();
    }

    public static void renderHeader(DrawContext context, String title, String subtitle,
            int width, int height, long startedAt) {
        MinecraftClient client = MinecraftClient.getInstance();
        float uiScale = uiScale(width, height);
        float intro = entrance(startedAt);
        float x = SPACE_GROUP * uiScale - (1.0F - intro) * SPACE_MAJOR * uiScale;
        float y = Math.max(SPACE_SECTION, 38.0F * uiScale);

        Text heading = Text.literal(title.toUpperCase(Locale.ROOT))
                .setStyle(Style.EMPTY.withBold(true));
        Text caption = Text.literal(subtitle.toUpperCase(Locale.ROOT))
                .setStyle(Style.EMPTY.withBold(true));

        context.getMatrices().push();
        context.getMatrices().translate(x, y, LAYER_FOREGROUND);
        float headingScale = 2.65F * uiScale;
        context.getMatrices().scale(headingScale, headingScale, 1.0F);
        int headingAlpha = MathHelper.clamp(Math.round(255.0F * intro), 0, 255);
        context.drawText(client.textRenderer, heading, 1, 1,
                (headingAlpha << 24) | 0x28324D, false);
        context.drawText(client.textRenderer, heading, 0, 0,
                (headingAlpha << 24) | (WHITE & 0x00FFFFFF), false);
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().translate(x + SPACE_MICRO * uiScale,
                y + SPACE_SECTION * uiScale, LAYER_FOREGROUND);
        float captionScale = 0.82F * uiScale;
        context.getMatrices().scale(captionScale, captionScale, 1.0F);
        int captionAlpha = MathHelper.clamp(Math.round(220.0F * intro), 0, 255);
        context.drawText(client.textRenderer, caption, 0, 0,
                (captionAlpha << 24) | (CYAN & 0x00FFFFFF), false);
        context.getMatrices().pop();

        int markerY = Math.round(y + SPACE_MAJOR * uiScale);
        int markerWidth = Math.round(82.0F * uiScale * intro);
        context.fill(Math.round(x), markerY, Math.round(x) + markerWidth,
                markerY + Math.max(2, Math.round(3.0F * uiScale)), COBALT);
    }

    public static void layoutButtons(Screen screen) {
        List<ButtonWidget> buttons = new ArrayList<>();
        for (Element element : screen.children()) {
            if (element instanceof ButtonWidget button && button.visible) {
                buttons.add(button);
            }
        }
        if (buttons.isEmpty()) {
            return;
        }

        float uiScale = uiScale(screen.width, screen.height);
        int regionLeft = Math.round(screen.width * 0.375F);
        int regionRight = screen.width - Math.max(SPACE_CONTROL, Math.round(20.0F * uiScale));
        int columns = buttons.size() <= 4 ? buttons.size() : Math.min(4, (buttons.size() + 1) / 2);
        int rows = (int) Math.ceil(buttons.size() / (double) columns);
        int gap = Math.max(SPACE_MICRO, Math.round(SPACE_TIGHT * uiScale));
        int buttonWidth = Math.max(64, (regionRight - regionLeft - gap * (columns - 1)) / columns);
        int rowStep = Math.max(20, Math.round(SPACE_GROUP * uiScale));
        int firstY = screen.height - Math.max(SPACE_MAJOR,
                Math.round((rows == 1 ? 38.0F : 58.0F) * uiScale));

        for (int i = 0; i < buttons.size(); i++) {
            ButtonWidget button = buttons.get(i);
            int row = i / columns;
            int rowStart = row * columns;
            int rowCount = Math.min(columns, buttons.size() - rowStart);
            int rowWidth = rowCount * buttonWidth + (rowCount - 1) * gap;
            int x = regionLeft + Math.max(0, (regionRight - regionLeft - rowWidth) / 2)
                    + (i - rowStart) * (buttonWidth + gap);
            button.setX(x);
            button.setY(firstY + row * rowStep);
            button.setWidth(buttonWidth);
        }
    }

    public static boolean isPersonaListScreen(Screen screen) {
        return screen instanceof net.minecraft.client.gui.screen.world.SelectWorldScreen
                || screen instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
    }

    private static float easeOutExpo(float value) {
        return value >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0D * value);
    }
}
