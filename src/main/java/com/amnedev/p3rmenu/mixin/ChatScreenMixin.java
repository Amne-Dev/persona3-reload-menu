package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RConfig;
import com.amnedev.p3rmenu.util.P3RChatAnimationState;
import com.amnedev.p3rmenu.util.P3RChatHistoryState;
import com.amnedev.p3rmenu.util.P3RHelper;
import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Persona-styled chat composer with a fast transform entrance. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen implements P3RChatAnimationState {
    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private long p3r_chatStartedAt;
    @Unique
    private boolean p3r_chatTransformPushed;
    @Unique
    private boolean p3r_startedWithHistory;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_initChat(CallbackInfo ci) {
        if (!P3RConfig.isCustomChatEnabled()) {
            return;
        }
        this.p3r_chatStartedAt = Util.getMeasuringTimeMs();
        this.p3r_startedWithHistory = this.client.inGameHud.getChatHud()
                instanceof P3RChatHistoryState history && history.p3r_hasChatMessages();
        int panelWidth = p3r_panelWidth();
        this.chatField.setX(Math.max(18, Math.round(this.width * 0.018F)));
        this.chatField.setY(this.height - Math.max(24, Math.round(this.height * 0.026F)));
        this.chatField.setWidth(panelWidth - this.chatField.getX() - 26);
        this.chatField.setEditableColor(0xFFF7FAFF);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void p3r_beginChatTransform(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        this.p3r_chatTransformPushed = P3RConfig.isCustomChatEnabled();
        if (!this.p3r_chatTransformPushed) {
            return;
        }
        float intro = p3r_chatIntro();
        p3r_pushComposerTransform(context, intro);

        int panelWidth = p3r_panelWidth();
        int bottom = this.height;
        int top = bottom - Math.max(40, Math.round(this.height * 0.052F));
        P3RHelper.drawSkewedRect(context, -64, top, panelWidth + 100,
                bottom - top + 5, 32, 0xED0B0875);
        P3RHelper.drawSkewedRect(context, -40, top - 4,
                Math.round(panelWidth * 0.58F) + 22, 4, 18, P3RSettingsShell.CYAN);
        context.fill(-32, bottom - 3, Math.round(panelWidth * intro), bottom,
                P3RSettingsShell.RED);

        Text heading = Text.literal("CHAT").setStyle(Style.EMPTY.withBold(true));
        context.getMatrices().push();
        context.getMatrices().translate(18.0F, top + 4.0F, 40.0F);
        context.getMatrices().scale(1.05F, 1.05F, 1.0F);
        context.drawText(this.textRenderer, heading, 0, 0, P3RSettingsShell.WHITE, true);
        context.getMatrices().pop();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void p3r_endChatTransform(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        p3r_popComposerTransform(context);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0),
            require = 0)
    private void p3r_removeVanillaInputBackdrop(DrawContext context,
            int x1, int y1, int x2, int y2, int color) {
        if (!P3RConfig.isCustomChatEnabled()) {
            context.fill(x1, y1, x2, y2, color);
        }
    }

    @Unique
    private int p3r_panelWidth() {
        return MathHelper.clamp(Math.round(this.width * 0.64F), 240,
                Math.max(240, this.width - 16));
    }

    @Unique
    private void p3r_pushComposerTransform(DrawContext context, float intro) {
        context.getMatrices().push();
        if (this.p3r_startedWithHistory) {
            float travel = Math.max(44.0F, this.height * 0.058F);
            context.getMatrices().translate(0.0F, (1.0F - intro) * travel, 0.0F);
            context.getMatrices().scale(1.0F, 0.92F + 0.08F * intro, 1.0F);
        } else {
            context.getMatrices().translate(-(1.0F - intro) * this.width * 0.15F,
                    (1.0F - intro) * 24.0F, 0.0F);
            context.getMatrices().scale(0.76F + 0.24F * intro,
                    0.92F + 0.08F * intro, 1.0F);
        }
        this.p3r_chatTransformPushed = true;
    }

    @Unique
    private void p3r_popComposerTransform(DrawContext context) {
        if (this.p3r_chatTransformPushed) {
            context.getMatrices().pop();
            this.p3r_chatTransformPushed = false;
        }
    }

    @Override
    public float p3r_chatIntro() {
        float raw = MathHelper.clamp((Util.getMeasuringTimeMs() - this.p3r_chatStartedAt)
                / 260.0F, 0.0F, 1.0F);
        return P3RSettingsShell.sharpOut(raw);
    }

    @Override
    public boolean p3r_startedWithChatHistory() {
        return this.p3r_startedWithHistory;
    }
}
