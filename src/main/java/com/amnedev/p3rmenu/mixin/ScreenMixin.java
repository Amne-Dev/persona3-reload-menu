package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RScreenShell;
import com.amnedev.p3rmenu.util.P3RSettingsShell;
import com.amnedev.p3rmenu.util.TransitionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Unique
    private static Screen p3r_lastInitializedScreen;

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        // Minecraft reuses parent Screen instances when navigating back. Tracking the
        // active object globally distinguishes that navigation from a resize re-init.
        if (p3r_lastInitializedScreen != self) {
            p3r_lastInitializedScreen = self;
            TransitionManager.startIn();
        }
    }

    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
    private void p3rmenu_onRenderBackground(DrawContext context, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (P3RSettingsShell.isSettingsDetail(self)) {
            P3RSettingsShell.renderDetailBackground(context, self.width, self.height,
                    net.minecraft.util.Util.getMeasuringTimeMs() - 400L);
            ci.cancel();
            return;
        }

        // If we are in menus (not in a world), replace the dirt background.
        if (client.world == null) {
            P3RScreenShell.renderFallbackBackground(context, self.width, self.height);
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
