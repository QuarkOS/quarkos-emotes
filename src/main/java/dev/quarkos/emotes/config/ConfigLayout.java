package dev.quarkos.emotes.config;

/** Config-screen columns. Tooltips stay in the options column, beside the preview. */
public final class ConfigLayout {
    private ConfigLayout() {}
    public record Box(int x, int y, int w, int h) {
        public int right() { return x + w; }
        public int bottom() { return y + h; }
        public boolean intersects(Box other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }
    public record Geometry(int left, int top, int right, int bottom, int optionWidth, int previewLeft, int rowHeight) {
        public Box preview() { return new Box(previewLeft, top, right - previewLeft, bottom - top); }
        public Box options() { return new Box(left, top, optionWidth, bottom - top); }
    }
    public record Fit(int scale, float yOffset, float yaw) {}

    public static Geometry geometry(int width, int height) {
        int contentWidth = Math.min(580, Math.max(200, width - 20));
        int left = (width - contentWidth) / 2;
        int right = left + contentWidth;
        int top = 36;
        int bottom = Math.max(top + 80, height - 36);
        int optionWidth = Math.min(260, Math.max(96, (contentWidth - 12) * 2 / 3));
        int previewLeft = left + optionWidth + 12;
        int rowHeight = Math.min(26, Math.max(20, (bottom - top) / 7));
        return new Geometry(left, top, right, bottom, optionWidth, previewLeft, rowHeight);
    }

    /**
     * Low poses use a smaller figure. InventoryScreen places the model point at
     * {@code hitboxHeight / 2 + yOffset} on the panel center, and that translation is in GUI
     * space (+Y down) before the Z flip. A smaller offset lifts the figure. Vanilla uses 0.0625,
     * which centers a standing body. A seated skeleton sits about half a block lower, so its
     * offset is negative. A prone body is turned so the inventory camera can see its length.
     */
    public static Fit preview(String id, int panelWidth, int panelHeight) {
        int base = Math.max(12, Math.min(70, Math.min(Math.max(1, panelHeight - 40) / 3, Math.max(1, panelWidth - 12) / 2)));
        String name = id == null ? "" : id.substring(id.indexOf(':') + 1);
        return switch (name) {
            case "pushups", "lay_down", "sleep", "worm" -> new Fit(Math.max(14, (int) (base * 0.72f)), 0.35f, 78f);
            case "meditate", "sit", "sit_full", "kneel" -> new Fit(Math.max(14, (int) (base * 0.72f)), -0.45f, 35f);
            case "bow" -> new Fit(Math.max(14, (int) (base * 0.9f)), 0.35f, 0f);
            default -> new Fit(base, 0.0625f, 0f);
        };
    }

    /**
     * Anchor a tooltip to its button and keep the box entirely to the left of {@code avoid}
     * (the preview panel). Width shrinks to the options column when the text is wider.
     */
    public static Box placeTooltip(int screenWidth, int screenHeight, Box anchor, Box avoid, int tipWidth, int tipHeight) {
        int limit = Math.max(44, avoid.x() - 4);
        int x = Math.max(4, Math.min(anchor.x(), limit - 40));
        int width = Math.min(tipWidth, limit - x);
        width = Math.max(40, width);
        if (x + width > limit) x = Math.max(4, limit - width);
        int y = anchor.bottom() + 4;
        if (y + tipHeight > screenHeight - 4) y = anchor.y() - tipHeight - 4;
        if (y < 4) y = 4;
        if (y + tipHeight > screenHeight - 2) y = Math.max(4, screenHeight - tipHeight - 2);
        return new Box(x, y, width, tipHeight);
    }
}
