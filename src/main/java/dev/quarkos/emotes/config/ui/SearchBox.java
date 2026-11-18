package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class SearchBox extends EditBox {
    private final Box frame;
    private final Font font;

    public SearchBox(Font font, Box frame, Component hint) {
        super(font, frame.x() + 19, frame.y(), frame.w() - 25, frame.h(), hint);
        this.frame = frame;
        this.font = font;
        setBordered(false);
        setTextColor(Ui.TEXT);
        setTextColorUneditable(Ui.MUTED);
        setHint(hint);
        setMaxLength(80);
    }

    @Override public boolean isMouseOver(double x, double y) {
        return active && visible && frame.contains(x, y);
    }

    @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        Ui.panel(g, frame, Ui.WELL, isFocused() ? Ui.ACCENT : isHovered ? Ui.HOVER : Ui.BORDER);
        int x = frame.x() + 7;
        int y = frame.y() + frame.h() / 2 - 4;
        g.fill(x + 1, y, x + 5, y + 1, Ui.MUTED);
        g.fill(x, y + 1, x + 1, y + 5, Ui.MUTED);
        g.fill(x + 5, y + 1, x + 6, y + 5, Ui.MUTED);
        g.fill(x + 1, y + 5, x + 5, y + 6, Ui.MUTED);
        g.fill(x + 6, y + 6, x + 7, y + 7, Ui.MUTED);
        g.fill(x + 7, y + 7, x + 8, y + 8, Ui.MUTED);
        // Borderless EditBox starts at its raw Y coordinate in both versions.
        int textShift = (frame.h() - font.lineHeight) / 2;
        Ui.clip(g, new Box(getX(), frame.y(), getWidth(), frame.h()));
        Ui.push(g, 0, textShift, 1);
        try {
            super.renderWidget(g, mouseX, mouseY - textShift, partial);
        } finally {
            Ui.pop(g);
            g.disableScissor();
        }
    }
}
