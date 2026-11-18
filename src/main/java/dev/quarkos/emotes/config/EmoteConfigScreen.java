package dev.quarkos.emotes.config;

import dev.quarkos.emotes.EmoteResources;
import dev.quarkos.emotes.EmotesClient;
import dev.quarkos.emotes.animation.Emote;
import dev.quarkos.emotes.config.ConfigLayout.Box;
import dev.quarkos.emotes.config.ui.EmotePreview;
import dev.quarkos.emotes.config.ui.FlatButton;
import dev.quarkos.emotes.config.ui.Segmented;
import dev.quarkos.emotes.config.ui.Slider;
import dev.quarkos.emotes.config.ui.ToggleSwitch;
import dev.quarkos.emotes.config.ui.Ui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class EmoteConfigScreen extends Screen {
    private record Row(Box box, String key, int labelWidth) {}

    private final Screen parent;
    private final EmotePreview.Motion motion = new EmotePreview.Motion();
    private final List<Segmented> segments = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private ConfigLayout.Geometry geometry;
    private Box portrait, volumeValue;
    private Emote emote;
    private Slider volume;
    private FlatButton clearFavorites;
    private int saveIn;
    private long confirmUntil;

    /** Capture a row's help when the harness cannot move the cursor. */
    public boolean harnessHoverSound;

    public EmoteConfigScreen(Screen parent) {
        super(Component.translatable("screen.quarkos_emotes.config"));
        this.parent = parent;
    }

    @Override protected void init() {
        geometry = ConfigLayout.geometry(width, height);
        var config = EmoteConfig.get();
        segments.clear();
        rows.clear();
        Box head = geometry.header();
        addRenderableWidget(new FlatButton(font, new Box(head.right() - 68, head.y(), 68, head.h()),
                Component.translatable("gui.done"), true, this::onClose));

        Box playback = geometry.playback();
        segmented(geometry.row(playback, 0), "first_person",
                List.of(value("camera_short"), value("arms_short"), value("off_short")), () -> config.firstPerson.ordinal(), i -> {
                    config.firstPerson = EmoteConfig.FirstPerson.values()[i];
                    persist();
                });
        buildOptions(config);
    }

    private void buildOptions(EmoteConfig config) {
        Box sound = geometry.sound();
        Box wheel = geometry.wheel();
        switchRow(geometry.row(geometry.playback(), 1), "cancel_on_move", () -> config.cancelOnMove, () -> config.cancelOnMove = !config.cancelOnMove);
        switchRow(geometry.row(geometry.playback(), 2), "show_others", () -> config.showOthers, () -> config.showOthers = !config.showOthers);
        switchRow(geometry.row(sound, 0), "play_sounds", () -> config.playSounds, () -> config.playSounds = !config.playSounds);
        Box row = geometry.row(sound, 1);
        Box control = rightControl(row, 98);
        volumeValue = new Box(control.x() - 29, row.y(), 25, row.h());
        rows.add(new Row(row, "sound_volume", volumeValue.x() - row.x() - 4));
        volume = addRenderableWidget(new Slider(control, option("sound_volume"), config.soundVolume, amount -> {
            config.soundVolume = (float) amount;
            saveIn = 6;
        }));
        segmented(geometry.row(wheel, 0), "wheel_mode", List.of(value("hold_short"), value("click_short")),
                () -> config.holdWheel ? 0 : 1, i -> {
                    config.holdWheel = i == 0;
                    persist();
                });
        segmented(geometry.row(wheel, 1), "wheel_content", List.of(Ui.text("favorites"), Ui.text("all")),
                () -> config.wheelFavoritesOnly ? 0 : 1, i -> {
                    config.wheelFavoritesOnly = i == 0;
                    persist();
                });

        Box reset = geometry.row(wheel, 2);
        rows.add(new Row(reset, "reset_favorites", 0));
        clearFavorites = addRenderableWidget(new FlatButton(font, rightControl(reset, 114), Ui.text("reset_favorites"), this::clearFavorites));
        clearFavorites.active = !config.favorites.isEmpty();
        confirmUntil = 0;

        Box panel = geometry.preview();
        portrait = new Box(panel.x() + 6, panel.y() + 21, panel.w() - 12, panel.h() - 43);
        updatePreview();
        clearFocus();
    }

    private static Box rightControl(Box row, int width) {
        int controlWidth = Math.min(width, row.w() * 5 / 8);
        int height = Math.min(20, row.h() - 2);
        return new Box(row.right() - controlWidth, row.y() + (row.h() - height) / 2, controlWidth, height);
    }

    private void segmented(Box row, String key, List<Component> labels, IntSupplier current, IntConsumer change) {
        int[] widths = labels.stream().mapToInt(label -> font.width(label.getString())).toArray();
        var control = ConfigLayout.choices(row, font.width(option(key).getString()), widths);
        rows.add(new Row(row, key, control.track().x() - row.x() - 7));
        segments.add(new Segmented(font, control, labels, current, change, this::addRenderableWidget));
    }

    private void switchRow(Box row, String key, BooleanSupplier value, Runnable change) {
        rows.add(new Row(row, key, 0));
        addRenderableWidget(new ToggleSwitch(font, row, option(key), value, () -> {
            change.run();
            persist();
        }));
    }

    private static Component option(String key) {
        return Component.translatable("option.quarkos_emotes." + key);
    }

    private static Component value(String key) {
        return Component.translatable("value.quarkos_emotes." + key);
    }

    private void clearFavorites() {
        long now = System.nanoTime();
        if (now < confirmUntil) {
            EmoteConfig.get().favorites.clear();
            clearFavorites.active = false;
            persist();
        } else {
            confirmUntil = now + 3_000_000_000L;
            clearFavorites.setMessage(Ui.text("confirm_clear"));
        }
    }

    private void persist() {
        EmoteConfig.save();
        saveIn = 0;
        confirmUntil = 0;
        if (clearFavorites != null) clearFavorites.setMessage(Ui.text("reset_favorites"));
    }

    private void updatePreview() {
        Emote next = EmoteResources.REGISTRY.get(EmotesClient.selected);
        if (next == null) next = EmoteResources.REGISTRY.get("quarkos_emotes:wave");
        if (next == null) next = EmoteResources.REGISTRY.all().stream().findFirst().orElse(null);
        if (next == emote) return;
        emote = next;
        motion.show(emote);
    }

    /** Pins the current preview to a named pose time. */
    public void freezePreview(double seconds) {
        motion.pin(seconds);
    }

    @Override protected void setInitialFocus() {}

    @Override public void tick() {
        if (saveIn > 0 && --saveIn == 0) persist();
        if (confirmUntil != 0 && System.nanoTime() >= confirmUntil) {
            confirmUntil = 0;
            clearFavorites.setMessage(Ui.text("reset_favorites"));
        }
        updatePreview();
        motion.tick();
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {}

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, minecraft.level == null ? Ui.GROUND : Ui.SCRIM);
        Ui.layer(g);
        Box head = geometry.header();
        Ui.heading(g, font, title, new Box(head.x(), head.y(), head.w() - 78, head.h()), 1.45f, false);
        group(g, geometry.playback(), "playback");
        group(g, geometry.sound(), "sound");
        group(g, geometry.wheel(), "wheel");
        for (Row row : rows) {
            if (row.labelWidth() > 0) {
                Box box = row.box();
                Ui.label(g, font, option(row.key()), new Box(box.x() + 3, box.y(), row.labelWidth(), box.h()), Ui.TEXT);
            }
        }
        for (Segmented segment : segments) segment.render(g);
        Ui.label(g, font, Component.literal(volume.percent()), volumeValue, Ui.MUTED);
        renderPreview(g, partial);
        super.render(g, mouseX, mouseY, partial);
        if (harnessHoverSound) {
            Box sound = geometry.row(geometry.sound(), 0);
            drawHelp(g, sound.x(), sound.y());
        } else {
            drawHelp(g, mouseX, mouseY);
        }
    }

    private void group(GuiGraphics g, Box section, String key) {
        Ui.panel(g, section, Ui.PANEL, Ui.BORDER);
        Ui.label(g, font, Component.translatable("section.quarkos_emotes." + key),
                new Box(section.x() + 9, section.y() + 1, section.w() - 18, geometry.headingHeight()), Ui.MUTED);
    }

    private void renderPreview(GuiGraphics g, float partial) {
        Box panel = geometry.preview();
        Ui.panel(g, panel, Ui.PANEL, Ui.BORDER);
        Ui.label(g, font, Ui.text("preview"), new Box(portrait.x(), panel.y() + 3, portrait.w(), 15), Ui.TEXT);
        Ui.softRect(g, portrait, Ui.WELL);
        if (minecraft.player == null || emote == null) {
            Ui.center(g, font, Ui.text(minecraft.player == null ? "preview_requires_world" : "empty"), portrait.inset(4), Ui.MUTED);
            return;
        }
        motion.draw(g, portrait.inset(1), minecraft.player, partial, true);
        Ui.center(g, font, EmoteWheelScreen.label(emote), new Box(portrait.x(), portrait.bottom() + 1, portrait.w(), 12), Ui.TEXT);
        Ui.center(g, font, Ui.text("drag_preview"), new Box(portrait.x(), portrait.bottom() + 12, portrait.w(), 10), Ui.MUTED);
    }

    private void drawHelp(GuiGraphics g, int mx, int my) {
        for (Row row : rows) {
            if (!row.box().contains(mx, my)) continue;
            Component message = Component.translatable("tooltip.quarkos_emotes." + row.key());
            Ui.label(g, font, message, geometry.help(), Ui.MUTED);
            return;
        }
    }

    private boolean beginDrag(double x, double y, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || minecraft.player == null || emote == null || !portrait.contains(x, y)) return false;
        clearFocus();
        motion.startDrag();
        return true;
    }

    //? if >=1.21.11 {
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        return super.mouseClicked(event, twice) || beginDrag(event.x(), event.y(), event.button());
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        return motion.drag(dx) || super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && motion.endDrag() || super.mouseReleased(event);
    }
    //?} else {
    /*@Override public boolean mouseClicked(double x, double y, int button) {
        return super.mouseClicked(x, y, button) || beginDrag(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return motion.drag(dx) || super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT && motion.endDrag() || super.mouseReleased(x, y, button);
    }
    *///?}

    @Override public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override public void removed() {
        if (saveIn > 0) persist();
        motion.endDrag();
    }

    @Override public boolean isPauseScreen() {
        return false;
    }
}
