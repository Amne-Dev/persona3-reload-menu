package com.amnedev.p3rmenu.screen;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import com.amnedev.p3rmenu.util.TransitionManager;
import com.amnedev.p3rmenu.util.WallpaperManager;
import com.amnedev.p3rmenu.util.WallpaperManager.WallpaperOption;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Persona-styled, keyboard-accessible selector for user supplied wallpapers. */
public final class WallpaperScreen extends Screen {
    private final Screen parent;
    private final List<WallpaperOption> wallpapers = new ArrayList<>();
    private long startedAt;
    private int selectedIndex;
    private int scrollOffset;
    private String status = "";

    public WallpaperScreen(Screen parent) {
        super(Text.literal("Wallpaper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.startedAt = Util.getMeasuringTimeMs();
        reloadWallpapers();
        addDrawableChild(ButtonWidget.builder(Text.literal("OPEN FOLDER"), button -> {
            Util.getOperatingSystem().open(WallpaperManager.getWallpaperDirectory().toFile());
            this.status = "ADD PNG, JPG, JPEG, OR WEBP FILES, THEN REFRESH";
        }).dimensions(0, 0, 120, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("REFRESH"), button -> {
            WallpaperManager.reloadSelection();
            reloadWallpapers();
            this.status = "WALLPAPER LIST REFRESHED";
        }).dimensions(0, 0, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE,
                button -> this.client.setScreen(this.parent))
                .dimensions(0, 0, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        P3RSettingsShell.layoutFooterButtons(this);
        P3RSettingsShell.renderDetailBackground(context, this.width, this.height, this.startedAt);
        P3RSettingsShell.renderDetailHeader(context, Text.literal("THEME / WALLPAPER"),
                this.width, this.height, this.startedAt);

        float intro = P3RSettingsShell.entrance(this.startedAt);
        int visibleRows = visibleRows();
        int end = Math.min(this.wallpapers.size(), this.scrollOffset + visibleRows);
        for (int index = this.scrollOffset; index < end; index++) {
            renderWallpaperRow(context, index, rowY(index - this.scrollOffset), intro);
        }

        Text help = Text.literal("DROP PNG / JPG / JPEG / WEBP FILES INTO CONFIG/P3RMENU/WALLPAPERS")
                .setStyle(Style.EMPTY.withBold(true));
        P3RSettingsShell.drawFittedText(context, help,
                this.width * 0.075F, this.height * 0.755F,
                this.width * 0.55F, P3RSettingsShell.CYAN, false);
        if (!this.status.isBlank()) {
            Text feedback = Text.literal(this.status).setStyle(Style.EMPTY.withBold(true));
            P3RSettingsShell.drawFittedText(context, feedback,
                    this.width * 0.075F, this.height * 0.79F,
                    this.width * 0.55F, P3RSettingsShell.INK, false);
        }

        P3RSettingsShell.renderDetailFooter(context, this.width, this.height, intro);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderWallpaperRow(DrawContext context, int index, int y, float intro) {
        WallpaperOption option = this.wallpapers.get(index);
        boolean selected = index == this.selectedIndex;
        int left = Math.round(this.width * 0.075F);
        int right = Math.round(this.width * 0.625F);
        int height = Math.max(20, Math.round(25.0F * P3RSettingsShell.uiScale(width, this.height)));
        if (selected) {
            P3RSettingsShell.renderSelection(context, left, y, right, height, intro);
        } else {
            context.fill(left, y, right, y + height, 0x20556A93);
        }
        Text label = Text.literal(option.name().toUpperCase())
                .setStyle(Style.EMPTY.withBold(true));
        P3RSettingsShell.drawFittedText(context, label,
                left + 8, y + height * 0.5F, right - left - 16,
                selected ? P3RSettingsShell.INK : P3RSettingsShell.CYAN, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TransitionManager.isBlockingInput()) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int left = Math.round(this.width * 0.075F);
            int right = Math.round(this.width * 0.625F);
            int rowHeight = rowStep();
            int firstY = rowY(0);
            if (mouseX >= left && mouseX <= right && mouseY >= firstY) {
                int visibleIndex = (int) ((mouseY - firstY) / rowHeight);
                int index = this.scrollOffset + visibleIndex;
                if (visibleIndex >= 0 && visibleIndex < visibleRows()
                        && index < this.wallpapers.size()) {
                    selectAndApply(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount != 0.0D) {
            moveSelection(amount > 0.0D ? -1 : 1, false);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
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
            moveSelection(-1, false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
            moveSelection(1, false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            selectAndApply(this.selectedIndex);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (!TransitionManager.isTransitioning()) {
            TransitionManager.startOut(Text.literal("BACK"), () -> this.client.setScreen(this.parent));
        }
    }

    public boolean shouldAnimateTransition(ClickableWidget widget) {
        return widget.getMessage().getString().equals(ScreenTexts.DONE.getString());
    }

    private void reloadWallpapers() {
        String selectedName = WallpaperManager.getSelectedName();
        this.wallpapers.clear();
        this.wallpapers.addAll(WallpaperManager.discover());
        this.selectedIndex = 0;
        for (int i = 0; i < this.wallpapers.size(); i++) {
            if (this.wallpapers.get(i).name().equals(selectedName)) {
                this.selectedIndex = i;
                break;
            }
        }
        this.scrollOffset = MathHelper.clamp(this.scrollOffset, 0,
                Math.max(0, this.wallpapers.size() - visibleRows()));
        String managerStatus = WallpaperManager.consumeStatus();
        if (!managerStatus.isBlank()) {
            this.status = managerStatus;
        }
        ensureVisible();
    }

    private void selectAndApply(int index) {
        if (this.wallpapers.isEmpty()) {
            return;
        }
        this.selectedIndex = MathHelper.clamp(index, 0, this.wallpapers.size() - 1);
        WallpaperManager.select(this.wallpapers.get(this.selectedIndex));
        String managerStatus = WallpaperManager.consumeStatus();
        this.status = managerStatus.isBlank() ? "WALLPAPER APPLIED" : managerStatus;
        reloadWallpapers();
    }

    private void moveSelection(int direction, boolean apply) {
        if (this.wallpapers.isEmpty()) {
            return;
        }
        this.selectedIndex = Math.floorMod(this.selectedIndex + direction, this.wallpapers.size());
        ensureVisible();
        if (apply) {
            selectAndApply(this.selectedIndex);
        }
    }

    private void ensureVisible() {
        if (this.selectedIndex < this.scrollOffset) {
            this.scrollOffset = this.selectedIndex;
        } else if (this.selectedIndex >= this.scrollOffset + visibleRows()) {
            this.scrollOffset = this.selectedIndex - visibleRows() + 1;
        }
    }

    private int visibleRows() {
        int available = Math.max(1, Math.round(this.height * 0.70F) - rowY(0));
        return Math.max(1, available / rowStep());
    }

    private int rowY(int visibleIndex) {
        return Math.max(48, Math.round(this.height * 0.14F)) + visibleIndex * rowStep();
    }

    private int rowStep() {
        return Math.max(23, Math.round(29.0F * P3RSettingsShell.uiScale(this.width, this.height)));
    }
}
