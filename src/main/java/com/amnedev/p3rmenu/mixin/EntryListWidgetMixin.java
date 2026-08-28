package com.amnedev.p3rmenu.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin {
    @Shadow
    protected abstract boolean isSelectedEntry(int index);

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
        if (p3r_isLanguageList()) {
            int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
            cir.setReturnValue(Math.round(width * 0.655F));
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
}
