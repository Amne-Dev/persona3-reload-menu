package com.amnedev.p3rmenu.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

public class TransitionManager {
    public static final int TRANSITION_NONE = 0;
    public static final int TRANSITION_IN = 1;  // Reveal (Screen is covered, then revealed)
    public static final int TRANSITION_OUT = 2; // Cover (Screen is visible, then covered)

    private static int transitionType = TRANSITION_NONE;
    private static float transitionProgress = 0.0f;
    private static Runnable queuedAction = null;
    private static String transitionLabel = "";
    
    // Config
    private static final int COLOR_BLUE = 0xFF0647E6;
    private static final int COLOR_DEEP_BLUE = 0xFF071C70;
    private static final int COLOR_CYAN = 0xFF58E7FF;

    private static long lastTime = 0;
    private static final float OUT_DURATION = 430.0F;
    private static final float IN_DURATION = 520.0F;

    private static Runnable pendingExecution = null;

    public static void startOut(Runnable action) {
        startOut(null, action);
    }

    public static void startOut(Text label, Runnable action) {
        transitionType = TRANSITION_OUT;
        transitionProgress = 0.0f;
        queuedAction = action;
        transitionLabel = label == null ? "" : label.getString().strip().toUpperCase(Locale.ROOT);
        pendingExecution = null;
        lastTime = Util.getMeasuringTimeMs();
    }

    public static void checkPendingExecution() {
        if (pendingExecution != null) {
            Runnable action = pendingExecution;
            pendingExecution = null;
            action.run();

            // Opening a world closes the final menu instead of initializing a new
            // Screen, so ScreenMixin never gets a chance to start the reveal. Do
            // not leave the global input hooks latched in that state.
            if (transitionType == TRANSITION_OUT) {
                if (MinecraftClient.getInstance().currentScreen == null) {
                    clear();
                } else {
                    startIn();
                }
            }
        }
    }

    public static void startIn() {
        transitionType = TRANSITION_IN;
        transitionProgress = 0.0f;
        queuedAction = null;
        transitionLabel = "";
        lastTime = Util.getMeasuringTimeMs();
    }

    public static boolean isTransitioning() {
        return transitionType != TRANSITION_NONE;
    }

    public static boolean isBlockingInput() {
        // These transitions belong to GUI screens only. Gameplay must never be
        // blocked by a stale menu animation, even if another mod changes screens
        // without passing through Screen.init.
        return transitionType != TRANSITION_NONE
                && MinecraftClient.getInstance().currentScreen != null;
    }

    public static void clear() {
        transitionType = TRANSITION_NONE;
        transitionProgress = 0.0F;
        queuedAction = null;
        pendingExecution = null;
        transitionLabel = "";
        lastTime = 0L;
    }

    public static void render(DrawContext context, float delta, int width, int height) {
        if (transitionType == TRANSITION_NONE) return;

        // Derive progress from the transition start. Incremental frame deltas made
        // wipes crawl (and input appear frozen) whenever Minecraft throttled an
        // unfocused window to a very low frame rate.
        long now = Util.getMeasuringTimeMs();
        float duration = transitionType == TRANSITION_OUT ? OUT_DURATION : IN_DURATION;
        transitionProgress = MathHelper.clamp((now - lastTime) / duration, 0.0F, 1.0F);

        if (transitionProgress >= 1.0f) {
            transitionProgress = 1.0f;

            if (transitionType == TRANSITION_OUT) {
                if (queuedAction != null) {
                    // setScreen is safe on the render thread. Dispatching at the
                    // covered edge avoids relying on another MinecraftClient frame
                    // hook, which could leave the wipe and GUI input latched forever.
                    Runnable action = queuedAction;
                    queuedAction = null;
                    action.run();
                    if (MinecraftClient.getInstance().currentScreen == null) {
                        clear();
                    } else if (transitionType == TRANSITION_OUT) {
                        startIn();
                    }
                    return;
                }
                clear();
                return;
            } else {
                // IN Complete
                transitionType = TRANSITION_NONE;
            }
        }
        
        // Render
        // Always draw on top (handled by calling last in render)
        // Ensure Z is high
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 500f); // High Z-Index

