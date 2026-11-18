package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class StarToggle extends FlatButton {
    public static final int SIZE = 15;
    private final BooleanSupplier starred;

    public StarToggle(Font font, Box box, BooleanSupplier starred, Runnable action) {
        super(font, box, Ui.tr("favorite"), action);
        this.starred = starred;
    }

    @Override public Component getMessage() {
        return Ui.tr(starred.getAsBoolean() ? "unfavorite" : "favorite");
    }

    public static Box cardBounds(Box card) {
        return new Box(card.right() - SIZE - 3, card.y() + 3, SIZE, SIZE);
    }

    public static void drawStar(GuiGraphics g, Font font, Box box, boolean starred, boolean hovered) {
        Ui.softRect(g, box, hovered ? Ui.RAISED : Ui.WELL);
        glyph(g, font, box, starred);
    }

    public static void glyph(GuiGraphics g, Font font, Box box, boolean starred) {
        Ui.center(g, font, Component.literal(starred ? "★" : "☆"), box, starred ? Ui.ACCENT : Ui.MUTED);
    }

    @Override protected void paint(GuiGraphics g) {
        Box box = box();
        boolean on = starred.getAsBoolean();
        Ui.panel(g, box, isHovered ? Ui.RAISED : Ui.CONTROL, edge());
        if (box.w() < 74) {
            glyph(g, font, box, on);
        } else {
            glyph(g, font, new Box(box.x() + 5, box.y(), SIZE, box.h()), on);
            Ui.label(g, font, Ui.tr(on ? "favorited" : "favorite"),
                    new Box(box.x() + 22, box.y(), box.w() - 26, box.h()), Ui.TEXT);
        }
    }
}
