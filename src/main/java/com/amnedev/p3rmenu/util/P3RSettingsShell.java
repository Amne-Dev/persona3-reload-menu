package com.amnedev.p3rmenu.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import com.amnedev.p3rmenu.screen.WallpaperScreen;
import com.amnedev.p3rmenu.screen.P3RMenuSettingsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared geometry, layout, and motion for the P3R-inspired settings family. */
public final class P3RSettingsShell {
    public static final int WHITE = 0xFFF5F6F8;
    public static final int INK = 0xFF05070C;
    public static final int PALE_CYAN = 0xFF9CF3F4;
    public static final int CYAN = 0xFF58E7FF;
    public static final int COBALT = 0xFF1715B8;
    public static final int DEEP_BLUE = 0xFF100872;
    public static final int RED = 0xFFF0442E;
    public static final int PINK = 0xFFFF3E98;

    private P3RSettingsShell() {
    }

    public static boolean isSettingsRoot(Screen screen) {
        return screen instanceof OptionsScreen;
    }

    public static boolean isSettingsDetail(Screen screen) {
        return screen instanceof WallpaperScreen || screen instanceof P3RMenuSettingsScreen
                || screen instanceof GameOptionsScreen && !(screen instanceof OptionsScreen);
    }

    public static float uiScale(int width, int height) {
        return MathHelper.clamp(Math.min(width / 960.0F, height / 540.0F), 0.72F, 1.55F);
    }

    public static float entrance(long startedAt) {
        float raw = MathHelper.clamp((Util.getMeasuringTimeMs() - startedAt) / 360.0F,
                0.0F, 1.0F);
        return sharpOut(raw);
    }

    public static float sharpOut(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        float inverse = 1.0F - t;
        return 1.0F - inverse * inverse * inverse;
    }

    public static void renderRootBackground(DrawContext context, int width, int height,
            long startedAt) {
        renderConfigBackground(context, width, height, startedAt);
    }

    public static void renderDetailBackground(DrawContext context, int width, int height,
            long startedAt) {
        renderConfigBackground(context, width, height, startedAt);
    }

