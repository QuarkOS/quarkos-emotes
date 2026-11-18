package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class Segmented {
    private final Box track;

    public Segmented(Font font, Box track, List<Component> options, IntSupplier value, IntConsumer select,
                     Consumer<FlatButton> add) {
        this.track = track;
        for (int i = 0; i < options.size(); i++) {
            int index = i;
            int x = track.x() + 1 + i * (track.w() - 2) / options.size();
            int right = track.x() + 1 + (i + 1) * (track.w() - 2) / options.size();
            Box box = new Box(x, track.y() + 1, right - x, track.h() - 2);
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

    public void render(GuiGraphics g) {
        Ui.panel(g, track, Ui.WELL, Ui.BORDER);
    }
}
