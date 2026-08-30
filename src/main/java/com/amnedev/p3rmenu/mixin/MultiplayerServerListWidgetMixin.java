package com.amnedev.p3rmenu.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiplayerServerListWidget.class)
public class MultiplayerServerListWidgetMixin {
    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_expandServerRows(CallbackInfoReturnable<Integer> cir) {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        cir.setReturnValue(MathHelper.clamp(Math.round(screenWidth * 0.54F), 260, 640));
    }

    @Inject(method = "getScrollbarPositionX", at = @At("RETURN"), cancellable = true)
    private void p3r_moveServerScrollbar(CallbackInfoReturnable<Integer> cir) {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        cir.setReturnValue(Math.max(6, Math.round(screenWidth * 0.035F)));
    }
}
