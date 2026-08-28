package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.P3RGraphics;
import com.amnedev.p3rmenu.v12111.Transition;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Shadow @Final private Screen lastScreen;
    @Shadow protected ServerSelectionList serverSelectionList;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_hideVanillaHeading(CallbackInfo ci) {
        for (GuiEventListener child : children()) {
            if (child instanceof StringWidget heading && heading.getMessage().equals(getTitle())) {
                heading.visible = false;
            }
        }
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void p3r_layout(CallbackInfo ci) {
        if (serverSelectionList != null) {
            int top = Math.max(48, Math.round(height * 0.12F));
            int bottom = height - Math.max(64, Math.round(70.0F * P3RGraphics.scale(width, height)));
            serverSelectionList.updateSizeAndPosition(width, Math.max(1, bottom - top), 0, top);
        }
        List<Button> buttons = new ArrayList<>();
        for (GuiEventListener child : children()) {
            if (child instanceof Button button && button.visible) buttons.add(button);
        }
        if (buttons.isEmpty()) return;
        float ui = P3RGraphics.scale(width, height);
        int regionLeft = Math.round(width * 0.375F);
        int regionRight = width - Math.max(16, Math.round(20.0F * ui));
        int columns = 4;
        int gap = Math.max(4, Math.round(8.0F * ui));
        int buttonWidth = Math.max(64, (regionRight - regionLeft - gap * (columns - 1)) / columns);
        int firstY = height - Math.max(54, Math.round(58.0F * ui));
        int rowStep = Math.max(21, Math.round(24.0F * ui));
        for (int index = 0; index < buttons.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            AbstractWidget button = buttons.get(index);
            button.setPosition(regionLeft + column * (buttonWidth + gap), firstY + row * rowStep);
            button.setWidth(buttonWidth);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void p3r_back(CallbackInfo ci) {
        if (!Transition.isActive()) {
            Transition.startOut(Component.literal("BACK"),
                    () -> minecraft.setScreen(lastScreen));
            ci.cancel();
        }
    }
}
