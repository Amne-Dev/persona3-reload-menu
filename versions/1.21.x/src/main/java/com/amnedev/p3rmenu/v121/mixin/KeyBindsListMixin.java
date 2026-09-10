package com.amnedev.p3rmenu.v121.mixin;

import com.amnedev.p3rmenu.v121.P3RGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin {
    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_rowWidth(CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().screen instanceof KeyBindsScreen screen) {
            cir.setReturnValue(P3RGraphics.configContentWidth(screen.width));
        }
    }
}
