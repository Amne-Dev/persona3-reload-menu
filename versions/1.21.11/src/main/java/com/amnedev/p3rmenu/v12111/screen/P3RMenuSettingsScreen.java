package com.amnedev.p3rmenu.v12111.screen;

import com.amnedev.p3rmenu.v12111.P3RConfig;
import com.amnedev.p3rmenu.v12111.P3RGraphics;
import com.amnedev.p3rmenu.v12111.Transition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

public final class P3RMenuSettingsScreen extends Screen {
    private static final int DONE = 2;
    private final Screen parent;
    private final float[] selection = {1.0F, 0.0F, 0.0F};
    private int selected;
    private long startedAt;
    private long lastFrame;

    public P3RMenuSettingsScreen(Screen parent) {
        super(Component.literal("P3R Menu Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        startedAt = Util.getMillis();
        lastFrame = startedAt;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float delta) {
        update();
        int hover = rowAt(mouseX, mouseY);
        if (hover >= 0) selected = hover;
        float intro = P3RGraphics.easeOut((Util.getMillis() - startedAt) / 380.0F);

        P3RGraphics.configBackground(graphics, width, height, intro);
        P3RGraphics.configHeader(graphics, font, "P3R MENU SETTINGS",
                width, height, intro);

        for (int index = 0; index < DONE; index++) {
            int y = rowY(index);
            int left = Math.round(width * 0.075F);
            int right = Math.round(width * 0.70F);
            int rowHeight = rowHeight();
            if (selection[index] > 0.01F) {
                P3RGraphics.configSelection(graphics, left, y, right,
                        rowHeight, selection[index] * intro);
            }
            P3RGraphics.fittedText(graphics, font, label(index), left + 8,
                    y + rowHeight * 0.52F, right - left - 16,
                    1.38F * P3RGraphics.scale(width, height),
                    index == selected ? P3RGraphics.INK : P3RGraphics.CYAN, false);
        }
        P3RGraphics.fittedText(graphics, font,
                P3RGraphics.bold("CUSTOM CHAT CHANGES THE IN-GAME CHAT PANEL AND ITS OPENING MOTION"),
                width * 0.075F, height * 0.70F, width * 0.60F,
                P3RGraphics.scale(width, height), P3RGraphics.CYAN, false);
        P3RGraphics.configFooter(graphics, font, null, width, height, intro);
        P3RGraphics.configFooterAction(graphics, font, label(DONE), width, height,
                intro, selected == DONE, true);
        Transition.extract(graphics, width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (Transition.blocksScreenInput()) return true;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int index = rowAt(event.x(), event.y());
            if (index >= 0) {
                selected = index;
                activate();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Transition.blocksScreenInput()) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeAnimated();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_W) {
            selected = Math.floorMod(selected - 1, 3);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DOWN || event.key() == GLFW.GLFW_KEY_S) {
            selected = Math.floorMod(selected + 1, 3);
            return true;
        }
        if (event.isConfirmation()) {
            activate();
            return true;
        }
        if ((event.isLeft() || event.isRight()) && selected == 1) {
            P3RConfig.toggleCustomChat();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        closeAnimated();
    }

    private void activate() {
        if (selected == 0) {
            Transition.startOut(Component.literal("WALLPAPER"),
                    () -> minecraft.setScreen(new WallpaperScreen(this)));
        } else if (selected == 1) {
            P3RConfig.toggleCustomChat();
        } else {
            closeAnimated();
        }
    }

    private void closeAnimated() {
        if (!Transition.isActive()) {
            Transition.startOut(Component.literal("BACK"),
                    () -> minecraft.setScreen(parent));
        }
    }

    private Component label(int index) {
        return P3RGraphics.bold(switch (index) {
            case 0 -> "WALLPAPER...";
            case 1 -> "CUSTOM CHAT: " + (P3RConfig.customChat() ? "ON" : "OFF");
            default -> "DONE";
        });
    }

    private int rowY(int index) {
        int step = Math.max(29, Math.round(36.0F * P3RGraphics.scale(width, height)));
        return Math.round(height * 0.30F) + index * step;
    }

    private int rowAt(double x, double y) {
        if (P3RGraphics.footerActionContains(x, y, width, height)) return DONE;
        if (x < width * 0.06F || x > width * 0.72F) return -1;
        for (int index = 0; index < DONE; index++) {
            if (y >= rowY(index) - 3 && y <= rowY(index) + rowHeight()) return index;
        }
        return -1;
    }

    private int rowHeight() {
        return Math.max(22, Math.round(26.0F * P3RGraphics.scale(width, height)));
    }

    private void update() {
        long now = Util.getMillis();
        float elapsed = Math.min(0.05F, Math.max(0.0F, (now - lastFrame) / 1000.0F));
        lastFrame = now;
        for (int index = 0; index < selection.length; index++) {
            float target = index == selected ? 1.0F : 0.0F;
            selection[index] = target + (selection[index] - target)
                    * (float) Math.exp(-16.0F * elapsed);
        }
    }
}
