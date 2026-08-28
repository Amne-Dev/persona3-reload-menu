package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin extends ClickableWidget {
    @Shadow
    protected double value;
    @Shadow
    protected abstract void updateMessage();
    @Shadow
    protected abstract void applyValue();

    protected SliderWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderButton", at = @At("HEAD"), cancellable = true)
    private void p3r_renderSettingsSlider(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null || !P3RSettingsShell.isSettingsDetail(screen)) {
            return;
        }
        ci.cancel();

        boolean selected = this.active && this.isSelected();
        int x = this.getX();
        int y = this.getY();
        int right = x + this.getWidth();
        int bottom = y + this.getHeight();
        context.fill(x, y, right, bottom,
                !this.active ? 0x544B536B : selected ? P3RSettingsShell.WHITE : 0x24556A93);
        if (selected) {
            context.fill(x, y, right, y + 2, P3RSettingsShell.RED);
            context.fill(x, y, x + 2, bottom, P3RSettingsShell.PINK);
        }

        int trackLeft = x + Math.round(this.getWidth() * 0.66F);
        int trackRight = right - 8;
        int trackY = y + this.getHeight() / 2;
        context.fill(trackLeft, trackY - 2, trackRight, trackY + 2, 0xFF090B18);
        int knobX = MathHelper.clamp(
                trackLeft + Math.round((trackRight - trackLeft) * (float) this.value),
                trackLeft, trackRight);
        context.fill(trackLeft, trackY - 2, knobX, trackY + 2,
                selected ? P3RSettingsShell.RED : P3RSettingsShell.WHITE);
        context.fill(knobX - 2, trackY - 4, knobX + 2, trackY + 4,
                selected ? P3RSettingsShell.PINK : P3RSettingsShell.CYAN);

        String message = this.getMessage().getString();
        int separator = message.indexOf(':');
        String labelText = separator > 0 ? message.substring(0, separator) : message;
        Text label = Text.literal(labelText.strip().toUpperCase(Locale.ROOT))
                .setStyle(Style.EMPTY.withBold(true));
        int color = !this.active ? 0xFF7E8AA7
                : selected ? P3RSettingsShell.INK : P3RSettingsShell.CYAN;
        P3RSettingsShell.drawFittedText(context, label,
                x + 7, y + this.getHeight() / 2.0F,
                Math.max(8, trackLeft - x - 14), color, false);
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void p3r_clickOnVisibleTrack(double mouseX, double mouseY, CallbackInfo ci) {
        if (p3r_isSettingsSlider()) {
            p3r_setValueFromVisibleTrack(mouseX);
            ci.cancel();
        }
    }

    @Inject(method = "onDrag", at = @At("HEAD"), cancellable = true)
    private void p3r_dragVisibleTrack(double mouseX, double mouseY,
            double deltaX, double deltaY, CallbackInfo ci) {
        if (p3r_isSettingsSlider()) {
            p3r_setValueFromVisibleTrack(mouseX);
            ci.cancel();
        }
    }

    @Unique
    private boolean p3r_isSettingsSlider() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen != null && P3RSettingsShell.isSettingsDetail(screen);
    }

    @Unique
    private void p3r_setValueFromVisibleTrack(double mouseX) {
        double oldValue = this.value;
        double left = this.getX() + this.getWidth() * 0.66D;
        double right = this.getX() + this.getWidth() - 8.0D;
        this.value = MathHelper.clamp((mouseX - left) / Math.max(1.0D, right - left),
                0.0D, 1.0D);
        if (Double.compare(oldValue, this.value) != 0) {
            this.applyValue();
        }
        this.updateMessage();
    }
}
