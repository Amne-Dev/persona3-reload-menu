package com.amnedev.p3rmenu.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class TransitionManager {
    public static final int TRANSITION_NONE = 0;
    public static final int TRANSITION_IN = 1;  // Reveal (Screen is covered, then revealed)
    public static final int TRANSITION_OUT = 2; // Cover (Screen is visible, then covered)

    private static int transitionType = TRANSITION_NONE;
    private static float transitionProgress = 0.0f;
    private static Runnable queuedAction = null;
    
    // Config
    private static final int COLOR_BLUE_STRIP = 0xFF0044FF; 
    private static final int SKEW_OFFSET = 100;

    private static long lastTime = 0;
    private static final float ANIMATION_DURATION = 500f; // ms

    private static Runnable pendingExecution = null;

    public static void startOut(Runnable action) {
        transitionType = TRANSITION_OUT;
        transitionProgress = 0.0f;
        queuedAction = action;
        lastTime = System.currentTimeMillis();
    }

    public static void checkPendingExecution() {
        if (pendingExecution != null) {
            Runnable action = pendingExecution;
            pendingExecution = null;
            action.run();
        }
    }

    public static void startIn() {
        transitionType = TRANSITION_IN;
        transitionProgress = 0.0f;
        queuedAction = null;
        lastTime = System.currentTimeMillis();
    }

    public static boolean isTransitioning() {
        return transitionType != TRANSITION_NONE;
    }

    public static boolean isBlockingInput() {
        return transitionType == TRANSITION_OUT;
    }

    public static void render(DrawContext context, float delta, int width, int height) {
        if (transitionType == TRANSITION_NONE) return;

        // Update Progress using real time
        long now = System.currentTimeMillis();
        long elapsed = now - lastTime;
        lastTime = now;
        
        // Safety for long pauses (loading screens)
        if (elapsed > 100) elapsed = 16; 

        transitionProgress += (float) (elapsed / ANIMATION_DURATION);

        if (transitionProgress >= 1.0f) {
            transitionProgress = 1.0f;

            if (transitionType == TRANSITION_OUT) {
                if (queuedAction != null) {
                    // Store for execution at start of next frame
                    pendingExecution = queuedAction;
                }
                queuedAction = null; 
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
        int gradientWidth = 300;
        int solidWidth = w + gradientWidth;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (transitionType == TRANSITION_OUT) {
            // Wiping ON from Right
            float t = transitionProgress;
            t = 1.0f - (1.0f - t) * (1.0f - t); // Ease Out
            
            int startX = (int) (w - (w + gradientWidth) * t);
            
            // Draw Gradient Leading Edge
            P3RHelper.drawGradientSkewedRect(context, startX, 0, gradientWidth, h, SKEW_OFFSET, 
                    0x000044FF, // Transparent
                    COLOR_BLUE_STRIP | 0xFF000000 // Solid
            );
            
            // Draw Solid Block
            context.fill(startX + gradientWidth, 0, startX + gradientWidth + solidWidth, h, COLOR_BLUE_STRIP | 0xFF000000);
            
        } else if (transitionType == TRANSITION_IN) {
            // Wiping OFF to Left
            float t = transitionProgress;
            t = 1.0f - (1.0f - t) * (1.0f - t); // Ease Out
            
            int solidEndX = (int) (w - (w + gradientWidth) * t);
            
            // Draw Solid Block
            // Ensure it covers enough to the left
            context.fill(solidEndX - solidWidth, 0, solidEndX, h, COLOR_BLUE_STRIP | 0xFF000000);
            
            // Draw Gradient Trailing Edge
            // Offset X by -SKEW_OFFSET because the Skewed Rect Top-Left is at (X + Skew),
            // creating a gap if we draw at solidEndX.
            // By shifting left, the Top-Left becomes (solidEndX - Skew + Skew) = solidEndX, matching the solid block.
            P3RHelper.drawGradientSkewedRect(context, solidEndX - SKEW_OFFSET, 0, gradientWidth, h, SKEW_OFFSET, 
                    COLOR_BLUE_STRIP | 0xFF000000, 
                    0x000044FF
            );
        }

        RenderSystem.disableBlend();
        context.getMatrices().pop();
    }
}
