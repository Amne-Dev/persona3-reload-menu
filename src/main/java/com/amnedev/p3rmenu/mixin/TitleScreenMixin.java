package com.amnedev.p3rmenu.mixin;

import com.amnedev.p3rmenu.util.P3RHelper;
import com.amnedev.p3rmenu.util.P3RLayout.FooterEntry;
import com.amnedev.p3rmenu.util.P3RLayout.MenuEntry;
import com.amnedev.p3rmenu.util.TransitionManager;
import com.amnedev.p3rmenu.util.WallpaperManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private static final Identifier P3R_LOGO_TEXTURE = new Identifier("p3rmenu",
            "textures/gui/title/p3r_logo.png");
    @Unique
    private static final int P3R_WHITE = 0xFFF7FAFF;
    @Unique
    private static final int P3R_INACTIVE = 0xFFE6EEFF;
    @Unique
    private static final int P3R_BLUE = 0xFF080CB5;
    @Unique
    private static final int P3R_CYAN = 0xFF58E7FF;
    @Unique
    private static final int P3R_TEXT_SHADOW = 0xFF52596A;

    @Unique
    private final List<ClickableWidget> p3r_menuItems = new ArrayList<>();
    @Unique
    private final List<ClickableWidget> p3r_utilityItems = new ArrayList<>();
    @Unique
    private final Map<ClickableWidget, Text> p3r_labels = new HashMap<>();
    @Unique
    private final Map<ClickableWidget, Float> p3r_selectionProgress = new HashMap<>();

    @Unique
    private int p3r_selectedIndex;
    @Unique
    private long p3r_introStartedAt;
    @Unique
    private long p3r_lastFrameAt;
    @Unique
    private boolean p3r_mouseIsNavigationSource;
    @Unique
    private double p3r_lastMouseX = Double.NaN;
    @Unique
    private double p3r_lastMouseY = Double.NaN;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void p3r_init(CallbackInfo ci) {
        this.p3r_menuItems.clear();
        this.p3r_utilityItems.clear();
        this.p3r_labels.clear();
        this.p3r_selectionProgress.clear();
        p3r_syncWidgets();
        long now = Util.getMeasuringTimeMs();
        this.p3r_introStartedAt = now;
        this.p3r_lastFrameAt = now;
        this.p3r_mouseIsNavigationSource = false;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void p3r_render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        p3r_syncWidgets();
        p3r_renderBackgroundLayer(context);
        p3r_updateAnimationState();
        p3r_updateMouseSelection(mouseX, mouseY);

        // Foreground layers are deliberately independent from the background texture.
        p3r_renderLogo(context);
        p3r_renderMenu(context);
        p3r_renderFooter(context, mouseX, mouseY);

        TransitionManager.render(context, delta, this.width, this.height);
    }

    @Unique
    private void p3r_syncWidgets() {
        List<? extends Element> currentChildren = this.children();
        this.p3r_menuItems.removeIf(widget -> !currentChildren.contains(widget));
        this.p3r_utilityItems.removeIf(widget -> !currentChildren.contains(widget));
        this.p3r_labels.keySet().removeIf(widget -> !currentChildren.contains(widget));
        this.p3r_selectionProgress.keySet().removeIf(widget -> !currentChildren.contains(widget));

        for (Element element : currentChildren) {
            if (!(element instanceof ClickableWidget widget)) {
                continue;
            }

            // Keep each real widget and its original action as the compatibility
            // layer. Only the vanilla visual is suppressed and replaced here.
            widget.visible = false;
            if (this.p3r_labels.containsKey(widget)) {
                continue;
            }

            this.p3r_labels.put(widget, p3r_createLabel(widget));
            if (p3r_isHiddenWidget(widget)) {
                continue;
            } else if (p3r_isUtilityWidget(widget)) {
                this.p3r_utilityItems.add(widget);
            } else if (!widget.getMessage().getString().isBlank()) {
                this.p3r_menuItems.add(widget);
            }
        }

        this.p3r_selectedIndex = MathHelper.clamp(this.p3r_selectedIndex, 0,
                Math.max(0, this.p3r_menuItems.size() - 1));
    }

    @Unique
    private void p3r_renderBackgroundLayer(DrawContext context) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Identifier wallpaper = WallpaperManager.getBackgroundTexture();
        context.drawTexture(wallpaper, 0, 0, 0, 0.0F, 0.0F,
                this.width, this.height, this.width, this.height);
        RenderSystem.disableBlend();
    }

    @Unique
    private void p3r_renderLogo(DrawContext context) {
        float uiScale = p3r_uiScale();
        float intro = p3r_introProgress(0L, 620.0F);
        if (intro <= 0.001F) {
            return;
        }

        int logoWidth = Math.round(174.0F * uiScale);
        int logoHeight = Math.round(logoWidth * (800.0F / 900.0F));
        float anchorX = this.width * 0.765F + (1.0F - intro) * 58.0F * uiScale;
        float anchorY = Math.max(70.0F * uiScale, this.height * 0.175F)
                - (1.0F - intro) * 12.0F * uiScale;

        context.getMatrices().push();
        context.getMatrices().translate(anchorX, anchorY, 20.0F);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-4.0F));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, intro);
        RenderSystem.setShaderTexture(0, P3R_LOGO_TEXTURE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        context.drawTexture(P3R_LOGO_TEXTURE, -logoWidth / 2, -logoHeight / 2,
                0, 0.0F, 0.0F, logoWidth, logoHeight, logoWidth, logoHeight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        context.getMatrices().pop();
    }

    @Unique
    private void p3r_renderMenu(DrawContext context) {
        List<MenuEntry> entries = p3r_layoutMenu();

        // Paint selection bands as one background layer so their result never
        // depends on whether the selected entry is above or below another label.
        for (int index = 0; index < entries.size(); index++) {
            MenuEntry entry = entries.get(index);
            float selected = this.p3r_selectionProgress.getOrDefault(entry.widget, 0.0F);
            float intro = p3r_introProgress(170L + index * 58L, 470.0F);
            if (intro <= 0.001F || selected <= 0.01F) {
                continue;
            }

            float selectedEase = p3r_easeOutQuint(selected);
            float slideX = (1.0F - intro) * 68.0F * entry.uiScale;
            float drawX = entry.textX + slideX;
            float barRight = this.width + 4.0F;
            float barLeft = Math.min(this.width * 0.70F,
                    drawX - 10.0F * entry.uiScale);
            float fullWidth = Math.max(1.0F, barRight - barLeft);
            float revealedWidth = fullWidth * selectedEase;
            int barX = Math.round(barRight - revealedWidth);
            int barY = Math.round(entry.y - entry.rowHeight() * 0.12F);
            int barHeight = Math.max(12, Math.round(entry.rowHeight()));
            context.fill(barX, barY, Math.round(barRight), barY + barHeight,
                    p3r_withAlpha(P3R_BLUE, intro * selected));
        }

        for (int index = 0; index < entries.size(); index++) {
            MenuEntry entry = entries.get(index);
            float selected = this.p3r_selectionProgress.getOrDefault(entry.widget, 0.0F);
            float intro = p3r_introProgress(170L + index * 58L, 470.0F);
            if (intro <= 0.001F) {
                continue;
            }

            float slideX = (1.0F - intro) * 68.0F * entry.uiScale;
            float drawX = entry.textX + slideX;

            context.getMatrices().push();
            context.getMatrices().translate(drawX, entry.y, 35.0F);
            context.getMatrices().scale(entry.textScale, entry.textScale, 1.0F);

            int alpha = MathHelper.clamp(Math.round(255.0F * intro), 0, 255);
            int foreground = (alpha << 24)
                    | ((selected > 0.12F ? P3R_WHITE : P3R_INACTIVE) & 0x00FFFFFF);
            int softShadow = p3r_withAlpha(P3R_TEXT_SHADOW, intro * 0.42F);
            int closeShadow = p3r_withAlpha(P3R_TEXT_SHADOW, intro * 0.78F);
            context.drawText(this.textRenderer, entry.label, 2, 2, softShadow, false);
            context.drawText(this.textRenderer, entry.label, 1, 1, closeShadow, false);
            context.drawText(this.textRenderer, entry.label, 0, 0, foreground, false);
            context.getMatrices().pop();
        }
    }

    @Unique
    private void p3r_renderFooter(DrawContext context, int mouseX, int mouseY) {
        List<FooterEntry> entries = p3r_layoutFooter();
        float intro = p3r_introProgress(440L, 420.0F);
        if (intro <= 0.001F) {
            return;
        }

        for (FooterEntry entry : entries) {
            boolean hovered = entry.contains(mouseX, mouseY);
            int textColor = p3r_withAlpha(hovered ? P3R_WHITE : P3R_INACTIVE,
                    intro * (hovered ? 1.0F : 0.78F));
            int diamondColor = p3r_withAlpha(hovered ? P3R_CYAN : P3R_BLUE, intro);

            context.getMatrices().push();
            context.getMatrices().translate(entry.x, entry.y, 40.0F);
            context.getMatrices().scale(entry.scale, entry.scale, 1.0F);
            P3RHelper.drawSkewedRect(context, 0, 2, 5, 5, 2, diamondColor);
            context.drawText(this.textRenderer, entry.label, 9, 0, textColor, true);
            context.getMatrices().pop();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TransitionManager.isBlockingInput()) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        List<MenuEntry> menuEntries = p3r_layoutMenu();
        for (int i = 0; i < menuEntries.size(); i++) {
            MenuEntry entry = menuEntries.get(i);
            if (entry.contains(mouseX, mouseY)) {
                this.p3r_selectedIndex = i;
                this.p3r_mouseIsNavigationSource = true;
                p3r_activate(entry.widget, mouseX, mouseY);
                return true;
            }
        }

        for (FooterEntry entry : p3r_layoutFooter()) {
            if (entry.contains(mouseX, mouseY)) {
                p3r_activate(entry.widget, mouseX, mouseY);
                return true;
            }
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (Double.isNaN(this.p3r_lastMouseX)
                || Math.abs(mouseX - this.p3r_lastMouseX) > 0.25D
                || Math.abs(mouseY - this.p3r_lastMouseY) > 0.25D) {
            this.p3r_mouseIsNavigationSource = true;
        }
        this.p3r_lastMouseX = mouseX;
        this.p3r_lastMouseY = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (TransitionManager.isBlockingInput()) {
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
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            p3r_moveSelection(hasShiftDown() ? -1 : 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            p3r_selectIndex(0, true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            p3r_selectIndex(this.p3r_menuItems.size() - 1, true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (!this.p3r_menuItems.isEmpty()) {
                ClickableWidget selected = this.p3r_menuItems.get(this.p3r_selectedIndex);
                p3r_activate(selected, selected.getX(), selected.getY());
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Unique
    private void p3r_updateAnimationState() {
        long now = Util.getMeasuringTimeMs();
        float deltaSeconds = Math.min(0.05F, Math.max(0.0F, (now - this.p3r_lastFrameAt) / 1000.0F));
        this.p3r_lastFrameAt = now;

        for (int i = 0; i < this.p3r_menuItems.size(); i++) {
            ClickableWidget widget = this.p3r_menuItems.get(i);
            float current = this.p3r_selectionProgress.getOrDefault(widget,
                    i == this.p3r_selectedIndex ? 1.0F : 0.0F);
            float target = i == this.p3r_selectedIndex ? 1.0F : 0.0F;
            float rate = target > current ? 20.0F : 13.0F;
            float next = target + (current - target) * (float) Math.exp(-rate * deltaSeconds);
            if (Math.abs(next - target) < 0.002F) {
                next = target;
            }
            this.p3r_selectionProgress.put(widget, next);
        }
    }

    @Unique
    private void p3r_updateMouseSelection(int mouseX, int mouseY) {
        if (!this.p3r_mouseIsNavigationSource || TransitionManager.isBlockingInput()) {
            return;
        }

        List<MenuEntry> entries = p3r_layoutMenu();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).contains(mouseX, mouseY)) {
                if (i != this.p3r_selectedIndex) {
                    p3r_selectIndex(i, true);
                }
                return;
            }
        }
    }

    @Unique
    private void p3r_moveSelection(int direction) {
        if (this.p3r_menuItems.isEmpty()) {
            return;
        }
        int next = Math.floorMod(this.p3r_selectedIndex + direction, this.p3r_menuItems.size());
        p3r_selectIndex(next, true);
    }

    @Unique
    private void p3r_selectIndex(int index, boolean playSound) {
        if (this.p3r_menuItems.isEmpty()) {
            return;
        }
        int clamped = MathHelper.clamp(index, 0, this.p3r_menuItems.size() - 1);
        if (clamped == this.p3r_selectedIndex) {
            this.p3r_mouseIsNavigationSource = false;
            return;
        }
        this.p3r_selectedIndex = clamped;
        this.p3r_mouseIsNavigationSource = false;
        if (playSound) {
            this.p3r_menuItems.get(clamped).playDownSound(this.client.getSoundManager());
        }
    }

    @Unique
    private void p3r_activate(ClickableWidget widget, double mouseX, double mouseY) {
        if (TransitionManager.isTransitioning()) {
            return;
        }
        widget.playDownSound(this.client.getSoundManager());
        TransitionManager.startOut(this.p3r_labels.getOrDefault(widget, p3r_createLabel(widget)), () -> {
            if (widget instanceof PressableWidget pressable) {
                pressable.onPress();
            } else {
                widget.onClick(mouseX, mouseY);
            }
        });
    }

    @Unique
    private List<MenuEntry> p3r_layoutMenu() {
        List<MenuEntry> result = new ArrayList<>();
        if (this.p3r_menuItems.isEmpty()) {
            return result;
        }

        float uiScale = p3r_uiScale();
        int itemCount = this.p3r_menuItems.size();
        float minimumY = Math.max(126.0F * uiScale, this.height * 0.36F);
        float bottomY = this.height - Math.max(45.0F, 38.0F * uiScale);
        float availableHeight = Math.max(1.0F, bottomY - minimumY);

        // Keep the standard title menu intentionally large, then scale both its
        // typography and rhythm when mods inject more (or unusually long) buttons.
        float baseStep = 24.5F * uiScale;
        float verticalDensity = itemCount <= 1 ? 1.0F
                : availableHeight / ((itemCount - 1) * baseStep);
        float baseTextScale = 2.75F * uiScale;
        float widestLabel = 1.0F;
        for (ClickableWidget widget : this.p3r_menuItems) {
            Text label = this.p3r_labels.getOrDefault(widget, p3r_createLabel(widget));
            widestLabel = Math.max(widestLabel, this.textRenderer.getWidth(label));
        }
        float availableTextWidth = Math.max(96.0F, this.width * 0.40F);
        float horizontalDensity = availableTextWidth / (widestLabel * baseTextScale);
        float density = MathHelper.clamp(Math.min(verticalDensity, horizontalDensity), 0.46F, 1.0F);
        float textScale = baseTextScale * density;
        float step = Math.max(12.0F, baseStep * density);
        float totalHeight = (itemCount - 1) * step;
        float desiredCenterY = this.height * 0.775F;
        float maximumY = Math.max(minimumY,
                this.height - Math.max(45.0F, 38.0F * uiScale) - totalHeight);
        float startY = MathHelper.clamp(desiredCenterY - totalHeight * 0.5F, minimumY, maximumY);
        float centerX = MathHelper.clamp(this.width * 0.83F,
                this.width * 0.62F, this.width - 68.0F * uiScale);

        for (int i = 0; i < this.p3r_menuItems.size(); i++) {
            ClickableWidget widget = this.p3r_menuItems.get(i);
            Text label = this.p3r_labels.getOrDefault(widget, p3r_createLabel(widget));
            float textWidth = this.textRenderer.getWidth(label) * textScale;
            float textX = centerX - textWidth * 0.5F;
            float y = startY + i * step;
            float hitPaddingX = Math.max(16.0F, 15.0F * uiScale);

            float hitX = Math.min(textX - hitPaddingX, this.width * 0.69F);
            result.add(new MenuEntry(widget, label, textX, y, textScale, uiScale,
                    hitX, y - step * 0.12F,
                    this.width + 4.0F - hitX,
                    step));
        }
        return result;
    }

    @Unique
    private List<FooterEntry> p3r_layoutFooter() {
        List<FooterEntry> result = new ArrayList<>();
        float uiScale = p3r_uiScale();
        float scale = MathHelper.clamp(uiScale * 0.78F, 0.68F, 1.0F);
        float gap = 18.0F * uiScale;
        float cursorX = this.width - Math.max(14.0F, 18.0F * uiScale);
        float y = this.height - Math.max(16.0F, 17.0F * uiScale);

        for (int i = this.p3r_utilityItems.size() - 1; i >= 0; i--) {
            ClickableWidget widget = this.p3r_utilityItems.get(i);
            Text label = this.p3r_labels.getOrDefault(widget, p3r_createLabel(widget));
            float contentWidth = (9.0F + this.textRenderer.getWidth(label)) * scale;
            float x = cursorX - contentWidth;
            result.add(0, new FooterEntry(widget, label, x, y, scale,
                    x - 4.0F, y - 4.0F, contentWidth + 8.0F, 14.0F * scale + 8.0F));
            cursorX = x - gap;
        }
        return result;
    }

    @Unique
    private Text p3r_createLabel(ClickableWidget widget) {
        String label = widget.getMessage().getString().strip().toUpperCase(Locale.ROOT);
        return Text.literal(label).setStyle(Style.EMPTY.withBold(true));
    }

    @Unique
    private boolean p3r_isUtilityWidget(ClickableWidget widget) {
        TextContent content = widget.getMessage().getContent();
        if (content instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey().toLowerCase(Locale.ROOT);
            if (key.contains("quit") || key.contains("language") || key.contains("accessibility")) {
                return true;
            }
        }

        String label = widget.getMessage().getString().toLowerCase(Locale.ROOT);
        return label.contains("accessib") || label.equals("language")
                || label.equals("quit game") || label.equals("end game");
    }

    @Unique
    private boolean p3r_isHiddenWidget(ClickableWidget widget) {
        TextContent content = widget.getMessage().getContent();
        if (content instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey().toLowerCase(Locale.ROOT);
            if (key.contains("copyright") || key.contains("credits")) {
                return true;
            }
        }

        String label = widget.getMessage().getString().toLowerCase(Locale.ROOT);
        return label.contains("copyright") || label.contains("do not distribute");
    }

    @Unique
    private float p3r_uiScale() {
        float proportional = Math.min(this.width / 960.0F, this.height / 540.0F);
        return MathHelper.clamp(proportional, 0.72F, 1.55F);
    }

    @Unique
    private float p3r_introProgress(long delayMs, float durationMs) {
        float raw = (Util.getMeasuringTimeMs() - this.p3r_introStartedAt - delayMs) / durationMs;
        return p3r_easeOutExpo(MathHelper.clamp(raw, 0.0F, 1.0F));
    }

    @Unique
    private static float p3r_easeOutExpo(float value) {
        return value >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0D * value);
    }

    @Unique
    private static float p3r_easeOutQuint(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse * inverse * inverse;
    }

    @Unique
    private static int p3r_withAlpha(int color, float alphaMultiplier) {
        int baseAlpha = color >>> 24;
        int alpha = MathHelper.clamp(Math.round(baseAlpha * MathHelper.clamp(alphaMultiplier, 0.0F, 1.0F)),
                0, 255);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

}
