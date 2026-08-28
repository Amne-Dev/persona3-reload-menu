package com.amnedev.p3rmenu.v121;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.Util;

import java.util.Locale;

/** Screen-scoped P3R wipe. It never hooks gameplay mouse or keyboard input. */
public final class Transition {
    private enum Phase { NONE, OUT, IN }

    private static Phase phase = Phase.NONE;
    private static long startedAt;
    private static Runnable action;
    private static String label = "";
    private static boolean dispatching;

    private Transition() {
    }

    public static void startOut(Component actionLabel, Runnable queuedAction) {
        if (phase != Phase.NONE || queuedAction == null) {
            return;
        }
        phase = Phase.OUT;
        startedAt = Util.getMillis();
        action = queuedAction;
        label = actionLabel == null ? "" : actionLabel.getString().strip().toUpperCase(Locale.ROOT);
    }

    public static void onScreenInitialized() {
        if (phase == Phase.OUT && action == null) {
            startIn();
        }
    }

    public static boolean blocksScreenInput() {
        return phase != Phase.NONE && Minecraft.getInstance().screen != null;
    }

    public static boolean isActive() {
        return phase != Phase.NONE;
    }

    public static boolean isDispatching() {
        return dispatching;
    }

    public static void runWithoutInterception(Runnable runnable) {
        dispatching = true;
        try {
            runnable.run();
        } finally {
            dispatching = false;
        }
    }

    public static void clearIfInGame() {
        if (Minecraft.getInstance().screen == null) {
            clear();
        }
    }

    public static void extract(GuiGraphics graphics, int width, int height) {
        if (phase == Phase.NONE) {
            return;
        }

        float duration = phase == Phase.OUT ? 430.0F : 520.0F;
        float raw = Mth.clamp((Util.getMillis() - startedAt) / duration, 0.0F, 1.0F);
        if (phase == Phase.OUT && raw >= 1.0F && action != null) {
            Runnable queued = action;
            action = null;
            runWithoutInterception(queued);
            if (Minecraft.getInstance().screen == null) {
                clear();
            } else if (phase == Phase.OUT) {
                startIn();
            }
            return;
        }
        if (phase == Phase.IN && raw >= 1.0F) {
            clear();
            return;
        }

        graphics.nextStratum();
        int edge = Math.max(92, Math.min(280, width / 3));
        int skew = Math.max(44, Math.min(126, height / 5));
        if (phase == Phase.OUT) {
            float t = raw < 0.5F
                    ? 4.0F * raw * raw * raw
                    : 1.0F - (float) Math.pow(-2.0F * raw + 2.0F, 3.0D) / 2.0F;
            int left = width - Math.round((width + edge + skew) * t);
            P3RGraphics.skewedRect(graphics, left - 32, 0, edge + 38, height,
                    skew, 0xB8041668);
            P3RGraphics.skewedRect(graphics, left - 8, 0, 18, height,
                    skew, 0xD85AEAFF);
            P3RGraphics.skewedRect(graphics, left, 0, edge, height,
                    skew, P3RGraphics.BLUE);
            graphics.fill(left + edge, 0, width + edge + skew, height, P3RGraphics.BLUE);
            drawLabel(graphics, left + edge / 4, width, height, raw);
        } else {
            float t = P3RGraphics.easeOut(raw);
            int right = width + edge - Math.round((width + edge + skew) * t);
            graphics.fill(-edge, 0, right, height, P3RGraphics.BLUE);
            P3RGraphics.skewedRect(graphics, right - edge, 0, edge, height,
                    skew, P3RGraphics.BLUE);
            P3RGraphics.skewedRect(graphics, right + 8, 0, 16, height,
                    skew, 0xA85AEAFF);
        }
    }

    private static void drawLabel(GuiGraphics graphics, int x, int width, int height, float progress) {
        if (label.isBlank()) {
            return;
        }
        float phaseProgress = Mth.clamp((progress - 0.10F) / 0.72F, 0.0F, 1.0F);
        int alpha = Mth.clamp(Math.round(185.0F * (float) Math.sin(Math.PI * phaseProgress)), 0, 185);
        if (alpha == 0) {
            return;
        }
        Component fragment = P3RGraphics.bold(label.substring(0, Math.min(4, label.length())));
        float scale = Mth.clamp(height / 38.0F, 7.0F, 15.0F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, height * 0.52F - 5.0F * scale);
        graphics.pose().scale(scale, scale);
        graphics.drawString(Minecraft.getInstance().font, fragment, 1, 1,
                (Math.min(150, alpha) << 24) | 0x02154E, false);
        graphics.drawString(Minecraft.getInstance().font, fragment, 0, 0,
                (alpha << 24) | 0x5AEAFF, false);
        graphics.pose().popMatrix();
    }

    private static void startIn() {
        phase = Phase.IN;
        startedAt = Util.getMillis();
        action = null;
        label = "";
    }

    private static void clear() {
        phase = Phase.NONE;
        startedAt = 0L;
        action = null;
        label = "";
    }
}
