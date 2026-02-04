package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RHelper;
import com.amnedev.p3rmenu.util.TransitionManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = new Identifier("p3rmenu",
            "textures/gui/title/p3r_background.png");
    private static final Identifier LOGO_TEXTURE = new Identifier("p3rmenu",
            "textures/gui/title/p3r_logo.png");
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_BLUE_STRIP = 0xFF0044FF; // Cobalt Blue
    private static final int COLOR_CYAN = 0xFF00FFFF;
    private static final int COLOR_COBALT = 0xFF0044FF;

    // Transition State handled by TransitionManager

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_init(CallbackInfo ci) {

        // Hide all buttons initially
        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof ClickableWidget widget) {
                widget.visible = false;
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        // 1. Render Background (Custom Texture)
        RenderSystem.enableBlend();
        context.drawTexture(BACKGROUND_TEXTURE, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
        RenderSystem.disableBlend();

        // 2. Render Logo (Top-Right)
        renderP3RLogo(context);

        // 3. Render Dynamic Menu (Bottom-Right)
        renderP3RMenu(context, mouseX, mouseY);

        // 4. Footer (Accessibility / Quit)
        renderFooter(context, mouseX, mouseY);

        // 5. Render Transition Overlay (if active)
        TransitionManager.render(context, delta, this.width, this.height);
    }



    private void renderP3RLogo(DrawContext context) {
        context.getMatrices().push();

        // Top-Right Anchor
        int baseX = this.width - 120;
        int baseY = 80;

        // Rotation -5 degrees
        context.getMatrices().translate(baseX, baseY, 0);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-5.0f));


        int targetWidth = 120;
        int targetHeight = (int) (targetWidth * (800.0f / 900.0f));

        // Draw texture centered on the anchor
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Enable Linear filtering for smooth scaling
        RenderSystem.setShaderTexture(0, LOGO_TEXTURE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // Draw centered. 
        // drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight)
        // We map the full texture (targetWidth x targetHeight in "texture pixels" abstraction) to the quad.
        context.drawTexture(LOGO_TEXTURE, -targetWidth / 2, -targetHeight / 2, 0, 0.0f, 0.0f, targetWidth, targetHeight, targetWidth, targetHeight);
        
        RenderSystem.disableBlend();

        context.getMatrices().pop();
    }

    // Helper for consistency
    private boolean shouldHideItem(String msg) {
        if (msg.isEmpty())
            return true;
        if (msg.contains("Copyright"))
            return true;
        if (msg.contains("Accessib"))
            return true; // Broader check
        if (msg.equals("Language"))
            return true;
        if (msg.equals("Quit Game"))
            return true;
        return false;
    }

    private java.util.Map<ClickableWidget, Float> hoverProgress = new java.util.HashMap<>();

    private void renderP3RMenu(DrawContext context, int mouseX, int mouseY) {
        // 1. Calculate valid items
        java.util.List<ClickableWidget> validItems = new java.util.ArrayList<>();
        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof ClickableWidget widget) {
                if (!shouldHideItem(widget.getMessage().getString())) {
                    validItems.add(widget);
                }
            }
        }

        int count = validItems.size();
        if (count == 0)
            return;

        // Dynamic Scaling (Count + GUI Scale)
        float baseScale = 1.0f;
        if (count > 5) {
            baseScale = 1.0f - ((count - 5) * 0.08f);
            if (baseScale < 0.6f)
                baseScale = 0.6f;
        }

        // Enforce max scale relative to screen (Target Scale 3.0 for bigger buttons)
        double guiFactor = this.client.getWindow().getScaleFactor();
        float scaleCorrection = (guiFactor > 2.0) ? (float) (3.0 / guiFactor) : 1.5f;

        float finalScale = baseScale * scaleCorrection;
        int stepY = (int) (25 * finalScale);
        // Ensure minimum spacing
        if (stepY < 12)
            stepY = 12;

        int totalHeight = count * stepY;

        // Layout
        int safeTop = 130;
        int safeBottom = this.height - 40;
        int startY = safeTop + (safeBottom - safeTop) / 2 - (totalHeight / 2);

        if (startY < safeTop)
            startY = safeTop;
        if (startY + totalHeight > safeBottom)
            startY = safeBottom - totalHeight;

        int rightMargin = 40;

        // 2. Render Check & Animation Frame Time
        float animSpeed = 0.4f;

        context.getMatrices().push();

        for (int i = 0; i < count; i++) {
            ClickableWidget widget = validItems.get(i);
            String msg = widget.getMessage().getString();

            int y = startY + (i * stepY);

            int textWidthBase = this.textRenderer.getWidth(msg);
            int textWidth = (int) (textWidthBase * finalScale);

            int x = this.width - rightMargin - textWidth;

            // EXPANDED HITBOX (Matches Visuals)
            // Visuals: Text ends at Right Edge (x+textWidth).
            // Strip: Extends 10px Right, ~25px Left.
            // Hitbox: Let's be generous. -30 Left, +20 Right.
            int hitX = x - 30;
            int hitW = textWidth + 50;
            int hitY = y - 5;
            int hitH = (int) (20 * finalScale);

            boolean hovered = (mouseX >= hitX && mouseX <= hitX + hitW && mouseY >= hitY && mouseY <= hitY + hitH);

            // Animation Logic
            float currentProgress = hoverProgress.getOrDefault(widget, 0.0f);
            float target = hovered ? 1.0f : 0.0f;

            // Simple Lerp
            float nextProgress = currentProgress + (target - currentProgress) * animSpeed;
            if (Math.abs(target - nextProgress) < 0.01f)
                nextProgress = target;

            hoverProgress.put(widget, nextProgress);

            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(finalScale, finalScale, 1.0f);

            if (nextProgress > 0.01f) {
                // Skewed Strip Animation
                // Slide in from RIGHT EDGE OF SCREEN

                // Visual Right Edge in local coords = textWidthBase + (rightMargin / scale)
                // Added +50 buffer to ensure it extends fully off-screen without gaps
                int localScreenRight = textWidthBase + (int) (rightMargin / finalScale) + 50;

                // We want to fill from Right Edge up to a bit past the text start (e.g. -20)
                int fullStripWidth = localScreenRight + 30;
                int currentStripWidth = (int) (fullStripWidth * nextProgress);

                int stripStart = localScreenRight - currentStripWidth;

                if (currentStripWidth > 0) {
                    // Skew 10px, Angle Left /|, Flat Right
                    P3RHelper.drawLeftSkewedRightFlatStrip(context, stripStart, -4, currentStripWidth, 18, 10,
                            COLOR_BLUE_STRIP);
                }
            }

            // Text Interaction
            // "Closer to shadow" -> Max offset 2px.
            // "Only set distance on hover" -> Default 0 distance.
            // Shadow at (2, 2)
            float popFactor = nextProgress;

            // textX calculated from existing popFactor
            float textX = -5.0f * popFactor;

            // Shadow: Follows text (textX + 1, 1)
            // Just standard offset from the dynamic text position
            context.drawText(this.textRenderer, msg, (int) textX + 1, 1, 0xFF000000, false);

            // Text: Slides Left
            context.drawText(this.textRenderer, msg, (int) textX, 0, hovered ? COLOR_WHITE : 0xFFEEEEEE, false);

            context.getMatrices().pop();
        }
        context.getMatrices().pop();
    }

    private void renderFooter(DrawContext context, int mouseX, int mouseY) {
        String accessText = "[LT] Accessibility";
        String exitText = "[=] End Game";

        int y = this.height - 20;

        int exitWidth = this.textRenderer.getWidth(exitText);
        int exitX = this.width - exitWidth - 20;

        int accessWidth = this.textRenderer.getWidth(accessText);
        int accessX = exitX - accessWidth - 30;

        boolean accessHover = (mouseX >= accessX && mouseX <= accessX + accessWidth && mouseY >= y && mouseY <= y + 10);
        context.drawText(this.textRenderer, accessText, accessX, y, accessHover ? COLOR_CYAN : 0xFFAAAAAA, true);

        boolean exitHover = (mouseX >= exitX && mouseX <= exitX + exitWidth && mouseY >= y && mouseY <= y + 10);
        context.drawText(this.textRenderer, exitText, exitX, y, exitHover ? 0xFFFF4444 : 0xFFAAAAAA, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0)
            return super.mouseClicked(mouseX, mouseY, button);

        // 1. Menu Items Logic (SYNC WITH RENDER)
        java.util.List<ClickableWidget> validItems = new java.util.ArrayList<>();
        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof ClickableWidget widget) {
                if (!shouldHideItem(widget.getMessage().getString())) {
                    validItems.add(widget);
                }
            }
        }

        int count = validItems.size();
        if (count > 0) {
            float baseScale = 1.0f;
            if (count > 5) {
                baseScale = 1.0f - ((count - 5) * 0.08f);
                if (baseScale < 0.6f)
                    baseScale = 0.6f;
            }

            double guiFactor = this.client.getWindow().getScaleFactor();
            float scaleCorrection = (guiFactor > 2.0) ? (float) (3.0 / guiFactor) : 1.5f;
            float finalScale = baseScale * scaleCorrection;
            int stepY = (int) (25 * finalScale);
            if (stepY < 12)
                stepY = 12;

            int totalHeight = count * stepY;
            int safeTop = 130;
            int safeBottom = this.height - 40;
            int startY = safeTop + (safeBottom - safeTop) / 2 - (totalHeight / 2);
            if (startY < safeTop)
                startY = safeTop;
            if (startY + totalHeight > safeBottom)
                startY = safeBottom - totalHeight;

            int rightMargin = 40;

            for (int i = 0; i < count; i++) {
                ClickableWidget widget = validItems.get(i);
                String msg = widget.getMessage().getString();

                int y = startY + (i * stepY);
                int textWidth = (int) (this.textRenderer.getWidth(msg) * finalScale);
                int x = this.width - rightMargin - textWidth;

                // Match Render Hitbox
                int hitX = x - 30;
                int hitW = textWidth + 50;
                int hitY = y - 5;
                int hitH = (int) (20 * finalScale);

                if (mouseX >= hitX && mouseX <= hitX + hitW && mouseY >= hitY && mouseY <= hitY + hitH) {
                    if (!TransitionManager.isTransitioning()) {
                        widget.playDownSound(this.client.getSoundManager());
                        
                        // Start OUT Transition
                        TransitionManager.startOut(() -> {
                            if (widget instanceof PressableWidget pressable) {
                                pressable.onPress();
                            } else {
                                widget.onClick(mouseX, mouseY);
                            }
                        });
                    }
                    return true;
                }
            }
        }

        // 2. Footer (Keep standard logic or check P3RHelper for footer hitbox?)
        // Footer is fine as is.
        return footerMouseClicked(mouseX, mouseY);
    }

    // Extracted footer logic to avoid code dup if needed, or just inline
    private boolean footerMouseClicked(double mouseX, double mouseY) {
        if (TransitionManager.isTransitioning()) return true; // Block input during transition

        // Footer
        int y = this.height - 20;
        String exitText = "[=] End Game";
        String accessText = "[LT] Accessibility";

        int exitWidth = this.textRenderer.getWidth(exitText);
        int exitX = this.width - exitWidth - 20;

        int accessWidth = this.textRenderer.getWidth(accessText);
        int accessX = exitX - accessWidth - 30;

        // Accessibility
        if (mouseX >= accessX && mouseX <= accessX + accessWidth && mouseY >= y && mouseY <= y + 10) {
            for (net.minecraft.client.gui.Element element : this.children()) {
                if (element instanceof ClickableWidget widget) {
                    if (widget.getMessage().getString().contains("Accessib")) {
                        widget.playDownSound(this.client.getSoundManager());
                        // Instant action for accessibility
                        ((PressableWidget) widget).onPress();
                        return true;
                    }
                }
            }
            return true;
        }

        // Quit
        if (mouseX >= exitX && mouseX <= exitX + exitWidth && mouseY >= y && mouseY <= y + 10) {
             if (!TransitionManager.isTransitioning()) {
                 this.client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                 
                 // Start Transition for Quit too
                 TransitionManager.startOut(() -> this.client.scheduleStop());
             }
            return true;
        }
        return false;
    }
}
