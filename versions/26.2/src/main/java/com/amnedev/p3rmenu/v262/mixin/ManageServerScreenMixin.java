package com.amnedev.p3rmenu.v262.mixin;

import com.amnedev.p3rmenu.v262.P3RGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ManageServerScreen.class)
public abstract class ManageServerScreenMixin extends Screen {
    @Shadow @Final private static Component NAME_LABEL;
    @Shadow @Final private static Component IP_LABEL;
    @Shadow private EditBox nameEdit;
    @Shadow private EditBox ipEdit;

    protected ManageServerScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_layoutForm(CallbackInfo ci) { p3r_applyFormLayout(); }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void p3r_renderServerForm(GuiGraphicsExtractor graphics, int mouseX,
            int mouseY, float delta, CallbackInfo ci) {
        p3r_applyFormLayout();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, P3RGraphics.bold(NAME_LABEL.getString()),
                nameEdit.getX() + 1, nameEdit.getY() - 12, P3RGraphics.CYAN, true);
        graphics.text(font, P3RGraphics.bold(IP_LABEL.getString()),
                ipEdit.getX() + 1, ipEdit.getY() - 12, P3RGraphics.CYAN, true);
        nameEdit.extractRenderState(graphics, mouseX, mouseY, delta);
        ipEdit.extractRenderState(graphics, mouseX, mouseY, delta);
        ci.cancel();
    }

    @Unique
    private void p3r_applyFormLayout() {
        int left = Math.round(width * 0.095F);
        int right = Math.round(width * 0.625F);
        int formWidth = Math.max(140, right - left);
        if (nameEdit != null) { nameEdit.setX(left); nameEdit.setWidth(formWidth); }
        if (ipEdit != null) { ipEdit.setX(left); ipEdit.setWidth(formWidth); }
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractButton button
                    && button.getY() < Math.round(height * 0.68F)) {
                button.setPosition(left, button.getY());
                button.setWidth(formWidth);
            }
        }
        P3RGraphics.layoutFooterButtons(this);
    }
}
