package com.amnedev.p3rmenu.v121.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "net.minecraft.client.gui.components.OptionsList$Entry")
public abstract class OptionsListEntryMixin {
    @Shadow @Final private List<AbstractWidget> children;
    @Shadow @Final private Screen screen;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_layoutEntry(GuiGraphics graphics, int index, int top,
            int rowLeft, int rowWidth, int rowHeight, int mouseX, int mouseY,
            boolean hovered, float delta, CallbackInfo ci) {
        ci.cancel();
        int count = Math.max(1, children.size());
        int left = Math.round(screen.width * 0.075F);
        int right = Math.round(screen.width * 0.70F);
        int gap = Math.max(6, Math.round(8.0F
                * com.amnedev.p3rmenu.v121.P3RGraphics.scale(screen.width, screen.height)));
        int widgetWidth = Math.max(90, (right - left - gap * (count - 1)) / count);
        int y = top + 2;
        for (int widgetIndex = 0; widgetIndex < children.size(); widgetIndex++) {
            AbstractWidget widget = children.get(widgetIndex);
            widget.setPosition(left + widgetIndex * (widgetWidth + gap), y);
            widget.setWidth(widgetWidth);
            widget.render(graphics, mouseX, mouseY, delta);
        }
    }
}
