package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RConfig;
import com.amnedev.p3rmenu.util.P3RChatAnimationState;
import com.amnedev.p3rmenu.util.P3RChatHistoryState;
import com.amnedev.p3rmenu.util.P3RHelper;
import com.amnedev.p3rmenu.util.P3RSettingsShell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Adds a readable P3R plane behind vanilla chat text while preserving chat behavior. */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements P3RChatHistoryState {
    @Shadow
    @Final
    private List<ChatHudLine.Visible> visibleMessages;

    @Shadow public abstract int getWidth();
    @Shadow public abstract int getVisibleLineCount();
    @Shadow public abstract double getChatScale();

    @org.spongepowered.asm.mixin.Unique
    private Double p3r_previousBackgroundOpacity;

    @org.spongepowered.asm.mixin.Unique
    private boolean p3r_bottomShiftPushed;

    @Inject(method = "render", at = @At("HEAD"))
    private void p3r_renderChatPlane(DrawContext context, int currentTick,
            int mouseX, int mouseY, CallbackInfo ci) {
        if (!P3RConfig.isCustomChatEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        boolean chatOpen = client.currentScreen instanceof ChatScreen;
        float verticalShift = 40.0F;
        if (chatOpen) {
            verticalShift = client.currentScreen instanceof P3RChatAnimationState animation
                    && animation.p3r_startedWithChatHistory()
                    ? 40.0F * (1.0F - animation.p3r_chatIntro()) : 0.0F;
        }
        this.p3r_bottomShiftPushed = verticalShift > 0.01F;
        if (this.p3r_bottomShiftPushed) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, verticalShift, 0.0F);
        }
        this.p3r_previousBackgroundOpacity = client.options.getTextBackgroundOpacity().getValue();
        if (this.p3r_previousBackgroundOpacity != 0.0D) {
            client.options.getTextBackgroundOpacity().setValue(0.0D);
        }
        if (this.visibleMessages.isEmpty()) {
            return;
        }
        int candidates = Math.min(this.visibleMessages.size(), this.getVisibleLineCount());
        int lineCount = 0;
        float planeOpacity = chatOpen ? 1.0F : 0.0F;
        for (int index = 0; index < candidates; index++) {
            float opacity = chatOpen ? 1.0F
                    : p3r_messageOpacity(currentTick - this.visibleMessages.get(index).addedTime());
            if (opacity > 1.0E-5F) {
                lineCount++;
                planeOpacity = Math.max(planeOpacity, opacity);
            }
        }
        if (lineCount <= 0) {
            return;
        }
        double scale = this.getChatScale();
        int screenHeight = client.getWindow().getScaledHeight();
        int bottom = (int) Math.floor((screenHeight - 40) / scale);
        int lineHeight = MathHelper.floor(9.0D
                * (client.options.getChatLineSpacing().getValue() + 1.0D));
        int height = lineCount * Math.max(1, lineHeight) + 8;
        int width = this.getWidth() + 14;

        context.getMatrices().push();
        context.getMatrices().scale((float) scale, (float) scale, 1.0F);
        context.getMatrices().translate(4.0F, 0.0F, -1.0F);
        P3RHelper.drawSkewedRect(context, -48, bottom - height,
                width + 70, height, 18, p3r_alpha(0xA70C0A68, planeOpacity));
        context.fill(-32, bottom - height, Math.round(width * 0.72F),
                bottom - height + 2, p3r_alpha(P3RSettingsShell.CYAN, planeOpacity));
        context.getMatrices().pop();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void p3r_restoreVanillaBackgroundOpacity(DrawContext context, int currentTick,
            int mouseX, int mouseY, CallbackInfo ci) {
        if (this.p3r_previousBackgroundOpacity != null) {
            MinecraftClient.getInstance().options.getTextBackgroundOpacity()
                    .setValue(this.p3r_previousBackgroundOpacity);
            this.p3r_previousBackgroundOpacity = null;
        }
        if (this.p3r_bottomShiftPushed) {
            context.getMatrices().pop();
            this.p3r_bottomShiftPushed = false;
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static float p3r_messageOpacity(int ageTicks) {
        double value = 1.0D - ageTicks / 200.0D;
        value = MathHelper.clamp(value * 10.0D, 0.0D, 1.0D);
        return (float) (value * value);
    }

    @org.spongepowered.asm.mixin.Unique
    private static int p3r_alpha(int color, float opacity) {
        int alpha = MathHelper.clamp(Math.round((color >>> 24) * opacity), 0, 255);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public boolean p3r_hasChatMessages() {
        return !this.visibleMessages.isEmpty();
    }
}
