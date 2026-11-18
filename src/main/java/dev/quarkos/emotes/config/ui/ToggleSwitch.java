package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class ToggleSwitch extends FlatButton {
    private final Component label;
    private final BooleanSupplier on;

    public ToggleSwitch(Font font, Box box, Component label, BooleanSupplier on, Runnable action) {
        super(font, box, label, action);
        this.label = label;
        this.on = on;
    }

    @Override public Component getMessage() {
        return label.copy().append(" · ").append(Component.translatable(on.getAsBoolean() ? "options.on" : "options.off"));
    }

    @Override protected void paint(GuiGraphics g) {
        Box row = box();
        if (isFocused() || isHovered) Ui.softRect(g, row, Ui.RAISED);
        if (isFocused()) g.fill(row.x(), row.y() + 2, row.x() + 1, row.bottom() - 2, Ui.ACCENT);
        int trackX = row.right() - 31;
        int trackY = row.y() + (row.h() - 12) / 2;
        int trackColor = on.getAsBoolean() ? Ui.ACCENT : Ui.HOVER;
        pill(g, trackX, trackY, 28, 12, trackColor);
        pill(g, trackX + (on.getAsBoolean() ? 17 : 3), trackY + 2, 8, 8, Ui.TEXT);
        Ui.label(g, font, label, new Box(row.x() + 3, row.y(), trackX - row.x() - 8, row.h()), Ui.TEXT);
    }

    private static void pill(GuiGraphics g, int x, int y, int width, int height, int color) {
        int radius = height / 2;
        for (int dy = 0; dy < height; dy++) {
            double vertical = dy + .5 - radius;
            int inset = radius - (int) Math.round(Math.sqrt(radius * radius - vertical * vertical));
            g.fill(x + inset, y + dy, x + width - inset, y + dy + 1, color);
        }
    }
}
