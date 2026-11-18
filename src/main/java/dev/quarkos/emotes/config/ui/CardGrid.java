package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.config.ConfigLayout.Box;
import dev.quarkos.emotes.config.GridLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
//? if >=26.3 {
/*import com.mojang.blaze3d.platform.InputConstants;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}

import java.util.function.IntConsumer;

public final class CardGrid extends AbstractWidget {
    @FunctionalInterface
    public interface CardPainter {
        void render(GuiGraphics graphics, int index, Box card, boolean hovered, boolean focused, float partial);
    }

    private final GridLayout.Cards layout;
    private final CardPainter painter;
    private final IntConsumer select;
    private final IntConsumer favorite;
    private final Runnable playSelected;
    private int count;
    private int selected = -1;
    private double scroll;
    private double thumbGrab = -1;

    public CardGrid(GridLayout geometry, CardPainter painter, IntConsumer select, IntConsumer favorite, Runnable playSelected) {
        super(geometry.library().x(), geometry.library().y(), geometry.library().w(), geometry.library().h(),
                Ui.text("library"));
        this.layout = geometry.cards();
        this.painter = painter;
        this.select = select;
        this.favorite = favorite;
        this.playSelected = playSelected;
    }

    public void update(int count, int selected, Component name, boolean resetScroll) {
        this.count = count;
        this.selected = selected;
        setMessage(name);
        scroll = resetScroll ? 0 : layout.clampScroll(offset(), count);
        active = count > 0;
        if (count == 0) releaseScrollbar();
    }

    public int offset() {
        return (int) Math.round(scroll);
    }

    public void setOffset(int offset) {
        scroll = layout.clampScroll(offset, count);
    }

    public void showSelection() {
        if (selected >= 0) scroll = layout.reveal(selected, offset(), count);
    }

    public void releaseScrollbar() {
        thumbGrab = -1;
    }

    public int cardAt(double x, double y) {
        return layout.cardAt(x, y, offset(), count);
    }

    @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int offset = offset();
        int hovered = cardAt(mouseX, mouseY);
        Box viewport = layout.viewport();
        Ui.clip(g, viewport);
        try {
            for (int index = layout.firstVisible(offset), end = layout.endVisible(offset, count); index < end; index++) {
                Box box = layout.card(index, offset);
                if (box.intersects(viewport)) painter.render(g, index, box, index == hovered, isFocused() && index == selected, partial);
            }
        } finally {
            g.disableScissor();
        }
        if (layout.maxScroll(count) > 0) {
            Box track = layout.track();
            boolean renderingThumb = thumbGrab >= 0 || scrollbarArea().contains(mouseX, mouseY);
            Ui.softRect(g, track, Ui.WELL);
            Ui.softRect(g, layout.thumb(offset, count), renderingThumb ? Ui.MUTED : Ui.HOVER);
        }
    }

    private Box scrollbarArea() {
        Box track = layout.track();
        return new Box(track.x() - 3, track.y(), track.w() + 6, track.h());
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        int max = layout.maxScroll(count);
        if (!isMouseOver(x, y) || vertical == 0 || max == 0) return false;
        scroll = Math.clamp(scroll - vertical * layout.pitch() / 3, 0, max);
        return true;
    }

    private void click(double x, double y) {
        if (layout.maxScroll(count) > 0 && scrollbarArea().contains(x, y)) {
            Box thumb = layout.thumb(offset(), count);
            thumbGrab = y >= thumb.y() && y < thumb.bottom() ? y - thumb.y() : thumb.h() / 2.0;
            scroll = layout.offsetAt(y - thumbGrab, count);
            return;
        }
        int index = cardAt(x, y);
        if (index < 0) return;
        if (StarToggle.cardBounds(layout.card(index, offset())).contains(x, y)) favorite.accept(index);
        else select.accept(index);
    }

    private void drag(double y) {
        if (thumbGrab >= 0) scroll = layout.offsetAt(y - thumbGrab, count);
    }

    private boolean key(int key) {
        if (count == 0) return false;
        if (key == GLFW.GLFW_KEY_F && selected >= 0) {
            favorite.accept(selected);
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) && selected >= 0) {
            playSelected.run();
            return true;
        }
        int step = switch (key) {
            case GLFW.GLFW_KEY_LEFT -> -1;
            case GLFW.GLFW_KEY_RIGHT -> 1;
            case GLFW.GLFW_KEY_UP -> -layout.columns();
            case GLFW.GLFW_KEY_DOWN -> layout.columns();
            case GLFW.GLFW_KEY_PAGE_UP -> -Math.max(1, layout.viewport().h() / layout.pitch()) * layout.columns();
            case GLFW.GLFW_KEY_PAGE_DOWN -> Math.max(1, layout.viewport().h() / layout.pitch()) * layout.columns();
            default -> 0;
        };
        if (step == 0) return false;
        int next = Math.clamp(Math.max(0, selected) + step, 0, count - 1);
        select.accept(next);
        showSelection();
        return true;
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    //? if >=1.21.11 {
    @Override public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean twice) { click(event.x(), event.y()); }
    @Override protected void onDrag(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) { drag(event.y()); }
    @Override public void onRelease(net.minecraft.client.input.MouseButtonEvent event) { releaseScrollbar(); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) { return key(event.key()); }
    //?} else {
    /*@Override public void onClick(double x, double y) { click(x, y); }
    @Override protected void onDrag(double x, double y, double dx, double dy) { drag(y); }
    @Override public void onRelease(double x, double y) { releaseScrollbar(); }
    @Override public boolean keyPressed(int key, int scan, int mods) { return key(key); }
    *///?}
}
