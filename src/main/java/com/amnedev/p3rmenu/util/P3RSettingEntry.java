package com.amnedev.p3rmenu.util;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** Immutable, screen-scaled hit and render geometry for one root settings item. */
public record P3RSettingEntry(ClickableWidget widget, Text label, float x, float y,
        float width, float textScale, float rowHeight, float uiScale) {
    public boolean contains(double mouseX, double mouseY, int textWidth) {
        float hitWidth = Math.max(width,
                Math.max(80.0F * uiScale, textWidth * textScale * 1.18F));
        return mouseX >= x - 14.0F * uiScale
                && mouseX <= x + hitWidth
                && mouseY >= y - 5.0F * uiScale
                && mouseY <= y + rowHeight;
    }
}
