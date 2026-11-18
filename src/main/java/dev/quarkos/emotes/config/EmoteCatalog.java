package dev.quarkos.emotes.config;

import dev.quarkos.emotes.animation.Emote;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

final class EmoteCatalog {
    enum Filter {
        ALL, GREETING, REACTION, DANCE, POSE, FUN, FAVORITES;

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final List<String> BUILTIN_ORDER = List.of(
            "wave", "thinking", "clap", "tableflip", "facepalm", "groove", "bigdance", "cheer",
            "yes", "no", "cry", "rage", "victory", "heart_hands", "wave_big", "slow_clap",
            "bow", "salute", "laugh", "shrug", "point", "dab", "sit", "sit_full",
            "backflip", "spin", "worm", "floss", "zombie_walk", "sleep", "tpose", "stretch",
            "jump_hype", "fishing_cast", "sword_salute", "guitar", "drums", "default_dance",
            "kneel", "lay_down", "pushups", "meditate").stream().map(id -> "quarkos_emotes:" + id).toList();

    private EmoteCatalog() {}

    static List<Emote> ordered(List<Emote> emotes) {
        return emotes.stream().sorted(Comparator.comparingInt((Emote emote) -> {
            int index = BUILTIN_ORDER.indexOf(emote.id());
            return index < 0 ? Integer.MAX_VALUE : index;
        }).thenComparing(Emote::id)).toList();
    }

    static List<Emote> wheel(List<Emote> ordered, List<String> favorites, boolean favoritesOnly) {
        Set<String> remaining = new LinkedHashSet<>(favorites);
        var byId = new java.util.HashMap<String, Emote>();
        for (Emote emote : ordered) byId.put(emote.id(), emote);
        List<Emote> result = new ArrayList<>(ordered.size());
        for (String id : remaining) {
            Emote emote = byId.get(id);
            if (emote != null) result.add(emote);
        }
        if (!favoritesOnly) for (Emote emote : ordered) {
            if (!remaining.contains(emote.id())) result.add(emote);
        }
        return result;
    }

    static List<Emote> filter(List<Emote> emotes, List<String> favorites, Filter filter, String query,
                              Function<Emote, String> displayName) {
        String[] words = query.strip().toLowerCase(Locale.ROOT).split("\\s+");
        return emotes.stream().filter(emote -> switch (filter) {
            case ALL -> true;
            case FAVORITES -> favorites.contains(emote.id());
            default -> filter.key().equalsIgnoreCase(emote.category());
        }).filter(emote -> {
            String name = displayName.apply(emote).toLowerCase(Locale.ROOT);
            String id = emote.id().substring(emote.id().indexOf(':') + 1).toLowerCase(Locale.ROOT);
            for (String word : words) if (!name.contains(word) && !id.contains(word)) return false;
            return true;
        }).toList();
    }
}
