package com.amnedev.p3rmenu.v262.mixin;

import com.amnedev.p3rmenu.v262.P3RConfig;
import com.amnedev.p3rmenu.v262.P3RChatAnimationState;
import com.amnedev.p3rmenu.v262.P3RChatHistoryState;
import com.amnedev.p3rmenu.v262.P3RGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements P3RChatHistoryState {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Unique private Double p3r_previousBackgroundOpacity;
    @Unique private boolean p3r_bottomShiftPushed;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"))
    private void p3r_chatPlane(GuiGraphicsExtractor graphics, Font font, int ticks,
            int mouseX, int mouseY, ChatComponent.DisplayMode displayMode,
            boolean changeCursorOnInsertions, CallbackInfo ci) {
        if (!P3RConfig.customChat()) return;
        boolean chatOpen = minecraft.gui.screen() instanceof ChatScreen;
        float verticalShift = 40.0F;
        if (chatOpen) {
            verticalShift = minecraft.gui.screen() instanceof P3RChatAnimationState animation
                    && animation.p3r_startedWithChatHistory()
                    ? 40.0F * (1.0F - animation.p3r_chatIntro()) : 0.0F;
        }
        p3r_bottomShiftPushed = verticalShift > 0.01F;
        if (p3r_bottomShiftPushed) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0.0F, verticalShift);
        }
        p3r_previousBackgroundOpacity = minecraft.options.textBackgroundOpacity().get();
        if (p3r_previousBackgroundOpacity != 0.0D) {
            minecraft.options.textBackgroundOpacity().set(0.0D);
        }
        if (trimmedMessages.isEmpty()) return;
        double scale = minecraft.options.chatScale().get();
        int candidates = Math.min(trimmedMessages.size(), minecraft.gui.hud.getChat().getLinesPerPage());
        int visible = 0;
        float planeOpacity = chatOpen ? 1.0F : 0.0F;
        for (int index = 0; index < candidates; index++) {
            float opacity = chatOpen ? 1.0F
                    : p3r_messageOpacity(ticks - trimmedMessages.get(index).addedTime());
            if (opacity > 1.0E-5F) {
                visible++;
                planeOpacity = Math.max(planeOpacity, opacity);
            }
        }
        if (visible == 0) return;
        int bottom = (int) Math.floor((graphics.guiHeight() - 40) / scale);
        int lineHeight = (int) (9.0D * (minecraft.options.chatLineSpacing().get() + 1.0D));
        int height = visible * Math.max(1, lineHeight) + 8;
        int width = ChatComponent.getWidth(minecraft.options.chatWidth().get()) + 14;
        graphics.pose().pushMatrix();
        graphics.pose().scale((float) scale, (float) scale);
        graphics.pose().translate(4.0F, 0.0F);
        P3RGraphics.skewedRect(graphics, -48, bottom - height,
                width + 70, height, 18, P3RGraphics.alpha(0xA70C0A68, planeOpacity));
        graphics.fill(-32, bottom - height, Math.round(width * 0.72F),
                bottom - height + 2, P3RGraphics.alpha(P3RGraphics.CYAN, planeOpacity));
        graphics.pose().popMatrix();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("RETURN"))
    private void p3r_restoreChatBackground(GuiGraphicsExtractor graphics, Font font, int ticks,
            int mouseX, int mouseY, ChatComponent.DisplayMode displayMode,
            boolean changeCursorOnInsertions, CallbackInfo ci) {
        if (p3r_previousBackgroundOpacity != null) {
            minecraft.options.textBackgroundOpacity().set(p3r_previousBackgroundOpacity);
            p3r_previousBackgroundOpacity = null;
        }
        if (p3r_bottomShiftPushed) {
            graphics.pose().popMatrix();
            p3r_bottomShiftPushed = false;
        }
    }

    @Unique
    private static float p3r_messageOpacity(int ageTicks) {
        double value = 1.0D - ageTicks / 200.0D;
        value = Math.max(0.0D, Math.min(1.0D, value * 10.0D));
        return (float) (value * value);
    }

    @Override
    public boolean p3r_hasChatMessages() {
        return !trimmedMessages.isEmpty();
    }
}
