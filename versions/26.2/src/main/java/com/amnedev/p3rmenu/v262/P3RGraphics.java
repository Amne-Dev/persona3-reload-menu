package com.amnedev.p3rmenu.v262;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public final class P3RGraphics {
    public static final int WHITE = 0xFFF7FAFF;
    public static final int PALE = 0xFFE6EEFF;
    public static final int BLUE = 0xFF080CB5;
    public static final int DEEP_BLUE = 0xFF041668;
    public static final int CYAN = 0xFF5AEAFF;
    public static final int INK = 0xFF07123F;
    public static final int PINK = 0xFFFF3E98;
    public static final int RED = 0xFFF0442E;
    public static final int CONFIG_WHITE = 0xFFF5F6F8;
    public static final int CONFIG_INK = 0xFF05070C;

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath(
            "p3rmenu", "textures/gui/title/p3r_logo.png");

    private P3RGraphics() {
    }

    public static float scale(int width, int height) {
        return Mth.clamp(Math.min(width / 960.0F, height / 540.0F), 0.72F, 1.65F);
    }

    public static Component bold(String value) {
        return Component.literal(value.toUpperCase()).withStyle(style -> style.withBold(true));
    }

    public static void wallpaper(GuiGraphicsExtractor graphics, int width, int height) {
        int sourceWidth = Math.max(1, WallpaperManager.imageWidth());
        int sourceHeight = Math.max(1, WallpaperManager.imageHeight());
        float sourceAspect = sourceWidth / (float) sourceHeight;
        float targetAspect = width / (float) Math.max(1, height);
        float u0 = 0.0F;
        float u1 = 1.0F;
        float v0 = 0.0F;
        float v1 = 1.0F;
        if (sourceAspect > targetAspect) {
            float visible = targetAspect / sourceAspect;
            u0 = (1.0F - visible) * 0.5F;
            u1 = 1.0F - u0;
        } else if (sourceAspect < targetAspect) {
            float visible = sourceAspect / targetAspect;
            v0 = (1.0F - visible) * 0.5F;
            v1 = 1.0F - v0;
        }
        graphics.blit(WallpaperManager.texture(), 0, 0, width, height, u0, u1, v0, v1);
    }

    public static void logo(GuiGraphicsExtractor graphics, int width, int height, float intro) {
        float ui = scale(width, height);
        int logoWidth = Math.round(174.0F * ui);
        int logoHeight = Math.round(logoWidth * 800.0F / 900.0F);
        float x = width * 0.765F + (1.0F - intro) * 58.0F * ui;
        float y = Math.max(70.0F * ui, height * 0.175F) - (1.0F - intro) * 12.0F * ui;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().rotate((float) Math.toRadians(-4.0D));
        int alpha = Mth.clamp(Math.round(255.0F * intro), 0, 255);
        graphics.blit(RenderPipelines.GUI_TEXTURED, LOGO,
                -logoWidth / 2, -logoHeight / 2, 0.0F, 0.0F,
                logoWidth, logoHeight, 900, 800, 900, 800,
                (alpha << 24) | 0xFFFFFF);
        graphics.pose().popMatrix();
    }

    /** Parallelogram rendered as horizontal spans for the 26.2 render-state API. */
    public static void skewedRect(GuiGraphicsExtractor graphics, int x, int y,
            int width, int height, int skew, int color) {
        int safeHeight = Math.max(1, height);
        for (int row = 0; row < safeHeight; row++) {
            float progress = row / (float) safeHeight;
            int offset = Math.round(skew * (1.0F - progress));
            graphics.fill(x + offset, y + row, x + width + offset, y + row + 1, color);
        }
    }

    /** Slate-white configuration shell shared by the settings root and every submenu. */
    public static void configBackground(GuiGraphicsExtractor graphics, int width, int height,
            float intro) {
        wallpaper(graphics, width, height);
        graphics.fill(0, 0, width, height, 0xB607123B);
        int footerTop = Math.round(height * 0.815F);
        float slide = (1.0F - intro) * width * 0.12F;
        int panelTopRight = Math.round(width * 0.740F - slide);
        int panelBottomRight = Math.round(width * 0.770F - slide);
        int panelBottom = footerTop + 2;
        int echo = Math.round(width * 0.045F);
        slantedPanel(graphics, panelTopRight + echo, panelBottomRight + echo,
                panelBottom, 0x6B203271);
        slantedPanel(graphics, panelTopRight, panelBottomRight,
                panelBottom, 0xEAB7C0D8);
        graphics.fill(0, footerTop, width, height, 0xFFF6F7F8);
        graphics.fill(0, footerTop, Math.round(width * 0.63F * intro),
                footerTop + Math.max(2, Math.round(height * 0.004F)), RED);
    }

    public static void configHeader(GuiGraphicsExtractor graphics, Font font, String title,
            int width, int height, float intro) {
        float ui = scale(width, height);
        Component heading = bold(title);
        float headingScale = Math.min(1.55F * ui,
                Math.max(0.86F, width * 0.30F / Math.max(1, font.width(heading))));
        graphics.pose().pushMatrix();
        graphics.pose().translate(width * 0.055F - (1.0F - intro) * 24.0F * ui,
                height * 0.045F);
        graphics.pose().scale(headingScale, headingScale);
        graphics.text(font, heading, 0, 0, alpha(0xFF58D7E5, intro), true);
        graphics.pose().popMatrix();
        int x = Math.round(width * 0.055F);
        int y = Math.max(22, Math.round(height * 0.078F));
        graphics.fill(x, y, Math.round(x + width * 0.24F * intro),
                y + Math.max(1, Math.round(2.0F * ui)), CYAN);
    }

    public static void configFooter(GuiGraphicsExtractor graphics, Font font,
            Component selected, int width, int height, float intro) {
        configFooter(graphics, font, selected, width, height, intro,
                width - Math.max(12, Math.round(18.0F * scale(width, height))));
    }

    public static void configFooter(GuiGraphicsExtractor graphics, Font font,
            Component selected, int width, int height, float intro, int selectedRight) {
        float ui = scale(width, height);
        int footerTop = Math.round(height * 0.815F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(8.0F * ui, footerTop + 2.0F * ui);
        graphics.pose().scale(6.7F * ui, 6.7F * ui);
        graphics.text(font, bold("CONFIG"), 0, 0,
                alpha(0xFFAAB0B8, intro * 0.57F), false);
        graphics.pose().popMatrix();
        Component controls = bold("ENTER  SELECT     ESC  BACK");
        int right = width - Math.max(12, Math.round(18.0F * ui));
        int controlsY = height - Math.max(12, Math.round(17.0F * ui));
        graphics.text(font, controls, right - font.width(controls), controlsY,
                alpha(0xFF3D5875, Math.min(0.88F, intro)), true);
        if (selected != null && !selected.getString().isBlank()) {
            graphics.text(font, selected, selectedRight - font.width(selected),
                    footerTop + Math.max(10, Math.round(12.0F * ui)),
                    alpha(0xFF222A3E, intro), true);
        }
    }

    public static int footerActionWidth(int width, int height) {
        return Math.max(90, Math.min(132, Math.round(width * 0.60F)));
    }

    public static int footerActionX(int width, int height) {
        return width - Math.max(14, Math.round(20.0F * scale(width, height)))
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

    public static void configFooterAction(GuiGraphicsExtractor graphics, Font font,
            Component label, int width, int height, float intro,
            boolean selected, boolean active) {
        int x = footerActionX(width, height);
        int y = footerActionY(width, height);
        int right = x + footerActionWidth(width, height);
        int bottom = y + 20;
        graphics.fill(x, y, right, bottom,
                alpha(active ? 0xE10B1022 : 0xA03B4150, intro));
        if (selected) {
            graphics.fill(x, y, right, y + 2, alpha(RED, intro));
            graphics.fill(x, bottom - 2, right, bottom, alpha(PINK, intro));
        }
        int color = !active ? 0xFF7E8799 : selected ? CONFIG_WHITE : CYAN;
        fittedText(graphics, font, label, x + 7, y + 10,
                Math.max(8, right - x - 14), 1.0F, alpha(color, intro), true);
    }

    public static void configSelection(GuiGraphicsExtractor graphics, int x, int y,
            int right, int height, float progress) {
        float eased = sharpOut(progress);
        int shownRight = Math.round(x + (right - x) * eased);
        int skew = Math.max(7, Math.round(height * 0.42F));
        graphics.fill(x - 3, y - 3, shownRight, y, alpha(RED, eased));
        skewedRect(graphics, x - skew, y,
                Math.max(1, shownRight - x + skew), height, skew,
                alpha(CONFIG_WHITE, eased));
        graphics.fill(x - 3, y, x, y + height, alpha(PINK, eased));
    }

    public static void layoutFooterButtons(Screen screen) {
        List<AbstractButton> buttons = new ArrayList<>();
        int footerCandidateY = Math.round(screen.height * 0.68F);
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractButton button && button.visible
                    && button.getY() >= footerCandidateY) buttons.add(button);
        }
        if (buttons.isEmpty()) return;
        float ui = scale(screen.width, screen.height);
        int gap = Math.max(6, Math.round(8.0F * ui));
        int right = screen.width - Math.max(14, Math.round(20.0F * ui));
        int buttonWidth = Math.max(90, Math.min(132,
                (Math.round(screen.width * 0.60F) - gap * (buttons.size() - 1))
                        / Math.max(1, buttons.size())));
        int x = right - buttons.size() * buttonWidth - (buttons.size() - 1) * gap;
        int y = Math.round(screen.height * 0.84F);
        for (AbstractButton button : buttons) {
            button.setPosition(x, y);
            button.setWidth(buttonWidth);
            x += buttonWidth + gap;
        }
    }

    public static boolean isFooterButton(Screen screen, AbstractButton button) {
        return button.getY() >= Math.round(screen.height * 0.815F);
    }

    public static void listBackground(GuiGraphicsExtractor graphics, int width, int height,
            float intro) {
        wallpaper(graphics, width, height);
        graphics.fill(0, 0, width, height, 0x76020A24);
        int slide = Math.round((1.0F - intro) * Math.min(96.0F, width * 0.12F));
        int leftWidth = Math.round(width * 0.345F);
        int skew = Math.max(32, Math.round(56.0F * scale(width, height)));
        skewedRect(graphics, -skew - slide, 0, leftWidth + skew,
                height, skew, 0xD906123D);
        skewedRect(graphics, -skew - slide, 0, Math.round(width * 0.105F) + skew,
                height, skew, 0xC9080CB5);
        int contentLeft = Math.round(width * 0.365F + slide);
        int contentTop = Math.max(48, Math.round(52.0F * scale(width, height)));
        int contentBottom = height - Math.max(64, Math.round(68.0F * scale(width, height)));
        int contentRight = width - Math.max(16, Math.round(20.0F * scale(width, height)));
        skewedRect(graphics, contentLeft - 22, contentTop,
                contentRight - contentLeft + 22, Math.max(1, contentBottom - contentTop),
                22, 0xB306174F);
        int ruleY = Math.max(8, Math.round(12.0F * scale(width, height)));
        graphics.fill(0, ruleY, Math.round(width * 0.29F * intro),
                ruleY + Math.max(2, Math.round(3.0F * scale(width, height))), CYAN);
    }

    public static void listHeader(GuiGraphicsExtractor graphics, Font font,
            String title, String subtitle, int width, int height, float intro) {
        float ui = scale(width, height);
        float x = 24.0F * ui - (1.0F - intro) * 48.0F * ui;
        float y = Math.max(32.0F, 38.0F * ui);
        fittedText(graphics, font, bold(title), x, y + 10.0F * ui,
                width * 0.31F, 2.65F * ui, alpha(WHITE, intro), true);
        fittedText(graphics, font, bold(subtitle), x + 4.0F * ui,
                y + 39.0F * ui, width * 0.29F, 0.82F * ui,
                alpha(CYAN, intro), false);
        int markerY = Math.round(y + 48.0F * ui);
        graphics.fill(Math.round(x), markerY,
                Math.round(x + 82.0F * ui * intro),
                markerY + Math.max(2, Math.round(3.0F * ui)), BLUE);
    }

    private static void triangle(GuiGraphicsExtractor graphics,
            int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));
        for (int y = minY; y <= maxY; y++) {
            float[] hits = new float[3];
            int count = 0;
            count = edgeHit(x1, y1, x2, y2, y, hits, count);
            count = edgeHit(x2, y2, x3, y3, y, hits, count);
            count = edgeHit(x3, y3, x1, y1, y, hits, count);
            if (count >= 2) {
                float left = hits[0];
                float right = hits[1];
                for (int index = 2; index < count; index++) {
                    left = Math.min(left, hits[index]);
                    right = Math.max(right, hits[index]);
                }
                graphics.fill(Math.round(left), y, Math.round(right) + 1, y + 1, color);
            }
        }
    }

    /** One continuous right edge; unlike the old rectangle-plus-triangle it has no seam or tip. */
    private static void slantedPanel(GuiGraphicsExtractor graphics,
            int topRight, int bottomRight, int height, int color) {
        int safeHeight = Math.max(1, height);
        for (int y = 0; y < safeHeight; y++) {
            float progress = y / (float) safeHeight;
            int right = Math.round(Mth.lerp(progress, topRight, bottomRight));
            graphics.fill(0, y, right, y + 1, color);
        }
    }

    private static int edgeHit(int x1, int y1, int x2, int y2, int y,
            float[] hits, int count) {
        if (y1 == y2 || y < Math.min(y1, y2) || y > Math.max(y1, y2)) return count;
        float t = (y - y1) / (float) (y2 - y1);
        if (count < hits.length) hits[count++] = x1 + (x2 - x1) * t;
        return count;
    }

    public static void fittedText(GuiGraphicsExtractor graphics, Font font, Component text,
            float x, float centerY, float maxWidth, float preferredScale, int color, boolean shadow) {
        float baseWidth = Math.max(1.0F, font.width(text));
        float textScale = Math.min(preferredScale, maxWidth / baseWidth);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, centerY - 4.5F * textScale);
        graphics.pose().scale(textScale, textScale);
        graphics.text(font, text, 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    public static int alpha(int color, float multiplier) {
        int value = Mth.clamp(Math.round((color >>> 24) * Mth.clamp(multiplier, 0.0F, 1.0F)), 0, 255);
        return (value << 24) | (color & 0x00FFFFFF);
    }

    public static float easeOut(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0F * clamped);
    }

    public static float sharpOut(float value) {
        float inverse = 1.0F - Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }
}
