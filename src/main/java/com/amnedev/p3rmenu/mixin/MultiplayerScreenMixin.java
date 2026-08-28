package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RScreenShell;
import com.amnedev.p3rmenu.util.TransitionManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Shadow
    @Final
    private Screen parent;
    @Shadow
    protected MultiplayerServerListWidget serverListWidget;
    @Shadow
    private List<Text> multiplayerScreenTooltip;

    @Unique
    private long p3r_screenStartedAt;

    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_initServerSelection(CallbackInfo ci) {
        this.p3r_screenStartedAt = Util.getMeasuringTimeMs();
        this.serverListWidget.setRenderBackground(false);
        this.serverListWidget.setRenderHorizontalShadows(false);
        p3r_layoutServerSelection();
    }

    @Unique
    private void p3r_layoutServerSelection() {
        float uiScale = P3RScreenShell.uiScale(this.width, this.height);
        this.serverListWidget.updateSize(this.width, this.height,
                Math.max(P3RScreenShell.SPACE_MAJOR, Math.round(52.0F * uiScale)),
                this.height - Math.max(P3RScreenShell.SPACE_HERO,
                        Math.round(68.0F * uiScale)));
        P3RScreenShell.layoutButtons(this);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_renderServerSelection(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        ci.cancel();
        this.multiplayerScreenTooltip = null;
        p3r_layoutServerSelection();
        P3RScreenShell.renderBackground(context, this.width, this.height, this.p3r_screenStartedAt);

        float slide = P3RScreenShell.contentSlide(this.p3r_screenStartedAt, this.width);
        context.getMatrices().push();
        context.getMatrices().translate(slide, 0.0F, P3RScreenShell.LAYER_CONTENT);
        this.serverListWidget.render(context, mouseX, mouseY, delta);
        context.getMatrices().pop();

        P3RScreenShell.renderHeader(context, "MULTIPLAYER", "CHOOSE A SERVER",
                this.width, this.height, this.p3r_screenStartedAt);
        if (this.multiplayerScreenTooltip != null) {
            context.drawTooltip(this.textRenderer, this.multiplayerScreenTooltip, mouseX, mouseY);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void p3r_animateEscape(int keyCode, int scanCode, int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !TransitionManager.isTransitioning()) {
            TransitionManager.startOut(Text.literal("BACK"), () -> this.client.setScreen(this.parent));
            cir.setReturnValue(true);
        }
    }
}
