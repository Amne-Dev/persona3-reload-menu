package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import com.amnedev.p3rmenu.util.TransitionManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptionsScreen.class)
public abstract class GameOptionsScreenMixin extends Screen {
    @Shadow
    @Final
    protected Screen parent;

    @Unique
    private long p3r_settingsStartedAt = Util.getMeasuringTimeMs();

    protected GameOptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/gui/widget/OptionListWidget;IIF)V",
            at = @At("HEAD"), cancellable = true)
    private void p3r_renderOptionList(DrawContext context, OptionListWidget list,
            int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();
        P3RSettingsShell.layoutOptionList(this, list);
        P3RSettingsShell.renderDetailBackground(context, this.width, this.height,
                this.p3r_settingsStartedAt);
        list.render(context, mouseX, mouseY, delta);
        P3RSettingsShell.renderDetailHeader(context, this.title,
                this.width, this.height, this.p3r_settingsStartedAt);
        P3RSettingsShell.renderDetailFooter(context, this.width, this.height,
                P3RSettingsShell.entrance(this.p3r_settingsStartedAt));
        super.render(context, mouseX, mouseY, delta);
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void p3r_animateSettingsBack(CallbackInfo ci) {
        if (!TransitionManager.isTransitioning()) {
            TransitionManager.startOut(Text.literal("BACK"),
                    () -> this.client.setScreen(this.parent));
            ci.cancel();
        }
    }
}
