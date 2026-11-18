package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class FlatButton extends Button {
    protected final Font font;
    private final boolean primary;

    public FlatButton(Font font, Box box, Component label, Runnable action) {
        this(font, box, label, false, action);
    }

    public FlatButton(Font font, Box box, Component label, boolean primary, Runnable action) {
        super(box.x(), box.y(), box.w(), box.h(), label, button -> action.run(), DEFAULT_NARRATION);
        this.font = font;
        this.primary = primary;
    }

    protected Box box() {
        return new Box(getX(), getY(), getWidth(), getHeight());
    }

    protected int edge() {
        return isFocused() ? Ui.ACCENT : active && isHovered ? Ui.HOVER : Ui.BORDER;
    }

    protected void paint(GuiGraphics g) {
        int fill = primary && active ? (isHovered ? 0xffffffff : Ui.TEXT) : active && isHovered ? Ui.RAISED : Ui.CONTROL;
        int text = primary && active ? Ui.GROUND : active ? Ui.TEXT : Ui.MUTED;
        Ui.panel(g, box(), fill, edge());
        Ui.center(g, font, getMessage(), new Box(getX() + 4, getY(), getWidth() - 8, getHeight()), text);
    }

    //? if >=1.21.11 {
    @Override protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partial) {
        paint(g);
    }
    //?} else {
    /*@Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        paint(g);
    }
    *///?}
}
