package dev.quarkos.emotes.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShotFramingTest {
    @Test void lowPosesPullBackAndPushupsDoesNotFillTheLens() {
        var wave = ShotFraming.forId("quarkos_emotes:wave", 0);
        var pushups = ShotFraming.forId("quarkos_emotes:pushups", 0);
        var meditate = ShotFraming.forId("quarkos_emotes:meditate", 0);
        var lay = ShotFraming.forId("quarkos_emotes:lay_down", 0);
        var kneel = ShotFraming.forId("quarkos_emotes:kneel", 35);
        assertTrue(pushups.distance() >= wave.distance() + 2f);
        assertTrue(pushups.drop() < 0 && pushups.orbit() > 45f);
        assertTrue(lay.drop() < 0 && lay.orbit() > 45f);
        assertTrue(meditate.drop() < wave.drop(), "seated camera stays above the standing portrait");
        assertTrue(meditate.pitch() >= 12f && meditate.orbit() > 10f);
        assertTrue(meditate.distance() > wave.distance());
        assertTrue(lay.distance() > wave.distance() + 1f);
        assertTrue(kneel.pitch() > 0);
        for (String id : new String[]{"wave", "pushups", "meditate", "sit", "sit_full", "kneel", "lay_down", "sleep", "worm", "bow"}) {
            var frame = ShotFraming.forId("quarkos_emotes:" + id, 0);
            // Eye is 1.62 above the feet. The harness stands one block above the ground,
            // so the camera stays clear of the floor after this drop.
            assertTrue(1.62f - frame.drop() > 0.35f, id);
            assertTrue(frame.distance() >= 3f, id);
        }
    }
}
