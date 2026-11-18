package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class Chip extends FlatButton {
    private final BooleanSupplier selected;
    private Component secondLine;

    public Chip(Font font, Box box, Component title, BooleanSupplier selected, Runnable action) {
        super(font, box, title, action);
        this.selected = selected;
    }

    public void setSecondLine(Component secondLine) {
        this.secondLine = secondLine;
    }

    @Override protected void paint(GuiGraphics g) {
        boolean chosen = this.selected.getAsBoolean();
        Box box = box();
        Ui.panel(g, box, chosen ? Ui.SELECTED : isHovered && active ? Ui.RAISED : Ui.PANEL,
                chosen || isFocused() ? Ui.ACCENT : edge());
        int text = active ? Ui.TEXT : Ui.MUTED;
        if (secondLine == null) {
            Ui.center(g, font, getMessage(), new Box(box.x() + 3, box.y(), box.w() - 6, box.h()), text);
        } else {
            int y = box.y() + (box.h() - 20) / 2;
            Ui.center(g, font, getMessage(), new Box(box.x() + 2, y, box.w() - 4, 10), text);
            Ui.center(g, font, secondLine, new Box(box.x() + 2, y + 10, box.w() - 4, 10), Ui.MUTED);
        }
    }
}
