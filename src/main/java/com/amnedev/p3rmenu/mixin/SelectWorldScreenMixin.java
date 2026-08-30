package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RScreenShell;
import com.amnedev.p3rmenu.util.TransitionManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {
    @Shadow
    @Final
    protected Screen parent;
    @Shadow
    protected TextFieldWidget searchBox;
    @Shadow
    private WorldListWidget levelList;

    @Unique
    private long p3r_screenStartedAt;

    protected SelectWorldScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_initWorldSelection(CallbackInfo ci) {
        this.p3r_screenStartedAt = Util.getMeasuringTimeMs();
        this.levelList.setRenderBackground(false);
        this.levelList.setRenderHorizontalShadows(false);
        p3r_layoutWorldSelection();
    }

    @Unique
    private void p3r_layoutWorldSelection() {
        float uiScale = P3RScreenShell.uiScale(this.width, this.height);
        int actionsTop = P3RScreenShell.layoutButtons(this);
        int searchX = Math.round(this.width * 0.53F);
        int searchRight = this.width - Math.max(P3RScreenShell.SPACE_GROUP,
                Math.round(28.0F * uiScale));
        this.searchBox.setX(searchX);
        this.searchBox.setY(Math.max(P3RScreenShell.SPACE_CONTROL,
                Math.round(20.0F * uiScale)));
        this.searchBox.setWidth(Math.max(100, searchRight - searchX));
        int defaultBottom = this.height - Math.max(P3RScreenShell.SPACE_HERO,
                Math.round(68.0F * uiScale));
        int contentBottom = Math.min(defaultBottom,
                actionsTop - Math.max(P3RScreenShell.SPACE_TIGHT,
                        Math.round(10.0F * uiScale)));
        this.levelList.updateSize(this.width, this.height,
                Math.max(P3RScreenShell.SPACE_MAJOR, Math.round(52.0F * uiScale)),
                Math.max(P3RScreenShell.SPACE_MAJOR + 1, contentBottom));
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_renderWorldSelection(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        ci.cancel();
        p3r_layoutWorldSelection();
        P3RScreenShell.renderBackground(context, this.width, this.height, this.p3r_screenStartedAt);

        float slide = P3RScreenShell.contentSlide(this.p3r_screenStartedAt, this.width);
        context.getMatrices().push();
        context.getMatrices().translate(slide, 0.0F, P3RScreenShell.LAYER_CONTENT);
        this.levelList.render(context, mouseX, mouseY, delta);
        this.searchBox.render(context, mouseX, mouseY, delta);
        context.getMatrices().pop();

        P3RScreenShell.renderHeader(context, "SINGLEPLAYER", "SELECT A WORLD",
                this.width, this.height, this.p3r_screenStartedAt);
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
