package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import dev.quarkos.emotes.config.ConfigLayout.Choices;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public final class Segmented {
    private final Box track;

    public Segmented(Font font, Box track, List<Component> options, IntSupplier value, IntConsumer select,
                     Consumer<FlatButton> add) {
        this(font, track, options, value, select, add, index -> equalSegment(track, index, options.size()));
    }

    public Segmented(Font font, Choices layout, List<Component> options, IntSupplier value, IntConsumer select,
                     Consumer<FlatButton> add) {
        this(font, layout.track(), options, value, select, add, layout.segments()::get);
    }

    private Segmented(Font font, Box track, List<Component> options, IntSupplier value, IntConsumer select,
                      Consumer<FlatButton> add, IntFunction<Box> bounds) {
        this.track = track;
        for (int i = 0; i < options.size(); i++) {
            int index = i;
            Box box = bounds.apply(i);
            add.accept(new FlatButton(font, box, options.get(i), () -> select.accept(index)) {
                @Override protected void paint(GuiGraphics g) {
                    boolean selected = value.getAsInt() == index;
                    if (selected || isHovered || isFocused()) Ui.softRect(g, box, selected ? Ui.RAISED : Ui.CONTROL);
                    if (isFocused()) g.fill(box.x() + 2, box.y(), box.right() - 2, box.y() + 1, Ui.ACCENT);
                    Ui.center(g, font, getMessage(), new Box(box.x() + 2, box.y(), box.w() - 4, box.h()),
                            selected || isHovered || isFocused() ? Ui.TEXT : Ui.MUTED);
                }
            });
        }
    }

    private static Box equalSegment(Box track, int index, int count) {
        int x = track.x() + 1 + index * (track.w() - 2) / count;
        int right = track.x() + 1 + (index + 1) * (track.w() - 2) / count;
        return new Box(x, track.y() + 1, right - x, track.h() - 2);
    }

    public void render(GuiGraphics g) {
        Ui.panel(g, track, Ui.WELL, Ui.BORDER);
    }
}
