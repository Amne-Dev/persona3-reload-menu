package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.P3RGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyBindsListKeyEntryMixin {
    @Shadow @Final private Button changeButton;
    @Shadow @Final private Button resetButton;

    @Unique private boolean p3r_rowActive;
    @Unique private Component p3r_label;
    @Unique private int p3r_labelY;

    @Inject(method = "renderContent", at = @At("HEAD"))
    private void p3r_captureState(GuiGraphics graphics, int mouseX, int mouseY,
            boolean hovered, float delta, CallbackInfo ci) {
        this.p3r_rowActive = hovered || this.changeButton.isFocused()
                || this.resetButton.isFocused();
        this.p3r_label = null;
    }

    @ModifyArg(method = "renderContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;setPosition(II)V", ordinal = 0), index = 0)
    private int p3r_resetX(int vanillaX) {
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return P3RGraphics.configContentRight(width) - this.resetButton.getWidth();
    }

    @ModifyArg(method = "renderContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;setPosition(II)V", ordinal = 1), index = 0)
    private int p3r_changeX(int vanillaX) {
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int gap = Math.max(5, Math.round(7.0F * P3RGraphics.scale(
                width, client.getWindow().getGuiScaledHeight())));
        return P3RGraphics.configContentRight(width) - this.resetButton.getWidth()
                - gap - this.changeButton.getWidth();
    }

    @ModifyArg(method = "renderContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 1)
    private Component p3r_captureLabel(Component label) {
        this.p3r_label = label;
        return Component.empty();
    }

    @ModifyArg(method = "renderContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 3)
    private int p3r_captureLabelY(int y) {
        this.p3r_labelY = y;
        return y;
    }

    @Inject(method = "renderContent", at = @At("TAIL"))
    private void p3r_drawLabel(GuiGraphics graphics, int mouseX, int mouseY,
            boolean hovered, float delta, CallbackInfo ci) {
        if (this.p3r_label == null) return;
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int left = P3RGraphics.configContentLeft(width) + 7;
        int gap = Math.max(5, Math.round(7.0F * P3RGraphics.scale(
                width, client.getWindow().getGuiScaledHeight())));
        P3RGraphics.fittedText(graphics, client.font, this.p3r_label,
                left, this.p3r_labelY + 4.5F,
                Math.max(8, this.changeButton.getX() - gap - left), 1.0F,
                this.p3r_rowActive ? P3RGraphics.configSelectedText() : P3RGraphics.CYAN,
                false);
    }
}
