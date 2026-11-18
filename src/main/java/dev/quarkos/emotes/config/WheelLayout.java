package dev.quarkos.emotes.config;

import dev.quarkos.emotes.config.ConfigLayout.Box;

/** Wheel geometry in logical GUI pixels. Slot zero is at twelve o'clock. */
public record WheelLayout(int cx, int cy, int radius, int innerRadius, Box pager, Box scope, Box hint) {
    public static final int SLOTS = 8;
    public static final double STEP = Math.PI * 2 / SLOTS;
    public static final int BADGE_WIDTH = 12;
    public static final int STAR_WIDTH = 9;
    private static final int MARKER_HEIGHT = 10;
    private static final int MARKER_WIDTH = BADGE_WIDTH + 2 + STAR_WIDTH;
    private static final int[] VERTICAL_OFFSETS = {0, -2, 2, -4, 4};

    public record Wedge(Box preview, Box name, Box markers) {}

    public static WheelLayout of(int width, int height) {
        Box header = header(width, height);
        int footerY = height - header.y() - 34;
        Box hint = new Box(header.x(), footerY - 11, header.w(), 10);
        int top = header.bottom() + 5;
        int bottom = hint.y() - 2;
        int cx = width / 2;
        int cy = (top + bottom) / 2;
        int radius = Math.max(1, Math.min(cx - 9, Math.min(cy - top, bottom - cy)));
        int pagerWidth = 132;
        int scopeWidth = Math.min(134, header.w() / 2 - 25);
        int pagerX = Math.max(cx - pagerWidth / 2, header.x() + scopeWidth + 7);
        Box pager = new Box(pagerX, footerY, pagerWidth, 19);
        return new WheelLayout(cx, cy, radius, (int) (radius * .43), pager,
                new Box(header.x(), footerY, scopeWidth, pager.h()), hint);
    }

    static Box header(int width, int height) {
        int margin = height < 370 ? 8 : 14;
        int frameWidth = Math.min(1000, width - margin * 2);
        return new Box((width - frameWidth) / 2, margin, frameWidth, height < 370 ? 22 : 26);
    }

    static Box viewToggle(Box header, int screenWidth) {
        int width = header.w() < 400 ? 93 : 116;
        return new Box(screenWidth / 2 - width / 2, header.y(), width, header.h());
    }

    static Box settings(Box toggle) {
        return new Box(toggle.x() - 66, toggle.y(), 60, toggle.h());
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

    public int slideDistance() {
        return Math.clamp((radius - 52) / 18, 1, 8);
    }

    public Wedge wedge(int slot) {
        double orbit = (innerRadius + radius) / 2.0;
        double angle = angle(slot);
        int x = (int) Math.round(cx + Math.cos(angle) * orbit);
        int y = (int) Math.round(cy + Math.sin(angle) * orbit);
        int nameHeight = radius >= 150 ? 13 : 10;
        int preferredHeight = Math.min(80, radius * 3 / 8);
        Wedge labeled = fitWedge(slot, x, y, preferredHeight, nameHeight);
        if (labeled != null) return labeled;
        Wedge compact = fitWedge(slot, x, y, preferredHeight, 0);
        if (compact != null) return compact;
        Box empty = new Box(x, y, 0, 0);
        return new Wedge(empty, empty, wedgeBand(slot, x, y - MARKER_HEIGHT / 2, MARKER_HEIGHT, MARKER_WIDTH));
    }

    private Wedge fitWedge(int slot, int x, int y, int preferredHeight, int nameHeight) {
        for (int height = preferredHeight; height >= 21; height--) {
            int top = y - (MARKER_HEIGHT + height + nameHeight) / 2;
            for (int offset : VERTICAL_OFFSETS) {
                int markerY = top + offset;
                Box markers = wedgeBand(slot, x, markerY, MARKER_HEIGHT, MARKER_WIDTH);
                if (markers.w() < MARKER_WIDTH) continue;
                Box preview = wedgeBand(slot, x, markers.bottom(), height, 90);
                if (preview.w() < Math.max(18, height / 2)) continue;
                Box name = wedgeBand(slot, x, preview.bottom(), nameHeight, 120);
                if (nameHeight == 0 || name.w() >= 20) return new Wedge(preview, name, markers);
            }
        }
        return null;
    }

    /** Fit a horizontal strip inside the annulus, with room for the page slide. */
    private Box wedgeBand(int slot, int centerX, int y, int height, int maxWidth) {
        if (height == 0) return new Box(centerX, y, 0, 0);
        int top = y - 1;
        int bottom = y + height + 1;
        int margin = 1 + slideDistance();
        int nearY = Math.clamp(cy, top, bottom) - cy;
        int longest = 0, start = centerX, run = 0;
        for (int x = cx - radius; x < cx + radius; x++) {
            int left = x - margin, right = x + 1 + margin;
            int nearX = Math.clamp(cx, left, right) - cx;
            boolean fits = nearX * nearX + nearY * nearY >= innerRadius * innerRadius
                    && slotAt(left, top) == slot && slotAt(right, top) == slot
                    && slotAt(left, bottom) == slot && slotAt(right, bottom) == slot;
            if (fits) {
                if (++run > longest) {
                    longest = run;
                    start = x - run + 1;
                }
            } else {
                run = 0;
            }
        }
        int width = Math.min(maxWidth, longest);
        int left = Math.clamp(centerX - width / 2, start, start + longest - width);
        return new Box(left, y, width, height);
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
