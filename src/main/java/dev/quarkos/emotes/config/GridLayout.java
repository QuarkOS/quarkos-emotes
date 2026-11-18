package dev.quarkos.emotes.config;

import dev.quarkos.emotes.config.ConfigLayout.Box;

/** Geometry and pixel scrolling for the library and its selected-emote panel. */
public record GridLayout(Box header, Box search, Box count, Box filters, Box library, Box detail, Box assignment, Cards cards) {
    public static GridLayout of(int width, int height) {
        Box header = WheelLayout.header(width, height);
        int gap = height < 370 ? 6 : 9;
        int y = header.bottom() + gap;
        int countWidth = Math.min(112, header.w() / 4);
        Box search = new Box(header.x(), y, header.w() - countWidth - gap, 20);
        Box count = new Box(search.right() + gap, y, countWidth, search.h());
        Box filters = new Box(header.x(), search.bottom() + gap, header.w(), 17);
        int top = filters.bottom() + gap + 1;
        int slotHeight = height < 370 ? 22 : 28;
        int bottom = Math.min(height - header.y(), top + 600);
        Box assignment = new Box(header.x(), bottom - slotHeight, header.w(), slotHeight);
        int bodyHeight = assignment.y() - 14 - gap - top;
        int detailWidth = Math.clamp(header.w() / 3, 128, 340);
        Box detail = new Box(header.right() - detailWidth, top, detailWidth, bodyHeight);
        Box library = new Box(header.x(), top, detail.x() - gap - header.x(), bodyHeight);
        return new GridLayout(header, search, count, filters, library, detail, assignment, Cards.of(library));
    }

    public Box filter(int index) {
        int[] edges = {0, 8, 25, 42, 55, 67, 77, 100};
        int width = Math.min(filters.w(), 500);
        int left = width * edges[index] / 100;
        int right = width * edges[index + 1] / 100;
        return new Box(filters.x() + left, filters.y(), right - left - 4, 17);
    }

    public Detail controls() {
        int inset = detail.w() >= 220 ? 10 : 6;
        int x = detail.x() + inset;
        int w = detail.w() - inset * 2;
        int buttonHeight = detail.h() >= 200 ? 22 : 18;
        Box actions = new Box(x, detail.bottom() - inset - buttonHeight, w, buttonHeight);
        Box preview = new Box(x, detail.y() + 32, w, actions.y() - detail.y() - 36);
        return new Detail(preview, actions);
    }

    public Box slot(int index) {
        int gap = 4;
        int available = assignment.w() - (EmoteConfig.SLOT_COUNT - 1) * gap;
        int x = assignment.x() + available * index / EmoteConfig.SLOT_COUNT + index * gap;
        int right = assignment.x() + available * (index + 1) / EmoteConfig.SLOT_COUNT + index * gap;
        return new Box(x, assignment.y(), right - x, assignment.h());
    }

    public record Detail(Box preview, Box actions) {}

    public record Cards(Box viewport, Box track, int columns, int cardHeight, int gap) {
        public static Cards of(Box panel) {
            int inset = 5;
            int gap = 6;
            int cardArea = panel.w() - inset * 2 - 9;
            int columns = Math.max(1, Math.min(6, (cardArea + gap) / (72 + gap)));
            int nominalWidth = (cardArea - (columns - 1) * gap) / columns;
            int cardHeight = Math.clamp(nominalWidth + 5, 70, 119);
            return new Cards(new Box(panel.x() + inset, panel.y() + inset, cardArea, panel.h() - inset * 2),
                    new Box(panel.right() - 8, panel.y() + inset, 4, panel.h() - inset * 2), columns, cardHeight, gap);
        }

        public int pitch() {
            return cardHeight + gap;
        }

        public int contentHeight(int count) {
            return Math.max(0, Math.ceilDiv(count, columns) * pitch() - gap);
        }

        public int maxScroll(int count) {
            return Math.max(0, contentHeight(count) - viewport.h());
        }

        public int clampScroll(int offset, int count) {
            return Math.clamp(offset, 0, maxScroll(count));
        }

        public Box card(int index, int offset) {
            int col = index % columns;
            int available = viewport.w() - (columns - 1) * gap;
            int x = viewport.x() + available * col / columns + col * gap;
            int right = viewport.x() + available * (col + 1) / columns + col * gap;
            return new Box(x, viewport.y() + index / columns * pitch() - offset, right - x, cardHeight);
        }

        public int firstVisible(int offset) {
            return offset / pitch() * columns;
        }

        public int endVisible(int offset, int count) {
            return Math.min(count, Math.ceilDiv(offset + viewport.h(), pitch()) * columns);
        }

        public int cardAt(double x, double y, int offset, int count) {
            if (!viewport.contains(x, y)) return -1;
            for (int i = firstVisible(offset), end = endVisible(offset, count); i < end; i++) {
                if (card(i, offset).contains(x, y)) return i;
            }
            return -1;
        }

        public int reveal(int index, int offset, int count) {
            int top = index / columns * pitch();
            int bottom = top + cardHeight;
            return clampScroll(Math.min(top, Math.max(offset, bottom - viewport.h())), count);
        }

        public Box thumb(int offset, int count) {
            int max = maxScroll(count);
            if (max == 0) return track;
            int size = Math.clamp((int) Math.round(track.h() * (double) viewport.h() / contentHeight(count)), 12, track.h());
            int travel = track.h() - size;
            return new Box(track.x(), track.y() + (int) Math.round(travel * (double) offset / max), track.w(), size);
        }

        public int offsetAt(double thumbY, int count) {
            int travel = track.h() - thumb(0, count).h();
            if (travel <= 0) return 0;
            double fraction = Math.clamp((thumbY - track.y()) / travel, 0, 1);
            return (int) Math.round(fraction * maxScroll(count));
        }
    }
}
