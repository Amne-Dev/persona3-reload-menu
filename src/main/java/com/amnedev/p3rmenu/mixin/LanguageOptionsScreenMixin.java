package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adapts the language selector, which bypasses GameOptionsScreen's list renderer. */
@Mixin(LanguageOptionsScreen.class)
public abstract class LanguageOptionsScreenMixin extends Screen {
    @Unique
    private long p3r_languageStartedAt = Util.getMeasuringTimeMs();

    @Unique
    private EntryListWidget<?> p3r_languageList;

    protected LanguageOptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_findLanguageList(CallbackInfo ci) {
        p3r_captureAndLayoutList();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_renderLanguage(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        ci.cancel();
        p3r_captureAndLayoutList();
        P3RSettingsShell.layoutFooterButtons(this);
        P3RSettingsShell.renderDetailBackground(context, this.width, this.height,
                this.p3r_languageStartedAt);
        if (this.p3r_languageList != null) {
            this.p3r_languageList.render(context, mouseX, mouseY, delta);
        }
        P3RSettingsShell.renderDetailHeader(context, this.title,
                this.width, this.height, this.p3r_languageStartedAt);

        Text warning = Text.translatable("options.languageWarning")
                .setStyle(Style.EMPTY.withBold(true));
        P3RSettingsShell.drawFittedText(context, warning,
                this.width * 0.095F, this.height * 0.785F,
                this.width * 0.53F, P3RSettingsShell.CYAN, true);
        P3RSettingsShell.renderDetailFooter(context, this.width, this.height,
                P3RSettingsShell.entrance(this.p3r_languageStartedAt));
        super.render(context, mouseX, mouseY, delta);
    }

    @Unique
    private void p3r_captureAndLayoutList() {
        if (this.p3r_languageList == null) {
            for (Element element : this.children()) {
                if (element instanceof EntryListWidget<?> list) {
                    this.p3r_languageList = list;
                    break;
                }
            }
        }
        if (this.p3r_languageList != null) {
            this.p3r_languageList.updateSize(this.width, this.height,
                    Math.max(42, Math.round(this.height * 0.105F)),
                    Math.round(this.height * 0.75F));
            this.p3r_languageList.setRenderBackground(false);
            this.p3r_languageList.setRenderHorizontalShadows(false);
        }
    }
}
