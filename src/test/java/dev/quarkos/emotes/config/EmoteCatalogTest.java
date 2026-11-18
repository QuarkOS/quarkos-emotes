package dev.quarkos.emotes.config;

import dev.quarkos.emotes.animation.Emote;
import dev.quarkos.emotes.config.EmoteCatalog.Filter;
import dev.quarkos.emotes.config.ui.TextFit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmoteCatalogTest {
    private final Emote wave = emote("quarkos_emotes:wave", "Hello there", "greeting");
    private final Emote clap = emote("quarkos_emotes:clap", "Applause", "reaction");
    private final Emote dance = emote("quarkos_emotes:bigdance", "Big Dance", "dance");
    private final Emote custom = emote("pack:custom_dance", "Twirl", "dance");
    private final List<Emote> ordered = EmoteCatalog.ordered(List.of(custom, dance, clap, wave));
    private final List<String> favorites = List.of("pack:missing", dance.id(), wave.id(), dance.id());

    @Test void favoritesLeadTheWheelInSavedOrderAndMissingIdsAreSkipped() {
        assertEquals(List.of(wave, clap, dance, custom), ordered);
        assertEquals(List.of(dance, wave), EmoteCatalog.wheel(ordered, favorites, true));
        assertEquals(List.of(dance, wave, clap, custom), EmoteCatalog.wheel(ordered, favorites, false));
        assertEquals(ordered, EmoteCatalog.wheel(ordered, List.of(), false));
    }

    @Test void filtersCombineCategoryAndSearchUsingLocalizedNamesAndResourcePaths() {
        assertEquals(ordered, search(Filter.ALL, "  "));
        assertEquals(List.of(dance, custom), search(Filter.DANCE, "  Da "));
        assertEquals(List.of(dance), search(Filter.FAVORITES, "da"));
        assertEquals(List.of(wave), search(Filter.GREETING, " wAvE  THERE "));
        assertEquals(List.of(custom), search(Filter.ALL, "custom TWIRL"));
        assertEquals(List.of(), search(Filter.REACTION, "da"));
        assertEquals(List.of(wave, dance), search(Filter.FAVORITES, ""));
    }

    @Test void ellipsisUsesMeasuredGlyphWidthAndPreservesSurrogatePairs() {
        ToIntFunction<String> width = value -> value.codePoints().map(cp -> cp == 'W' || cp > 0xffff ? 5 : 2).sum();
        assertEquals("Wi", TextFit.ellipsize("Wi", 7, width));
        assertEquals("W…", TextFit.ellipsize("Wi!", 7, width));
        assertEquals("😀…", TextFit.ellipsize("😀abc", 7, width));
        assertEquals("…", TextFit.ellipsize("😀abc", 3, width));
        assertEquals("", TextFit.ellipsize("😀abc", 1, width));
        assertEquals("", TextFit.ellipsize("abc", 0, width));
    }

    private List<Emote> search(Filter filter, String query) {
        return EmoteCatalog.filter(ordered, favorites, filter, query, Emote::name);
    }

    private static Emote emote(String id, String name, String category) {
        return new Emote(id, name, "minecraft:stick", category, 1, Emote.Loop.ONCE, false, "linear",
                Map.of(), List.of(), null);
    }
}
