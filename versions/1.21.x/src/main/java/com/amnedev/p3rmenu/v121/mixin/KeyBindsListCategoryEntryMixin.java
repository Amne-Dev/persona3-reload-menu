package com.amnedev.p3rmenu.v121.mixin;

import com.amnedev.p3rmenu.v121.P3RGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(KeyBindsList.CategoryEntry.class)
public abstract class KeyBindsListCategoryEntryMixin {
    @Shadow @Final private int width;

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 2)
    private int p3r_categoryX(int vanillaX) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return P3RGraphics.configContentLeft(screenWidth)
                + (P3RGraphics.configContentWidth(screenWidth) - this.width) / 2;
    }
}
