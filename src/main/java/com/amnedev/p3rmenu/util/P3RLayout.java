package com.amnedev.p3rmenu.util;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class P3RLayout {
    private P3RLayout() {
    }

    public static final class MenuEntry {
        public final ClickableWidget widget;
        public final Text label;
        public final float textX;
        public final float y;
        public final float textScale;
        public final float uiScale;
        private final float hitX;
        private final float hitY;
        private final float hitWidth;
        private final float hitHeight;

        public MenuEntry(ClickableWidget widget, Text label, float textX, float y,
                float textScale, float uiScale,
                float hitX, float hitY, float hitWidth, float hitHeight) {
            this.widget = widget;
            this.label = label;
            this.textX = textX;
            this.y = y;
            this.textScale = textScale;
            this.uiScale = uiScale;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitWidth = hitWidth;
            this.hitHeight = hitHeight;
        }

        public boolean contains(double x, double y) {
            return x >= this.hitX && x <= this.hitX + this.hitWidth
                    && y >= this.hitY && y <= this.hitY + this.hitHeight;
        }

        public float rowHeight() {
            return this.hitHeight;
        }
    }

    public static final class FooterEntry {
        public final ClickableWidget widget;
        public final Text label;
        public final float x;
        public final float y;
        public final float scale;
        private final float hitX;
        private final float hitY;
        private final float hitWidth;
        private final float hitHeight;

        public FooterEntry(ClickableWidget widget, Text label, float x, float y,
                float scale, float hitX, float hitY, float hitWidth, float hitHeight) {
            this.widget = widget;
            this.label = label;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitWidth = hitWidth;
            this.hitHeight = hitHeight;
        }

        public boolean contains(double x, double y) {
            return x >= this.hitX && x <= this.hitX + this.hitWidth
                    && y >= this.hitY && y <= this.hitY + this.hitHeight;
        }
    }
}
