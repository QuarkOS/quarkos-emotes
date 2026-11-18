package dev.quarkos.emotes.config;

/** Settings geometry and shared preview framing, in logical GUI pixels. */
public final class ConfigLayout {
    private ConfigLayout() {}

    public record Box(int x, int y, int w, int h) {
        public int right() { return x + w; }
        public int bottom() { return y + h; }
        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }
        public boolean intersects(Box other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
        public Box inset(int amount) {
            return new Box(x + amount, y + amount, w - 2 * amount, h - 2 * amount);
        }
    }

    public record Geometry(Box header, Box playback, Box sound, Box wheel, Box preview, int rowHeight, int headingHeight) {
        public Box row(Box section, int index) {
            return new Box(section.x() + 6, section.y() + headingHeight + index * rowHeight, section.w() - 12, rowHeight);
        }
    }

    public record Fit(int scale, float yOffset, float yaw) {}

    public static Geometry geometry(int width, int height) {
        int contentWidth = Math.min(width - 16, 720);
        int left = (width - contentWidth) / 2;
        int gap = height < 370 ? 6 : 10;
        int heading = height < 370 ? 15 : 21;
        int headerH = height < 370 ? 22 : 26;
        int row = Math.clamp((height - headerH - 3 * gap - 3 * heading - 6 - 16) / 8, 15, 34);
        int bodyHeight = row * 8 + (heading + 2) * 3 + gap * 2;
        int top = Math.max(5, (height - headerH - gap - bodyHeight) / 2);
        Box header = new Box(left, top, contentWidth, headerH);
        int optionsWidth = Math.min(contentWidth - gap - 92, Math.max(198, contentWidth * 3 / 5));
        int y = header.bottom() + gap;
        Box playback = new Box(left, y, optionsWidth, heading + 3 * row + 2);
        Box sound = new Box(left, playback.bottom() + gap, optionsWidth, heading + 2 * row + 2);
        Box wheel = new Box(left, sound.bottom() + gap, optionsWidth, heading + 3 * row + 2);
        Box preview = new Box(playback.right() + gap, y, contentWidth - optionsWidth - gap, bodyHeight);
        return new Geometry(header, playback, sound, wheel, preview, row, heading);
    }

    /**
     * The inventory pivot includes half the entity height. A negative offset
     * lifts a seated model; a side angle reveals the length of a prone model.
     */
    public static Fit preview(String id, int panelWidth, int panelHeight) {
        int base = Math.max(6, Math.min(92, Math.min((panelHeight - 6) / 3, (panelWidth - 6) / 2)));
        String name = id == null ? "" : id.substring(id.indexOf(':') + 1);
        return switch (name) {
            case "pushups", "lay_down", "sleep", "worm" -> new Fit(Math.max(5, (int) (base * .72f)), .35f, 78f);
            case "meditate", "sit", "sit_full", "kneel" -> new Fit(Math.max(5, (int) (base * .76f)), -.45f, 35f);
            case "bow" -> new Fit(Math.max(5, (int) (base * .9f)), .35f, 0f);
            case "tableflip" -> new Fit(Math.max(5, (int) (base * .78f)), .13f, 18f);
            case "backflip", "jump_hype" -> new Fit(Math.max(5, (int) (base * .76f)), .22f, 15f);
            default -> new Fit(base, .0625f, 0f);
        };
    }

    /** The tooltip may cover settings rows, and is bounded by the options column. */
    public static Box placeTooltip(int screenWidth, int screenHeight, Box anchor, Box avoid, int tipWidth, int tipHeight) {
        int rightLimit = Math.min(screenWidth - 4, avoid.x() - 4);
        int width = Math.clamp(tipWidth, 1, rightLimit - 4);
        int x = Math.clamp(anchor.x(), 4, rightLimit - width);
        int height = Math.clamp(tipHeight, 1, screenHeight - 8);
        int below = anchor.bottom() + 4;
        int y = below + height <= screenHeight - 4 ? below : anchor.y() - height - 4;
        return new Box(x, Math.clamp(y, 4, screenHeight - 4 - height), width, height);
    }
}