        int w = width;
        int h = height;
        int edgeWidth = Math.max(110, Math.min(340, w / 3));
        int skewOffset = Math.max(48, Math.min(132, h / 5));
        int solidWidth = w + edgeWidth + skewOffset + 8;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (transitionType == TRANSITION_OUT) {
            float t = easeInOutCubic(transitionProgress);
            int startX = (int) (w - (w + edgeWidth + skewOffset) * t);

            // A dark under-slice and a thin cyan glint give the wipe the layered,
            // cut-paper movement used throughout P3R without tying it to a background.
            P3RHelper.drawSkewedRect(context, startX - 34, 0,
                    edgeWidth + 42, h, skewOffset, withAlpha(COLOR_DEEP_BLUE, 0xB8));
            P3RHelper.drawSkewedRect(context, startX - 10, 0,
                    24, h, skewOffset, withAlpha(COLOR_CYAN, 0xD8));
            P3RHelper.drawGradientSkewedRect(context, startX, 0, edgeWidth, h, skewOffset,
                    0x000647E6, COLOR_BLUE);
            context.fill(startX + edgeWidth, 0,
                    startX + edgeWidth + solidWidth, h, COLOR_BLUE);
            renderTransitionType(context, startX, edgeWidth, w, h);

        } else if (transitionType == TRANSITION_IN) {
            float t = easeOutExpo(transitionProgress);
            int solidEndX = (int) (w - (w + edgeWidth + skewOffset) * t);

            context.fill(solidEndX - solidWidth, 0, solidEndX, h, COLOR_BLUE);
            P3RHelper.drawGradientSkewedRect(context, solidEndX - skewOffset, 0,
                    edgeWidth, h, skewOffset, COLOR_BLUE, 0x000647E6);
            P3RHelper.drawSkewedRect(context, solidEndX + edgeWidth - 12, 0,
                    18, h, skewOffset, withAlpha(COLOR_CYAN, 0xA8));
            P3RHelper.drawSkewedRect(context, solidEndX + edgeWidth + 12, 0,
                    34, h, skewOffset, withAlpha(COLOR_DEEP_BLUE, 0x6B));
        }

        RenderSystem.disableBlend();
        context.getMatrices().pop();
    }

    private static void renderTransitionType(DrawContext context, int wipeX,
            int edgeWidth, int width, int height) {
        if (transitionLabel.isBlank()) {
            return;
        }

        String fragment = transitionLabel.substring(0, Math.min(4, transitionLabel.length()));
        float phase = MathHelper.clamp((transitionProgress - 0.10F) / 0.72F, 0.0F, 1.0F);
        int alpha = MathHelper.clamp(Math.round(185.0F * (float) Math.sin(Math.PI * phase)), 0, 185);
        if (alpha <= 0) {
            return;
        }

        Text text = Text.literal(fragment).setStyle(Style.EMPTY.withBold(true));
        float scale = MathHelper.clamp(height / 38.0F, 7.0F, 15.0F);
        float x = wipeX + edgeWidth * 0.20F;
        float y = height * 0.52F - 5.0F * scale;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 510.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        int shadow = (Math.min(150, alpha) << 24) | 0x02154E;
        int foreground = (alpha << 24) | 0x58E7FF;
        context.drawText(MinecraftClient.getInstance().textRenderer, text, 2, 1, shadow, false);
        context.drawText(MinecraftClient.getInstance().textRenderer, text, 0, 0, foreground, false);
        context.getMatrices().pop();
    }

    private static float easeInOutCubic(float value) {
        return value < 0.5F
                ? 4.0F * value * value * value
                : 1.0F - (float) Math.pow(-2.0F * value + 2.0F, 3.0D) / 2.0F;
    }

    private static float easeOutExpo(float value) {
        return value >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0F * value);
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
