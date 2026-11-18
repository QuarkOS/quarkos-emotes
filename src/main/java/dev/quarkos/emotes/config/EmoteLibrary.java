package dev.quarkos.emotes.config;

import dev.quarkos.emotes.EmoteResources;
import dev.quarkos.emotes.EmotesClient;
import dev.quarkos.emotes.animation.Emote;
import dev.quarkos.emotes.animation.EmoteFiles;
import dev.quarkos.emotes.config.ConfigLayout.Box;
import dev.quarkos.emotes.config.EmoteCatalog.Filter;
import dev.quarkos.emotes.config.ui.CardGrid;
import dev.quarkos.emotes.config.ui.Chip;
import dev.quarkos.emotes.config.ui.EmoteIcon;
import dev.quarkos.emotes.config.ui.EmotePreview;
import dev.quarkos.emotes.config.ui.FlatButton;
import dev.quarkos.emotes.config.ui.SearchBox;
import dev.quarkos.emotes.config.ui.StarToggle;
import dev.quarkos.emotes.config.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class EmoteLibrary {
    private final Font font;
    private final Minecraft minecraft;
    private final Consumer<Emote> play;
    private final Consumer<AbstractWidget> focus;
    private final EmotePreview.Motion detailMotion = new EmotePreview.Motion();
    private final EmotePreview.Motion hoverMotion = new EmotePreview.Motion();
    private final Chip[] slots = new Chip[EmoteConfig.SLOT_COUNT];

    private GridLayout layout;
    private GridLayout.Detail detail;
    private SearchBox search;
    private CardGrid cards;
    private FlatButton playButton, assignButton, clearButton;
    private StarToggle starButton;
    private List<Emote> catalog = List.of();
    private List<Emote> results = List.of();
    private String query = "";
    private Filter filter = Filter.ALL;
    private Emote selected;
    private Emote over;
    private int mouseX, mouseY;
    private boolean focusAssignment, focusSearch;

    EmoteLibrary(Font font, Minecraft minecraft, Consumer<Emote> play, Consumer<AbstractWidget> focus) {
        this.font = font;
        this.minecraft = minecraft;
        this.play = play;
        this.focus = focus;
    }

    void init(GridLayout layout, Consumer<AbstractWidget> add) {
        this.layout = layout;
        detail = layout.controls();
        int offset = cards == null ? 0 : cards.offset();

        search = new SearchBox(font, layout.search(), Ui.text("search"));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            updateResults(true);
        });
        add.accept(search);
        for (Filter category : Filter.values()) {
            add.accept(new Chip(font, layout.filter(category.ordinal()), Component.translatable("filter.quarkos_emotes." + category.key()),
                    () -> filter == category, () -> {
                        filter = category;
                        updateResults(true);
                    }));
        }

        cards = new CardGrid(layout, this::paintCard, this::select, this::toggleFavorite, () -> play.accept(selected));
        Box library = layout.library();
        clearButton = new FlatButton(font, new Box(library.x() + (library.w() - 96) / 2, library.y() + library.h() / 2 + 4, 96, 18),
                Ui.text("clear_filters"), this::clearFilters);
        add.accept(clearButton);
        add.accept(cards);

        Box actions = detail.actions();
        int starWidth = actions.w() >= 216 ? (actions.w() - 8) / 3 : 18;
        int playWidth = (actions.w() - starWidth - 8) / 2;
        playButton = new FlatButton(font, new Box(actions.x(), actions.y(), playWidth, actions.h()), Ui.text("play"),
                true, () -> play.accept(selected));
        starButton = new StarToggle(font, new Box(actions.x() + playWidth + 4, actions.y(), starWidth, actions.h()),
                () -> selected != null && starred(selected), this::toggleSelectedFavorite);
        int assignX = actions.x() + playWidth + starWidth + 8;
        assignButton = new FlatButton(font, new Box(assignX, actions.y(), actions.right() - assignX, actions.h()),
                Ui.text("assign"), () -> focusAssignment = true);
        add.accept(playButton);
        add.accept(starButton);
        add.accept(assignButton);
        for (int i = 0; i < slots.length; i++) {
            int slot = i;
            slots[i] = new Chip(font, layout.slot(i), slotLabel(i), () -> isAssigned(slot), () -> assign(slot));
            add.accept(slots[i]);
        }
        updateResults(false);
        cards.setOffset(offset);
    }

    void setCatalog(List<Emote> catalog) {
        boolean firstLoad = this.catalog.isEmpty();
        this.catalog = catalog;
        updateResults(firstLoad);
    }

    private void clearFilters() {
        filter = Filter.ALL;
        search.setValue("");
        focusSearch = true;
    }

    private void updateResults(boolean resetScroll) {
        String selectedId = selected == null ? EmotesClient.selected : selected.id();
        results = EmoteCatalog.filter(catalog, EmoteConfig.get().favorites, filter, query,
                emote -> EmoteWheelScreen.label(emote).getString());
        int index = 0;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).id().equals(selectedId)) {
                index = i;
                break;
            }
        }
        updateSelection(results.isEmpty() ? -1 : index, resetScroll);
        if (resetScroll) cards.showSelection();
        clearButton.visible = results.isEmpty() && (!query.isBlank() || filter != Filter.ALL);
    }

    private void select(int index) {
        updateSelection(index, false);
    }

    private void updateSelection(int index, boolean resetScroll) {
        selected = index < 0 ? null : results.get(index);
        if (selected != null) EmotesClient.selected = selected.id();
        detailMotion.show(selected);
        cards.update(results.size(), index, Ui.text("library_selection",
                selected == null ? Ui.text("empty") : EmoteWheelScreen.label(selected)), resetScroll);
        playButton.visible = starButton.visible = assignButton.visible = selected != null;
        playButton.active = minecraft.player != null;
        updateSlotLabels();
    }

    private static boolean starred(Emote emote) {
        return EmoteConfig.get().favorites.contains(emote.id());
    }

    private void toggleFavorite(int index) {
        EmoteConfig.get().favorite(results.get(index).id());
        updateResults(false);
    }

    private void toggleSelectedFavorite() {
        if (selected != null) {
            EmoteConfig.get().favorite(selected.id());
            updateResults(false);
        }
    }

    private boolean isAssigned(int index) {
        return selected != null && selected.id().equals(EmoteConfig.get().slot(index));
    }

    private void assign(int index) {
        if (selected == null) return;
        EmoteConfig.get().setSlot(index, isAssigned(index) ? "" : selected.id());
        EmoteConfig.save();
        updateSlotLabels();
    }

    private void updateSlotLabels() {
        for (int i = 0; i < slots.length; i++) {
            slots[i].visible = selected != null;
            slots[i].setMessage(slotLabel(i));
            slots[i].setSecondLine(assignedName(i));
        }
    }

    private Component slotLabel(int index) {
        var key = EmotesClient.QUICK_SLOTS[index];
        return key.isUnbound() ? Component.literal(Integer.toString(index + 1))
                : Ui.text("slot_key", index + 1, key.getTranslatedKeyMessage());
    }

    private Component assignedName(int index) {
        String id = EmoteConfig.get().slot(index);
        Emote emote = EmoteResources.REGISTRY.get(id);
        if (emote != null) return EmoteWheelScreen.label(emote);
        return id.isEmpty() ? Ui.text("slot_empty") : Component.literal(id);
    }

    void tick() {
        detailMotion.tick();
        hoverMotion.tick();
        if (focusAssignment && selected != null) focus.accept(slots[0]);
        if (focusSearch) focus.accept(search);
        focusAssignment = focusSearch = false;
    }

    boolean searchFocused() {
        return search.isFocused();
    }

    boolean clickOutsideWidgets(double x, double y) {
        if (layout.search().contains(x, y)) {
            focus.accept(search);
            return true;
        }
        if (selected == null || minecraft.player == null || !detail.preview().contains(x, y)) return false;
        focus.accept(null);
        detailMotion.startDrag();
        return true;
    }

    boolean drag(double dx) {
        return detailMotion.drag(dx);
    }

    boolean endDrag() {
        if (cards != null) cards.releaseScrollbar();
        return detailMotion.endDrag();
    }

    void renderPanels(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        int hovered = cards.cardAt(mouseX, mouseY);
        over = hovered < 0 ? null : results.get(hovered);
        hoverMotion.show(over);

        Box count = layout.count();
        Ui.center(g, font, results.size() == 1 ? Ui.text("result_one") : Ui.text("result_count", results.size()), count, Ui.MUTED);
        Ui.panel(g, layout.library(), Ui.WELL, Ui.BORDER);
        Ui.panel(g, layout.detail(), Ui.PANEL, Ui.BORDER);
        if (results.isEmpty()) {
            Box library = layout.library();
            Component message = filter == Filter.FAVORITES && EmoteConfig.get().favorites.isEmpty()
                    ? Ui.text("no_favorites") : Ui.text("empty");
            Ui.center(g, font, message, new Box(library.x() + 4, library.y() + library.h() / 2 - 16, library.w() - 8, 14), Ui.MUTED);
        }
        renderDetail(g, partial);
    }

    private void paintCard(GuiGraphics g, int index, Box card, boolean hovered, boolean focused, float partial) {
        Emote emote = results.get(index);
        boolean chosen = emote == selected;
        Ui.panel(g, card, hovered || focused ? Ui.RAISED : Ui.PANEL, chosen || focused ? Ui.ACCENT : hovered ? Ui.HOVER : Ui.BORDER);
        int footer = 15;
        Box portrait = new Box(card.x() + 2, card.y() + 2, card.w() - 4, card.h() - footer - 3);
        Ui.fill(g, portrait, Ui.WELL);
        if (portrait.intersects(layout.cards().viewport())) {
            if (minecraft.player == null) {
                EmoteIcon.draw(g, emote, card.x() + card.w() / 2f, portrait.y() + portrait.h() / 2f, 1.5f);
            } else if (hovered) {
                (chosen ? detailMotion : hoverMotion).draw(g, portrait, minecraft.player, partial, false);
            } else {
                EmotePreview.draw(g, portrait, minecraft.player, emote, EmotePreview.stillTime(emote), 14);
            }
        }
        Box star = StarToggle.cardBounds(card);
        StarToggle.drawStar(g, font, star, starred(emote), hovered && star.contains(mouseX, mouseY));
        Ui.center(g, font, EmoteWheelScreen.label(emote), new Box(card.x() + 4, card.bottom() - footer, card.w() - 8, footer), Ui.TEXT);
    }

    private void renderDetail(GuiGraphics g, float partial) {
        Box panel = layout.detail();
        Box portrait = detail.preview();
        if (selected == null) {
            Ui.center(g, font, Ui.text("choose"), panel.inset(6), Ui.MUTED);
            return;
        }
        Ui.heading(g, font, EmoteWheelScreen.label(selected),
                new Box(portrait.x(), panel.y() + 5, portrait.w(), 14), panel.w() >= 180 ? 1.35f : 1.12f, false);
        String categoryKey = "filter.quarkos_emotes." + selected.category();
        Component category = I18n.exists(categoryKey) ? Component.translatable(categoryKey)
                : Component.literal(EmoteFiles.title(selected.category()));
        Component mode = Ui.text("loop_" + selected.loop().name().toLowerCase(Locale.ROOT));
        Component metadata = category.copy().append(" · ").append(mode);
        int badge = selected.prop() != null || !selected.props().isEmpty() ? Math.min(font.width(Ui.text("prop")) + 8, portrait.w() / 3) : 0;
        Ui.label(g, font, metadata, new Box(portrait.x(), panel.y() + 19, portrait.w() - badge - 4, 12), Ui.MUTED);
        if (badge > 0) {
            Box pill = new Box(portrait.right() - badge, panel.y() + 19, badge, 12);
            Ui.softRect(g, pill, Ui.RAISED);
            Ui.center(g, font, Ui.text("prop"), pill.inset(1), Ui.TEXT);
        }
        Ui.softRect(g, portrait, Ui.WELL);
        if (minecraft.player == null) {
            Ui.center(g, font, Ui.text("preview_requires_world"), portrait.inset(4), Ui.MUTED);
        } else {
            detailMotion.draw(g, portrait.inset(1), minecraft.player, partial, true);
        }

        Box assignment = layout.assignment();
        int captionWidth = assignment.w() / 2;
        Ui.label(g, font, Ui.text("assign_to_slot"), new Box(assignment.x(), assignment.y() - 14, captionWidth, 12), Ui.TEXT);
        String hint = Ui.trim(font, Ui.text("slot_controls"), captionWidth);
        g.drawString(font, hint, assignment.right() - font.width(hint), assignment.y() - 12, Ui.MUTED, false);
    }

    void renderTooltip(GuiGraphics g, int x, int y) {
        if (selected == null || !layout.assignment().contains(x, y)) return;
        for (int i = 0; i < slots.length; i++) {
            Box slot = layout.slot(i);
            if (!slot.contains(x, y)) continue;
            Component name = Ui.text("slot_assignment", i + 1, assignedName(i));
            Component action = Ui.text(isAssigned(i) ? "slot_remove" : "slot_set");
            Box library = layout.library();
            int tooltipWidth = Math.min(library.w() - 8, Math.max(font.width(name), font.width(action)) + 12);
            Box box = new Box(Math.clamp(slot.x(), library.x() + 4, library.right() - tooltipWidth - 4),
                    library.bottom() - 28, tooltipWidth, 24);
            Ui.layer(g);
            Ui.panel(g, box, Ui.GROUND, Ui.BORDER);
            Ui.label(g, font, name, new Box(box.x() + 5, box.y() + 2, box.w() - 10, 10), Ui.TEXT);
            Ui.label(g, font, action, new Box(box.x() + 5, box.y() + 12, box.w() - 10, 10), Ui.MUTED);
            break;
        }
    }
}
