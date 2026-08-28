package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RScreenShell;
import com.amnedev.p3rmenu.util.P3RSettingsShell;
import com.amnedev.p3rmenu.util.TransitionManager;
import com.amnedev.p3rmenu.screen.WallpaperScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {
    protected PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Shadow
    public abstract void onPress();

    @Inject(method = "renderButton", at = @At("HEAD"), cancellable = true)
    private void p3r_renderPersonaButton(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        boolean listStyle = screen != null && P3RScreenShell.isPersonaListScreen(screen);
        boolean settingsStyle = screen != null && P3RSettingsShell.isSettingsDetail(screen);
        if (!listStyle && !settingsStyle) {
            return;
        }
        ci.cancel();

        boolean selected = this.active && this.isSelected();
        if (settingsStyle) {
            if (this.getY() >= Math.round(screen.height * 0.815F)) {
                p3r_renderConfigFooterButton(context, selected);
                return;
            }
            p3r_renderConfigButton(context, selected);
            return;
        }
        int x = this.getX();
        int y = this.getY();
        int right = x + this.getWidth();
        int bottom = y + this.getHeight();
        int surface = !this.active ? 0x78050D27
                : selected ? 0xF0080CB5 : 0xC006174F;
        context.fill(x, y, right, bottom, surface);
        if (selected) {
            context.fill(x, y, right, y + 2,
                    0xFF58E7FF);
            context.fill(x, bottom - 1, right, bottom,
                    0xB50447E6);
        }

        Text label = Text.literal(this.getMessage().getString().toUpperCase(Locale.ROOT))
                .setStyle(Style.EMPTY.withBold(true));
        int color = !this.active ? 0xFF7E8AA7
                : selected ? 0xFFF8FAFF : 0xFFE6EEFF;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, label,
                x + this.getWidth() / 2, y + (this.getHeight() - 8) / 2, color);
    }

    @Unique
    private void p3r_renderConfigButton(DrawContext context, boolean selected) {
        MinecraftClient client = MinecraftClient.getInstance();
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

        String message = this.getMessage().getString();
        int separator = message.indexOf(':');
        int labelColor = !this.active ? 0xFF7E8799
                : selected ? P3RSettingsShell.INK : P3RSettingsShell.CYAN;
        if (separator > 0 && separator < message.length() - 1) {
            Text label = Text.literal(message.substring(0, separator).strip()
                    .toUpperCase(Locale.ROOT)).setStyle(Style.EMPTY.withBold(true));
            Text value = Text.literal(message.substring(separator + 1).strip()
                    .toUpperCase(Locale.ROOT)).setStyle(Style.EMPTY.withBold(true));
            int valueWidth = Math.max(42, Math.round(this.getWidth() * 0.32F));
            int valueLeft = right - valueWidth - 3;
            context.fill(valueLeft, y + 3, right - 3, bottom - 3,
                    this.active ? 0xFF090B18 : 0xFF333746);
            P3RSettingsShell.drawFittedText(context, label,
                    x + 7, y + this.getHeight() / 2.0F,
                    Math.max(8, valueLeft - x - 12), labelColor, false);
            context.drawCenteredTextWithShadow(client.textRenderer, value,
                    valueLeft + (valueWidth - 3) / 2,
                    y + (this.getHeight() - 8) / 2, 0xFFF5F6F8);
        } else {
            Text label = Text.literal(message.toUpperCase(Locale.ROOT))
                    .setStyle(Style.EMPTY.withBold(true));
            P3RSettingsShell.drawFittedText(context, label,
                    x + 7, y + this.getHeight() / 2.0F,
                    Math.max(8, this.getWidth() - 14), labelColor, true);
        }
    }

    @Unique
    private void p3r_renderConfigFooterButton(DrawContext context, boolean selected) {
        int x = this.getX();
        int y = this.getY();
        int right = x + this.getWidth();
        int bottom = y + this.getHeight();
        context.fill(x, y, right, bottom,
                this.active ? 0xE10B1022 : 0xA03B4150);
        if (selected) {
            context.fill(x, y, right, y + 2, P3RSettingsShell.RED);
            context.fill(x, bottom - 2, right, bottom, P3RSettingsShell.PINK);
        }
        Text label = Text.literal(this.getMessage().getString().toUpperCase(Locale.ROOT))
                .setStyle(Style.EMPTY.withBold(true));
        int color = !this.active ? 0xFF7E8799
                : selected ? P3RSettingsShell.WHITE : P3RSettingsShell.CYAN;
        P3RSettingsShell.drawFittedText(context, label,
                x + 7, y + this.getHeight() / 2.0F,
                Math.max(8, this.getWidth() - 14), color, true);
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void p3r_transitionMouseAction(double mouseX, double mouseY, CallbackInfo ci) {
        if (!p3r_shouldTransition()) {
            return;
        }
        ci.cancel();
        Screen origin = MinecraftClient.getInstance().currentScreen;
        TransitionManager.startOut(this.getMessage(), () -> {
            this.onPress();
            if (MinecraftClient.getInstance().currentScreen == origin) {
                TransitionManager.startIn();
            }
        });
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void p3r_transitionKeyboardAction(int keyCode, int scanCode, int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        if (!this.active || !this.visible || !KeyCodes.isToggle(keyCode) || !p3r_shouldTransition()) {
            return;
        }
        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        Screen origin = MinecraftClient.getInstance().currentScreen;
        TransitionManager.startOut(this.getMessage(), () -> {
            this.onPress();
            if (MinecraftClient.getInstance().currentScreen == origin) {
                TransitionManager.startIn();
            }
        });
        cir.setReturnValue(true);
    }

    private boolean p3r_shouldTransition() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        boolean listAction = screen != null && P3RScreenShell.isPersonaListScreen(screen);
        boolean settingsNavigation = screen != null
                && P3RSettingsShell.isSettingsDetail(screen)
                && (Object) this instanceof ButtonWidget
                && !((Object) this instanceof CyclingButtonWidget<?>)
                && (!(screen instanceof WallpaperScreen wallpaper)
                        || wallpaper.shouldAnimateTransition(this));
        return (listAction || settingsNavigation) && !TransitionManager.isTransitioning();
    }
}
