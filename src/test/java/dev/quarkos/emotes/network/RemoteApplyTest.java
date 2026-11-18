package dev.quarkos.emotes.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RemoteApplyTest {
    @Test void missingEntityIsKeptLongEnoughForALateSpawnThenDropped() {
        assertTrue(RemoteApply.keepWhileEntityMissing(1));
        assertTrue(RemoteApply.keepWhileEntityMissing(RemoteApply.ENTITY_GRACE_TICKS));
        assertFalse(RemoteApply.keepWhileEntityMissing(RemoteApply.ENTITY_GRACE_TICKS + 1));
    }

    @Test void remoteCuesSurviveLoginAndDropOnALevelChange() {
        assertFalse(RemoteApply.dropRemoteCues(false, true));
        assertFalse(RemoteApply.dropRemoteCues(true, false));
        assertTrue(RemoteApply.dropRemoteCues(true, true));
    }

    @Test void replacementDropsAPoseAndTheFirstSightOfAnEntityDoesNot() {
        assertFalse(RemoteApply.dropOnEntityReplacement(false, false, true));
        assertFalse(RemoteApply.dropOnEntityReplacement(true, true, true));
        assertTrue(RemoteApply.dropOnEntityReplacement(true, false, true));
        assertTrue(RemoteApply.dropOnEntityReplacement(true, true, false));
    }

    @Test void firstConnectionKeepsACueThatArrivedEarly() {
        assertTrue(RemoteApply.retainCueOnConnectionChange(false));
        assertFalse(RemoteApply.retainCueOnConnectionChange(true));
    }
}
