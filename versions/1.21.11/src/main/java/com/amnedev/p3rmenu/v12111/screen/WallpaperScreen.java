package com.amnedev.p3rmenu.v12111.screen;

import com.amnedev.p3rmenu.v12111.P3RGraphics;
import com.amnedev.p3rmenu.v12111.Transition;
import com.amnedev.p3rmenu.v12111.WallpaperManager;
import com.amnedev.p3rmenu.v12111.WallpaperManager.Option;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Persona-styled, mouse/keyboard-accessible picker for user-provided image files. */
public final class WallpaperScreen extends Screen {
    private final Screen parent;
    private final List<Option> wallpapers = new ArrayList<>();
    private int selectedIndex;
    private int scrollOffset;
    private String status = "";
    private long startedAt;

    public WallpaperScreen(Screen parent) {
        super(Component.literal("Wallpaper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        startedAt = Util.getMillis();
        reload();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        float intro = P3RGraphics.easeOut((Util.getMillis() - startedAt) / 420.0F);
        P3RGraphics.configBackground(graphics, width, height, intro);
        P3RGraphics.configHeader(graphics, font, "THEME / WALLPAPER",
                width, height, intro);
        int rows = visibleRows();
        int end = Math.min(wallpapers.size(), scrollOffset + rows);
        for (int index = scrollOffset; index < end; index++) {
            drawRow(graphics, index, rowY(index - scrollOffset), intro);
        }

        P3RGraphics.fittedText(graphics, font,
                P3RGraphics.bold("DROP PNG / JPG / JPEG / WEBP FILES INTO CONFIG/P3RMENU/WALLPAPERS"),
                width * 0.07F, height * 0.79F, width * 0.58F,
                1.0F * P3RGraphics.scale(width, height), P3RGraphics.CYAN, false);
        if (!status.isBlank()) {
            P3RGraphics.fittedText(graphics, font, P3RGraphics.bold(status),
                    width * 0.07F, height * 0.83F, width * 0.58F,
                    0.92F * P3RGraphics.scale(width, height), P3RGraphics.WHITE, false);
        }

        int footerY = Math.round(height * 0.88F);
        drawFooter(graphics, "OPEN FOLDER", footerX(0), footerY, mouseX, mouseY);
        drawFooter(graphics, "REFRESH", footerX(1), footerY, mouseX, mouseY);
        drawFooter(graphics, "DONE", footerX(2), footerY, mouseX, mouseY);
        Transition.extract(graphics, width, height);
    }

    private void drawRow(GuiGraphics graphics, int index, int y, float intro) {
        boolean selected = index == selectedIndex;
        int left = Math.round(width * 0.07F - (1.0F - intro) * 42.0F);
        int right = Math.round(width * 0.65F);
        int rowHeight = Math.max(22, Math.round(29.0F * P3RGraphics.scale(width, height)));
        if (selected) {
            P3RGraphics.skewedRect(graphics, left - 34, y, right - left + 46,
                    rowHeight, 24, P3RGraphics.WHITE);
            P3RGraphics.skewedRect(graphics, left - 46, y + 2, 18,
                    rowHeight - 4, 8, P3RGraphics.PINK);
        } else {
            graphics.fill(left, y, right, y + rowHeight, 0x40556A93);
        }
        P3RGraphics.fittedText(graphics, font,
                P3RGraphics.bold(wallpapers.get(index).name()),
                left + 10, y + rowHeight * 0.52F, right - left - 20,
                1.2F * P3RGraphics.scale(width, height),
                selected ? P3RGraphics.INK : P3RGraphics.PALE, false);
    }

    private void drawFooter(GuiGraphics graphics, String value, int x, int y,
            int mouseX, int mouseY) {
        int width = footerWidth();
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y - 8 && mouseY <= y + 18;
        if (hovered) {
            P3RGraphics.skewedRect(graphics, x - 8, y - 6, width + 16, 22, 8, P3RGraphics.CYAN);
        }
        P3RGraphics.fittedText(graphics, font, P3RGraphics.bold(value), x, y + 3,
                width, 1.0F * P3RGraphics.scale(this.width, height),
                hovered ? P3RGraphics.INK : P3RGraphics.CYAN, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (Transition.blocksScreenInput()) {
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        int firstY = rowY(0);
        if (event.x() >= width * 0.055F && event.x() <= width * 0.67F && event.y() >= firstY) {
            int visible = (int) ((event.y() - firstY) / rowStep());
            int index = scrollOffset + visible;
            if (visible >= 0 && visible < visibleRows() && index < wallpapers.size()) {
                apply(index);
                return true;
            }
        }
        int footerY = Math.round(height * 0.88F);
        if (event.y() >= footerY - 8 && event.y() <= footerY + 18) {
            if (insideFooter(event.x(), 0)) {
                Util.getPlatform().openPath(WallpaperManager.directory());
                status = "ADD PNG, JPG, JPEG, OR WEBP FILES, THEN REFRESH";
                return true;
            }
            if (insideFooter(event.x(), 1)) {
                WallpaperManager.reloadSelection();
                reload();
                status = "WALLPAPER LIST REFRESHED";
                return true;
            }
            if (insideFooter(event.x(), 2)) {
                closeWithTransition();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D) {
            move(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Transition.blocksScreenInput()) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeWithTransition();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_W) {
            move(-1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DOWN || event.key() == GLFW.GLFW_KEY_S) {
            move(1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER
                || event.key() == GLFW.GLFW_KEY_SPACE) {
            apply(selectedIndex);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        closeWithTransition();
    }

    private void closeWithTransition() {
        if (!Transition.isActive()) {
            Transition.startOut(Component.literal("BACK"), () -> minecraft.setScreen(parent));
        }
    }

    private void reload() {
        String current = WallpaperManager.selectedName();
        wallpapers.clear();
        wallpapers.addAll(WallpaperManager.discover());
        selectedIndex = 0;
        for (int index = 0; index < wallpapers.size(); index++) {
            if (wallpapers.get(index).name().equals(current)) {
                selectedIndex = index;
                break;
            }
        }
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, wallpapers.size() - visibleRows()));
        String managerStatus = WallpaperManager.consumeStatus();
        if (!managerStatus.isBlank()) {
            status = managerStatus;
        }
        ensureVisible();
    }

    private void apply(int index) {
        if (wallpapers.isEmpty()) {
            return;
        }
        selectedIndex = Mth.clamp(index, 0, wallpapers.size() - 1);
        WallpaperManager.select(wallpapers.get(selectedIndex));
        status = WallpaperManager.consumeStatus();
        reload();
    }

    private void move(int direction) {
        if (wallpapers.isEmpty()) {
            return;
        }
        selectedIndex = Math.floorMod(selectedIndex + direction, wallpapers.size());
        ensureVisible();
    }

    private void ensureVisible() {
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows()) {
            scrollOffset = selectedIndex - visibleRows() + 1;
        }
    }

    private int visibleRows() {
        int available = Math.max(1, Math.round(height * 0.74F) - rowY(0));
        return Math.max(1, available / rowStep());
    }

    private int rowY(int visibleIndex) {
        return Math.max(48, Math.round(height * 0.14F)) + visibleIndex * rowStep();
    }

    private int rowStep() {
        return Math.max(25, Math.round(33.0F * P3RGraphics.scale(width, height)));
    }

    private int footerWidth() {
        return Math.max(72, Math.round(112.0F * P3RGraphics.scale(width, height)));
    }

    private int footerX(int index) {
        int gap = Math.max(10, Math.round(16.0F * P3RGraphics.scale(width, height)));
        int total = footerWidth() * 3 + gap * 2;
        return width - Math.max(12, Math.round(20.0F * P3RGraphics.scale(width, height)))
                - total + index * (footerWidth() + gap);
    }

    private boolean insideFooter(double mouseX, int index) {
        return mouseX >= footerX(index) && mouseX <= footerX(index) + footerWidth();
    }
}
