package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.animation.Emote;
import dev.quarkos.emotes.config.ConfigLayout.Box;
import dev.quarkos.emotes.config.WheelLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Cached horizontal spans keep the radial outlines pixel-aligned at any GUI scale. */
public final class WheelRing {
    private static final int HUB = 16, HUB_BORDER = 17;
    private record Span(int start, int end, int y, int colorIndex) {}

    private final WheelLayout layout;
    private final List<Span> spans;
    private final float[] emphasis = new float[WheelLayout.SLOTS];
    private long lastFrame = System.nanoTime();

    public WheelRing(WheelLayout layout) {
        this.layout = layout;
        this.spans = buildSpans();
    }

    private List<Span> buildSpans() {
        int radius = layout.radius();
        List<Span> lines = new ArrayList<>();
        for (int y = layout.cy() - radius; y < layout.cy() + radius; y++) {
            int previous = -1;
            int start = 0;
            for (int x = layout.cx() - radius; x <= layout.cx() + radius; x++) {
                int next = colorIndex(x + .5 - layout.cx(), y + .5 - layout.cy());
                if (next != previous) {
                    if (previous >= 0) lines.add(new Span(start, x, y, previous));
                    start = x;
                    previous = next;
                }
            }
        }
        return lines;
    }

    private int colorIndex(double dx, double dy) {
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > layout.radius()) return -1;
        if (distance < layout.innerRadius() - 3) return HUB;
        if (distance < layout.innerRadius() - 2) return HUB_BORDER;
        if (distance < layout.innerRadius()) return -1;
        int slot = WheelLayout.direction(dx, dy);
        double wedgeEdge = Math.abs(Math.sin(WheelLayout.STEP / 2 - Math.abs(
                Math.IEEEremainder(Math.atan2(dy, dx) + Math.PI / 2, WheelLayout.STEP)))) * distance;
        if (wedgeEdge < 1) return -1;
        boolean outline = distance >= layout.radius() - 1.3 || distance < layout.innerRadius() + 1.1 || wedgeEdge < 2;
        return outline ? slot + WheelLayout.SLOTS : slot;
    }

    public void render(GuiGraphics g, Font font, List<Emote> emotes, int page, int hover, float slide, Predicate<String> starred) {
        long now = System.nanoTime();
        float transition = Math.clamp((now - lastFrame) / 55_000_000f, .10f, 1f);
        lastFrame = now;
        int[] colors = new int[18];
        colors[HUB] = Ui.WELL;
        colors[HUB_BORDER] = Ui.BORDER;
        for (int slot = 0; slot < WheelLayout.SLOTS; slot++) {
            boolean filled = WheelLayout.slotIndex(page, slot, emotes.size()) >= 0;
            emphasis[slot] += ((slot == hover && filled ? 1f : 0f) - emphasis[slot]) * transition;
            colors[slot] = mix(filled ? Ui.CONTROL : Ui.WELL, Ui.SELECTED, emphasis[slot]);
            colors[slot + WheelLayout.SLOTS] = mix(filled ? Ui.BORDER : Ui.PANEL, Ui.ACCENT, emphasis[slot]);
        }
        // These row bands never overlap. Separate them so deferred GUI bounds
        // checks stay local when the wheel is several hundred pixels high.
        int band = -1;
        int top = layout.cy() - layout.radius();
        for (Span span : spans) {
            int nextBand = (span.y() - top) / 24;
            if (nextBand != band) {
                Ui.layer(g);
                band = nextBand;
            }
            g.fill(span.start(), span.y(), span.end(), span.y() + 1, colors[span.colorIndex()]);
        }
        Ui.layer(g);

        float scale = layout.contentScale();
        double orbit = (layout.innerRadius() + layout.radius()) * .5;
        for (int slot = 0; slot < WheelLayout.SLOTS; slot++) {
            int index = WheelLayout.slotIndex(page, slot, emotes.size());
            if (index < 0) break;
            Emote emote = emotes.get(index);
            double angle = layout.angle(slot);
            float x = layout.cx() + (float) (Math.cos(angle) * orbit) + slide;
            float y = layout.cy() + (float) (Math.sin(angle) * orbit);
            EmoteIcon.draw(g, emote, x, y - 4 * scale, scale);
            Ui.push(g, x, y + 7 * scale, Math.max(1, scale * .7f));
            Ui.center(g, font, Component.literal(Integer.toString(slot + 1)), new Box(-8, 0, 16, 10), Ui.TEXT);
            if (starred.test(emote.id())) StarToggle.glyph(g, font, new Box(7, 0, 9, 10), true);
            Ui.pop(g);
        }
    }

    private static int mix(int a, int b, float progress) {
        int red = interpolate(a >> 16 & 255, b >> 16 & 255, progress);
        int green = interpolate(a >> 8 & 255, b >> 8 & 255, progress);
        int blue = interpolate(a & 255, b & 255, progress);
        return 0xff000000 | red << 16 | green << 8 | blue;
    }

    private static int interpolate(int a, int b, float progress) {
        return a + Math.round((b - a) * progress);
    }
}
