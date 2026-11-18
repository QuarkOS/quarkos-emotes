package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.animation.Emote;
import dev.quarkos.emotes.config.ConfigLayout.Box;
import dev.quarkos.emotes.config.EmoteWheelScreen;
import dev.quarkos.emotes.config.WheelLayout;
import dev.quarkos.emotes.config.WheelLayout.Wedge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
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
    private final Wedge[] wedges = new Wedge[WheelLayout.SLOTS];
    private final boolean compactNames;
    private final float nameScale;
    private final float[] emphasis = new float[WheelLayout.SLOTS];
    private long lastFrame = System.nanoTime();

    public WheelRing(WheelLayout layout, boolean compactNames) {
        this.layout = layout;
        this.compactNames = compactNames;
        this.nameScale = compactNames ? 1f : Math.clamp(layout.radius() / 175f, 1f, 1.15f);
        this.spans = buildSpans();
        for (int slot = 0; slot < wedges.length; slot++) wedges[slot] = layout.wedge(slot);
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

    public void render(GuiGraphics g, Font font, LocalPlayer player, List<Emote> emotes, int page, int hover, int slide,
                       Predicate<String> starred) {
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

        for (int slot = 0; slot < WheelLayout.SLOTS; slot++) {
            int index = WheelLayout.slotIndex(page, slot, emotes.size());
            if (index < 0) break;
            Emote emote = emotes.get(index);
            Wedge wedge = wedges[slot];
            EmotePreview.draw(g, slide(wedge.preview(), slide), player, emote, EmotePreview.stillTime(emote), 14);
            Box name = slide(wedge.name(), slide);
            Component label = EmoteWheelScreen.label(emote);
            if (name.w() > 0 && (!compactNames || font.width(label) <= name.w())) {
                Ui.heading(g, font, label, name, nameScale, true);
            }
            drawMarkers(g, font, slide(wedge.markers(), slide), slot, starred.test(emote.id()));
        }
    }

    private static void drawMarkers(GuiGraphics g, Font font, Box markers, int slot, boolean starred) {
        if (markers.w() < WheelLayout.BADGE_WIDTH) return;
        int badgeWidth = WheelLayout.BADGE_WIDTH;
        int starWidth = WheelLayout.STAR_WIDTH;
        boolean showStar = starred && markers.w() >= badgeWidth + 2 + starWidth;
        int x = showStar ? markers.x() : markers.x() + (markers.w() - badgeWidth) / 2;
        Box badge = new Box(x, markers.y(), badgeWidth, markers.h());
        Ui.softRect(g, badge, Ui.WELL);
        Ui.center(g, font, Component.literal(Integer.toString(slot + 1)), badge, Ui.MUTED);
        if (showStar) {
            StarToggle.glyph(g, font, new Box(markers.right() - starWidth, markers.y(), starWidth, markers.h()), true);
        }
    }

    private static Box slide(Box box, int offset) {
        return new Box(box.x() + offset, box.y(), box.w(), box.h());
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
