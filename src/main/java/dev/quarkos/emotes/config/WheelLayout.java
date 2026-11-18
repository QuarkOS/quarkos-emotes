package dev.quarkos.emotes.config;

import dev.quarkos.emotes.config.ConfigLayout.Box;

/** Wheel geometry in logical GUI pixels. Slot zero is at twelve o'clock. */
public record WheelLayout(int cx, int cy, int radius, int innerRadius, Box pager, Box scope, int helpY) {
    public static final int SLOTS = 8;
    public static final double STEP = Math.PI * 2 / SLOTS;

    public static WheelLayout of(int width, int height) {
        Box header = header(width, height);
        int helpY = height - header.y() - 9;
        int footerY = helpY - 25;
        int top = header.bottom() + 5;
        int bottom = footerY - 3;
        int cx = width / 2;
        int cy = (top + bottom) / 2;
        int radius = Math.max(1, Math.min(cx - 9, Math.min(cy - top, bottom - cy)));
        int pagerWidth = 132;
        int scopeWidth = Math.min(134, header.w() / 2 - 25);
        int pagerX = Math.max(cx - pagerWidth / 2, header.x() + scopeWidth + 7);
        Box pager = new Box(pagerX, footerY, pagerWidth, 19);
        return new WheelLayout(cx, cy, radius, (int) (radius * .43), pager,
                new Box(header.x(), footerY, scopeWidth, pager.h()), helpY);
    }

    static Box header(int width, int height) {
        int margin = height < 370 ? 8 : 14;
        int frameWidth = Math.min(1000, width - margin * 2);
        return new Box((width - frameWidth) / 2, margin, frameWidth, height < 370 ? 22 : 26);
    }

    public int slotAt(double x, double y) {
        double dx = x - cx;
        double dy = y - cy;
        double distanceSquared = dx * dx + dy * dy;
        if (distanceSquared < (double) innerRadius * innerRadius || distanceSquared > (double) radius * radius) {
            return -1;
        }
        return direction(dx, dy);
    }

    public static int direction(double dx, double dy) {
        return Math.floorMod((int) Math.floor((Math.atan2(dy, dx) + Math.PI / 2 + STEP / 2) / STEP), SLOTS);
    }

    public double angle(int slot) {
        return -Math.PI / 2 + STEP * slot;
    }

    public float contentScale() {
        return Math.clamp(radius / 56f, .85f, 3.25f);
    }

    public static int pageCount(int itemCount) {
        return Math.max(1, Math.ceilDiv(itemCount, SLOTS));
    }

    public static int clampPage(int page, int itemCount) {
        return Math.clamp(page, 0, pageCount(itemCount) - 1);
    }

    public static int wrapPage(int page, int step, int itemCount) {
        return Math.floorMod(clampPage(page, itemCount) + step, pageCount(itemCount));
    }

    public static int slotIndex(int page, int slot, int itemCount) {
        int index = clampPage(page, itemCount) * SLOTS + slot;
        return slot >= 0 && slot < SLOTS && index < itemCount ? index : -1;
    }

    /** Minecraft keeps the logical GUI at least 320×240 when increasing the scale. */
    public static int effectiveScale(int framebufferWidth, int framebufferHeight, int requested) {
        int scale = 1;
        int limit = requested <= 0 ? Integer.MAX_VALUE : requested;
        while (scale < limit && framebufferWidth / (scale + 1) >= 320 && framebufferHeight / (scale + 1) >= 240) {
            scale++;
        }
        return scale;
    }

    public static int logicalWidth(int framebufferWidth, int framebufferHeight, int requested) {
        return Math.ceilDiv(framebufferWidth, effectiveScale(framebufferWidth, framebufferHeight, requested));
    }

    public static int logicalHeight(int framebufferWidth, int framebufferHeight, int requested) {
        return Math.ceilDiv(framebufferHeight, effectiveScale(framebufferWidth, framebufferHeight, requested));
    }
}
