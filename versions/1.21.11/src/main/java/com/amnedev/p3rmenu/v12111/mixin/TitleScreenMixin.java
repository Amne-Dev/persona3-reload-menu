package com.amnedev.p3rmenu.v12111.mixin;

import com.amnedev.p3rmenu.v12111.P3RGraphics;
import com.amnedev.p3rmenu.v12111.Transition;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique private static final String P3R_ESSENTIAL_PROXY_CLASS =
            "gg.essential.gui.proxies.EssentialProxyElement";
    @Unique private static boolean p3r_checkedEssentialProxyClass;
    @Unique private static Class<?> p3r_essentialProxyClass;
    @Unique private final List<AbstractButton> p3r_main = new ArrayList<>();
    @Unique private final List<AbstractButton> p3r_footer = new ArrayList<>();
    @Unique private final List<AbstractButton> p3r_passthrough = new ArrayList<>();
    @Unique private final Map<AbstractButton, String> p3r_essentialActions = new HashMap<>();
    @Unique private AbstractButton p3r_essentialPlayer;
    @Unique private PlayerSkinWidget p3r_skinPreview;
    @Unique private final Map<AbstractButton, Component> p3r_labels = new HashMap<>();
    @Unique private final Map<AbstractButton, Float> p3r_selection = new HashMap<>();
    @Unique private int p3r_selected;
    @Unique private long p3r_startedAt;
    @Unique private long p3r_lastFrame;
    @Unique private boolean p3r_mouseNavigation;
    @Unique private double p3r_lastMouseX = Double.NaN;
    @Unique private double p3r_lastMouseY = Double.NaN;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_init(CallbackInfo ci) {
        p3r_main.clear();
        p3r_footer.clear();
        p3r_passthrough.clear();
        p3r_essentialActions.clear();
        p3r_essentialPlayer = null;
        p3r_skinPreview = null;
        p3r_labels.clear();
        p3r_selection.clear();
        p3r_syncButtons();
        p3r_startedAt = Util.getMillis();
        p3r_lastFrame = p3r_startedAt;
        p3r_mouseNavigation = false;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_extract(GuiGraphics graphics, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        ci.cancel();
        p3r_syncButtons();
        p3r_prepareEssentialActions(graphics, mouseX, mouseY, delta);
        p3r_updateSelection();
        p3r_updateMouse(mouseX, mouseY);
        P3RGraphics.wallpaper(graphics, width, height);
        graphics.fill(0, 0, width, height, 0x18020A2B);
        p3r_drawEssentialPlayer(graphics, mouseX, mouseY, delta);
        float logoIntro = P3RGraphics.easeOut((Util.getMillis() - p3r_startedAt) / 620.0F);
        P3RGraphics.logo(graphics, width, height, logoIntro);
        p3r_drawMenu(graphics);
        p3r_drawFooter(graphics, mouseX, mouseY);
        Transition.extract(graphics, width, height);
    }

    @Unique
    private void p3r_syncButtons() {
        List<? extends GuiEventListener> children = children();
        p3r_main.removeIf(button -> !children.contains(button));
        p3r_footer.removeIf(button -> !children.contains(button));
        p3r_passthrough.removeIf(button -> !children.contains(button));
        p3r_essentialActions.keySet().removeIf(button -> !children.contains(button));
        if (p3r_essentialPlayer != null && !children.contains(p3r_essentialPlayer)) {
            p3r_essentialPlayer = null;
        }
        p3r_labels.keySet().removeIf(button -> !children.contains(button));
        p3r_selection.keySet().removeIf(button -> !children.contains(button));
        for (GuiEventListener child : children) {
            if (!(child instanceof AbstractButton button)) {
                continue;
            }
            if (p3r_labels.containsKey(button)) {
                button.visible = p3r_essentialActions.containsKey(button);
                continue;
            }
            if (p3r_isExternallyManagedWidget(button)) {
                String id = p3r_essentialId(button);
                if ("player".equals(id)) {
                    p3r_essentialPlayer = button;
                    button.visible = true;
                    continue;
                }
                if (!button.visible) {
                    if (!p3r_passthrough.contains(button)) p3r_passthrough.add(button);
                    continue;
                }
                String action = p3r_essentialAction(id);
                String label = p3r_essentialLabel(action);
                if (action != null && label != null
                        && !p3r_essentialActions.containsValue(action)) {
                    p3r_essentialActions.put(button, action);
                    p3r_labels.put(button, P3RGraphics.bold(label));
                    if (p3r_isEssentialFooterAction(action)) {
                        p3r_footer.add(button);
                    } else {
                        p3r_main.add(button);
                    }
                    button.visible = true;
                } else if (!p3r_passthrough.contains(button)) {
                    p3r_passthrough.add(button);
                }
                continue;
            }
            if (!button.visible) {
                continue;
            }
            String raw = button.getMessage().getString().strip();
            if (raw.isBlank() || p3r_hidden(raw)) {
                if (p3r_hidden(raw)) button.visible = false;
                continue;
            }
            p3r_labels.put(button, P3RGraphics.bold(raw));
            button.visible = false;
            if (p3r_isFooter(raw)) {
                p3r_footer.add(button);
            } else {
                // Unknown buttons injected by other mods remain first-class P3R entries.
                p3r_main.add(button);
            }
        }
        if (p3r_essentialPlayer != null && p3r_skinPreview == null) {
            p3r_skinPreview = new PlayerSkinWidget(1, 1, minecraft.getEntityModels(),
                    minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false));
            addRenderableWidget(p3r_skinPreview);
        }
        p3r_selected = Mth.clamp(p3r_selected, 0, Math.max(0, p3r_main.size() - 1));
    }

    @Unique
    private void p3r_drawMenu(GuiGraphics graphics) {
        List<Row> rows = p3r_rows();

        // Match the established title menu: selection planes are a dedicated
        // background layer, reveal from the display edge, and never cover text.
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            float selected = p3r_selection.getOrDefault(row.button(), 0.0F);
            float intro = P3RGraphics.easeOut(
                    (Util.getMillis() - p3r_startedAt - 170L - index * 58L) / 470.0F);
            if (intro <= 0.001F || selected <= 0.01F) continue;

            float selectedEase = p3r_easeOutQuint(selected);
            float slide = (1.0F - intro) * 68.0F * row.uiScale();
            float drawX = row.textX() + slide;
            float barRight = width + 4.0F;
            float barLeft = Math.min(width * 0.70F, drawX - 10.0F * row.uiScale());
            float fullWidth = Math.max(1.0F, barRight - barLeft);
            int barX = Math.round(barRight - fullWidth * selectedEase);
            int barY = Math.round(row.y() - row.rowHeight() * 0.12F);
            int barHeight = Math.max(12, Math.round(row.rowHeight()));
            graphics.fill(barX, barY, Math.round(barRight), barY + barHeight,
                    P3RGraphics.alpha(P3RGraphics.BLUE, intro * selected));
        }

        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            float selected = p3r_selection.getOrDefault(row.button(), 0.0F);
            float intro = P3RGraphics.easeOut(
                    (Util.getMillis() - p3r_startedAt - 170L - index * 58L) / 470.0F);
            if (intro <= 0.001F) continue;

            float slide = (1.0F - intro) * 68.0F * row.uiScale();
            float x = row.textX() + slide;
            int alpha = Mth.clamp(Math.round(255.0F * intro), 0, 255);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, row.y());
            graphics.pose().scale(row.textScale(), row.textScale());
            Component label = p3r_labels.get(row.button());
            graphics.drawString(font, label, 2, 2,
                    P3RGraphics.alpha(0xFF52596A, intro * 0.42F), false);
            graphics.drawString(font, label, 1, 1,
                    P3RGraphics.alpha(0xFF52596A, intro * 0.78F), false);
            graphics.drawString(font, label, 0, 0,
                    (alpha << 24) | ((selected > 0.12F
                            ? P3RGraphics.WHITE : P3RGraphics.PALE) & 0xFFFFFF), false);
            graphics.pose().popMatrix();
        }
    }

    @Unique
    private void p3r_drawFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        float intro = P3RGraphics.easeOut((Util.getMillis() - p3r_startedAt - 440L) / 420.0F);
        if (intro <= 0.001F) return;

        for (FooterRow row : p3r_footerRows()) {
            boolean hovered = row.contains(mouseX, mouseY);
            graphics.pose().pushMatrix();
            graphics.pose().translate(row.x(), row.y());
            graphics.pose().scale(row.scale(), row.scale());
            P3RGraphics.skewedRect(graphics, 0, 2, 5, 5, 2,
                    P3RGraphics.alpha(hovered ? P3RGraphics.CYAN : P3RGraphics.BLUE,
                            intro));
            graphics.drawString(font, p3r_labels.get(row.button()), 9, 0,
                    P3RGraphics.alpha(hovered ? P3RGraphics.WHITE : P3RGraphics.PALE,
                            intro * (hovered ? 1.0F : 0.78F)), true);
            graphics.pose().popMatrix();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (Transition.blocksScreenInput()) {
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        for (AbstractButton button : p3r_passthrough) {
            if (button.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        List<Row> rows = p3r_rows();
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).contains(event.x(), event.y())) {
                p3r_selected = rows.get(index).sourceIndex();
                p3r_activate(rows.get(index).button(), event);
                return true;
            }
        }
        for (FooterRow row : p3r_footerRows()) {
            if (row.contains(event.x(), event.y())) {
                p3r_activate(row.button(), event);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (Double.isNaN(p3r_lastMouseX) || Math.abs(mouseX - p3r_lastMouseX) > 0.25D
                || Math.abs(mouseY - p3r_lastMouseY) > 0.25D) {
            p3r_mouseNavigation = true;
        }
        p3r_lastMouseX = mouseX;
        p3r_lastMouseY = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (Transition.blocksScreenInput()) {
            return true;
        }
        if (scrollY != 0.0D && !p3r_main.isEmpty()) {
            p3r_move(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Transition.blocksScreenInput()) {
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
        if (event.key() == GLFW.GLFW_KEY_HOME) {
            p3r_select(0);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_END) {
            p3r_select(p3r_main.size() - 1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER
                || event.key() == GLFW.GLFW_KEY_SPACE) {
            if (!p3r_main.isEmpty()) {
                p3r_activate(p3r_main.get(p3r_selected), event);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Unique
    private void p3r_activate(AbstractButton button, net.minecraft.client.input.InputWithModifiers input) {
        if (!button.active) {
            return;
        }
        button.playDownSound(minecraft.getSoundManager());
        if (p3r_essentialActions.containsKey(button)) {
            p3r_invokeEssentialAction(button, input);
            return;
        }
        Transition.startOut(p3r_labels.get(button), () -> button.onPress(input));
    }

    @Unique
    private void p3r_prepareEssentialActions(GuiGraphics graphics, int mouseX,
            int mouseY, float delta) {
        if (p3r_essentialActions.isEmpty()) return;
        graphics.enableScissor(0, 0, 0, 0);
        try {
            for (AbstractButton button : p3r_essentialActions.keySet()) {
                button.render(graphics, mouseX, mouseY, delta);
            }
        } finally {
            graphics.disableScissor();
        }
    }

    @Unique
    private void p3r_drawEssentialPlayer(GuiGraphics graphics, int mouseX,
            int mouseY, float delta) {
        if (p3r_essentialPlayer == null) return;
        float ui = P3RGraphics.scale(width, height);
        int playerHeight = Math.max(1, Math.round(Math.min(height * 0.62F, 360.0F * ui)));
        int playerWidth = Math.max(1,
                Math.round(Math.min(width * 0.26F, playerHeight * 0.58F)));
        int x = Math.round(Math.max(18.0F * ui, width * 0.08F));
        int y = Math.round(Math.max(76.0F * ui, height * 0.24F));
        p3r_applyHitbox(p3r_essentialPlayer, x, y, playerWidth, playerHeight);
        p3r_essentialPlayer.render(graphics, mouseX, mouseY, delta);
        boolean essentialDrawsPlayer = p3r_hasEssentialPlayerEntity(p3r_essentialPlayer);
        if (p3r_skinPreview != null) {
            p3r_skinPreview.visible = !essentialDrawsPlayer;
            p3r_skinPreview.setPosition(x, y);
            p3r_skinPreview.setWidth(playerWidth);
            p3r_skinPreview.setHeight(playerHeight);
            if (p3r_skinPreview.visible) {
                p3r_skinPreview.render(graphics, mouseX, mouseY, delta);
            }
        }
    }

    @Unique
    private static boolean p3r_hasEssentialPlayerEntity(AbstractButton proxy) {
        try {
            Object component = proxy.getClass().getMethod("getEssentialComponent").invoke(proxy);
            if (component == null) return false;
            Object player = component.getClass().getMethod("getPlayer").invoke(component);
            return player != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Essential remains optional and may change its internal component API.
            return false;
        }
    }

    @Unique
    private void p3r_invokeEssentialAction(AbstractButton button,
            net.minecraft.client.input.InputWithModifiers input) {
        try {
            Object component = button.getClass().getMethod("getEssentialComponent").invoke(button);
            if (component != null) {
                for (Method method : button.getClass().getMethods()) {
                    if (method.getName().equals("click") && method.getParameterCount() == 1
                            && method.getParameterTypes()[0].isInstance(component)) {
                        method.invoke(button, component);
                        return;
                    }
                }
            }
        } catch (InvocationTargetException exception) {
            p3r_rethrow(exception.getCause());
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Fall through to the proxy's vanilla callback for forward compatibility.
        }
        button.onPress(input);
    }

    @Unique
    private void p3r_move(int direction) {
        if (!p3r_main.isEmpty()) {
            p3r_select(Math.floorMod(p3r_selected + direction, p3r_main.size()));
        }
    }

    @Unique
    private void p3r_select(int index) {
        if (p3r_main.isEmpty()) {
            return;
        }
        int next = Mth.clamp(index, 0, p3r_main.size() - 1);
        if (next != p3r_selected) {
            p3r_selected = next;
            p3r_main.get(next).playDownSound(minecraft.getSoundManager());
        }
        p3r_mouseNavigation = false;
    }

    @Unique
    private void p3r_updateSelection() {
        long now = Util.getMillis();
        float seconds = Math.min(0.05F, Math.max(0.0F, (now - p3r_lastFrame) / 1000.0F));
        p3r_lastFrame = now;
        for (int index = 0; index < p3r_main.size(); index++) {
            AbstractButton button = p3r_main.get(index);
            float current = p3r_selection.getOrDefault(button, index == p3r_selected ? 1.0F : 0.0F);
            float target = index == p3r_selected ? 1.0F : 0.0F;
            float rate = target > current ? 20.0F : 13.0F;
            p3r_selection.put(button, target + (current - target) * (float) Math.exp(-rate * seconds));
        }
    }

    @Unique
    private void p3r_updateMouse(int mouseX, int mouseY) {
        if (!p3r_mouseNavigation || Transition.blocksScreenInput()) {
            return;
        }
        List<Row> rows = p3r_rows();
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).contains(mouseX, mouseY)) {
                p3r_select(rows.get(index).sourceIndex());
                return;
            }
        }
    }

    @Unique
    private List<Row> p3r_rows() {
        List<Row> result = new ArrayList<>();
        if (p3r_main.isEmpty()) {
            return result;
        }
        float ui = P3RGraphics.scale(width, height);
        int count = p3r_main.size();
        float top = Math.max(126.0F * ui, height * 0.36F);
        float bottom = height - Math.max(45.0F, 38.0F * ui);
        float available = Math.max(1.0F, bottom - top);
        float baseStep = 24.5F * ui;
        float minimumStep = Math.max(12.0F, baseStep * 0.46F);
        int maximumVisible = Math.max(1, (int) Math.floor(available / minimumStep) + 1);
        int visibleCount = Math.min(count, maximumVisible);
        int firstVisible = Mth.clamp(p3r_selected - visibleCount / 2,
                0, Math.max(0, count - visibleCount));
        float verticalDensity = visibleCount <= 1 ? 1.0F
                : available / ((visibleCount - 1) * baseStep);
        float baseTextScale = 2.75F * ui;
        float widest = 1.0F;
        for (AbstractButton button : p3r_main) {
            widest = Math.max(widest, font.width(p3r_labels.get(button)));
        }
        float availableTextWidth = Math.max(96.0F, width * 0.40F);
        float horizontalDensity = availableTextWidth / (widest * baseTextScale);
        float density = Mth.clamp(Math.min(verticalDensity, horizontalDensity), 0.46F, 1.0F);
        float textScale = baseTextScale * density;
        float step = Math.max(12.0F, baseStep * density);
        float total = (visibleCount - 1) * step;
        float start = Mth.clamp(height * 0.775F - total * 0.5F,
                top, Math.max(top, bottom - total));
        float center = Mth.clamp(width * 0.83F, width * 0.62F, width - 68.0F * ui);
        for (int slot = 0; slot < visibleCount; slot++) {
            int index = firstVisible + slot;
            AbstractButton button = p3r_main.get(index);
            float textX = center - font.width(p3r_labels.get(button)) * textScale * 0.5F;
            float y = start + slot * step;
            float hitPaddingX = Math.max(16.0F, 15.0F * ui);
            float hitX = Math.min(textX - hitPaddingX, width * 0.69F);
            result.add(new Row(index, button, textX, y, textScale, ui,
                    hitX, y - step * 0.12F, width + 4.0F - hitX, step));
        }
        return result;
    }

    @Unique
    private List<FooterRow> p3r_footerRows() {
        List<FooterRow> rows = new ArrayList<>();
        if (p3r_footer.isEmpty()) return rows;

        List<AbstractButton> orderedButtons = new ArrayList<>(p3r_footer);
        orderedButtons.sort((left, right) -> Integer.compare(
                p3r_footerOrder(left), p3r_footerOrder(right)));
        java.util.Collections.reverse(orderedButtons);

        float ui = P3RGraphics.scale(width, height);
        float preferredScale = Mth.clamp(ui * 0.78F, 0.68F, 1.0F);
        float margin = Math.max(14.0F, 18.0F * ui);
        float availableWidth = Math.max(1.0F, width - margin * 2.0F);
        float contentUnits = 0.0F;
        for (AbstractButton button : orderedButtons) {
            contentUnits += 9.0F + font.width(p3r_labels.get(button));
        }
        int gapCount = Math.max(0, orderedButtons.size() - 1);
        float preferredGap = 18.0F * preferredScale;
        float preferredWidth = contentUnits * preferredScale + gapCount * preferredGap;
        float compression = Math.min(1.0F, availableWidth / Math.max(1.0F, preferredWidth));
        float gap = Math.max(4.0F, preferredGap * compression);
        float textScale = Math.min(preferredScale,
                Math.max(0.01F, (availableWidth - gapCount * gap) / Math.max(1.0F, contentUnits)));
        float y = height - Math.max(16.0F, 17.0F * ui);
        float cursor = width - margin;
        for (int index = orderedButtons.size() - 1; index >= 0; index--) {
            AbstractButton button = orderedButtons.get(index);
            float contentWidth = (9.0F + font.width(p3r_labels.get(button))) * textScale;
            float x = cursor - contentWidth;
            p3r_applyHitbox(button, x - 2.0F, y - 3.0F,
                    contentWidth + 4.0F, 15.0F * textScale + 3.0F);
            rows.add(0, new FooterRow(button, x, y, contentWidth, textScale));
            cursor = x - gap;
        }
        return rows;
    }

    @Unique
    private static void p3r_applyHitbox(AbstractWidget widget, float x, float y,
            float hitWidth, float hitHeight) {
        widget.setPosition(Math.round(x), Math.round(y));
        widget.setWidth(Math.max(1, Math.round(hitWidth)));
        widget.setHeight(Math.max(1, Math.round(hitHeight)));
    }

    @Unique
    private static boolean p3r_isFooter(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        return value.contains("quit") || value.contains("language") || value.contains("accessib")
                || value.contains("friend");
    }

    @Unique
    private int p3r_footerOrder(AbstractButton button) {
        if (button.getMessage().getString().toLowerCase(Locale.ROOT).contains("quit")) {
            return 0;
        }
        return "account".equals(p3r_essentialActions.get(button)) ? 1 : 2;
    }

    @Unique
    private static boolean p3r_hidden(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        return value.contains("copyright") || value.contains("do not distribute") || value.equals("tw");
    }

    @Unique
    private static boolean p3r_isExternallyManagedWidget(AbstractButton button) {
        if (!FabricLoader.getInstance().isModLoaded("essential")
                && !FabricLoader.getInstance().isModLoaded("essential-container")) {
            return false;
        }
        if (!p3r_checkedEssentialProxyClass) {
            p3r_checkedEssentialProxyClass = true;
            try {
                p3r_essentialProxyClass = Class.forName(P3R_ESSENTIAL_PROXY_CLASS, false,
                        button.getClass().getClassLoader());
            } catch (ClassNotFoundException | LinkageError ignored) {
                p3r_essentialProxyClass = null;
            }
        }
        return p3r_essentialProxyClass != null
                && p3r_essentialProxyClass.isInstance(button);
    }

    @Unique
    private static String p3r_essentialId(AbstractButton button) {
        try {
            Object value = button.getClass().getMethod("getEssentialId").invoke(button);
            return value instanceof String id ? id : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    @Unique
    private static String p3r_essentialAction(String id) {
        if (id == null) return null;
        return switch (id) {
            case "wardrobe_2" -> "wardrobe";
            case "invite_host", "world_host", "social", "wardrobe", "pictures",
                    "settings", "account" -> id;
            default -> null;
        };
    }

    @Unique
    private static String p3r_essentialLabel(String action) {
        if (action == null) return null;
        return switch (action) {
            case "invite_host" -> "INVITE FRIENDS";
            case "world_host" -> "HOST WORLD";
            case "social" -> "SOCIAL";
            case "wardrobe" -> "WARDROBE";
            case "pictures" -> "PICTURES";
            case "settings" -> "ESSENTIAL SETTINGS";
            case "account" -> "ACCOUNT";
            default -> null;
        };
    }

    @Unique
    private static boolean p3r_isEssentialFooterAction(String action) {
        return !"invite_host".equals(action) && !"world_host".equals(action);
    }

    @Unique
    private static void p3r_rethrow(Throwable throwable) {
        if (throwable instanceof RuntimeException runtime) throw runtime;
        if (throwable instanceof Error error) throw error;
        throw new RuntimeException(throwable);
    }

    @Unique
    private static float p3r_easeOutQuint(float value) {
        float inverse = 1.0F - Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse * inverse * inverse;
    }

    @Unique
    private record Row(int sourceIndex, AbstractButton button, float textX, float y,
            float textScale, float uiScale, float hitX, float hitY,
            float hitWidth, float hitHeight) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= hitX && mouseX <= hitX + hitWidth
                    && mouseY >= hitY && mouseY <= hitY + hitHeight;
        }

        float rowHeight() {
            return hitHeight;
        }
    }

    @Unique
    private record FooterRow(AbstractButton button, float x, float y, float width, float scale) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x - 2.0F && mouseX <= x + width + 2.0F
                    && mouseY >= y - 3.0F && mouseY <= y + 15.0F * scale;
        }
    }
}
