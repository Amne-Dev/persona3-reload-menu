package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.TransitionManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(boolean tick, CallbackInfo ci) {
        // Execute any screen switches scheduled by transitions before rendering starts
        TransitionManager.checkPendingExecution();
    }
}
