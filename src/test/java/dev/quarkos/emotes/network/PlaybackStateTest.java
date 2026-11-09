package dev.quarkos.emotes.network;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackStateTest {
    @Test void lateTrackerGetsCurrentEmoteAndElapsedWithoutRestartingSource() {
        var state=new PlaybackState();var actor=UUID.randomUUID();var other=UUID.randomUUID();
        state.update(actor,"test:dance",1_000_000_000L);
        assertNull(state.get(other));
        assertEquals("test:dance",state.get(actor).id());
        assertEquals(3250,state.get(actor).elapsed(4_250_000_000L));
        assertEquals(0,state.get(actor).elapsed(0));
        state.update(actor,"test:wave",5_000_000_000L);
        assertEquals(100,state.get(actor).elapsed(5_100_000_000L));
        state.update(actor,"",5_200_000_000L);assertNull(state.get(actor));
        state.update(actor,"test:dance",0);state.remove(actor);assertNull(state.get(actor));
    }
    @Test void elapsedHasFiniteWireBounds() {
        assertFalse(PlaybackState.validElapsed(-1));assertTrue(PlaybackState.validElapsed(0));
        assertTrue(PlaybackState.validElapsed(PlaybackState.MAX_ELAPSED_MS));
        assertFalse(PlaybackState.validElapsed(Integer.MAX_VALUE));
        var active=new PlaybackState.Playing("test:loop",0);
        assertEquals(PlaybackState.MAX_ELAPSED_MS,active.elapsed(Long.MAX_VALUE));
    }
    @Test void stopDoesNotConsumeThePlayBudgetAndDisconnectResetsIt() {
        var guard=new ProtocolGuard();Object actor=new Object();
        assertTrue(guard.accept(actor,"test:dance",0));assertTrue(guard.accept(actor,"",1));
        assertFalse(guard.accept(actor,"test:dance",2));guard.remove(actor);
        assertTrue(guard.accept(actor,"test:dance",3));
        for(String bad:new String[]{"x:",":x","X:y","x:y\\z","x:y\nz","x:y z"})assertFalse(guard.accept(new Object(),bad,0),bad);
        assertFalse(guard.accept(actor,null,0));
    }
}
