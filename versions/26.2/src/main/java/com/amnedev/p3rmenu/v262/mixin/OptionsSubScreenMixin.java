package com.amnedev.p3rmenu.v262.mixin;

import com.amnedev.p3rmenu.v262.Transition;
import com.amnedev.p3rmenu.v262.P3RGraphics;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsSubScreen.class)
public abstract class OptionsSubScreenMixin extends Screen {
    @Shadow @Final protected Screen lastScreen;
    @Shadow @Final protected Options options;
    @Shadow protected OptionsList list;

    protected OptionsSubScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_hideVanillaHeading(CallbackInfo ci) {
        for (GuiEventListener child : children()) {
            if (child instanceof StringWidget heading) heading.visible = false;
        }
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void p3r_layoutOptions(CallbackInfo ci) {
        if (list != null) {
            int top = Math.max(42, Math.round(height * 0.105F));
            int bottom = Math.round(height * 0.80F);
            list.updateSizeAndPosition(width, Math.max(1, bottom - top), 0, top);
        }
        P3RGraphics.layoutFooterButtons(this);
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void p3r_animateBack(CallbackInfo ci) {
        if (!Transition.isActive()) {
            if (list != null) list.applyUnsavedChanges();
            Transition.startOut(Component.literal("BACK"),
                    () -> minecraft.gui.setScreen(lastScreen));
            ci.cancel();
        }
    }
}
