package com.amnedev.p3rmenu.v262.mixin;

import com.amnedev.p3rmenu.v262.P3RGraphics;
import com.amnedev.p3rmenu.v262.Transition;
import com.amnedev.p3rmenu.v262.screen.P3RMenuSettingsScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    @Shadow @Final private Options options;
    @Unique private final List<AbstractWidget> p3r_items = new ArrayList<>();
    @Unique private final Map<AbstractWidget, Component> p3r_labels = new HashMap<>();
    @Unique private final Map<AbstractWidget, Float> p3r_selection = new HashMap<>();
    @Unique private int p3r_selected;
    @Unique private long p3r_startedAt;
    @Unique private long p3r_lastFrame;
    @Unique private boolean p3r_mouseNavigation;
    @Unique private boolean p3r_draggingFov;
    @Unique private AbstractWidget p3r_fovWidget;

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_init(CallbackInfo ci) {
        p3r_fovWidget = null;
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractSliderButton slider) {
                p3r_fovWidget = slider;
                break;
            }
        }
        addRenderableWidget(Button.builder(Component.literal("P3R Menu Settings"),
                button -> minecraft.gui.setScreen(new P3RMenuSettingsScreen(this))).bounds(0, 0, 150, 20).build());
        p3r_items.clear();
        p3r_labels.clear();
        p3r_selection.clear();
        p3r_sync();
        p3r_startedAt = Util.getMillis();
        p3r_lastFrame = p3r_startedAt;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float delta) {
        p3r_sync();
        p3r_update();
        p3r_updateMouse(mouseX, mouseY);
        List<Entry> entries = p3r_entries();
        float intro = P3RGraphics.easeOut((Util.getMillis() - p3r_startedAt) / 430.0F);
        P3RGraphics.configBackground(graphics, width, height, intro);
        P3RGraphics.configHeader(graphics, font, "CONFIGURATION SETTINGS",
                width, height, intro);
        for (Entry entry : entries) {
            float selection = p3r_selection.getOrDefault(entry.button(), 0.0F);
            if (selection > 0.01F) {
                P3RGraphics.configSelection(graphics,
                        Math.round(entry.x() - 10.0F * entry.ui()),
                        Math.round(entry.y() - 2.0F * entry.ui()),
                        Math.round(entry.x() + entry.width()), entry.rowHeight(), selection);
            }
        }
        for (Entry entry : entries) {
            float selection = p3r_selection.getOrDefault(entry.button(), 0.0F);
            if (p3r_isFov(entry.button())) {
                p3r_drawFov(graphics, entry, selection, intro);
                continue;
            }
            float selectedScale = 1.0F + 0.04F * P3RGraphics.sharpOut(selection);
            Component label = p3r_labels.get(entry.button());
            float fittedScale = Math.min(entry.textScale(),
                    entry.width() / Math.max(1.0F, font.width(label)));
            graphics.pose().pushMatrix();
            graphics.pose().translate(entry.x(), entry.y());
            graphics.pose().scale(fittedScale * selectedScale,
                    fittedScale * selectedScale);
            graphics.text(font, label, 1, 1, P3RGraphics.alpha(0xFF263556, intro * 0.55F), false);
            graphics.text(font, label, 0, 0,
                    P3RGraphics.alpha(selection > 0.08F ? P3RGraphics.CONFIG_INK : P3RGraphics.CYAN, intro), false);
            graphics.pose().popMatrix();
        }

        Component current = p3r_items.isEmpty() ? Component.empty() : p3r_labels.get(p3r_items.get(p3r_selected));
        P3RGraphics.configFooter(graphics, font, current, width, height, intro);
        Transition.extract(graphics, width, height);
    }

    @Unique
    private void p3r_sync() {
        List<? extends GuiEventListener> children = children();
        p3r_items.removeIf(button -> !children.contains(button));
        p3r_labels.keySet().removeIf(button -> !children.contains(button));
        p3r_selection.keySet().removeIf(button -> !children.contains(button));
        for (GuiEventListener child : children) {
            if (!(child instanceof AbstractWidget button)) {
                continue;
            }
            button.visible = false;
            if (!p3r_labels.containsKey(button) && !button.getMessage().getString().isBlank()) {
                p3r_labels.put(button, P3RGraphics.bold(button.getMessage().getString().strip()));
                // Includes buttons added by other mods without needing a fixed registry.
                p3r_items.add(button);
            }
        }
        p3r_selected = Mth.clamp(p3r_selected, 0, Math.max(0, p3r_items.size() - 1));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (Transition.blocksScreenInput()) {
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        List<Entry> entries = p3r_entries();
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).contains(event.x(), event.y())) {
                p3r_selected = index;
                if (p3r_isFov(entries.get(index).button())) {
                    p3r_draggingFov = true;
                    p3r_setFov(entries.get(index), event.x());
                    return true;
                }
                p3r_activate(entries.get(index).button(), event);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (p3r_draggingFov && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            List<Entry> entries = p3r_entries();
            if (p3r_selected >= 0 && p3r_selected < entries.size()) {
                p3r_setFov(entries.get(p3r_selected), event.x());
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (p3r_draggingFov && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            p3r_draggingFov = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        p3r_mouseNavigation = true;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D) {
            p3r_move(scrollY > 0.0D ? -1 : 1);
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
            Transition.startOut(Component.literal("BACK"), this::onClose);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_W) {
            p3r_move(-1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DOWN || event.key() == GLFW.GLFW_KEY_S) {
            p3r_move(1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_A) {
            if (!p3r_items.isEmpty() && p3r_isFov(p3r_items.get(p3r_selected))) {
                p3r_adjustFov(-1);
            } else {
                p3r_move(-1);
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_D) {
            if (!p3r_items.isEmpty() && p3r_isFov(p3r_items.get(p3r_selected))) {
                p3r_adjustFov(1);
            } else {
                p3r_move(1);
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER
                || event.key() == GLFW.GLFW_KEY_SPACE) {
            if (!p3r_items.isEmpty()) {
                p3r_activate(p3r_items.get(p3r_selected), event);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Unique
    private void p3r_activate(AbstractWidget button, net.minecraft.client.input.InputWithModifiers input) {
        if (!button.active) {
            return;
        }
        if (p3r_isFov(button)) {
            p3r_adjustFov(1);
            return;
        }
        button.playDownSound(minecraft.getSoundManager());
        if (button instanceof AbstractButton pressable) {
            Transition.startOut(p3r_labels.get(button), () -> pressable.onPress(input));
        }
    }

    @Unique
    private void p3r_move(int amount) {
        if (p3r_items.isEmpty()) {
            return;
        }
        p3r_selected = Math.floorMod(p3r_selected + amount, p3r_items.size());
        p3r_items.get(p3r_selected).playDownSound(minecraft.getSoundManager());
        p3r_mouseNavigation = false;
    }

    @Unique
    private void p3r_update() {
        long now = Util.getMillis();
        float seconds = Math.min(0.05F, Math.max(0.0F, (now - p3r_lastFrame) / 1000.0F));
        p3r_lastFrame = now;
        for (int index = 0; index < p3r_items.size(); index++) {
            AbstractWidget button = p3r_items.get(index);
            float target = index == p3r_selected ? 1.0F : 0.0F;
            float current = p3r_selection.getOrDefault(button, target);
            p3r_selection.put(button, target + (current - target)
                    * (float) Math.exp(-(target > current ? 20.0F : 13.0F) * seconds));
        }
    }

    @Unique
    private void p3r_updateMouse(int mouseX, int mouseY) {
        if (!p3r_mouseNavigation || Transition.blocksScreenInput()) {
            return;
        }
        List<Entry> entries = p3r_entries();
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).contains(mouseX, mouseY) && index != p3r_selected) {
                p3r_selected = index;
                p3r_items.get(index).playDownSound(minecraft.getSoundManager());
                return;
            }
        }
    }

    @Unique
    private List<Entry> p3r_entries() {
        List<Entry> result = new ArrayList<>();
        int count = p3r_items.size();
        if (count == 0) {
            return result;
        }
        float ui = P3RGraphics.scale(width, height);
        int columns = count > 7 ? 2 : 1;
        int rows = (int) Math.ceil(count / (double) columns);
        float top = Math.max(54.0F * ui, height * 0.125F);
        float bottom = height * 0.765F;
        float baseStep = 32.0F * ui;
        float density = rows <= 1 ? 1.0F : Mth.clamp((bottom - top) / ((rows - 1) * baseStep), 0.72F, 1.0F);
        float step = Math.max(19.0F, baseStep * density);
        float total = (rows - 1) * step;
        float startY = Mth.clamp(height * 0.42F - total * 0.5F, top, Math.max(top, bottom - total));
        float panelLeft = width * 0.075F;
        float panelRight = width * 0.72F;
        float gap = 12.0F * ui;
        float columnWidth = (panelRight - panelLeft - gap * (columns - 1)) / columns;
        float textScale = 1.32F * ui * Mth.clamp(density, 0.82F, 1.0F);
        int rowHeight = Math.max(18, Math.round(20.0F * ui));
        for (int index = 0; index < count; index++) {
            int column = columns == 1 ? 0 : index % columns;
            int row = columns == 1 ? index : index / columns;
            result.add(new Entry(p3r_items.get(index), panelLeft + column * (columnWidth + gap),
                    startY + row * step, columnWidth - 6.0F * ui,
                    textScale, step, rowHeight, ui));
        }
        return result;
    }

    @Unique
    private record Entry(AbstractWidget button, float x, float y, float width,
            float textScale, float step, int rowHeight, float ui) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x - 20.0F * ui && mouseX <= x + width
                    && mouseY >= y - step * 0.15F && mouseY <= y + step * 0.82F;
        }
    }

    @Unique
    private boolean p3r_isFov(AbstractWidget button) {
        return button == p3r_fovWidget;
    }

    @Unique
    private void p3r_drawFov(GuiGraphicsExtractor graphics, Entry entry,
            float selection, float intro) {
        int color = selection > 0.08F ? P3RGraphics.INK : P3RGraphics.CYAN;
        P3RGraphics.fittedText(graphics, font, P3RGraphics.bold("FOV"),
                entry.x(), entry.y() + 5.0F * entry.ui(), entry.width() * 0.32F,
                entry.textScale(), P3RGraphics.alpha(color, intro), false);
        int left = Math.round(entry.x() + entry.width() * 0.38F);
        int right = Math.round(entry.x() + entry.width() - 9.0F * entry.ui());
        int y = Math.round(entry.y() + entry.step() * 0.38F);
        int value = options.fov().get();
        float ratio = Mth.clamp((value - 30) / 80.0F, 0.0F, 1.0F);
        int knobX = Math.round(Mth.lerp(ratio, left, right));
        graphics.fill(left, y - 2, right, y + 2,
                selection > 0.08F ? 0xFF2A3150 : P3RGraphics.CYAN);
        graphics.fill(left, y - 2, knobX, y + 2, P3RGraphics.RED);
        int knob = Math.max(4, Math.round(5.0F * entry.ui()));
        graphics.fill(knobX - knob, y - knob, knobX + knob, y + knob,
                selection > 0.08F ? P3RGraphics.INK : P3RGraphics.WHITE);
        P3RGraphics.fittedText(graphics, font, P3RGraphics.bold(Integer.toString(value)),
                right - 30.0F * entry.ui(), y - 11.0F * entry.ui(),
                30.0F * entry.ui(), 0.92F * entry.ui(),
                P3RGraphics.alpha(color, intro), false);
    }

    @Unique
    private void p3r_setFov(Entry entry, double mouseX) {
        double left = entry.x() + entry.width() * 0.38F;
        double right = entry.x() + entry.width() - 9.0F * entry.ui();
        double ratio = Mth.clamp((mouseX - left) / Math.max(1.0D, right - left), 0.0D, 1.0D);
        options.fov().set(30 + (int) Math.round(ratio * 80.0D));
    }

    @Unique
    private void p3r_adjustFov(int direction) {
        options.fov().set(Mth.clamp(options.fov().get() + direction, 30, 110));
    }
}
