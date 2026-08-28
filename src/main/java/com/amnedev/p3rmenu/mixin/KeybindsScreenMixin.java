package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/** Adapts the key-binding list, which owns a renderer outside OptionListWidget. */
@Mixin(KeybindsScreen.class)
public abstract class KeybindsScreenMixin extends Screen {
    @Unique
    private long p3r_keybindsStartedAt = Util.getMeasuringTimeMs();

    @Unique
    private EntryListWidget<?> p3r_controlsList;

    protected KeybindsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_findControlsList(CallbackInfo ci) {
        p3r_captureAndLayoutList();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_renderKeybinds(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        ci.cancel();
        p3r_captureAndLayoutList();
        P3RSettingsShell.layoutFooterButtons(this);
        p3r_updateResetAvailability();
        P3RSettingsShell.renderDetailBackground(context, this.width, this.height,
                this.p3r_keybindsStartedAt);
        if (this.p3r_controlsList != null) {
            this.p3r_controlsList.render(context, mouseX, mouseY, delta);
        }
        P3RSettingsShell.renderDetailHeader(context, this.title,
                this.width, this.height, this.p3r_keybindsStartedAt);
        P3RSettingsShell.renderDetailFooter(context, this.width, this.height,
                P3RSettingsShell.entrance(this.p3r_keybindsStartedAt));
        super.render(context, mouseX, mouseY, delta);
    }

    @Unique
    private void p3r_captureAndLayoutList() {
        if (this.p3r_controlsList == null) {
            for (Element element : this.children()) {
                if (element instanceof EntryListWidget<?> list) {
                    this.p3r_controlsList = list;
                    break;
                }
            }
        }
        if (this.p3r_controlsList != null) {
            this.p3r_controlsList.updateSize(this.width, this.height,
                    Math.max(42, Math.round(this.height * 0.105F)),
                    Math.round(this.height * 0.80F));
            this.p3r_controlsList.setRenderBackground(false);
            this.p3r_controlsList.setRenderHorizontalShadows(false);
        }
    }

    @Unique
    private void p3r_updateResetAvailability() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean changed = Arrays.stream(client.options.allKeys)
                .anyMatch(binding -> !binding.isDefault());
        for (Element element : this.children()) {
            if (element instanceof ClickableWidget widget
                    && widget.getMessage().getString()
                            .equals(Text.translatable("controls.resetAll").getString())) {
                widget.active = changed;
            }
        }
    }
}
