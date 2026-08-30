package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.P3RScreenFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the option entry hitbox aligned with the custom two-column layout. */
@Mixin(OptionsList.class)
public abstract class OptionsListMixin {
    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_rowWidth(CallbackInfoReturnable<Integer> cir) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null && P3RScreenFamily.isConfiguration(screen)) {
            cir.setReturnValue(Math.max(1, Math.round(screen.width * 0.85F)));
        }
    }
}
