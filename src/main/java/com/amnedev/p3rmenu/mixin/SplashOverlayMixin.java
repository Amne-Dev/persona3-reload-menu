package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    @Unique
    private static final Identifier P3R_SPLASH_BACKGROUND = new Identifier("p3rmenu",
            "textures/gui/title/p3r_background.png");
    @Unique
    private static final Identifier P3R_SPLASH_FONT = new Identifier("p3rmenu", "menu");
    @Unique
    private static final int P3R_SPLASH_BLUE = 0xFF061FD1;
    @Unique
    private static final int P3R_SPLASH_DEEP_BLUE = 0xFF07136D;
    @Unique
    private static final int P3R_SPLASH_CYAN = 0xFF58E7FF;
    @Unique
    private static final int P3R_SPLASH_WHITE = 0xFFF8FAFF;

    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private ResourceReload reload;
    @Shadow
    @Final
    private Consumer<Optional<Throwable>> exceptionHandler;
    @Shadow
    @Final
    private boolean reloading;
    @Shadow
    private float progress;
    @Shadow
    private long reloadCompleteTime;
    @Shadow
    private long reloadStartTime;

    @Unique
    private long p3r_splashStartedAt = -1L;
    @Unique
    private long p3r_fontReadyAt = Long.MAX_VALUE;
    @Unique
    private boolean p3r_fontWarmed;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_renderPersonaSplash(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        // Replace the vanilla renderer completely. Drawing at TAIL leaves Mojang's
        // red/logo transition underneath, which becomes visible through our fade.
        ci.cancel();

        long now = Util.getMeasuringTimeMs();
        if (this.p3r_splashStartedAt < 0L) {
            this.p3r_splashStartedAt = now;
        }
        if (this.reloading && this.reloadStartTime < 0L) {
            this.reloadStartTime = now;
        }

        float reloadStartSeconds = this.reloadStartTime > 0L
                ? (now - this.reloadStartTime) / 1000.0F : -1.0F;
        p3r_updateReloadState(now, reloadStartSeconds, context);
        p3r_warmFontProvider(context, now);

        float completionSeconds = this.reloadCompleteTime > 0L
                ? (now - this.reloadCompleteTime) / 1000.0F : -1.0F;
        if (completionSeconds >= 1.0F && this.client.currentScreen != null) {
            this.client.currentScreen.render(context, mouseX, mouseY, delta);
        }

        float fade = p3r_fadeOpacity(completionSeconds, reloadStartSeconds);
        if (fade <= 0.001F) {
            if (completionSeconds >= 2.0F) {
                this.client.setOverlay(null);
            }
            return;
        }

        int width = this.client.getWindow().getScaledWidth();
        int height = this.client.getWindow().getScaledHeight();
        float uiScale = MathHelper.clamp(Math.min(width / 960.0F, height / 540.0F), 0.72F, 1.55F);
        float intro = p3r_easeOutExpo(MathHelper.clamp((now - this.p3r_splashStartedAt) / 620.0F,
                0.0F, 1.0F));

        p3r_drawBackground(context, width, height, fade);
        p3r_drawPromptPanel(context, width, height, uiScale, intro, fade);
        p3r_drawStudioMark(context, width, height, uiScale, intro, fade);

        if (completionSeconds >= 2.0F) {
            this.client.setOverlay(null);
        }
    }

    @Unique
    private void p3r_updateReloadState(long now, float reloadStartSeconds, DrawContext context) {
        float targetProgress = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + targetProgress * 0.050000012F,
                0.0F, 1.0F);

        if (this.reloadCompleteTime >= 0L || !this.reload.isComplete()
                || (this.reloading && reloadStartSeconds < 2.0F)) {
            return;
        }

        try {
            this.reload.throwException();
            this.exceptionHandler.accept(Optional.empty());
        } catch (Throwable throwable) {
            this.exceptionHandler.accept(Optional.of(throwable));
        }

        this.reloadCompleteTime = now;
        // Populate the custom glyph atlas off-screen first. Until that warm-up has
        // settled, visible text deliberately uses Minecraft's always-ready font.
        this.p3r_fontReadyAt = now + 140L;
        if (this.client.currentScreen != null) {
            this.client.currentScreen.init(this.client,
                    context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }
    }

    @Unique
    private void p3r_drawBackground(DrawContext context, int width, int height, float opacity) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        context.drawTexture(P3R_SPLASH_BACKGROUND, 0, 0, 0, 0.0F, 0.0F,
                width, height, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    @Unique
    private void p3r_drawPromptPanel(DrawContext context, int width, int height,
            float uiScale, float intro, float fade) {
        int panelTop = Math.round(height * 0.215F);
        int panelHeight = Math.round(height * 0.48F);
        int panelWidth = Math.round(width * 0.305F * intro);
        int skew = Math.max(12, Math.round(26.0F * uiScale));
        int slide = Math.round((1.0F - intro) * 54.0F * uiScale);
        int panelAlpha = Math.round(242.0F * fade);

        P3RHelper.drawSkewedStrip(context, -skew - slide,
                panelTop + Math.max(2, Math.round(5.0F * uiScale)),
                panelWidth + skew, panelHeight, skew,
                p3r_withAlpha(P3R_SPLASH_DEEP_BLUE, Math.round(panelAlpha * 0.82F)));
        P3RHelper.drawSkewedStrip(context, -skew - slide, panelTop,
                panelWidth + skew, panelHeight, skew,
                p3r_withAlpha(P3R_SPLASH_BLUE, panelAlpha));

        float textX = width * 0.055F - slide * 0.35F;
        float labelY = height * 0.185F;
        context.getMatrices().push();
        context.getMatrices().translate(textX, labelY, 50.0F);
        float labelScale = 1.55F * uiScale;
        context.getMatrices().scale(labelScale, labelScale, 1.0F);
        context.drawText(this.client.textRenderer, p3r_text("MINECRAFT RELOAD"), 0, 0,
                p3r_withAlpha(P3R_SPLASH_WHITE, Math.round(255.0F * fade * intro)), true);
        context.getMatrices().pop();

        String[] lines = { "PRESS", "ANY", "BUTTON" };
        float mainScale = 6.0F * uiScale;
        float lineStep = 10.7F * mainScale;
        float mainY = height * 0.235F;
        for (int i = 0; i < lines.length; i++) {
            float stagger = p3r_easeOutExpo(MathHelper.clamp(
                    (intro - i * 0.075F) / (1.0F - i * 0.075F), 0.0F, 1.0F));
            float lineSlide = (1.0F - stagger) * 38.0F * uiScale;
            context.getMatrices().push();
            context.getMatrices().translate(textX - lineSlide, mainY + i * lineStep, 55.0F);
            context.getMatrices().scale(mainScale, mainScale, 1.0F);
            context.drawText(this.client.textRenderer, p3r_text(lines[i]), 0, 0,
                    p3r_withAlpha(P3R_SPLASH_WHITE,
                            Math.round(255.0F * fade * stagger)), false);
            context.getMatrices().pop();
        }

        int progressY = panelTop + panelHeight - Math.max(2, Math.round(3.0F * uiScale));
        int progressWidth = Math.round(MathHelper.clamp(this.progress, 0.0F, 1.0F)
                * (panelWidth + skew));
        context.fill(-slide, progressY, progressWidth - slide,
                progressY + Math.max(2, Math.round(3.0F * uiScale)),
                p3r_withAlpha(P3R_SPLASH_CYAN, Math.round(225.0F * fade)));
    }

    @Unique
    private void p3r_drawStudioMark(DrawContext context, int width, int height,
            float uiScale, float intro, float fade) {
        float x = width * 0.055F;
        float y = height * 0.875F;
        float opacity = intro * fade;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 60.0F);
        float studioScale = 1.75F * uiScale;
        context.getMatrices().scale(studioScale, studioScale, 1.0F);
        context.drawText(this.client.textRenderer, p3r_text("MOJANG"), 0, 0,
                p3r_withAlpha(P3R_SPLASH_WHITE, Math.round(255.0F * opacity)), false);
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().translate(x, y + 15.0F * uiScale, 60.0F);
        float detailScale = 0.72F * uiScale;
        context.getMatrices().scale(detailScale, detailScale, 1.0F);
        context.drawText(this.client.textRenderer, p3r_text("STUDIOS  •  MINECRAFT JAVA EDITION"),
                0, 0, p3r_withAlpha(P3R_SPLASH_WHITE,
                        Math.round(215.0F * opacity)), false);
        context.getMatrices().pop();
    }

    @Unique
    private float p3r_fadeOpacity(float completionSeconds, float reloadStartSeconds) {
        float entrance = this.reloading
                ? MathHelper.clamp(reloadStartSeconds / 0.15F, 0.0F, 1.0F) : 1.0F;
        float exit = completionSeconds < 0.0F ? 1.0F
                : 1.0F - MathHelper.clamp(completionSeconds - 1.0F, 0.0F, 1.0F);
        return entrance * exit;
    }

    @Unique
    private Text p3r_text(String value) {
        Style style = Style.EMPTY.withBold(true);
        if (this.p3r_fontWarmed && Util.getMeasuringTimeMs() >= this.p3r_fontReadyAt) {
            style = style.withFont(P3R_SPLASH_FONT);
        }
        return Text.literal(value).setStyle(style);
    }

    @Unique
    private void p3r_warmFontProvider(DrawContext context, long now) {
        if (this.p3r_fontWarmed || this.reloadCompleteTime < 0L) {
            return;
        }
        Text warmup = Text.literal("ABCDEFGHIJKLMNOPQRSTUVWXYZ 0123456789")
                .setStyle(Style.EMPTY.withBold(true).withFont(P3R_SPLASH_FONT));
        // Off-screen drawing requests both metrics and renderable glyphs without
        // exposing a frame of missing-glyph squares to the player.
        context.drawText(this.client.textRenderer, warmup,
                -10000, -10000, 0x01FFFFFF, false);
        this.p3r_fontWarmed = true;
        this.p3r_fontReadyAt = Math.max(this.p3r_fontReadyAt, now + 140L);
    }

    @Unique
    private static float p3r_easeOutExpo(float value) {
        return value >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0D * value);
    }

    @Unique
    private static int p3r_withAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }
}
