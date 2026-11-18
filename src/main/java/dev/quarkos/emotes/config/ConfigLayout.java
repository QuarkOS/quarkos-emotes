package dev.quarkos.emotes.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public record Geometry(Box header, Box playback, Box sound, Box wheel, Box help, Box preview, int rowHeight, int headingHeight) {
        public Box row(Box section, int index) {
            return new Box(section.x() + 6, section.y() + headingHeight + index * rowHeight, section.w() - 12, rowHeight);
        }
    }

    public record Choices(Box track, List<Box> segments) {}

    public record Fit(int scale, float yOffset, float yaw) {}

    public static Geometry geometry(int width, int height) {
        int contentWidth = Math.min(width - 16, 720);
        int left = (width - contentWidth) / 2;
        int gap = height < 370 ? 6 : 10;
        int heading = height < 370 ? 15 : 21;
        int headerH = height < 370 ? 22 : 26;
        int helpHeight = 12;
        int helpGap = 4;
        int row = Math.clamp((height - headerH - 3 * gap - 3 * heading - 6 - 16 - helpGap - helpHeight) / 8, 15, 34);
        int bodyHeight = row * 8 + (heading + 2) * 3 + gap * 2 + helpGap + helpHeight;
        int top = Math.max(5, (height - headerH - gap - bodyHeight) / 2);
        Box header = new Box(left, top, contentWidth, headerH);
        int optionsWidth = Math.min(contentWidth - gap - 92, Math.max(198, contentWidth * 3 / 5));
        int y = header.bottom() + gap;
        Box playback = new Box(left, y, optionsWidth, heading + 3 * row + 2);
        Box sound = new Box(left, playback.bottom() + gap, optionsWidth, heading + 2 * row + 2);
        Box wheel = new Box(left, sound.bottom() + gap, optionsWidth, heading + 3 * row + 2 + helpGap + helpHeight);
        Box help = new Box(left + 9, wheel.bottom() - helpHeight - 2, optionsWidth - 18, helpHeight);
        Box preview = new Box(playback.right() + gap, y, contentWidth - optionsWidth - gap, bodyHeight);
        return new Geometry(header, playback, sound, wheel, help, preview, row, heading);
    }

    static Choices choices(Box row, int rowLabelWidth, int... optionWidths) {
        int count = optionWidths.length;
        int textPadding = 4;
        int textWidth = Arrays.stream(optionWidths).sum();
        int preferred = Math.max(114, textWidth + 12 * count + 2);
        int minimum = textWidth + textPadding * count + 2;
        int labelRoom = Math.min(rowLabelWidth + 10, row.w() / 2);
        // Give back extra label-column space before truncating a choice.
        int available = Math.max(row.w() - labelRoom, Math.min(minimum, row.w() - 28));
        int width = Math.min(preferred, available);
        int height = Math.min(20, row.h() - 2);
        Box track = new Box(row.right() - width, row.y() + (row.h() - height) / 2, width, height);

        int room = width - 2 - textPadding * count;
        int labelLimit = room;
        // Cap the longest labels first so shorter choices still fit in full.
        while (textWidth > room) {
            labelLimit--;
            textWidth = 0;
            for (int optionWidth : optionWidths) textWidth += Math.min(optionWidth, labelLimit);
        }
        int extra = room - textWidth;
        List<Box> segments = new ArrayList<>(count);
        int offset = 0;
        int measured = 0;
        for (int i = 0; i < count; i++) {
            measured += Math.min(optionWidths[i], labelLimit) + textPadding;
            int end = measured + (i + 1) * extra / count;
            segments.add(new Box(track.x() + 1 + offset, track.y() + 1, end - offset, track.h() - 2));
            offset = end;
        }
        return new Choices(track, List.copyOf(segments));
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
}
