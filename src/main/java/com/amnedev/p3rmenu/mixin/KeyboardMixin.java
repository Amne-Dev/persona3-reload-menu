package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.TransitionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void p3r_blockKeyPressDuringTransition(long window, int key, int scanCode,
            int action, int modifiers, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen != null
                && TransitionManager.isBlockingInput()
                && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
            ci.cancel();
        }
    }
}
