package com.amnedev.p3rmenu.v121.mixin;

import com.amnedev.p3rmenu.v121.Transition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {
    @Shadow public abstract void popScreen();

    protected CreateWorldScreenMixin(Component title) {
        super(title);
    }

    /** Keep the P3R footer clean by omitting Minecraft's vanilla separator texture. */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_renderWithoutVanillaFooter(GuiGraphics graphics,
            int mouseX, int mouseY, float delta, CallbackInfo ci) {
        super.render(graphics, mouseX, mouseY, delta);
        ci.cancel();
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void p3r_animateBack(CallbackInfo ci) {
        if (!Transition.isActive()) {
            Transition.startOut(Component.literal("BACK"), this::popScreen);
            ci.cancel();
        }
    }
}
