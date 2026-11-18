package dev.quarkos.emotes.config;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LayoutTest {
    @Test void wheelFillsLogicalScreensFromWindowAndGuiScale() {
        int[][] windows = {{320, 240}, {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}, {2560, 1440}};
        for (int[] window : windows) for (int scale : new int[]{1, 2, 3, 4, 0}) {
            int w = WheelLayout.logicalWidth(window[0], window[1], scale);
            int h = WheelLayout.logicalHeight(window[0], window[1], scale);
            var layout = WheelLayout.of(w, h);
            var header = WheelLayout.header(w, h);
            var screen = new Box(0, 0, w, h);
            var toggle = WheelLayout.viewToggle(header, w);
            var settings = WheelLayout.settings(toggle);
            inside(settings, header);
            assertTrue(settings.right() < toggle.x());
            assertTrue(settings.right() <= w - 160, settings + " intersects the movement tutorial toast");
            assertTrue(layout.radius() >= 36, layout.toString());
            assertTrue(layout.cy() - layout.radius() > header.bottom(), layout.toString());
            assertTrue(layout.cy() + layout.radius() < layout.hint().y(), layout.toString());
            assertTrue(layout.hint().bottom() < layout.pager().y(), layout.toString());
            assertTrue(layout.hint().bottom() <= h - 22);
            assertTrue(layout.cx() - layout.radius() >= 0 && layout.cx() + layout.radius() <= w);
            assertFalse(layout.pager().intersects(layout.scope()));
            inside(layout.pager(), screen);
            inside(layout.scope(), screen);
            inside(layout.hint(), screen);
            assertTrue(layout.radius() * 2.0 / Math.min(w, h) >= .62, layout.toString());
            for (int slot = 0; slot < WheelLayout.SLOTS; slot++) {
                var wedge = layout.wedge(slot);
                assertTrue(wedge.preview().w() >= 18 && wedge.preview().h() >= 21, wedge.toString());
                assertTrue(wedge.markers().w() >= WheelLayout.BADGE_WIDTH + 2 + WheelLayout.STAR_WIDTH);
                assertTrue(wedge.markers().bottom() <= wedge.preview().y());
                assertTrue(wedge.preview().bottom() <= wedge.name().y());
                for (Box box : new Box[]{wedge.markers(), wedge.preview(), wedge.name()}) {
                    if (box.w() == 0) continue;
                    for (int slide : new int[]{-layout.slideDistance(), layout.slideDistance()}) {
                        insideWedge(layout, slot, new Box(box.x() + slide, box.y(), box.w(), box.h()));
                    }
                }
            }
        }
        assertTrue(WheelLayout.of(WheelLayout.logicalWidth(1280, 720, 0), WheelLayout.logicalHeight(1280, 720, 0)).radius() >= 80);
        assertTrue(WheelLayout.of(1280, 720).radius() > 240);
        assertEquals(2, WheelLayout.effectiveScale(854, 480, 4));
        assertEquals(4, WheelLayout.effectiveScale(1920, 1080, 4));
        var scale4 = WheelLayout.of(480, 270);
        assertTrue(scale4.wedge(0).name().w() >= 24); // Wave
        assertTrue(scale4.wedge(2).name().w() >= 21); // Clap
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

    @Test void configHelpHasItsOwnLineAndSegmentsFitChoiceLabels() {
        for (int[] size : new int[][]{{320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1280, 720}, {1920, 1080}}) {
            var geo = ConfigLayout.geometry(size[0], size[1]);
            var screen = new Box(0, 0, size[0], size[1]);
            var regions = new Box[]{geo.header(), geo.playback(), geo.sound(), geo.wheel(), geo.preview()};
            for (int i = 0; i < regions.length; i++) {
                inside(regions[i], screen);
                for (int j = i + 1; j < regions.length; j++) assertFalse(regions[i].intersects(regions[j]));
            }
            assertEquals(geo.wheel().bottom(), geo.preview().bottom());
            assertTrue(geo.playback().bottom() < geo.sound().y());
            assertTrue(geo.sound().bottom() < geo.wheel().y());
            inside(geo.help(), geo.wheel().inset(1));
            assertTrue(geo.row(geo.wheel(), 2).bottom() < geo.help().y());
            // Camera / Arms / Off, Hold / Click and Favourites / All in the vanilla font.
            choicesFit(geo.row(geo.playback(), 0), 57, 36, 24, 16);
            choicesFit(geo.row(geo.wheel(), 0), 53, 21, 22);
            choicesFit(geo.row(geo.wheel(), 1), 58, 54, 12);
        }
        var row = new Box(16, 20, 186, 16);
        choicesFit(row, 120, 70, 45, 19);
        var narrow = ConfigLayout.choices(row, 120, 500, 200, 13);
        inside(narrow.track(), row);
        int x = narrow.track().x() + 1;
        for (Box segment : narrow.segments()) {
            inside(segment, narrow.track().inset(1));
            assertEquals(x, segment.x());
            x = segment.right();
        }
        assertEquals(narrow.track().right() - 1, x);
        assertTrue(narrow.segments().getLast().w() - 4 >= 13);
    }

    @Test void previewFramingAdaptsToSeatedAndPronePoses() {
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

    private static void insideWedge(WheelLayout wheel, int slot, Box box) {
        for (int x : new int[]{box.x(), box.right()}) for (int y : new int[]{box.y(), box.bottom()}) {
            assertEquals(slot, wheel.slotAt(x, y), box.toString());
        }
        int closestX = Math.clamp(wheel.cx(), box.x(), box.right());
        int closestY = Math.clamp(wheel.cy(), box.y(), box.bottom());
        assertEquals(slot, wheel.slotAt(closestX, closestY), box.toString());
    }

    private static void choicesFit(Box row, int rowLabelWidth, int... widths) {
        var choices = ConfigLayout.choices(row, rowLabelWidth, widths);
        inside(choices.track(), row);
        assertEquals(row.right(), choices.track().right());
        int x = choices.track().x() + 1;
        for (int i = 0; i < widths.length; i++) {
            Box segment = choices.segments().get(i);
            inside(segment, choices.track().inset(1));
            assertEquals(x, segment.x());
            assertTrue(segment.w() - 4 >= widths[i], segment.toString());
            x = segment.right();
        }
        assertEquals(choices.track().right() - 1, x);
    }

    private static void inside(Box box, Box bounds) {
        assertTrue(box.w() > 0 && box.h() > 0, box.toString());
        assertTrue(box.x() >= bounds.x() && box.y() >= bounds.y()
                && box.right() <= bounds.right() && box.bottom() <= bounds.bottom(), box + " outside " + bounds);
    }
}
