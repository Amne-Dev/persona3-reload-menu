package com.amnedev.p3rmenu.v121.mixin;

import com.amnedev.p3rmenu.v121.P3RGraphics;
import com.amnedev.p3rmenu.v121.P3RScreenFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {
    @Shadow protected abstract boolean isSelectedItem(int index);
    @Shadow public abstract int getRowLeft();
    @Shadow public abstract int getRowWidth();

    @Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
    private void p3r_clearListBackground(GuiGraphics graphics, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null && P3RScreenFamily.isStyled(screen)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
    private void p3r_clearListSeparators(GuiGraphics graphics, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null && P3RScreenFamily.isStyled(screen)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void p3r_entrySurface(GuiGraphics graphics, int mouseX, int mouseY,
            float delta, int index, int left, int top, int width, int height,
            CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || !P3RScreenFamily.isList(screen)) return;
        boolean selected = isSelectedItem(index);
        boolean hovered = mouseX >= left && mouseX <= left + width
                && mouseY >= top && mouseY <= top + height;
        int color = selected ? 0xE0080CB5 : hovered ? 0xC20A286D : 0x9B06143E;
        graphics.fill(left - 2, top, left + width + 2,
                top + Math.max(1, height - 2), color);
        if (hovered && !selected) {
            graphics.fill(left - 2, top, left + width + 2, top + 1,
                    P3RGraphics.CYAN);
        }
    }

    @Inject(method = "renderSelection", at = @At("HEAD"), cancellable = true)
    private void p3r_selection(GuiGraphics graphics, int top, int width,
            int height, int outerColor, int innerColor, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || !P3RScreenFamily.isList(screen)) return;
        ci.cancel();
        int left = getRowLeft();
        graphics.fill(left - 4, top - 2, left + width + 4,
                top + height + 2, 0xF0080CB5);
        graphics.fill(left - 4, top - 2, left + width + 4, top + 1,
                P3RGraphics.CYAN);
    }

    @Inject(method = "getRowLeft", at = @At("RETURN"), cancellable = true)
    private void p3r_rowLeft(CallbackInfoReturnable<Integer> cir) {
        Screen screen = Minecraft.getInstance().screen;
        if ((Object) this instanceof OptionsList && screen != null
                && P3RScreenFamily.isConfiguration(screen)) {
            cir.setReturnValue(Math.round(screen.width * 0.075F));
        } else if ((Object) this instanceof ServerSelectionList
                || (Object) this instanceof WorldSelectionList) {
            cir.setReturnValue(Math.round(Minecraft.getInstance().getWindow().getGuiScaledWidth() * 0.385F));
        }
    }

    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_rowWidth(CallbackInfoReturnable<Integer> cir) {
        Screen screen = Minecraft.getInstance().screen;
        if ((Object) this instanceof OptionsList && screen != null
                && P3RScreenFamily.isConfiguration(screen)) {
            cir.setReturnValue(Math.max(1, Math.round(screen.width * 0.625F)));
        } else if ((Object) this instanceof ServerSelectionList
                || (Object) this instanceof WorldSelectionList) {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            cir.setReturnValue((Object) this instanceof WorldSelectionList
                    ? Math.max(220, Math.min(620, Math.round(width * 0.54F)))
                    : Math.max(260, Math.min(640, Math.round(width * 0.54F))));
        }
    }

    @Inject(method = "scrollBarX", at = @At("RETURN"), cancellable = true)
    private void p3r_scrollbar(CallbackInfoReturnable<Integer> cir) {
        Screen screen = Minecraft.getInstance().screen;
        if ((Object) this instanceof OptionsList && screen != null
                && P3RScreenFamily.isConfiguration(screen)) {
            cir.setReturnValue(Math.round(screen.width * 0.70F) + 6);
        } else if ((Object) this instanceof ServerSelectionList
                || (Object) this instanceof WorldSelectionList) {
            cir.setReturnValue(getRowLeft() + getRowWidth() + 6);
        }
    }
}
