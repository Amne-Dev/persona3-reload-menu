package com.amnedev.p3rmenu.v262.mixin;

import com.amnedev.p3rmenu.v262.P3RGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Unique private static final Identifier P3R_BACKGROUND =
            Identifier.fromNamespaceAndPath("p3rmenu", "textures/gui/title/p3r_background.png");
    @Unique private static final int P3R_TEXTURE_WIDTH = 2560;
    @Unique private static final int P3R_TEXTURE_HEIGHT = 1369;

    @Shadow @Final private Minecraft minecraft;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;

    @Unique private long p3r_startedAt = -1L;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void p3r_render(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        long now = Util.getMillis();
        if (p3r_startedAt < 0L) p3r_startedAt = now;

        float fade = 1.0F;
        if (fadeOutStart > 0L) {
            float seconds = (now - fadeOutStart) / 1000.0F;
            fade = 1.0F - Mth.clamp(seconds - 1.0F, 0.0F, 1.0F);
            Screen screen = minecraft.gui.screen();
            if (screen != null) {
                // Replace the vanilla Mojang fade with the actual destination
                // screen before compositing the translucent P3R presentation.
                graphics.nextStratum();
                screen.extractRenderStateWithTooltipAndSubtitles(
                        graphics, mouseX, mouseY, delta);
            }
        }
        if (fade <= 0.001F) return;

        graphics.nextStratum();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float ui = P3RGraphics.scale(width, height);
        float intro = P3RGraphics.easeOut((now - p3r_startedAt) / 620.0F);
        int tint = P3RGraphics.alpha(0xFFFFFFFF, fade);

        graphics.blit(RenderPipelines.GUI_TEXTURED, P3R_BACKGROUND,
                0, 0, 0.0F, 0.0F, width, height,
                P3R_TEXTURE_WIDTH, P3R_TEXTURE_HEIGHT,
                P3R_TEXTURE_WIDTH, P3R_TEXTURE_HEIGHT, tint);
        graphics.fill(0, 0, width, height, P3RGraphics.alpha(0x35020A2B, fade));

        int panelTop = Math.round(height * 0.215F);
        int panelHeight = Math.round(height * 0.48F);
        int panelWidth = Math.round(width * 0.315F * intro);
        int skew = Math.max(14, Math.round(28.0F * ui));
        int slide = Math.round((1.0F - intro) * 58.0F * ui);
        P3RGraphics.skewedRect(graphics, -skew - slide, panelTop + 4,
                panelWidth + skew, panelHeight, skew,
                P3RGraphics.alpha(P3RGraphics.DEEP_BLUE, fade * 0.82F));
        P3RGraphics.skewedRect(graphics, -skew - slide, panelTop,
                panelWidth + skew, panelHeight, skew,
                P3RGraphics.alpha(P3RGraphics.BLUE, fade * 0.95F));

        int progressY = panelTop + panelHeight - Math.max(2, Math.round(3.0F * ui));
        int progressRight = Math.round((panelWidth + skew)
                * Mth.clamp(currentProgress, 0.0F, 1.0F)) - slide;
        graphics.fill(-slide, progressY, progressRight,
                progressY + Math.max(2, Math.round(3.0F * ui)),
                P3RGraphics.alpha(P3RGraphics.CYAN, fade));

        // Wait for the initial resource reload to complete before drawing text,
        // so the custom font cannot briefly render as missing-glyph squares.
        if (fadeOutStart > 0L) {
            float textX = width * 0.055F - slide * 0.35F;
            P3RGraphics.fittedText(graphics, minecraft.font,
                    P3RGraphics.bold("MINECRAFT RELOAD"), textX, height * 0.19F,
                    width * 0.29F, 1.55F * ui,
                    P3RGraphics.alpha(P3RGraphics.WHITE, fade * intro), true);
            String[] prompt = { "PRESS", "ANY", "BUTTON" };
            for (int index = 0; index < prompt.length; index++) {
                float lineIntro = P3RGraphics.easeOut((intro - index * 0.075F)
                        / Math.max(0.1F, 1.0F - index * 0.075F));
                P3RGraphics.fittedText(graphics, minecraft.font,
                        P3RGraphics.bold(prompt[index]),
                        textX - (1.0F - lineIntro) * 38.0F * ui,
                        height * 0.285F + index * 0.105F * height,
                        width * 0.27F, 6.0F * ui,
                        P3RGraphics.alpha(P3RGraphics.WHITE, fade * lineIntro), false);
            }
            P3RGraphics.fittedText(graphics, minecraft.font, P3RGraphics.bold("MOJANG"),
                    width * 0.055F, height * 0.875F, width * 0.25F, 1.75F * ui,
                    P3RGraphics.alpha(P3RGraphics.WHITE, fade * intro), false);
            P3RGraphics.fittedText(graphics, minecraft.font,
                    Component.literal("STUDIOS  -  MINECRAFT JAVA EDITION"),
                    width * 0.055F, height * 0.91F, width * 0.30F, 0.72F * ui,
                    P3RGraphics.alpha(P3RGraphics.WHITE, fade * intro * 0.84F), false);
        }
    }
}
