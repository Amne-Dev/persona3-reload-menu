package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.Transition;
import com.amnedev.p3rmenu.v12111.P3RGraphics;
import com.amnedev.p3rmenu.v12111.P3RScreenFamily;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Unique private long p3r_screenStartedAt;

    @Inject(method = "init()V", at = @At("TAIL"))
    private void p3r_screenInitialized(CallbackInfo ci) {
        p3r_screenStartedAt = Util.getMillis();
        Transition.onScreenInitialized();
        Screen self = (Screen) (Object) this;
        if (P3RScreenFamily.isStyled(self)) {
            for (GuiEventListener child : self.children()) {
                if (child instanceof EditBox field) field.setBordered(false);
                if (child instanceof StringWidget heading
                        && heading.getMessage().equals(self.getTitle())) {
                    heading.visible = false;
                }
            }
            if (P3RScreenFamily.isConfiguration(self)) {
                P3RGraphics.layoutFooterButtons(self);
            }
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void p3r_screenBackground(GuiGraphics graphics, int mouseX,
            int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        float intro = P3RGraphics.easeOut((Util.getMillis() - p3r_screenStartedAt) / 420.0F);
        if (P3RScreenFamily.isConfiguration(self)) {
            P3RGraphics.configBackground(graphics, self.width, self.height, intro);
            ci.cancel();
        } else if (P3RScreenFamily.isList(self)) {
            P3RGraphics.listBackground(graphics, self.width, self.height, intro);
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void p3r_transitionLayer(GuiGraphics graphics, int mouseX,
            int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        float intro = P3RGraphics.easeOut((Util.getMillis() - p3r_screenStartedAt) / 420.0F);
        if (P3RScreenFamily.isConfiguration(self)) {
            P3RGraphics.configHeader(graphics, self.getFont(), self.getTitle().getString(),
                    self.width, self.height, intro);
            P3RGraphics.configFooter(graphics, self.getFont(), null,
                    self.width, self.height, intro);
        } else if (self instanceof SelectWorldScreen) {
            P3RGraphics.listHeader(graphics, self.getFont(), "SINGLEPLAYER",
                    "SELECT A WORLD", self.width, self.height, intro);
        } else if (self instanceof JoinMultiplayerScreen) {
            P3RGraphics.listHeader(graphics, self.getFont(), "MULTIPLAYER",
                    "CHOOSE A SERVER", self.width, self.height, intro);
        }
        Transition.extract(graphics, self.width, self.height);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void p3r_prepareLayout(GuiGraphics graphics, int mouseX,
            int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!P3RScreenFamily.isStyled(self)) return;
        for (GuiEventListener child : self.children()) {
            if (child instanceof EditBox field) field.setBordered(false);
            if (child instanceof StringWidget heading
                    && heading.getMessage().equals(self.getTitle())) heading.visible = false;
        }
        if (P3RScreenFamily.isConfiguration(self)) P3RGraphics.layoutFooterButtons(self);
    }
}
