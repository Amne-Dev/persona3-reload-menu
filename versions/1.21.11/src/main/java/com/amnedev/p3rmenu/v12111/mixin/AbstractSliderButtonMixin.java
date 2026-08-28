package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.P3RGraphics;
import com.amnedev.p3rmenu.v12111.P3RScreenFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin extends AbstractWidget {
    @Shadow protected double value;
    @Shadow protected abstract void updateMessage();
    @Shadow protected abstract void applyValue();

    protected AbstractSliderButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void p3r_slider(GuiGraphics graphics, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || !P3RScreenFamily.isConfiguration(screen)) return;
        ci.cancel();
        boolean selected = isActive() && (isHovered() || isFocused());
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                !isActive() ? 0x544B536B
                        : selected ? P3RGraphics.CONFIG_WHITE : 0x24556A93);
        if (selected) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 2, P3RGraphics.RED);
            graphics.fill(getX(), getY(), getX() + 2, getY() + getHeight(), P3RGraphics.PINK);
        }
        int trackLeft = getX() + Math.round(getWidth() * 0.66F);
        int trackRight = getX() + getWidth() - 8;
        int y = getY() + getHeight() / 2;
        int knobX = Math.round((float) (trackLeft + (trackRight - trackLeft) * value));
        graphics.fill(trackLeft, y - 2, trackRight, y + 2, 0xFF090B18);
        graphics.fill(trackLeft, y - 2, knobX, y + 2,
                selected ? P3RGraphics.RED : P3RGraphics.CONFIG_WHITE);
        graphics.fill(knobX - 2, y - 4, knobX + 2, y + 4,
                selected ? P3RGraphics.PINK : P3RGraphics.CYAN);
        String message = getMessage().getString();
        int separator = message.indexOf(':');
        Component label = P3RGraphics.bold(
                (separator > 0 ? message.substring(0, separator) : message).strip());
        int color = !isActive() ? 0xFF7E8AA7
                : selected ? P3RGraphics.CONFIG_INK : P3RGraphics.CYAN;
        P3RGraphics.fittedText(graphics, Minecraft.getInstance().font,
                label, getX() + 7, getY() + getHeight() * 0.52F,
                Math.max(8, trackLeft - getX() - 14), 1.0F, color, false);
        this.handleCursor(graphics);
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void p3r_clickOnVisibleTrack(MouseButtonEvent event,
            boolean doubleClick, CallbackInfo ci) {
        if (p3r_isConfigurationSlider()) {
            p3r_setValueFromVisibleTrack(event.x());
            ci.cancel();
        }
    }

    @Inject(method = "onDrag", at = @At("HEAD"), cancellable = true)
    private void p3r_dragVisibleTrack(MouseButtonEvent event,
            double deltaX, double deltaY, CallbackInfo ci) {
        if (p3r_isConfigurationSlider()) {
            p3r_setValueFromVisibleTrack(event.x());
            ci.cancel();
        }
    }

    @Unique
    private boolean p3r_isConfigurationSlider() {
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && P3RScreenFamily.isConfiguration(screen);
    }

    @Unique
    private void p3r_setValueFromVisibleTrack(double mouseX) {
        double oldValue = value;
        double left = getX() + getWidth() * 0.66D;
        double right = getX() + getWidth() - 8.0D;
        value = Mth.clamp((mouseX - left) / Math.max(1.0D, right - left), 0.0D, 1.0D);
        if (Double.compare(oldValue, value) != 0) applyValue();
        updateMessage();
    }
}
