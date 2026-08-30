package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.P3RGraphics;
import com.amnedev.p3rmenu.v12111.P3RScreenFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces Minecraft's gray list scrollbar with the P3R navigation rail. */
@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaMixin {
    @Shadow protected abstract boolean scrollbarVisible();
    @Shadow protected abstract int scrollerHeight();
    @Shadow protected abstract int scrollBarX();
    @Shadow protected abstract int scrollBarY();

    @Inject(method = "renderScrollbar", at = @At("HEAD"), cancellable = true)
    private void p3r_renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY,
            CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().screen;
        if (!((Object) this instanceof AbstractSelectionList<?>)
                || screen == null || !P3RScreenFamily.isStyled(screen)) {
            return;
        }

        ci.cancel();
        if (!scrollbarVisible()) {
            return;
        }

        AbstractScrollArea area = (AbstractScrollArea) (Object) this;
        p3r_drawRail(graphics, scrollBarX(), area.getY(), area.getHeight(),
                scrollBarY(), scrollerHeight());
    }

    private static void p3r_drawRail(GuiGraphics graphics, int x, int y,
            int height, int thumbY, int thumbHeight) {
        int bottom = y + height;
        graphics.fill(x - 1, y, x + 7, bottom, 0xE504113D);
        graphics.fill(x + 1, y + 2, x + 5, bottom - 2, 0xFF163E78);
        graphics.fill(x, thumbY, x + 6, thumbY + thumbHeight, P3RGraphics.BLUE);
        graphics.fill(x, thumbY, x + 6, Math.min(bottom, thumbY + 2), P3RGraphics.CYAN);
        if (thumbHeight > 4) {
            graphics.fill(x + 2, thumbY + 3, x + 4,
                    thumbY + thumbHeight - 1, 0xFF3B7DE8);
        }
    }
}
