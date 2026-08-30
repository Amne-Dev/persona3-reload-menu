package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RScreenShell;
import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin {
    @Shadow protected int top;
    @Shadow protected int bottom;
    @Shadow protected int itemHeight;
    @Shadow protected int headerHeight;

    @Shadow
    protected abstract boolean isSelectedEntry(int index);

    @Shadow protected abstract int getEntryCount();
    @Shadow public abstract int getRowLeft();
    @Shadow public abstract int getRowRight();
    @Shadow protected abstract int getMaxPosition();
    @Shadow protected abstract int getScrollbarPositionX();
    @Shadow public abstract int getMaxScroll();
    @Shadow public abstract double getScrollAmount();

    @Inject(method = "getRowLeft", at = @At("RETURN"), cancellable = true)
    private void p3r_alignPersonaRows(CallbackInfoReturnable<Integer> cir) {
        if (p3r_isPersonaList()) {
            int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
            cir.setReturnValue(Math.round(width * 0.385F));
        } else if (p3r_isLanguageList()) {
            int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
            cir.setReturnValue(Math.round(width * 0.095F));
        }
    }

    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void p3r_sizeConfigRows(CallbackInfoReturnable<Integer> cir) {
        if (p3r_isLanguageList()) {
            int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
            cir.setReturnValue(Math.round(width * 0.53F));
        }
    }

    @Inject(method = "getScrollbarPositionX", at = @At("RETURN"), cancellable = true)
    private void p3r_positionConfigScrollbar(CallbackInfoReturnable<Integer> cir) {
        if (p3r_shouldStyleScrollbar()) {
            int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
            cir.setReturnValue(Math.max(6, Math.round(width * 0.035F)));
        }
    }

    @Inject(method = "getEntryAtPosition", at = @At("HEAD"), cancellable = true)
    private void p3r_hitTestRowsWithLeftScrollbar(double mouseX, double mouseY,
            CallbackInfoReturnable<Object> cir) {
        if (!p3r_shouldStyleScrollbar()) {
            return;
        }

        // 1.20.1's vanilla implementation assumes mouseX must be left of the
        // scrollbar. That condition invalidates every row when the scrollbar is
        // intentionally placed on the left, so test the row bounds directly.
        int relativeY = MathHelper.floor(mouseY - top) - headerHeight
                + (int) getScrollAmount() - 4;
        int index = relativeY / itemHeight;
        if (mouseX >= getRowLeft() && mouseX <= getRowRight()
                && relativeY >= 0 && index >= 0 && index < getEntryCount()) {
            cir.setReturnValue(((EntryListWidget<?>) (Object) this).children().get(index));
        } else {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void p3r_renderScrollbar(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        int maxScroll = getMaxScroll();
        int viewportHeight = bottom - top;
        if (!p3r_shouldStyleScrollbar() || maxScroll <= 0 || viewportHeight <= 0) {
            return;
        }

        int x = getScrollbarPositionX();
        int contentHeight = Math.max(1, getMaxPosition());
        int thumbHeight = MathHelper.clamp(
                viewportHeight * viewportHeight / contentHeight,
                Math.min(18, viewportHeight), viewportHeight);
        int thumbY = top + MathHelper.clamp(
                (int) Math.round(getScrollAmount() * (viewportHeight - thumbHeight)
                        / Math.max(1, maxScroll)),
                0, Math.max(0, viewportHeight - thumbHeight));

        // Opaque outer rail fully replaces the vanilla six-pixel scrollbar.
        context.fill(x - 1, top, x + 7, bottom, 0xE504113D);
        context.fill(x + 1, top + 2, x + 5, bottom - 2, 0xFF163E78);
        context.fill(x, thumbY, x + 6, thumbY + thumbHeight, 0xFF080CB5);
        context.fill(x, thumbY, x + 6, Math.min(bottom, thumbY + 2), 0xFF58E7FF);
        if (thumbHeight > 4) {
            context.fill(x + 2, thumbY + 3, x + 4,
                    thumbY + thumbHeight - 1, 0xFF3B7DE8);
        }
    }

    @Inject(method = "renderEntry", at = @At("HEAD"))
    private void p3r_renderEntrySurface(DrawContext context, int mouseX, int mouseY,
            float delta, int index, int x, int y, int entryWidth, int entryHeight,
            CallbackInfo ci) {
        boolean persona = p3r_isPersonaList();
        boolean language = p3r_isLanguageList();
        if (!persona && !language) {
            return;
        }

        boolean selected = this.isSelectedEntry(index);
        boolean hovered = mouseX >= x && mouseX <= x + entryWidth
                && mouseY >= y && mouseY <= y + entryHeight;
        int bottom = y + Math.max(1, entryHeight - 2);
        int color = language
                ? selected ? 0xEA080C74 : hovered ? 0x5A556A93 : 0x10556A93
                : selected ? 0xE0080CB5 : hovered ? 0xC20A286D : 0x9B06143E;
        if (!language || selected || hovered) {
            context.fill(x - 2, y, x + entryWidth + 2, bottom, color);
        }
        if (selected && language) {
            context.fill(x - 2, y, x + entryWidth + 2, y + 2, 0xFFF0442E);
            context.fill(x - 2, y, x + 1, bottom, 0xFFFF3E98);
        } else if (hovered && !selected) {
            context.fill(x - 2, y, x + entryWidth + 2, y + 1, 0xB558E7FF);
        }
    }

    @Inject(method = "drawSelectionHighlight", at = @At("HEAD"), cancellable = true)
    private void p3r_drawPersonaSelection(DrawContext context, int x, int y,
            int width, int height, int color, CallbackInfo ci) {
        boolean persona = p3r_isPersonaList();
        boolean language = p3r_isLanguageList();
        if (!persona && !language) {
            return;
        }
        ci.cancel();
        context.fill(x - 4, y - 2, x + width + 4, y + height + 2,
                language ? 0xEA080C74 : 0xF0080CB5);
        context.fill(x - 4, y - 2, x + width + 4, y + 1,
                language ? 0xFFF0442E : 0xFF58E7FF);
    }

    private boolean p3r_isPersonaList() {
        Object self = this;
        if (!(self instanceof WorldListWidget) && !(self instanceof MultiplayerServerListWidget)) {
            return false;
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen instanceof net.minecraft.client.gui.screen.world.SelectWorldScreen
                || screen instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
    }

    private boolean p3r_isLanguageList() {
        return MinecraftClient.getInstance().currentScreen instanceof LanguageOptionsScreen;
    }

    private boolean p3r_shouldStyleScrollbar() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen != null && (P3RSettingsShell.isSettingsDetail(screen)
                || P3RScreenShell.isPersonaListScreen(screen));
    }
}