    private static void renderConfigBackground(DrawContext context, int width, int height,
            long startedAt) {
        float intro = entrance(startedAt);
        float slide = (1.0F - intro) * width * 0.12F;
        int footerTop = Math.round(height * 0.815F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.drawTexture(WallpaperManager.getBackgroundTexture(), 0, 0,
                0, 0.0F, 0.0F,
                width, height, width, height);
        context.fill(0, 0, width, height, 0xB607123B);

        // A single continuous edge keeps the foreground independent of the
        // wallpaper without creating the old rectangle/triangle seam.
        int panelTopRight = Math.round(width * 0.740F - slide);
        int panelBottomRight = Math.round(width * 0.770F - slide);
        int panelBottom = footerTop + 2;
        int echo = Math.round(width * 0.045F);
        drawSlantedPanel(context, panelTopRight + echo,
                panelBottomRight + echo, panelBottom, 0x6B203271);
        drawSlantedPanel(context, panelTopRight,
                panelBottomRight, panelBottom, 0xEAB7C0D8);

        context.fill(0, footerTop, width, height, 0xFFF6F7F8);
        context.fill(0, footerTop, Math.round(width * 0.63F * intro),
                footerTop + Math.max(2, Math.round(height * 0.004F)), 0xFFF0442E);
        RenderSystem.disableBlend();
    }

    private static void drawSlantedPanel(DrawContext context,
            int topRight, int bottomRight, int height, int color) {
        int safeHeight = Math.max(1, height);
        for (int y = 0; y < safeHeight; y++) {
            float progress = y / (float) safeHeight;
            int right = Math.round(MathHelper.lerp(progress, topRight, bottomRight));
            context.fill(0, y, right, y + 1, color);
        }
    }

    public static void renderSelection(DrawContext context, int x, int y,
            int right, int height, float progress) {
        float eased = sharpOut(progress);
        int shownRight = Math.round(x + (right - x) * eased);
        int skew = Math.max(7, Math.round(height * 0.42F));
        context.fill(x - 3, y - 3, shownRight, y,
                withAlpha(RED, Math.round(255.0F * eased)));
        P3RHelper.drawSkewedRect(context, x - skew, y,
                Math.max(1, shownRight - x + skew), height, skew,
                withAlpha(WHITE, Math.round(255.0F * eased)));
        context.fill(x - 3, y, x, y + height,
                withAlpha(PINK, Math.round(255.0F * eased)));
    }

    public static void renderRootFooter(DrawContext context, Text selected,
            int width, int height, float intro) {
        renderRootFooter(context, selected, width, height, intro,
                width - Math.max(12, Math.round(18.0F * uiScale(width, height))));
    }

    public static void renderRootFooter(DrawContext context, Text selected,
            int width, int height, float intro, int selectedRight) {
        MinecraftClient client = MinecraftClient.getInstance();
        float uiScale = uiScale(width, height);
        String value = selected == null ? "" : selected.getString();
        Text prompt = Text.literal(value).setStyle(Style.EMPTY.withBold(true));
        Text controls = Text.literal("ENTER  SELECT     ESC  BACK")
                .setStyle(Style.EMPTY.withBold(true));
        int alpha = MathHelper.clamp(Math.round(255.0F * intro), 0, 255);
        int footerTop = Math.round(height * 0.815F);
        renderFooterWord(context, footerTop, uiScale, alpha);

        int right = width - Math.max(12, Math.round(18.0F * uiScale));
        int y = footerTop + Math.max(10, Math.round(12.0F * uiScale));
        int laneRight = Math.min(right, selectedRight);
        int laneLeft = Math.min(laneRight - 8, Math.max(
                Math.round(width * 0.38F),
                Math.round(150.0F * uiScale)));
        int laneWidth = Math.max(8, laneRight - laneLeft);
        drawRightFittedText(context, prompt, laneRight, y, laneWidth,
                (alpha << 24) | 0x222A3E);
        drawRightFittedText(context, controls, laneRight,
                y + Math.max(10, Math.round(11.0F * uiScale)), laneWidth,
                (Math.min(225, alpha) << 24) | 0x3D5875);
    }

    public static int footerActionWidth(int width, int height) {
        return MathHelper.clamp(Math.round(width * 0.20F), 78, 132);
    }

    public static int footerActionX(int width, int height) {
        return width - Math.max(14, Math.round(20.0F * uiScale(width, height)))
                - footerActionWidth(width, height);
    }

    public static int footerActionY(int width, int height) {
        return Math.round(height * 0.84F);
    }

    public static boolean footerActionContains(double mouseX, double mouseY,
            int width, int height) {
        int x = footerActionX(width, height);
        int y = footerActionY(width, height);
        return mouseX >= x && mouseX <= x + footerActionWidth(width, height)
                && mouseY >= y && mouseY <= y + 20;
    }

    public static void renderFooterAction(DrawContext context, Text label,
            int width, int height, float intro, boolean selected, boolean active) {
        int x = footerActionX(width, height);
        int y = footerActionY(width, height);
        int right = x + footerActionWidth(width, height);
        int bottom = y + 20;
        context.fill(x, y, right, bottom, withAlpha(
                active ? 0xE10B1022 : 0xA03B4150, Math.round(255.0F * intro)));
        if (selected) {
            context.fill(x, y, right, y + 2,
                    withAlpha(RED, Math.round(255.0F * intro)));
            context.fill(x, bottom - 2, right, bottom,
                    withAlpha(PINK, Math.round(255.0F * intro)));
        }
        int color = !active ? 0xFF7E8799 : selected ? WHITE : CYAN;
        drawFittedText(context, label, x + 7, y + 10,
                Math.max(8, right - x - 14),
                withAlpha(color, Math.round(255.0F * intro)), true);
    }

    public static void renderDetailFooter(DrawContext context,
            int width, int height, float intro) {
        MinecraftClient client = MinecraftClient.getInstance();
        float uiScale = uiScale(width, height);
        int alpha = MathHelper.clamp(Math.round(255.0F * intro), 0, 255);
        int footerTop = Math.round(height * 0.815F);
        renderFooterWord(context, footerTop, uiScale, alpha);

        Text controls = Text.literal("ENTER  SELECT     ESC  BACK")
                .setStyle(Style.EMPTY.withBold(true));
        int right = footerActionX(width, height)
                - Math.max(6, Math.round(8.0F * uiScale));
        int left = Math.min(right - 8, Math.max(
                Math.round(width * 0.38F), Math.round(150.0F * uiScale)));
        int y = height - Math.max(12, Math.round(17.0F * uiScale));
        drawRightFittedText(context, controls, right, y,
                Math.max(8, right - left),
                (Math.min(225, alpha) << 24) | 0x3D5875);
    }

    private static void renderFooterWord(DrawContext context, int footerTop,
            float uiScale, int alpha) {
        Text config = Text.literal("CONFIG").setStyle(Style.EMPTY.withBold(true));
        context.getMatrices().push();
        context.getMatrices().translate(8.0F * uiScale, footerTop + 2.0F * uiScale, 20.0F);
        context.getMatrices().scale(6.7F * uiScale, 6.7F * uiScale, 1.0F);
        context.drawText(MinecraftClient.getInstance().textRenderer, config, 0, 0,
                (Math.min(145, alpha) << 24) | 0xAAB0B8, false);
        context.getMatrices().pop();
    }

    /** Draws bold UI copy without allowing long translations to collide with values. */
    public static void drawFittedText(DrawContext context, Text text,
            float left, float centerY, float maxWidth, int color, boolean centered) {
        MinecraftClient client = MinecraftClient.getInstance();
        int textWidth = Math.max(1, client.textRenderer.getWidth(text));
        float scale = MathHelper.clamp(maxWidth / textWidth, 0.48F, 1.0F);
        float drawnWidth = textWidth * scale;
        float x = centered ? left + Math.max(0.0F, (maxWidth - drawnWidth) * 0.5F) : left;
        context.getMatrices().push();
        context.getMatrices().translate(x, centerY - 4.0F * scale, 80.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawText(client.textRenderer, text, 0, 0, color, true);
        context.getMatrices().pop();
    }

    private static void drawRightFittedText(DrawContext context, Text text,
            float right, float top, float maxWidth, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        int textWidth = Math.max(1, client.textRenderer.getWidth(text));
        float scale = MathHelper.clamp(maxWidth / textWidth, 0.48F, 1.0F);
        context.getMatrices().push();
        context.getMatrices().translate(right - textWidth * scale,
                top + (1.0F - scale) * 4.0F, 80.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawText(client.textRenderer, text, 0, 0, color, true);
        context.getMatrices().pop();
    }

    public static void renderDetailHeader(DrawContext context, Text title,
            int width, int height, long startedAt) {
        float intro = entrance(startedAt);
        float uiScale = uiScale(width, height);
        String heading = title.getString().toUpperCase(Locale.ROOT);
        Text text = Text.literal(heading).setStyle(Style.EMPTY.withBold(true));

        context.getMatrices().push();
        context.getMatrices().translate(width * 0.055F - (1.0F - intro) * 24.0F * uiScale,
                height * 0.045F, 70.0F);
        float scale = Math.min(1.55F * uiScale,
                Math.max(0.86F, width * 0.30F
                        / Math.max(1, MinecraftClient.getInstance().textRenderer.getWidth(text))));
        context.getMatrices().scale(scale, scale, 1.0F);
        int alpha = MathHelper.clamp(Math.round(255.0F * intro), 0, 255);
        context.drawText(MinecraftClient.getInstance().textRenderer, text,
                0, 0, (alpha << 24) | 0x58D7E5, true);
        context.getMatrices().pop();

        int x = Math.round(width * 0.055F);
        int y = Math.max(22, Math.round(height * 0.078F));
        context.fill(x, y, Math.round(x + width * 0.24F * intro),
                y + Math.max(1, Math.round(2.0F * uiScale)), CYAN);
    }

    public static void layoutOptionList(Screen screen, OptionListWidget list) {
        float uiScale = uiScale(screen.width, screen.height);
        int top = Math.max(42, Math.round(screen.height * 0.105F));
        int bottom = Math.round(screen.height * 0.80F);
        list.updateSize(screen.width, screen.height, top, bottom);
        list.setRenderBackground(false);
        list.setRenderHorizontalShadows(false);

        int left = Math.round(screen.width * 0.095F);
        int right = Math.round(screen.width * 0.625F);
        int gap = Math.max(6, Math.round(8.0F * uiScale));
        for (Object rawEntry : list.children()) {
            List<ClickableWidget> widgets = new ArrayList<>();
            if (rawEntry instanceof ParentElement entry) {
                for (Element element : entry.children()) {
                    if (element instanceof ClickableWidget widget) {
                        widgets.add(widget);
                    }
                }
            }
            int columns = Math.max(1, widgets.size());
            int widgetWidth = Math.max(90, (right - left - gap * (columns - 1)) / columns);
            for (int i = 0; i < widgets.size(); i++) {
                ClickableWidget widget = widgets.get(i);
                widget.setX(left + i * (widgetWidth + gap));
                widget.setWidth(widgetWidth);
            }
        }
        layoutFooterButtons(screen);
    }

    public static void layoutFooterButtons(Screen screen) {
        List<ClickableWidget> buttons = new ArrayList<>();
        for (Element element : screen.children()) {
            if (element instanceof ClickableWidget widget && widget.visible) {
                buttons.add(widget);
            }
        }
        if (buttons.isEmpty()) {
            return;
        }
        float uiScale = uiScale(screen.width, screen.height);
        int gap = Math.max(6, Math.round(8.0F * uiScale));
        int right = screen.width - Math.max(14, Math.round(20.0F * uiScale));
        int buttonWidth = Math.max(90, Math.min(132,
                (Math.round(screen.width * 0.60F) - gap * (buttons.size() - 1))
                        / Math.max(1, buttons.size())));
        int total = buttons.size() * buttonWidth + (buttons.size() - 1) * gap;
        int x = right - total;
        int y = Math.round(screen.height * 0.84F);
        for (ClickableWidget button : buttons) {
            button.setX(x);
            button.setY(y);
            button.setWidth(buttonWidth);
            x += buttonWidth + gap;
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }
}
