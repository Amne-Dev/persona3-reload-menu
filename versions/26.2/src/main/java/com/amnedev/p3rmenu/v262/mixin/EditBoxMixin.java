package com.amnedev.p3rmenu.v262.mixin;

import com.amnedev.p3rmenu.v262.P3RGraphics;
import com.amnedev.p3rmenu.v262.P3RScreenFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
    protected EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void p3r_fieldSurface(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen == null || !P3RScreenFamily.isStyled(screen)) return;
        boolean selected = isFocused();
        graphics.fill(getX() - 3, getY() - 2,
                getX() + getWidth() + 3, getY() + getHeight() + 2,
                selected ? 0xEFF7FAFF : 0xB506174F);
        graphics.fill(getX() - 3, getY() - 2,
                getX() + getWidth() + 3, getY() + 1,
                selected ? P3RGraphics.RED : P3RGraphics.CYAN);
    }
}
