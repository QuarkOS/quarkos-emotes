package dev.quarkos.emotes.config;

import dev.quarkos.emotes.EmoteResources;
import dev.quarkos.emotes.EmotesClient;
import dev.quarkos.emotes.animation.Emote;
import dev.quarkos.emotes.animation.EmoteFiles;
import dev.quarkos.emotes.config.ConfigLayout.Box;
import dev.quarkos.emotes.config.EmoteConfig.MenuView;
import dev.quarkos.emotes.config.ui.FlatButton;
import dev.quarkos.emotes.config.ui.Segmented;
import dev.quarkos.emotes.config.ui.Ui;
import dev.quarkos.emotes.config.ui.WheelRing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class EmoteWheelScreen extends Screen {
    private MenuView view;
    private boolean openingKeyDown;
    private boolean releaseToPlay;
    private List<Emote> registry = List.of();
    private List<Emote> ordered = List.of();

    private Box titleBox;
    private Segmented views;
    private WheelLayout wheel;
    private WheelRing ring;
    private Segmented wheelScope;
    private FlatButton previousPage, nextPage;
    private List<Emote> wheelItems = List.of();
    private int page, pageDirection;
    private double scrollRemainder;
    private long pageChanged;

    private EmoteLibrary library;

    /** Fixed highlight for captures where the window cannot warp the cursor. */
    public int harnessHover = -1;

    public EmoteWheelScreen() {
        this(false);
    }

    public EmoteWheelScreen(boolean openingKeyDown) {
        this(EmoteConfig.get().menuView);
        this.openingKeyDown = openingKeyDown;
        this.releaseToPlay = openingKeyDown && view == MenuView.WHEEL;
    }

    public EmoteWheelScreen(MenuView initialView) {
        super(Component.translatable("screen.quarkos_emotes.wheel"));
        this.view = initialView;
    }

    @Override protected void init() {
        Box header = WheelLayout.header(width, height);
        int modeWidth = header.w() < 400 ? 93 : 116;
        Box mode = new Box(width / 2 - modeWidth / 2, header.y(), modeWidth, header.h());
        titleBox = new Box(header.x(), header.y(), mode.x() - header.x() - 8, header.h());
        views = new Segmented(font, mode, List.of(Ui.text("wheel"), Ui.text("grid")), view::ordinal,
                this::changeView, this::addRenderableWidget);
        addRenderableWidget(new FlatButton(font, new Box(header.right() - 74, header.y(), 74, header.h()), Ui.text("settings"),
                () -> minecraft.setScreen(new EmoteConfigScreen(this))));

        if (view == MenuView.WHEEL) {
            wheel = WheelLayout.of(width, height);
            ring = new WheelRing(wheel);
            wheelScope = new Segmented(font, wheel.scope(), List.of(Ui.text("favorites"), Ui.text("all")),
                    () -> EmoteConfig.get().wheelFavoritesOnly ? 0 : 1, this::changeScope, this::addRenderableWidget);
            Box pager = wheel.pager();
            previousPage = addRenderableWidget(pageArrow(new Box(pager.x(), pager.y(), 20, pager.h()), -1));
            nextPage = addRenderableWidget(pageArrow(new Box(pager.right() - 20, pager.y(), 20, pager.h()), 1));
        } else {
            if (library == null) library = new EmoteLibrary(font, minecraft, this::play, this::setFocused);
            library.init(GridLayout.of(width, height), this::addRenderableWidget);
        }
        refreshCatalog(EmoteResources.REGISTRY.all());
        clearFocus();
    }

    @Override protected void setInitialFocus() {}

    private FlatButton pageArrow(Box box, int step) {
        return new FlatButton(font, box, Ui.text(step < 0 ? "previous_page" : "next_page"), () -> changePage(step)) {
            @Override protected void paint(GuiGraphics g) {
                Ui.panel(g, box, isHovered && active ? Ui.RAISED : Ui.CONTROL, edge());
                Ui.center(g, font, Component.literal(step < 0 ? "‹" : "›"), box, active ? Ui.TEXT : Ui.MUTED);
            }
        };
    }

    private void changeView(int index) {
        MenuView next = MenuView.values()[index];
        if (next == view) return;
        releaseToPlay = false;
        view = next;
        EmoteConfig.get().menuView = next;
        EmoteConfig.save();
        rebuildWidgets();
    }

    private void changeScope(int index) {
        boolean favoritesOnly = index == 0;
        if (EmoteConfig.get().wheelFavoritesOnly == favoritesOnly) return;
        EmoteConfig.get().wheelFavoritesOnly = favoritesOnly;
        EmoteConfig.save();
        page = 0;
        refreshWheel();
    }

    private void refreshCatalog(List<Emote> definitions) {
        registry = definitions;
        ordered = EmoteCatalog.ordered(definitions);
        if (view == MenuView.WHEEL) refreshWheel();
        else library.setCatalog(ordered);
    }

    private void refreshWheel() {
        var config = EmoteConfig.get();
        wheelItems = EmoteCatalog.wheel(ordered, config.favorites, config.wheelFavoritesOnly);
        page = WheelLayout.clampPage(page, wheelItems.size());
        previousPage.active = nextPage.active = WheelLayout.pageCount(wheelItems.size()) > 1;
    }

    private void changePage(int step) {
        int next = WheelLayout.wrapPage(page, step, wheelItems.size());
        if (next == page) return;
        page = next;
        pageDirection = Integer.signum(step);
        pageChanged = System.nanoTime();
    }

    private Emote atSlot(int slot) {
        int index = WheelLayout.slotIndex(page, slot, wheelItems.size());
        return index < 0 ? null : wheelItems.get(index);
    }

    private void play(Emote emote) {
        if (emote == null || minecraft.player == null || EmoteResources.REGISTRY.get(emote.id()) == null) return;
        minecraft.setScreen(null);
        EmotesClient.play(emote.id());
    }

    public static Component label(Emote emote) {
        if (I18n.exists(emote.name())) return Component.translatable(emote.name());
        String name = emote.name();
        if (name.startsWith("emote.") || name.startsWith("quarkos_emotes.") || name.contains(":")) {
            name = EmoteFiles.title(emote.id().substring(emote.id().indexOf(':') + 1));
        }
        return Component.literal(name);
    }

    @Override public void tick() {
        List<Emote> definitions = EmoteResources.REGISTRY.all();
        if (!definitions.equals(registry)) refreshCatalog(definitions);
        if (view == MenuView.GRID) library.tick();
    }

    // The screen draws its own scrim in render on both versions.
    @Override public void renderBackground(GuiGraphics g, int x, int y, float partial) {}

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, minecraft.level == null ? Ui.GROUND : Ui.SCRIM);
        Ui.layer(g);
        float titleScale = titleBox.w() >= 145 ? 1.45f : 1.18f;
        Ui.heading(g, font, title, titleBox, titleScale, false);
        views.render(g);
        if (view == MenuView.WHEEL) renderWheel(g, mouseX, mouseY);
        else library.renderPanels(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        if (view == MenuView.GRID) library.renderTooltip(g, mouseX, mouseY);
    }

    private void renderWheel(GuiGraphics g, int mouseX, int mouseY) {
        int hover = harnessHover >= 0 ? harnessHover : wheel.slotAt(mouseX, mouseY);
        Emote hovered = atSlot(hover);
        float phase = 1 - Math.clamp((System.nanoTime() - pageChanged) / 140_000_000f, 0f, 1f);
        float slide = pageDirection * phase * phase * Math.min(9, wheel.radius() * .055f);
        ring.render(g, font, wheelItems, page, hover, slide, EmoteConfig.get().favorites::contains);
        int span = (int) (wheel.innerRadius() * 1.8);
        int cx = wheel.cx(), cy = wheel.cy();
        Component name = hovered != null ? label(hovered) : wheelItems.isEmpty() ? Ui.text("nothing_on_wheel") : Ui.text("pick_emote");
        boolean hold = releaseToPlay && EmoteConfig.get().holdWheel;
        Component help = hovered != null ? Ui.text(hold ? "release_to_play" : "click_to_play")
                : wheelItems.isEmpty() && EmoteConfig.get().wheelFavoritesOnly ? Ui.text("switch_to_all") : Ui.text("number_keys");
        if (hovered != null) {
            EmotesClient.selected = hovered.id();
            if (hold && font.width(help) > span) help = Ui.text("release");
        }
        float nameScale = Math.clamp(wheel.radius() / 77f, 1f, 2.3f);
        Ui.heading(g, font, name, new Box(cx - span / 2, cy - 20, span, 20), nameScale, true);
        if (!wheelItems.isEmpty() || EmoteConfig.get().wheelFavoritesOnly) {
            Ui.center(g, font, help, new Box(cx - span / 2, cy + 3, span, 12), Ui.MUTED);
        }

        wheelScope.render(g);
        renderDots(g);
        Ui.center(g, font, Ui.text("wheel_help"), new Box(8, wheel.helpY(), width - 16, 10), Ui.MUTED);
    }

    private void renderDots(GuiGraphics g) {
        Box pager = wheel.pager();
        int count = WheelLayout.pageCount(wheelItems.size());
        if (count > 8) {
            Ui.center(g, font, Ui.text("page_number", page + 1, count),
                    new Box(pager.x() + 22, pager.y(), pager.w() - 44, pager.h()), Ui.MUTED);
            return;
        }
        int start = pager.x() + pager.w() / 2 - (count * 10 - 4) / 2;
        for (int i = 0; i < count; i++) {
            Ui.softRect(g, new Box(start + i * 10, pager.y() + 7, 6, 5), i == page ? Ui.ACCENT : Ui.HOVER);
        }
    }

    private boolean click(double x, double y, int button) {
        if (view == MenuView.GRID) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) return library.clickOutsideWidgets(x, y);
            return false;
        }
        Emote emote = atSlot(wheel.slotAt(x, y));
        if (emote == null) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            EmoteConfig.get().favorite(emote.id());
            refreshWheel();
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) play(emote);
        else return false;
        return true;
    }

    private boolean openPressed() {
        if (view == MenuView.GRID && library.searchFocused()) return false;
        if (view == MenuView.WHEEL && EmoteConfig.get().holdWheel) return false;
        if (!openingKeyDown) onClose();
        return true;
    }

    private boolean openReleased(double x, double y) {
        openingKeyDown = false;
        if (!releaseToPlay) return false;
        releaseToPlay = false;
        if (view != MenuView.WHEEL || !EmoteConfig.get().holdWheel) return false;
        Emote emote = atSlot(wheel.slotAt(x, y));
        if (emote == null) onClose();
        else play(emote);
        return true;
    }

    private double pointerX() {
        return minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
    }

    private double pointerY() {
        return minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
    }

    private boolean wheelKey(int key) {
        if (view != MenuView.WHEEL) return false;
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8) {
            play(atSlot(key - GLFW.GLFW_KEY_1));
            return true;
        }
        if (key == GLFW.GLFW_KEY_F) return click(pointerX(), pointerY(), GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            changePage(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            changePage(-1);
            return true;
        }
        return false;
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (view == MenuView.GRID) return super.mouseScrolled(x, y, horizontal, vertical);
        if (vertical == 0) return false;
        scrollRemainder -= vertical;
        int step = (int) scrollRemainder;
        if (step != 0) {
            scrollRemainder -= step;
            changePage(step);
        }
        return true;
    }

    //? if >=1.21.11 {
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean twice) {
        MenuView before = view;
        if (super.mouseClicked(e, twice)) {
            if (view != before) clearFocus();
            return true;
        }
        return click(e.x(), e.y(), e.button()) || EmotesClient.OPEN.matchesMouse(e) && openPressed();
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent e, double dx, double dy) {
        return view == MenuView.GRID && library.drag(dx) || super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e) {
        return EmotesClient.OPEN.matchesMouse(e) && openReleased(e.x(), e.y()) ||
                view == MenuView.GRID && e.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && library.endDrag() || super.mouseReleased(e);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
        return EmotesClient.OPEN.matches(e) && openPressed() || wheelKey(e.key()) || super.keyPressed(e);
    }
    @Override public boolean keyReleased(net.minecraft.client.input.KeyEvent e) {
        return EmotesClient.OPEN.matches(e) && openReleased(pointerX(), pointerY()) || super.keyReleased(e);
    }
    //?} else {
    /*@Override public boolean mouseClicked(double x, double y, int button) {
        MenuView before = view;
        if (super.mouseClicked(x, y, button)) {
            if (view != before) clearFocus();
            return true;
        }
        return click(x, y, button) || EmotesClient.OPEN.matchesMouse(button) && openPressed();
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return view == MenuView.GRID && library.drag(dx) || super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        return EmotesClient.OPEN.matchesMouse(button) && openReleased(x, y) ||
                view == MenuView.GRID && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && library.endDrag() || super.mouseReleased(x, y, button);
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        return EmotesClient.OPEN.matches(key, scan) && openPressed() || wheelKey(key) || super.keyPressed(key, scan, mods);
    }
    @Override public boolean keyReleased(int key, int scan, int mods) {
        return EmotesClient.OPEN.matches(key, scan) && openReleased(pointerX(), pointerY()) || super.keyReleased(key, scan, mods);
    }
    *///?}

    @Override public void removed() {
        releaseToPlay = openingKeyDown = false;
        if (library != null) library.endDrag();
    }

    @Override public boolean isPauseScreen() {
        return false;
    }
}
