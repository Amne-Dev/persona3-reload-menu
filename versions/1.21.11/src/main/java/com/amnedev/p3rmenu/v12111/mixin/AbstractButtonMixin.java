package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.P3RGraphics;
import com.amnedev.p3rmenu.v12111.P3RScreenFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {
    protected AbstractButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void p3r_button(GuiGraphics graphics, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        boolean settings = screen != null && P3RScreenFamily.isConfiguration(screen);
        boolean list = screen != null && P3RScreenFamily.isList(screen);
        if (!settings && !list) return;
        ci.cancel();
        boolean selected = this.isActive() && (this.isHovered() || this.isFocused());
        if (settings && P3RGraphics.isFooterButton(screen, (AbstractButton) (Object) this)) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                    this.isActive() ? 0xE10B1022 : 0xA03B4150);
            if (selected) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 2, P3RGraphics.RED);
                graphics.fill(getX(), getY() + getHeight() - 2,
                        getX() + getWidth(), getY() + getHeight(), P3RGraphics.PINK);
            }
            int color = !this.isActive() ? 0xFF7E8799
                    : selected ? P3RGraphics.CONFIG_WHITE : P3RGraphics.CYAN;
            P3RGraphics.fittedText(graphics, Minecraft.getInstance().font,
                    P3RGraphics.bold(getMessage().getString()), getX() + 7,
                    getY() + getHeight() * 0.52F, Math.max(8, getWidth() - 14), 1.0F, color, true);
        } else if (settings) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                    !this.isActive() ? 0x544B536B
                            : selected ? P3RGraphics.CONFIG_WHITE : 0x24556A93);
            if (selected) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 2, P3RGraphics.RED);
                graphics.fill(getX(), getY(), getX() + 2, getY() + getHeight(), P3RGraphics.PINK);
            }
            String message = getMessage().getString();
            int separator = message.indexOf(':');
            int color = !this.isActive() ? 0xFF7E8799
                    : selected ? P3RGraphics.CONFIG_INK : P3RGraphics.CYAN;
            if (separator > 0 && separator < message.length() - 1) {
                Component label = P3RGraphics.bold(message.substring(0, separator).strip());
                Component value = P3RGraphics.bold(message.substring(separator + 1).strip());
                int valueWidth = Math.max(42, Math.round(getWidth() * 0.32F));
                int valueLeft = getX() + getWidth() - valueWidth - 3;
                graphics.fill(valueLeft, getY() + 3, getX() + getWidth() - 3,
                        getY() + getHeight() - 3, this.isActive() ? 0xFF090B18 : 0xFF333746);
                P3RGraphics.fittedText(graphics, Minecraft.getInstance().font, label,
                        getX() + 7, getY() + getHeight() * 0.52F,
                        Math.max(8, valueLeft - getX() - 12), 1.0F, color, false);
                graphics.drawString(Minecraft.getInstance().font, value,
                        valueLeft + Math.max(0, (valueWidth - 3 - Minecraft.getInstance().font.width(value)) / 2),
                        getY() + (getHeight() - 8) / 2, 0xFFF5F6F8, true);
            } else {
                P3RGraphics.fittedText(graphics, Minecraft.getInstance().font,
                        P3RGraphics.bold(message), getX() + 7,
                        getY() + getHeight() * 0.52F, Math.max(8, getWidth() - 14), 1.0F, color, true);
            }
        } else {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                    !this.isActive() ? 0x78050D27 : selected ? 0xF0080CB5 : 0xC006174F);
            if (selected) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 2, P3RGraphics.CYAN);
                graphics.fill(getX(), getY() + getHeight() - 1,
                        getX() + getWidth(), getY() + getHeight(), 0xB50447E6);
            }
            int color = !this.isActive() ? 0xFF7E8AA7
                    : selected ? P3RGraphics.WHITE : P3RGraphics.PALE;
            P3RGraphics.fittedText(graphics, Minecraft.getInstance().font,
                    P3RGraphics.bold(getMessage().getString()), getX() + 7,
                    getY() + getHeight() * 0.52F, Math.max(8, getWidth() - 14), 1.0F, color, true);
        }
        this.handleCursor(graphics);
    }
}
