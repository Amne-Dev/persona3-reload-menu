package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Centers key-binding section labels within the P3R panel, not the whole screen. */
@Mixin(ControlsListWidget.CategoryEntry.class)
public abstract class ControlsListWidgetCategoryEntryMixin {
    @Shadow @Final private int textWidth;

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"), index = 2)
    private int p3r_categoryX(int vanillaX) {
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        return P3RSettingsShell.contentLeft(width)
                + (P3RSettingsShell.contentWidth(width) - this.textWidth) / 2;
    }
}
