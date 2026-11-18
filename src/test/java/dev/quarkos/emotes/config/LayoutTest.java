package dev.quarkos.emotes.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LayoutTest {
    @Test void wheelFillsLogicalScreensFromWindowAndGuiScale() {
        int[][] windows = {{854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440}};
        for (int[] window : windows) for (int scale : new int[]{1, 2, 3, 4, 0}) {
            int w = WheelLayout.logicalWidth(window[0], window[1], scale);
            int h = WheelLayout.logicalHeight(window[0], window[1], scale);
            var layout = WheelLayout.of(w, h);
            assertTrue(layout.radius() >= 36, window[0] + "x" + window[1] + " s" + scale);
            assertTrue(layout.cy() - layout.radius() >= layout.searchY() + layout.searchH() - 1, layout.toString());
            assertTrue(layout.cy() + layout.radius() + 12 <= layout.bottomY(), layout.toString());
            assertTrue(layout.categoryX() + layout.categoryW() <= layout.settingsX(), layout.toString());
            assertTrue(layout.searchX() >= 4 && layout.nextX() + layout.controlH() <= w, layout.toString());
            double fraction = (layout.radius() * 2.0) / Math.min(w, h);
            assertTrue(fraction >= 0.62, window[0] + "x" + window[1] + " s" + scale + " fraction " + fraction + " r=" + layout.radius());
        }
        // 1280×720 auto is a short logical GUI. The old 104px cap left a small ring there.
        int autoW = WheelLayout.logicalWidth(1280, 720, 0);
        int autoH = WheelLayout.logicalHeight(1280, 720, 0);
        assertTrue(WheelLayout.of(autoW, autoH).radius() >= 80);
        assertTrue(WheelLayout.of(1280, 720).radius() > 240);
        assertEquals(WheelLayout.effectiveScale(854, 480, 4), WheelLayout.effectiveScale(854, 480, 2));
    }

    @Test void configTooltipStaysOutOfThePreview() {
        for (int[] size : new int[][]{{854, 480}, {1280, 720}, {1920, 1080}, {320, 240}, {427, 240}, {640, 360}}) {
            var geo = ConfigLayout.geometry(size[0], size[1]);
            var anchor = new ConfigLayout.Box(geo.left(), geo.top(), geo.optionWidth(), 20);
            var tip = ConfigLayout.placeTooltip(size[0], size[1], anchor, geo.preview(), 360, 48);
            assertFalse(tip.intersects(geo.preview()), tip + " vs " + geo.preview());
            assertTrue(tip.right() <= geo.previewLeft(), tip.toString());
            assertTrue(tip.x() >= 4 && tip.bottom() <= size[1], tip.toString());
        }
        var standing = ConfigLayout.preview("quarkos_emotes:wave", 200, 180);
        var meditate = ConfigLayout.preview("quarkos_emotes:meditate", 200, 180);
        var pushups = ConfigLayout.preview("quarkos_emotes:pushups", 200, 180);
        assertTrue(meditate.scale() < standing.scale());
        assertTrue(pushups.scale() <= meditate.scale());
        // A negative offset lifts a seated skeleton; a larger offset sinks it out of the panel.
        assertTrue(meditate.yOffset() < 0f);
        assertTrue(meditate.yOffset() < standing.yOffset());
        assertTrue(pushups.yaw() > 60f && meditate.yaw() > 0f);
        assertTrue(standing.yOffset() > 0 && standing.yaw() == 0f);
    }
}
