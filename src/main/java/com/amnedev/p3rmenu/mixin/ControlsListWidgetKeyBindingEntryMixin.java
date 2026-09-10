package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Aligns the real key-binding controls to the same panel geometry as their rows. */
@Mixin(ControlsListWidget.KeyBindingEntry.class)
public abstract class ControlsListWidgetKeyBindingEntryMixin {
    @Shadow @Final private ButtonWidget editButton;
    @Shadow @Final private ButtonWidget resetButton;

    @Unique private boolean p3r_rowActive;
    @Unique private Text p3r_label;
    @Unique private int p3r_labelY;

    @Inject(method = "render", at = @At("HEAD"))
    private void p3r_prepareControls(DrawContext context, int index, int y, int x,
            int entryWidth, int entryHeight, int mouseX, int mouseY,
            boolean hovered, float delta, CallbackInfo ci) {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int contentWidth = P3RSettingsShell.contentWidth(screenWidth);
        this.resetButton.setWidth(MathHelper.clamp(Math.round(contentWidth * 0.16F), 42, 58));
        this.editButton.setWidth(MathHelper.clamp(Math.round(contentWidth * 0.27F), 68, 112));
        this.p3r_rowActive = hovered || this.editButton.isFocused()
                || this.resetButton.isFocused();
        this.p3r_label = null;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"), index = 1)
    private Text p3r_captureLabel(Text label) {
        this.p3r_label = label;
        return Text.empty();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"), index = 3)
    private int p3r_captureLabelY(int y) {
        this.p3r_labelY = y;
        return y;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void p3r_drawLabel(DrawContext context, int index, int y, int x,
            int entryWidth, int entryHeight, int mouseX, int mouseY,
            boolean hovered, float delta, CallbackInfo ci) {
        if (this.p3r_label == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int left = P3RSettingsShell.contentLeft(width) + 7;
        int gap = Math.max(5, Math.round(7.0F * P3RSettingsShell.uiScale(
                width, client.getWindow().getScaledHeight())));
        int editX = P3RSettingsShell.contentRight(width) - this.resetButton.getWidth()
                - gap - this.editButton.getWidth();
        P3RSettingsShell.drawFittedText(context, this.p3r_label, left, this.p3r_labelY + 4.0F,
                Math.max(8, editX - gap - left),
                this.p3r_rowActive ? P3RSettingsShell.configSelectedText()
                        : P3RSettingsShell.CYAN,
                false);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/ButtonWidget;setX(I)V", ordinal = 0), index = 0)
    private int p3r_resetX(int vanillaX) {
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        return P3RSettingsShell.contentRight(width) - this.resetButton.getWidth();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/ButtonWidget;setX(I)V", ordinal = 1), index = 0)
    private int p3r_editX(int vanillaX) {
        MinecraftClient client = MinecraftClient.getInstance();
        int gap = Math.max(5, Math.round(7.0F * P3RSettingsShell.uiScale(
                client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight())));
        return P3RSettingsShell.contentRight(client.getWindow().getScaledWidth())
                - this.resetButton.getWidth() - gap - this.editButton.getWidth();
    }
}
