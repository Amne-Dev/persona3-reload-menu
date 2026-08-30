package com.amnedev.p3rmenu.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Responsive content sizing for key-binding rows and their scrollbar. */
@Mixin(ControlsListWidget.class)
public abstract class ControlsListWidgetMixin {
    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_keybindRowWidth(CallbackInfoReturnable<Integer> cir) {
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        cir.setReturnValue(Math.round(width * 0.53F));
    }

    @Inject(method = "getScrollbarPositionX", at = @At("RETURN"), cancellable = true)
    private void p3r_keybindScrollbar(CallbackInfoReturnable<Integer> cir) {
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        cir.setReturnValue(Math.max(6, Math.round(width * 0.035F)));
    }
}
