package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.P3RGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(KeyBindsList.CategoryEntry.class)
public abstract class KeyBindsListCategoryEntryMixin {
    @Shadow @Final private FocusableTextWidget categoryName;

    @ModifyArg(method = "renderContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/FocusableTextWidget;setPosition(II)V"), index = 0)
    private int p3r_categoryX(int vanillaX) {
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return P3RGraphics.configContentLeft(width)
                + (P3RGraphics.configContentWidth(width) - this.categoryName.getWidth()) / 2;
    }
}
