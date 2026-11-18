package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

public final class Slider extends AbstractSliderButton {
    private final Component label;
    private final DoubleConsumer change;

    public Slider(Box box, Component label, double value, DoubleConsumer change) {
        super(box.x(), box.y(), box.w(), box.h(), label, value);
        this.label = label;
        this.change = change;
        updateMessage();
    }

    @Override protected void updateMessage() {
        setMessage(label.copy().append(" · " + percent()));
    }

    @Override protected void applyValue() {
        change.accept(value);
    }

    public String percent() {
        return Math.round(value * 100) + "%";
    }

    @Override public void renderWidget(GuiGraphics g, int x, int y, float partial) {
        Box box = new Box(getX(), getY(), getWidth(), getHeight());
        int centerY = box.y() + box.h() / 2;
        int left = box.x() + 4;
        int right = box.right() - 4;
        int knob = left + (int) Math.round((right - left) * value);
        Ui.panel(g, box, Ui.WELL, isFocused() ? Ui.ACCENT : isHovered ? Ui.HOVER : Ui.BORDER);
        Ui.softRect(g, new Box(left, centerY - 2, right - left, 4), Ui.BORDER);
        Ui.softRect(g, new Box(left, centerY - 2, knob - left, 4), Ui.ACCENT);
        Ui.softRect(g, new Box(knob - 3, centerY - 4, 7, 8), Ui.TEXT);
    }
}
