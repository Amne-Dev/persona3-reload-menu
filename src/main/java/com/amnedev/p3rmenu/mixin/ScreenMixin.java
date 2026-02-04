package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.TransitionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // When any screen initializes, start the IN transition (reveal)
        TransitionManager.startIn();
    }

    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
    private void p3rmenu_onRenderBackground(DrawContext context, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        
        // If we are in menus (not in a world), replace the Dirt background with Cobalt Blue
        if (client.world == null) {
            // Cobalt Blue (approximate P3R deep blue: 0xFF003380)
            context.fill(0, 0, self.width, self.height, 0xFF003380);
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        // System.out.println("Rendering Screen: " + self.getClass().getName()); // Debug

        TransitionManager.render(context, delta, self.width, self.height);
    }
}
