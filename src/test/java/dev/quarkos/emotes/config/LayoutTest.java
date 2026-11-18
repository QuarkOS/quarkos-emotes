package dev.quarkos.emotes.config;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LayoutTest {
    @Test void wheelFillsLogicalScreensFromWindowAndGuiScale() {
        int[][] windows = {{854, 480}, {960, 540}, {1280, 720}, {1920, 1080}, {2560, 1440}};
        for (int[] window : windows) for (int scale : new int[]{1, 2, 3, 4, 0}) {
            int w = WheelLayout.logicalWidth(window[0], window[1], scale);
            int h = WheelLayout.logicalHeight(window[0], window[1], scale);
            var layout = WheelLayout.of(w, h);
            var header = WheelLayout.header(w, h);
            assertTrue(layout.radius() >= 36, layout.toString());
            assertTrue(layout.cy() - layout.radius() > header.bottom(), layout.toString());
            assertTrue(layout.cy() + layout.radius() < layout.pager().y(), layout.toString());
            assertTrue(layout.cx() - layout.radius() >= 0 && layout.cx() + layout.radius() <= w);
            assertFalse(layout.pager().intersects(layout.scope()));
            inside(layout.pager(), new Box(0, 0, w, h));
            inside(layout.scope(), new Box(0, 0, w, h));
            assertTrue(layout.radius() * 2.0 / Math.min(w, h) >= .62, layout.toString());
        }
        assertTrue(WheelLayout.of(WheelLayout.logicalWidth(1280, 720, 0), WheelLayout.logicalHeight(1280, 720, 0)).radius() >= 80);
        assertTrue(WheelLayout.of(1280, 720).radius() > 240);
        assertEquals(2, WheelLayout.effectiveScale(854, 480, 4));
        assertEquals(4, WheelLayout.effectiveScale(1920, 1080, 4));
    }

    @Test void wedgesStartAtTwelveAndRespectAnnularHitAreaAtEdges() {
        var wheel = WheelLayout.of(480, 270);
        int radius = (wheel.radius() + wheel.innerRadius()) / 2;
        for (int slot = 0; slot < WheelLayout.SLOTS; slot++) {
            double angle = wheel.angle(slot);
            assertEquals(slot, hit(wheel, radius, angle));
            assertEquals(slot, hit(wheel, radius, angle + WheelLayout.STEP / 2 - .001));
            assertEquals((slot + 1) % 8, hit(wheel, radius, angle + WheelLayout.STEP / 2 + .001));
            assertEquals(-1, hit(wheel, wheel.innerRadius() - .1, angle));
            assertEquals(-1, hit(wheel, wheel.radius() + .1, angle));
        }
        assertEquals(0, wheel.slotAt(wheel.cx(), wheel.cy() - wheel.radius()));
        assertEquals(0, wheel.slotAt(wheel.cx(), wheel.cy() - wheel.innerRadius()));
        assertEquals(-1, wheel.slotAt(wheel.cx(), wheel.cy()));
    }

    @Test void wheelPagesWrapAndNeverSelectAnEmptySlot() {
        assertEquals(1, WheelLayout.pageCount(0));
        assertEquals(1, WheelLayout.pageCount(8));
        assertEquals(2, WheelLayout.pageCount(9));
        assertEquals(6, WheelLayout.pageCount(42));
        assertEquals(5, WheelLayout.wrapPage(0, -1, 42));
        assertEquals(0, WheelLayout.wrapPage(5, 1, 42));
        assertEquals(1, WheelLayout.clampPage(5, 9));
        assertEquals(0, WheelLayout.wrapPage(20, 1, 0));
        assertEquals(40, WheelLayout.slotIndex(5, 0, 42));
        assertEquals(41, WheelLayout.slotIndex(5, 1, 42));
        assertEquals(-1, WheelLayout.slotIndex(5, 2, 42));
        assertEquals(-1, WheelLayout.slotIndex(0, 8, 42));
        assertEquals(-1, WheelLayout.slotIndex(0, -1, 42));
        assertEquals(-1, WheelLayout.slotIndex(0, 0, 0));
    }

    @Test void gridFitsScreenAndScrollsThroughEveryCard() {
        for (int[] size : new int[][]{{320, 240}, {427, 240}, {480, 270}, {960, 540}, {1920, 1080}}) {
            GridLayout grid = GridLayout.of(size[0], size[1]);
            var screen = new Box(0, 0, size[0], size[1]);
            var regions = new Box[]{grid.header(), grid.search(), grid.count(), grid.filters(), grid.library(), grid.detail(), grid.assignment()};
            for (int i = 0; i < regions.length; i++) {
                inside(regions[i], screen);
                for (int j = i + 1; j < regions.length; j++) assertFalse(regions[i].intersects(regions[j]));
            }
            assertTrue(grid.controls().preview().h() >= 40);
            inside(grid.controls().preview(), grid.detail());
            inside(grid.controls().actions(), grid.detail());
            assertFalse(grid.controls().preview().intersects(grid.controls().actions()));
            for (int i = 0; i < EmoteConfig.SLOT_COUNT; i++) {
                Box slot = grid.slot(i);
                inside(slot, grid.assignment());
                assertTrue(slot.w() >= 32 && slot.h() >= 20, slot.toString());
                if (i > 0) assertFalse(slot.intersects(grid.slot(i - 1)));
            }
            for (int i = 0; i < EmoteCatalog.Filter.values().length; i++) {
                inside(grid.filter(i), grid.filters());
                if (i > 0) assertFalse(grid.filter(i).intersects(grid.filter(i - 1)));
            }

            var cards = grid.cards();
            inside(cards.viewport(), grid.library());
            inside(cards.track(), grid.library());
            assertFalse(cards.viewport().intersects(cards.track()));
            assertTrue(cards.columns() >= 2);
            assertEquals(0, cards.maxScroll(0));
            assertEquals(0, cards.maxScroll(1));
            int count = 42;
            int max = cards.maxScroll(count);
            assertTrue(max > 0);
            assertEquals(0, cards.clampScroll(-100, count));
            assertEquals(max, cards.clampScroll(max + 100, count));
            assertEquals(cards.track().bottom(), cards.thumb(max, count).bottom());
            assertEquals(cards.track().y(), cards.thumb(0, count).y());
            assertEquals(max, cards.offsetAt(cards.thumb(max, count).y(), count));
            for (int index = 0; index < count; index++) {
                int offset = cards.reveal(index, max, count);
                Box card = cards.card(index, offset);
                inside(card, cards.viewport());
                assertTrue(card.w() >= 72 && card.h() >= 70);
                assertEquals(index, cards.cardAt(card.x() + 1, card.y() + 1, offset, count));
                assertTrue(index >= cards.firstVisible(offset) && index < cards.endVisible(offset, count));
                for (int next = index + 1; next < count; next++) {
                    assertFalse(card.intersects(cards.card(next, offset)));
                }
            }
            assertEquals(count, cards.endVisible(max, count));
            Box last = cards.card(count - 1, max);
            assertEquals(cards.viewport().bottom(), last.bottom());
        }
    }

    @Test void configSectionsAndTooltipsStayOutOfThePreview() {
        for (int[] size : new int[][]{{854, 480}, {1280, 720}, {1920, 1080}, {320, 240}, {427, 240}, {480, 270}, {640, 360}}) {
            var geo = ConfigLayout.geometry(size[0], size[1]);
            var screen = new Box(0, 0, size[0], size[1]);
            for (Box region : new Box[]{geo.header(), geo.playback(), geo.sound(), geo.wheel(), geo.preview()}) {
                inside(region, screen);
            }
            assertEquals(geo.wheel().bottom(), geo.preview().bottom());
            assertTrue(geo.playback().bottom() < geo.sound().y());
            assertTrue(geo.sound().bottom() < geo.wheel().y());
            assertFalse(geo.preview().intersects(geo.playback()));
            for (Box anchor : new Box[]{geo.row(geo.playback(), 0), geo.row(geo.wheel(), 2)}) {
                var tip = ConfigLayout.placeTooltip(size[0], size[1], anchor, geo.preview(), 360, 48);
                assertFalse(tip.intersects(geo.preview()), tip + " vs " + geo.preview());
                inside(tip, screen);
            }
        }
        var standing = ConfigLayout.preview("quarkos_emotes:wave", 200, 180);
        var meditate = ConfigLayout.preview("quarkos_emotes:meditate", 200, 180);
        var pushups = ConfigLayout.preview("quarkos_emotes:pushups", 200, 180);
        assertTrue(meditate.scale() < standing.scale());
        assertTrue(pushups.scale() <= meditate.scale());
        assertTrue(meditate.yOffset() < 0f);
        assertTrue(pushups.yaw() > 60f && meditate.yaw() > 0f);
        assertTrue(standing.yOffset() > 0 && standing.yaw() == 0f);
    }

    private static int hit(WheelLayout wheel, double distance, double angle) {
        return wheel.slotAt(wheel.cx() + distance * Math.cos(angle), wheel.cy() + distance * Math.sin(angle));
    }

    private static void inside(Box box, Box bounds) {
        assertTrue(box.w() > 0 && box.h() > 0, box.toString());
        assertTrue(box.x() >= bounds.x() && box.y() >= bounds.y()
                && box.right() <= bounds.right() && box.bottom() <= bounds.bottom(), box + " outside " + bounds);
    }
}
