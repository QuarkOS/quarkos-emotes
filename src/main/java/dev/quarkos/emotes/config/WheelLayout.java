package dev.quarkos.emotes.config;

/**
 * Screen-space wheel metrics in logical GUI pixels (after Minecraft's GUI scale).
 * The ring grows with the window instead of stopping at a fixed radius.
 */
public record WheelLayout(
        int cx, int cy, int radius,
        int searchX, int searchY, int searchW, int searchH,
        int categoryX, int categoryW,
        int settingsX, int settingsW,
        int bottomY, int controlH,
        int prevX, int favX, int favW, int nextX,
        boolean compact) {
    public float contentScale() { return Math.max(0.85f, Math.min(3.5f, radius / 84f)); }

    /** Minecraft's GUI-scale clamp: logical size stays at least 320×240. */
    public static int effectiveScale(int framebufferWidth, int framebufferHeight, int requested) {
        int scale = 1;
        int limit = requested <= 0 ? Integer.MAX_VALUE : requested;
        while (scale != limit && scale < framebufferWidth && scale < framebufferHeight
                && framebufferWidth / (scale + 1) >= 320 && framebufferHeight / (scale + 1) >= 240) scale++;
        return scale;
    }

    public static int logicalWidth(int framebufferWidth, int framebufferHeight, int requested) {
        return (int) Math.ceil(framebufferWidth / (double) effectiveScale(framebufferWidth, framebufferHeight, requested));
    }

    public static int logicalHeight(int framebufferWidth, int framebufferHeight, int requested) {
        return (int) Math.ceil(framebufferHeight / (double) effectiveScale(framebufferWidth, framebufferHeight, requested));
    }

    public static WheelLayout of(int width, int height) {
        boolean compact = height < 400;
        int controlH = compact ? 16 : 20;
        int searchY = compact ? 4 : 28;
        int searchH = compact ? 16 : 18;
        int settingsW = Math.min(78, Math.max(compact ? 52 : 70, width / 10));
        int settingsX = width - settingsW - 6;
        int titleReserve = compact ? 52 : 0;
        int left = 8 + titleReserve;
        int right = Math.max(left + 120, settingsX - 4);
        int row = right - left;
        int categoryW = Math.min(110, Math.max(64, row / 4));
        int searchW = Math.min(240, Math.max(72, row - categoryW - 4));
        int rowW = searchW + 4 + categoryW;
        if (left + rowW > right) {
            searchW = Math.max(60, right - left - categoryW - 4);
            rowW = searchW + 4 + categoryW;
        }
        int searchX = left + Math.max(0, (right - left - rowW) / 2);
        int categoryX = searchX + searchW + 4;
        int bottomY = height - controlH - (compact ? 4 : 7);
        int topLimit = searchY + searchH + (compact ? 4 : 8);
        // Room for the selected name under the ring, and the help line on tall windows.
        int bottomLimit = bottomY - (compact ? 14 : 28);
        int cx = width / 2;
        int availableH = Math.max(64, bottomLimit - topLimit);
        int availableW = Math.max(64, width - 16);
        int cy = topLimit + availableH / 2;
        int radius = Math.min(availableW, availableH) / 2;
        int maxR = Math.min(Math.min(cx - 8, cy - topLimit), bottomLimit - cy);
        radius = Math.max(36, Math.min(radius, Math.max(36, maxR)));
        int pagerW = Math.min(width - 16, Math.max(radius * 2, 168));
        pagerW = Math.min(pagerW, width - 16);
        int prevX = Math.max(8, cx - pagerW / 2);
        int nextX = Math.min(width - 8 - controlH, prevX + pagerW - controlH);
        int favX = prevX + controlH + 4;
        int favW = Math.max(72, nextX - 4 - favX);
        return new WheelLayout(cx, cy, radius, searchX, searchY, searchW, searchH, categoryX, categoryW,
                settingsX, settingsW, bottomY, controlH, prevX, favX, favW, nextX, compact);
    }
}
