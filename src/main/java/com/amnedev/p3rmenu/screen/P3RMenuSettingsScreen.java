package com.amnedev.p3rmenu.screen;

import com.amnedev.p3rmenu.util.P3RConfig;
import com.amnedev.p3rmenu.util.P3RSettingsShell;
import com.amnedev.p3rmenu.util.TransitionManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/** One home for options owned by this mod, without consuming multiple vanilla menu rows. */
public final class P3RMenuSettingsScreen extends Screen {
    private static final int WALLPAPER = 0;
    private static final int CUSTOM_CHAT = 1;
    private static final int DONE = 2;

    private final Screen parent;
    private int selected;
    private long startedAt;
    private long lastFrameAt;
    private final float[] selection = {1.0F, 0.0F, 0.0F};

    public P3RMenuSettingsScreen(Screen parent) {
        super(Text.literal("P3R Menu Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.startedAt = Util.getMeasuringTimeMs();
        this.lastFrameAt = this.startedAt;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateAnimation();
        updateMouse(mouseX, mouseY);
        P3RSettingsShell.renderDetailBackground(context, width, height, startedAt);
        P3RSettingsShell.renderDetailHeader(context, Text.literal("P3R MENU SETTINGS"),
                width, height, startedAt);

        float intro = P3RSettingsShell.entrance(startedAt);
        for (int index = 0; index < 3; index++) {
            int y = rowY(index);
            int left = Math.round(width * 0.075F);
            int right = Math.round(width * 0.70F);
            int rowHeight = rowHeight();
            if (selection[index] > 0.01F) {
                P3RSettingsShell.renderSelection(context, left, y, right,
                        rowHeight, selection[index] * intro);
            } else {
                context.fill(left, y, right, y + rowHeight, 0x20556A93);
            }
            P3RSettingsShell.drawFittedText(context, label(index),
                    left + 9, y + rowHeight * 0.5F, right - left - 18,
                    index == selected ? P3RSettingsShell.INK : P3RSettingsShell.CYAN,
                    false);
        }

        Text hint = Text.literal("CUSTOM CHAT CHANGES THE IN-GAME CHAT PANEL AND ITS OPENING MOTION")
                .setStyle(Style.EMPTY.withBold(true));
        P3RSettingsShell.drawFittedText(context, hint, width * 0.075F, height * 0.70F,
                width * 0.60F, P3RSettingsShell.CYAN, false);
        P3RSettingsShell.renderDetailFooter(context, width, height, intro);
        TransitionManager.render(context, delta, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TransitionManager.isBlockingInput()) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int index = rowAt(mouseX, mouseY);
            if (index >= 0) {
                selected = index;
                activate();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (TransitionManager.isBlockingInput()) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
            move(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
            move(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A
                || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            if (selected == CUSTOM_CHAT) {
                toggleChat();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            activate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (!TransitionManager.isTransitioning()) {
            TransitionManager.startOut(Text.literal("BACK"),
                    () -> client.setScreen(parent));
        }
    }

    private void activate() {
        if (selected == CUSTOM_CHAT) {
            toggleChat();
        } else if (selected == WALLPAPER) {
            TransitionManager.startOut(Text.literal("WALLPAPER"),
                    () -> client.setScreen(new WallpaperScreen(this)));
        } else {
            close();
        }
    }

    private void toggleChat() {
        P3RConfig.toggleCustomChat();
        if (client != null) {
            client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance
                    .master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private Text label(int index) {
        String value = switch (index) {
            case WALLPAPER -> "WALLPAPER...";
            case CUSTOM_CHAT -> "CUSTOM CHAT: "
                    + (P3RConfig.isCustomChatEnabled() ? "ON" : "OFF");
            default -> "DONE";
        };
        return Text.literal(value).setStyle(Style.EMPTY.withBold(true));
    }

    private void move(int amount) {
        selected = Math.floorMod(selected + amount, 3);
        if (client != null) {
            client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance
                    .master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void updateMouse(int mouseX, int mouseY) {
        int hover = rowAt(mouseX, mouseY);
        if (hover >= 0) {
            selected = hover;
        }
    }

    private int rowAt(double mouseX, double mouseY) {
        int left = Math.round(width * 0.06F);
        int right = Math.round(width * 0.72F);
        if (mouseX < left || mouseX > right) {
            return -1;
        }
        for (int index = 0; index < 3; index++) {
            if (mouseY >= rowY(index) - 3 && mouseY <= rowY(index) + rowHeight()) {
                return index;
            }
        }
        return -1;
    }

    private int rowY(int index) {
        int step = Math.max(29, Math.round(36.0F * P3RSettingsShell.uiScale(width, height)));
        return Math.round(height * 0.30F) + index * step;
    }

    private int rowHeight() {
        return Math.max(22, Math.round(26.0F * P3RSettingsShell.uiScale(width, height)));
    }

    private void updateAnimation() {
        long now = Util.getMeasuringTimeMs();
        float amount = P3RSettingsShell.sharpOut(MathHelper.clamp(
                (now - lastFrameAt) / 120.0F, 0.0F, 1.0F));
        lastFrameAt = now;
        for (int index = 0; index < selection.length; index++) {
            float target = index == selected ? 1.0F : 0.0F;
            selection[index] += (target - selection[index]) * amount;
        }
    }
}
