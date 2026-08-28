package com.amnedev.p3rmenu.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldListWidget.class)
public class WorldListWidgetMixin {
    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_expandWorldRows(CallbackInfoReturnable<Integer> cir) {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        cir.setReturnValue(MathHelper.clamp(Math.round(screenWidth * 0.54F), 220, 620));
    }

    @Inject(method = "getScrollbarPositionX", at = @At("RETURN"), cancellable = true)
    private void p3r_moveWorldScrollbar(CallbackInfoReturnable<Integer> cir) {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int rowWidth = MathHelper.clamp(Math.round(screenWidth * 0.54F), 220, 620);
        cir.setReturnValue(Math.round(screenWidth * 0.385F) + rowWidth + 6);
    }
}
