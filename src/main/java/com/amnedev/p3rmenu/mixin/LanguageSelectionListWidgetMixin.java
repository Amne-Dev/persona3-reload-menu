package com.amnedev.p3rmenu.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Removes the language list's second background pass and its vanilla fixed sizing. */
@Mixin(targets = "net.minecraft.client.gui.screen.option.LanguageOptionsScreen$LanguageSelectionListWidget")
public abstract class LanguageSelectionListWidgetMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void p3r_skipDuplicateConfigBackground(DrawContext context, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_languageRowWidth(CallbackInfoReturnable<Integer> cir) {
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        cir.setReturnValue(Math.round(width * 0.53F));
    }

    @Inject(method = "getScrollbarPositionX", at = @At("RETURN"), cancellable = true)
    private void p3r_languageScrollbar(CallbackInfoReturnable<Integer> cir) {
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        cir.setReturnValue(Math.round(width * 0.655F));
    }
}
