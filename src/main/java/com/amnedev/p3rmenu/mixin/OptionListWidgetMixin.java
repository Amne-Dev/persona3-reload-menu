package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.OptionListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionListWidget.class)
public abstract class OptionListWidgetMixin {
    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_widenSettingsRows(CallbackInfoReturnable<Integer> cir) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null && P3RSettingsShell.isSettingsDetail(screen)) {
            cir.setReturnValue(Math.round(screen.width * 0.58F));
        }
    }

    @Inject(method = "getScrollbarPositionX", at = @At("HEAD"), cancellable = true)
    private void p3r_alignSettingsScrollbar(CallbackInfoReturnable<Integer> cir) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null && P3RSettingsShell.isSettingsDetail(screen)) {
            cir.setReturnValue(Math.round(screen.width * 0.655F));
        }
    }
}
