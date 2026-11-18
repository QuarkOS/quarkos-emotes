package dev.quarkos.emotes.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmoteConfigTest {
    @TempDir Path game;
    private Path settings() throws Exception {Path path=game.resolve("config/quarkos_emotes.json");Files.createDirectories(path.getParent());return path;}
    @Test void malformedSettingsArePreservedAndReplacedWithDefaults() throws Exception {
        Path path=settings();Files.writeString(path,"{broken");EmoteConfig.load(game);
        assertEquals(8,EmoteConfig.get().favorites.size());assertEquals(EmoteConfig.FirstPerson.THIRD_PERSON_CAMERA,EmoteConfig.get().firstPerson);
        try(var files=Files.list(path.getParent())) {
            Path backup=files.filter(p->p.getFileName().toString().contains(".corrupt-")).findFirst().orElseThrow();
            assertEquals("{broken",Files.readString(backup));
        }
        assertTrue(Files.readString(path).contains("firstPerson"));
        EmoteConfig.load(game);assertEquals(8,EmoteConfig.get().favorites.size());
    }
    @Test void nullableInvalidAndDuplicateSettingsNormalizeWithoutLosingValidValues() throws Exception {
        Files.writeString(settings(),"{\"firstPerson\":\"unknown\",\"soundVolume\":5,\"favorites\":[null,\"test:wave\",\"test:wave\"],\"showOthers\":false}");
        EmoteConfig.load(game);assertEquals(EmoteConfig.FirstPerson.THIRD_PERSON_CAMERA,EmoteConfig.get().firstPerson);
        assertEquals(1,EmoteConfig.get().soundVolume);assertEquals(java.util.List.of("test:wave"),EmoteConfig.get().favorites);
        assertFalse(EmoteConfig.get().showOthers);
        EmoteConfig.load(game.resolve("another"));assertTrue(EmoteConfig.get().showOthers);assertEquals(8,EmoteConfig.get().favorites.size());
    }
    @Test void oldSettingsKeepTheirValuesAndStartWithEmptyQuickSlots() throws Exception {
        Files.writeString(settings(), """
                {"firstPerson":"SHOW_ARMS","holdWheel":false,"showOthers":false,"cancelOnMove":false,
                 "playSounds":false,"soundVolume":0.37,"favorites":["test:wave","test:clap"]}
                """);
        EmoteConfig.load(game);
        var config = EmoteConfig.get();
        assertEquals(EmoteConfig.FirstPerson.SHOW_ARMS, config.firstPerson);
        assertFalse(config.holdWheel);
        assertFalse(config.showOthers);
        assertFalse(config.cancelOnMove);
        assertFalse(config.playSounds);
        assertEquals(.37f, config.soundVolume);
        assertEquals(List.of("test:wave", "test:clap"), config.favorites);
        assertEquals(EmoteConfig.MenuView.WHEEL, config.menuView);
        assertFalse(config.wheelFavoritesOnly);
        for (int i = 0; i < EmoteConfig.SLOT_COUNT; i++) assertEquals("", config.slot(i));
    }

    @Test void favoritesAcrossWheelPagesAndQuickSlotPositionsRoundTrip() throws Exception {
        EmoteConfig.load(game);
        var config = EmoteConfig.get();
        config.favorite("pack:new");
        assertEquals(9, config.favorites.size());
        config.favorite("quarkos_emotes:clap");
        config.favorite("pack:another");
        List<String> favorites = List.copyOf(config.favorites);
        config.menuView = EmoteConfig.MenuView.GRID;
        config.wheelFavoritesOnly = true;
        config.setSlot(0, "quarkos_emotes:wave");
        config.setSlot(7, "pack:new");
        EmoteConfig.save();

        EmoteConfig.load(game);
        config = EmoteConfig.get();
        assertEquals(favorites, config.favorites);
        assertEquals(EmoteConfig.MenuView.GRID, config.menuView);
        assertTrue(config.wheelFavoritesOnly);
        assertEquals("quarkos_emotes:wave", config.slot(0));
        assertEquals("", config.slot(1));
        assertEquals("pack:new", config.slot(7));

        config.setSlot(0, null);
        EmoteConfig.save();
        EmoteConfig.load(game);
        assertEquals("", EmoteConfig.get().slot(0));
        assertEquals("pack:new", EmoteConfig.get().slot(7));
    }

    @Test void shortOrNullableQuickSlotArraysKeepTheirIndices() throws Exception {
        Files.writeString(settings(), """
                {"menuView":null,"quickSlots":[null,"pack:missing","  ","test:wave"]}
                """);
        EmoteConfig.load(game);
        var config = EmoteConfig.get();
        assertEquals(EmoteConfig.MenuView.WHEEL, config.menuView);
        assertEquals("", config.slot(0));
        assertEquals("pack:missing", config.slot(1));
        assertEquals("", config.slot(2));
        assertEquals("test:wave", config.slot(3));
        assertEquals("", config.slot(7));
    }
}
