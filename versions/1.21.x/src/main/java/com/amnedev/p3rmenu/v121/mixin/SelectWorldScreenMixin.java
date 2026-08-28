package com.amnedev.p3rmenu.v121.mixin;

import com.amnedev.p3rmenu.v121.P3RGraphics;
import com.amnedev.p3rmenu.v121.Transition;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {
    @Shadow @Final protected Screen lastScreen;
    @Shadow protected EditBox searchBox;
    @Shadow private WorldSelectionList list;

    protected SelectWorldScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_initialize(CallbackInfo ci) {
        for (GuiEventListener child : children()) {
            if (child instanceof StringWidget heading && heading.getMessage().equals(getTitle())) {
                heading.visible = false;
            }
        }
        if (searchBox != null) searchBox.setBordered(false);
        p3r_layout();
    }

    private void p3r_layout() {
        float ui = P3RGraphics.scale(width, height);
        int contentLeft = Math.round(width * 0.385F);
        int contentRight = width - Math.max(16, Math.round(20.0F * ui));
        int top = Math.max(52, Math.round(58.0F * ui));
        int bottom = height - Math.max(64, Math.round(70.0F * ui));
        if (searchBox != null) {
            searchBox.setPosition(contentLeft, Math.max(18, top - Math.round(28.0F * ui)));
            searchBox.setWidth(Math.max(120, contentRight - contentLeft));
        }
        if (list != null) {
            list.updateSizeAndPosition(width, Math.max(1, bottom - top), top);
        }

        List<Button> buttons = new ArrayList<>();
        for (GuiEventListener child : children()) {
            if (child instanceof Button button && button.visible) buttons.add(button);
        }
        int columns = 4;
        int gap = Math.max(4, Math.round(8.0F * ui));
        int buttonWidth = Math.max(64,
                (contentRight - contentLeft - gap * (columns - 1)) / columns);
        int firstY = height - Math.max(54, Math.round(58.0F * ui));
        int rowStep = Math.max(21, Math.round(24.0F * ui));
        for (int index = 0; index < buttons.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            AbstractWidget button = buttons.get(index);
            button.setPosition(contentLeft + column * (buttonWidth + gap),
                    firstY + row * rowStep);
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
