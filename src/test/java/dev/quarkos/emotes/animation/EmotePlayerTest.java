package dev.quarkos.emotes.animation;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmotePlayerTest {
    private Emote emote(String loop) {
        var parsed=new EmoteParser().parse(new StringReader("""
            {"animations":{"animation.emote.test":{"animation_length":1,"loop":%s,
            "bones":{"head":{"rotation":{"0":[0,0,0],"1":[40,0,0]}}}}}}
            """.formatted(loop)),"test","test.animation.json",null);
        assertTrue(parsed.valid(),parsed.errors().toString());return parsed.emotes().getFirst();
    }
    private void ticks(EmotePlayer p,int count) { for(int i=0;i<count;i++)p.tick(); }
    private void exit(EmotePlayer p) {
        double angle=p.pose(0).get("head").rotation().x();
        assertEquals(EmotePlayer.State.BLENDING_OUT,p.state());
        assertEquals(1,p.weight(0),1e-8);
        // Any repeated cancel (including a server stop) must not restart the fade.
        p.tick();p.stop();assertEquals(.75,p.weight(0),1e-8);
        assertEquals(angle,p.pose(.9).get("head").rotation().x(),1e-8);
        p.tick();assertEquals(.5,p.weight(0),1e-8);
        p.tick();assertEquals(.25,p.weight(0),1e-8);assertFalse(p.finished());
        p.tick();assertEquals(EmotePlayer.State.DONE,p.state());assertEquals(0,p.weight(0),1e-8);
    }
    @Test void holdPlaysOnceThenHoldsUntilAnyStopAndBlendsOut() {
        var p=new EmotePlayer(emote("\"hold_on_last_frame\""));
        assertEquals(EmotePlayer.State.PLAYING,p.state());ticks(p,10);
        assertEquals(20,p.pose(0).get("head").rotation().x(),1e-8);
        ticks(p,400);assertEquals(EmotePlayer.State.HOLDING,p.state());
        assertEquals(1,p.time(.8));assertEquals(40,p.pose(.8).get("head").rotation().x());
        assertEquals(1,p.weight(0));assertFalse(p.stopping());assertFalse(p.finished());
        p.stop();exit(p);
    }
    @Test void cancelDuringPlayFreezesCurrentPoseAndBlendsOutForAllModes() {
        for(String mode:new String[]{"false","true","\"hold_on_last_frame\""}) {
            var p=new EmotePlayer(emote(mode));ticks(p,10);p.stop();
            assertEquals(20,p.pose(0).get("head").rotation().x(),1e-8);exit(p);
        }
    }
    @Test void naturalOnceEndKeepsFinalPoseThroughoutFadeIncludingRenderBoundary() {
        var p=new EmotePlayer(emote("false"));ticks(p,19);
        assertEquals(EmotePlayer.State.PLAYING,p.state());
        assertEquals(1,p.weight(.999),1e-8);
        // Partial-tick sampling also crosses the natural endpoint smoothly.
        assertEquals(.99975,p.weight(1.001),1e-8);
        p.tick();assertEquals(1,p.time(0));assertEquals(40,p.pose(0).get("head").rotation().x());
        exit(p);
    }
    @Test void cancellingDuringEntryFadesFromTheCurrentWeight() {
        var p=new EmotePlayer(emote("\"hold_on_last_frame\""));p.tick();
        double initial=p.weight(0);assertEquals(1.0/3,initial,1e-8);p.stop();
        for(int i=0;i<=4;i++) {
            assertEquals(initial*(1-i/4.0),p.weight(0),1e-8);p.tick();
        }
        assertTrue(p.finished());
    }
    @Test void sharedHoldNeedsOnlyPlayAndStop() {
        var e=emote("\"hold_on_last_frame\"");
        var local=new EmotePlayer(e);var remote=new EmotePlayer(e);
        ticks(local,80);ticks(remote,80);assertEquals(EmotePlayer.State.HOLDING,remote.state());
        local.stop();remote.stop();
        for(int i=0;i<=4;i++) {
            assertEquals(local.state(),remote.state());assertEquals(local.pose(.5),remote.pose(.5));
            assertEquals(local.weight(.5),remote.weight(.5));local.tick();remote.tick();
        }
    }
}
