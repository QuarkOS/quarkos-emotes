package dev.quarkos.emotes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class EmoteConfig {
    public enum FirstPerson { THIRD_PERSON_CAMERA, SHOW_ARMS, NONE }
    public enum MenuView { WHEEL, GRID }

    public static final int SLOT_COUNT = 8;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EmoteConfig instance = new EmoteConfig();
    private static Path file;

    public FirstPerson firstPerson = FirstPerson.THIRD_PERSON_CAMERA;
    public boolean cancelOnMove = true, playSounds = true, showOthers = true, holdWheel = true;
    public float soundVolume = 1;
    public List<String> favorites = new ArrayList<>(List.of(
            "quarkos_emotes:wave", "quarkos_emotes:thinking", "quarkos_emotes:clap", "quarkos_emotes:tableflip",
            "quarkos_emotes:facepalm", "quarkos_emotes:groove", "quarkos_emotes:bigdance", "quarkos_emotes:cheer"));
    public MenuView menuView = MenuView.WHEEL;
    public boolean wheelFavoritesOnly;
    private List<String> quickSlots = emptySlots();

    public static EmoteConfig get() {
        return instance;
    }

    public static void load(Path game) {
        instance = new EmoteConfig();
        file = game.resolve("config/quarkos_emotes.json");
        if (Files.isRegularFile(file)) try (Reader reader = Files.newBufferedReader(file)) {
            EmoteConfig loaded = GSON.fromJson(reader, EmoteConfig.class);
            if (loaded == null) throw new IOException("Empty settings");
            instance = loaded;
        } catch (Exception e) {
            try {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException backup) {
                e.addSuppressed(backup);
            }
            LoggerFactory.getLogger("quarkos_emotes").warn("Recovered emote settings; preserved corrupt file", e);
        }
        if (instance.firstPerson == null) instance.firstPerson = FirstPerson.THIRD_PERSON_CAMERA;
        if (instance.menuView == null) instance.menuView = MenuView.WHEEL;
        if (instance.favorites == null) instance.favorites = new ArrayList<>();
        instance.favorites = new ArrayList<>(instance.favorites.stream().filter(Objects::nonNull).distinct().toList());
        instance.soundVolume = Float.isFinite(instance.soundVolume) ? Math.clamp(instance.soundVolume, 0, 1) : 1;
        instance.quickSlots = normalizeSlots(instance.quickSlots);
        save();
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(instance));
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger("quarkos_emotes").warn("Could not save emote settings", e);
        }
    }

    public void favorite(String id) {
        if (!favorites.remove(id)) favorites.add(id);
        save();
    }

    public String slot(int index) {
        return quickSlots.get(index);
    }

    public void setSlot(int index, String id) {
        quickSlots.set(index, id == null ? "" : id);
    }

    private static List<String> emptySlots() {
        return new ArrayList<>(Collections.nCopies(SLOT_COUNT, ""));
    }

    private static List<String> normalizeSlots(List<String> slots) {
        var normalized = emptySlots();
        if (slots != null) for (int i = 0; i < Math.min(SLOT_COUNT, slots.size()); i++) {
            String id = slots.get(i);
            if (id != null && !id.isBlank()) normalized.set(i, id);
        }
        return normalized;
    }
}
