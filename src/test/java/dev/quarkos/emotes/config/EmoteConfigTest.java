package dev.quarkos.emotes.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
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
}
