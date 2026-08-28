package com.amnedev.p3rmenu.v121.mixin;

import com.amnedev.p3rmenu.v121.P3RConfig;
import com.amnedev.p3rmenu.v121.P3RChatAnimationState;
import com.amnedev.p3rmenu.v121.P3RChatHistoryState;
import com.amnedev.p3rmenu.v121.P3RGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen implements P3RChatAnimationState {
    @Shadow protected EditBox input;
    @Unique private long p3r_startedAt;
    @Unique private boolean p3r_pushed;
    @Unique private boolean p3r_startedWithHistory;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_initChat(CallbackInfo ci) {
        if (!P3RConfig.customChat()) return;
        p3r_startedAt = Util.getMillis();
        p3r_startedWithHistory = minecraft.gui.getChat()
                instanceof P3RChatHistoryState history && history.p3r_hasChatMessages();
        int panelWidth = p3r_panelWidth();
        input.setX(Math.max(18, Math.round(width * 0.018F)));
        input.setY(height - Math.max(24, Math.round(height * 0.026F)));
        input.setWidth(panelWidth - input.getX() - 26);
        input.setTextColor(P3RGraphics.WHITE);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void p3r_begin(GuiGraphics graphics, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        p3r_pushed = P3RConfig.customChat();
        if (!p3r_pushed) return;
        float intro = p3r_chatIntro();
        p3r_pushComposerTransform(graphics, intro);
        int panelWidth = p3r_panelWidth();
        int top = height - Math.max(40, Math.round(height * 0.052F));
        P3RGraphics.skewedRect(graphics, -64, top, panelWidth + 100,
                height - top + 5, 32, 0xED0B0875);
        P3RGraphics.skewedRect(graphics, -40, top - 4,
                Math.round(panelWidth * 0.58F) + 22, 4, 18, P3RGraphics.CYAN);
        graphics.fill(-32, height - 3, Math.round(panelWidth * intro), height, P3RGraphics.RED);
        P3RGraphics.fittedText(graphics, font, P3RGraphics.bold("CHAT"),
                18, top + 9, 80, 1.05F, P3RGraphics.WHITE, true);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V",
            shift = At.Shift.BEFORE))
    private void p3r_releaseComposerForHistory(GuiGraphics graphics, int mouseX,
            int mouseY, float delta, CallbackInfo ci) {
        p3r_popComposerTransform(graphics);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V",
            shift = At.Shift.AFTER))
    private void p3r_restoreComposerAfterHistory(GuiGraphics graphics, int mouseX,
            int mouseY, float delta, CallbackInfo ci) {
        if (P3RConfig.customChat()) {
            p3r_pushComposerTransform(graphics, p3r_chatIntro());
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void p3r_end(GuiGraphics graphics, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        p3r_popComposerTransform(graphics);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0),
            index = 4, require = 1)
    private int p3r_removeVanillaInputBackdrop(int color) {
        return P3RConfig.customChat() ? 0 : color;
    }

    @Unique
    private int p3r_panelWidth() {
        return Mth.clamp(Math.round(width * 0.64F), 240, Math.max(240, width - 16));
    }

    @Unique
    private void p3r_pushComposerTransform(GuiGraphics graphics, float intro) {
        graphics.pose().pushMatrix();
        if (p3r_startedWithHistory) {
            float travel = Math.max(44.0F, height * 0.058F);
            graphics.pose().translate(0.0F, (1.0F - intro) * travel);
            graphics.pose().scale(1.0F, 0.92F + intro * 0.08F);
        } else {
            graphics.pose().translate(-(1.0F - intro) * width * 0.15F,
                    (1.0F - intro) * 24.0F);
            graphics.pose().scale(0.76F + intro * 0.24F,
                    0.92F + intro * 0.08F);
        }
        p3r_pushed = true;
    }

    @Unique
    private void p3r_popComposerTransform(GuiGraphics graphics) {
        if (p3r_pushed) {
            graphics.pose().popMatrix();
            p3r_pushed = false;
        }
    }

    @Override
    public float p3r_chatIntro() {
        return P3RGraphics.easeOut((Util.getMillis() - p3r_startedAt) / 260.0F);
    }

    @Override
    public boolean p3r_startedWithChatHistory() {
        return p3r_startedWithHistory;
    }
}
