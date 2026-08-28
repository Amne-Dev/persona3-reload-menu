package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RSettingsShell;
import com.amnedev.p3rmenu.util.P3RSettingEntry;
import com.amnedev.p3rmenu.util.TransitionManager;
import com.amnedev.p3rmenu.screen.P3RMenuSettingsScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    @Shadow
    @Final
    private Screen parent;

    @Unique
    private final List<ClickableWidget> p3r_settingsItems = new ArrayList<>();
    @Unique
    private final Map<ClickableWidget, Text> p3r_settingsLabels = new HashMap<>();
    @Unique
    private final Map<ClickableWidget, Float> p3r_selectionProgress = new HashMap<>();
    @Unique
    private int p3r_selectedIndex;
    @Unique
    private long p3r_startedAt;
    @Unique
    private long p3r_lastFrameAt;
    @Unique
    private double p3r_lastMouseX = Double.NaN;
    @Unique
    private double p3r_lastMouseY = Double.NaN;
    @Unique
    private boolean p3r_mouseNavigation;
    @Unique
    private boolean p3r_draggingFov;
    @Unique
    private ClickableWidget p3r_fovWidget;

    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_initSettings(CallbackInfo ci) {
        this.p3r_fovWidget = null;
        for (Element child : this.children()) {
            if (child instanceof SliderWidget slider) {
                this.p3r_fovWidget = slider;
                break;
            }
        }
        this.addDrawableChild(ButtonWidget.builder(Text.literal("P3R Menu Settings"), button ->
                this.client.setScreen(new P3RMenuSettingsScreen(this)))
                .dimensions(0, 0, 150, 20).build());
        this.p3r_settingsItems.clear();
        this.p3r_settingsLabels.clear();
        this.p3r_selectionProgress.clear();
        p3r_syncSettingsWidgets();
        long now = Util.getMeasuringTimeMs();
        this.p3r_startedAt = now;
        this.p3r_lastFrameAt = now;
        this.p3r_mouseNavigation = false;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_renderSettings(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        ci.cancel();
        p3r_syncSettingsWidgets();
        p3r_updateAnimation();
        p3r_updateMouseSelection(mouseX, mouseY);

        float intro = P3RSettingsShell.entrance(this.p3r_startedAt);
        P3RSettingsShell.renderRootBackground(context, this.width, this.height,
                this.p3r_startedAt);
        P3RSettingsShell.renderDetailHeader(context, Text.literal("CONFIGURATION SETTINGS"),
                this.width, this.height, this.p3r_startedAt);
        List<P3RSettingEntry> entries = p3r_layoutEntries();

        for (P3RSettingEntry entry : entries) {
            float selection = this.p3r_selectionProgress.getOrDefault(entry.widget(), 0.0F);
            if (selection > 0.01F) {
                P3RSettingsShell.renderSelection(context,
                        Math.round(entry.x() - 10.0F * entry.uiScale()),
                        Math.round(entry.y() - 2.0F * entry.uiScale()),
                        Math.round(entry.x() + entry.width()),
                        Math.max(16, Math.round(entry.rowHeight())), selection);
            }
        }

        for (P3RSettingEntry entry : entries) {
            float selection = this.p3r_selectionProgress.getOrDefault(entry.widget(), 0.0F);
            if (p3r_isFov(entry.widget())) {
                p3r_renderFovSlider(context, entry, selection, intro);
                continue;
            }
            float selectedScale = 1.0F + 0.04F * P3RSettingsShell.sharpOut(selection);
            int color = selection > 0.08F
                    ? P3RSettingsShell.INK : P3RSettingsShell.CYAN;
            int alpha = MathHelper.clamp(Math.round(255.0F * intro), 0, 255);

            context.getMatrices().push();
            context.getMatrices().translate(entry.x(), entry.y(), 80.0F);
            context.getMatrices().scale(entry.textScale() * selectedScale,
                    entry.textScale() * selectedScale, 1.0F);
            int shadow = (Math.min(110, alpha) << 24) | 0x263556;
            context.drawText(this.textRenderer, entry.label(), 1, 1, shadow, false);
            context.drawText(this.textRenderer, entry.label(), 0, 0,
                    (alpha << 24) | (color & 0x00FFFFFF), false);
            context.getMatrices().pop();
        }

        Text selected = this.p3r_settingsItems.isEmpty() ? Text.empty()
                : this.p3r_settingsLabels.get(this.p3r_settingsItems.get(this.p3r_selectedIndex));
        P3RSettingsShell.renderRootFooter(context, selected,
                this.width, this.height, intro);
        TransitionManager.render(context, delta, this.width, this.height);
    }

    @Unique
    private void p3r_syncSettingsWidgets() {
        List<? extends Element> children = this.children();
        this.p3r_settingsItems.removeIf(widget -> !children.contains(widget));
        this.p3r_settingsLabels.keySet().removeIf(widget -> !children.contains(widget));
        this.p3r_selectionProgress.keySet().removeIf(widget -> !children.contains(widget));

        for (Element element : children) {
            if (!(element instanceof ClickableWidget widget)) {
                continue;
            }
            widget.visible = false;
            if (this.p3r_settingsLabels.containsKey(widget)) {
                continue;
            }
            String value = widget.getMessage().getString().strip();
            if (value.isBlank()) {
                continue;
            }
            Text label = Text.literal(value.toUpperCase(Locale.ROOT))
                    .setStyle(Style.EMPTY.withBold(true));
            this.p3r_settingsLabels.put(widget, label);
            this.p3r_settingsItems.add(widget);
        }
        this.p3r_selectedIndex = MathHelper.clamp(this.p3r_selectedIndex, 0,
                Math.max(0, this.p3r_settingsItems.size() - 1));
    }

    @Unique
    private List<P3RSettingEntry> p3r_layoutEntries() {
        List<P3RSettingEntry> result = new ArrayList<>();
        int count = this.p3r_settingsItems.size();
        if (count == 0) {
            return result;
        }

        float uiScale = P3RSettingsShell.uiScale(this.width, this.height);
        int columns = count > 7 ? 2 : 1;
        int rows = (int) Math.ceil(count / (double) columns);
        float top = Math.max(54.0F * uiScale, this.height * 0.125F);
        float bottom = this.height * 0.765F;
        float available = Math.max(1.0F, bottom - top);
        float baseStep = 32.0F * uiScale;
        float density = rows <= 1 ? 1.0F
                : MathHelper.clamp(available / ((rows - 1) * baseStep), 0.72F, 1.0F);
        float step = Math.max(19.0F, baseStep * density);
        float textScale = 1.32F * uiScale * MathHelper.clamp(density, 0.82F, 1.0F);
        float panelLeft = this.width * 0.075F;
        float panelRight = this.width * 0.72F;
        float columnGap = 12.0F * uiScale;
        float columnWidth = (panelRight - panelLeft - columnGap * (columns - 1)) / columns;
        float total = (rows - 1) * step;
        float y = MathHelper.clamp(this.height * 0.42F - total * 0.5F,
                top, Math.max(top, bottom - total));
        float rowHeight = Math.max(18.0F, 20.0F * uiScale);

        for (int i = 0; i < count; i++) {
            ClickableWidget widget = this.p3r_settingsItems.get(i);
            Text label = this.p3r_settingsLabels.get(widget);
            int column = columns == 1 ? 0 : i % columns;
            int row = columns == 1 ? i : i / columns;
            float x = panelLeft + column * (columnWidth + columnGap);
            result.add(new P3RSettingEntry(widget, label, x,
                    y + row * step, columnWidth - 6.0F * uiScale,
                    textScale, rowHeight, uiScale));
        }
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TransitionManager.isBlockingInput()) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        List<P3RSettingEntry> entries = p3r_layoutEntries();
        for (int i = 0; i < entries.size(); i++) {
            P3RSettingEntry entry = entries.get(i);
            if (entry.contains(mouseX, mouseY, this.textRenderer.getWidth(entry.label()))) {
                this.p3r_selectedIndex = i;
                this.p3r_mouseNavigation = true;
                if (p3r_isFov(entry.widget())) {
                    this.p3r_draggingFov = true;
                    p3r_setFovFromMouse(entry, mouseX);
                    return true;
                }
                p3r_activate(entry.widget());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
            double deltaX, double deltaY) {
        if (this.p3r_draggingFov && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            List<P3RSettingEntry> entries = p3r_layoutEntries();
            if (this.p3r_selectedIndex >= 0 && this.p3r_selectedIndex < entries.size()) {
                p3r_setFovFromMouse(entries.get(this.p3r_selectedIndex), mouseX);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.p3r_draggingFov && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.p3r_draggingFov = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (Double.isNaN(this.p3r_lastMouseX)
                || Math.abs(mouseX - this.p3r_lastMouseX) > 0.25D
                || Math.abs(mouseY - this.p3r_lastMouseY) > 0.25D) {
            this.p3r_mouseNavigation = true;
        }
        this.p3r_lastMouseX = mouseX;
        this.p3r_lastMouseY = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (TransitionManager.isBlockingInput()) {
            return true;
        }
        if (amount != 0.0D) {
            p3r_moveSelection(amount > 0.0D ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (TransitionManager.isBlockingInput()) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            TransitionManager.startOut(Text.literal("BACK"),
                    () -> this.client.setScreen(this.parent));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
            p3r_moveSelection(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
            p3r_moveSelection(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A
                || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            if (!this.p3r_settingsItems.isEmpty()
                    && p3r_isFov(this.p3r_settingsItems.get(this.p3r_selectedIndex))) {
                p3r_adjustFov(keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A
                        ? -1 : 1);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            p3r_select(0, true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            p3r_select(this.p3r_settingsItems.size() - 1, true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            p3r_moveSelection(hasShiftDown() ? -1 : 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (!this.p3r_settingsItems.isEmpty()) {
                p3r_activate(this.p3r_settingsItems.get(this.p3r_selectedIndex));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Unique
    private void p3r_updateAnimation() {
        long now = Util.getMeasuringTimeMs();
        float delta = MathHelper.clamp((now - this.p3r_lastFrameAt) / 120.0F,
                0.0F, 1.0F);
        this.p3r_lastFrameAt = now;
        float eased = P3RSettingsShell.sharpOut(delta);
        for (int i = 0; i < this.p3r_settingsItems.size(); i++) {
            ClickableWidget widget = this.p3r_settingsItems.get(i);
            float current = this.p3r_selectionProgress.getOrDefault(widget,
                    i == this.p3r_selectedIndex ? 1.0F : 0.0F);
            float target = i == this.p3r_selectedIndex ? 1.0F : 0.0F;
            this.p3r_selectionProgress.put(widget,
                    current + (target - current) * eased);
        }
    }

    @Unique
    private void p3r_updateMouseSelection(int mouseX, int mouseY) {
        if (!this.p3r_mouseNavigation || TransitionManager.isBlockingInput()) {
            return;
        }
        List<P3RSettingEntry> entries = p3r_layoutEntries();
        for (int i = 0; i < entries.size(); i++) {
            P3RSettingEntry entry = entries.get(i);
            if (entry.contains(mouseX, mouseY, this.textRenderer.getWidth(entry.label()))) {
                p3r_select(i, true);
                return;
            }
        }
    }

    @Unique
    private void p3r_moveSelection(int direction) {
        if (this.p3r_settingsItems.isEmpty()) {
            return;
        }
        p3r_select(Math.floorMod(this.p3r_selectedIndex + direction,
                this.p3r_settingsItems.size()), true);
    }

    @Unique
    private void p3r_select(int index, boolean sound) {
        if (this.p3r_settingsItems.isEmpty()) {
            return;
        }
        int clamped = MathHelper.clamp(index, 0, this.p3r_settingsItems.size() - 1);
        if (clamped == this.p3r_selectedIndex) {
            this.p3r_mouseNavigation = false;
            return;
        }
        this.p3r_selectedIndex = clamped;
        this.p3r_mouseNavigation = false;
        if (sound) {
            this.p3r_settingsItems.get(clamped)
                    .playDownSound(this.client.getSoundManager());
        }
    }

    @Unique
    private void p3r_activate(ClickableWidget widget) {
        if (!widget.active || TransitionManager.isTransitioning()) {
            return;
        }
        if (p3r_isFov(widget)) {
            p3r_adjustFov(1);
            return;
        }
        widget.playDownSound(this.client.getSoundManager());
        Text label = this.p3r_settingsLabels.getOrDefault(widget, widget.getMessage());
        TransitionManager.startOut(label, () -> {
            Screen origin = this.client.currentScreen;
            if (widget instanceof PressableWidget pressable) {
                pressable.onPress();
            } else {
                widget.onClick(widget.getX(), widget.getY());
            }
            if (this.client.currentScreen == origin) {
                TransitionManager.startIn();
            }
        });
    }

    @Unique
    private boolean p3r_isFov(ClickableWidget widget) {
        return widget == this.p3r_fovWidget;
    }

    @Unique
    private void p3r_renderFovSlider(DrawContext context, P3RSettingEntry entry,
            float selection, float intro) {
        float selectedScale = 1.0F + 0.04F * P3RSettingsShell.sharpOut(selection);
        int alpha = MathHelper.clamp(Math.round(255.0F * intro), 0, 255);
        int color = selection > 0.08F ? P3RSettingsShell.INK : P3RSettingsShell.CYAN;
        Text label = Text.literal("FOV").setStyle(Style.EMPTY.withBold(true));
        Text value = Text.literal(Integer.toString(this.client.options.getFov().getValue()))
                .setStyle(Style.EMPTY.withBold(true));

        context.getMatrices().push();
        context.getMatrices().translate(entry.x(), entry.y(), 82.0F);
        context.getMatrices().scale(entry.textScale() * selectedScale,
                entry.textScale() * selectedScale, 1.0F);
        context.drawText(this.textRenderer, label, 0, 0,
                (alpha << 24) | (color & 0x00FFFFFF), true);
        context.getMatrices().pop();

        int trackLeft = Math.round(entry.x() + entry.width() * 0.40F);
        int trackRight = Math.round(entry.x() + entry.width() - 10.0F * entry.uiScale());
        int trackY = Math.round(entry.y() + entry.rowHeight() * 0.45F);
        int fov = this.client.options.getFov().getValue();
        float ratio = MathHelper.clamp((fov - 30) / 80.0F, 0.0F, 1.0F);
        int knobX = Math.round(MathHelper.lerp(ratio, trackLeft, trackRight));
        context.fill(trackLeft, trackY - 2, trackRight, trackY + 2,
                selection > 0.08F ? 0xFF2A3150 : 0xFF9CF3F4);
        context.fill(trackLeft, trackY - 2, knobX, trackY + 2, P3RSettingsShell.RED);
        int knob = Math.max(4, Math.round(5.0F * entry.uiScale()));
        context.fill(knobX - knob, trackY - knob, knobX + knob, trackY + knob,
                selection > 0.08F ? P3RSettingsShell.INK : P3RSettingsShell.WHITE);
        context.drawTextWithShadow(this.textRenderer, value,
                trackRight - this.textRenderer.getWidth(value),
                trackY - Math.max(12, Math.round(14.0F * entry.uiScale())),
                (alpha << 24) | (color & 0x00FFFFFF));
    }

    @Unique
    private void p3r_setFovFromMouse(P3RSettingEntry entry, double mouseX) {
        double left = entry.x() + entry.width() * 0.40F;
        double right = entry.x() + entry.width() - 10.0F * entry.uiScale();
        double ratio = MathHelper.clamp((mouseX - left) / Math.max(1.0D, right - left),
                0.0D, 1.0D);
        this.client.options.getFov().setValue(30 + (int) Math.round(ratio * 80.0D));
    }

    @Unique
    private void p3r_adjustFov(int direction) {
        int oldValue = this.client.options.getFov().getValue();
        this.client.options.getFov().setValue(MathHelper.clamp(oldValue + direction, 30, 110));
    }

}
