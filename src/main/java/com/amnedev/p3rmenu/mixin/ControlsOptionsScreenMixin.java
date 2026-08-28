package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Gives the direct-widget controls page the same CONFIG composition as option lists. */
@Mixin(ControlsOptionsScreen.class)
public abstract class ControlsOptionsScreenMixin extends Screen {
    @Unique
    private long p3r_controlsStartedAt = Util.getMeasuringTimeMs();

    protected ControlsOptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_layoutControls(CallbackInfo ci) {
        p3r_applyControlsLayout();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_renderControls(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        ci.cancel();
        p3r_applyControlsLayout();
        P3RSettingsShell.renderDetailBackground(context, this.width, this.height,
                this.p3r_controlsStartedAt);
        P3RSettingsShell.renderDetailHeader(context, this.title,
                this.width, this.height, this.p3r_controlsStartedAt);
        P3RSettingsShell.renderDetailFooter(context, this.width, this.height,
                P3RSettingsShell.entrance(this.p3r_controlsStartedAt));
        super.render(context, mouseX, mouseY, delta);
    }

    @Unique
    private void p3r_applyControlsLayout() {
        List<ClickableWidget> content = new ArrayList<>();
        ClickableWidget done = null;
        for (Element element : this.children()) {
            if (!(element instanceof ClickableWidget widget) || !widget.visible) {
                continue;
            }
            if (widget.getMessage().getString().equals(ScreenTexts.DONE.getString())) {
                done = widget;
            } else {
                content.add(widget);
            }
        }

        float scale = P3RSettingsShell.uiScale(this.width, this.height);
        int left = Math.round(this.width * 0.095F);
        int right = Math.round(this.width * 0.625F);
        int gap = Math.max(6, Math.round(8.0F * scale));
        int widgetWidth = Math.max(90, (right - left - gap) / 2);
        int top = Math.round(this.height * 0.18F);
        int rowStep = Math.max(27, Math.round(this.height * 0.087F));
        for (int i = 0; i < content.size(); i++) {
            ClickableWidget widget = content.get(i);
            widget.setX(left + (i % 2) * (widgetWidth + gap));
            widget.setY(top + (i / 2) * rowStep);
            widget.setWidth(widgetWidth);
        }

        if (done != null) {
            int footerWidth = Math.max(90, Math.round(118.0F * scale));
            done.setWidth(footerWidth);
            done.setX(this.width - footerWidth - Math.max(14, Math.round(20.0F * scale)));
            done.setY(Math.round(this.height * 0.84F));
        }
    }
}
