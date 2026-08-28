package com.amnedev.p3rmenu.v262.mixin;

import com.amnedev.p3rmenu.v262.P3RGraphics;
import com.amnedev.p3rmenu.v262.P3RScreenFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {
    @Inject(method = "extractListBackground", at = @At("HEAD"), cancellable = true)
    private void p3r_clearListBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen != null && P3RScreenFamily.isStyled(screen)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractListSeparators", at = @At("HEAD"), cancellable = true)
    private void p3r_clearListSeparators(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen != null && P3RScreenFamily.isStyled(screen)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractItem", at = @At("HEAD"))
    private void p3r_entrySurface(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float delta, @Coerce Object rawEntry, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen == null || !P3RScreenFamily.isList(screen)) return;
        LayoutElement entry;
        boolean selected;
        if (rawEntry instanceof ServerSelectionList.Entry serverEntry
                && (Object) this instanceof ServerSelectionList serverList) {
            entry = serverEntry;
            selected = serverList.getSelected() == serverEntry;
        } else if (rawEntry instanceof WorldSelectionList.Entry worldEntry
                && (Object) this instanceof WorldSelectionList worldList) {
            entry = worldEntry;
            selected = worldList.getSelected() == worldEntry;
        } else {
            return;
        }
        boolean hovered = rawEntry instanceof GuiEventListener listener
                && listener.isMouseOver(mouseX, mouseY);
        int color = selected ? 0xE0080CB5 : hovered ? 0xC20A286D : 0x9B06143E;
        graphics.fill(entry.getX() - 2, entry.getY(),
                entry.getX() + entry.getWidth() + 2,
                entry.getY() + Math.max(1, entry.getHeight() - 2), color);
        if (hovered && !selected) {
            graphics.fill(entry.getX() - 2, entry.getY(),
                    entry.getX() + entry.getWidth() + 2, entry.getY() + 1,
                    P3RGraphics.CYAN);
        }
    }

    @Inject(method = "extractSelection", at = @At("HEAD"), cancellable = true)
    private void p3r_selection(GuiGraphicsExtractor graphics,
            @Coerce Object rawEntry, int outlineColor, CallbackInfo ci) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen == null || !P3RScreenFamily.isList(screen)) return;
        LayoutElement entry;
        if (rawEntry instanceof ServerSelectionList.Entry serverEntry) {
            entry = serverEntry;
        } else if (rawEntry instanceof WorldSelectionList.Entry worldEntry) {
            entry = worldEntry;
        } else {
            return;
        }
        ci.cancel();
        graphics.fill(entry.getX() - 4, entry.getY() - 2,
                entry.getX() + entry.getWidth() + 4,
                entry.getY() + entry.getHeight() + 2, 0xF0080CB5);
        graphics.fill(entry.getX() - 4, entry.getY() - 2,
                entry.getX() + entry.getWidth() + 4, entry.getY() + 1,
                P3RGraphics.CYAN);
    }

    @Inject(method = "getRowLeft", at = @At("RETURN"), cancellable = true)
    private void p3r_rowLeft(CallbackInfoReturnable<Integer> cir) {
        Screen screen = Minecraft.getInstance().gui.screen();
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
        Screen screen = Minecraft.getInstance().gui.screen();
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
        Screen screen = Minecraft.getInstance().gui.screen();
        if ((Object) this instanceof OptionsList && screen != null
                && P3RScreenFamily.isConfiguration(screen)) {
            cir.setReturnValue(Math.round(screen.width * 0.70F) + 6);
        } else if ((Object) this instanceof ServerSelectionList
                || (Object) this instanceof WorldSelectionList) {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int rowWidth = (Object) this instanceof WorldSelectionList
                    ? Math.max(220, Math.min(620, Math.round(width * 0.54F)))
                    : Math.max(260, Math.min(640, Math.round(width * 0.54F)));
            cir.setReturnValue(Math.round(width * 0.385F) + rowWidth + 6);
        }
    }
}
