package dev.quarkos.emotes.animation;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrackingPlaybackTest {
    private Emote emote(Emote.Loop loop) {
        return new Emote("test:loop","Loop","","",2,loop,true,"linear",Map.of(),
                List.of(new Emote.Sound(.1,"test:old",1,1),new Emote.Sound(.5,"test:new",1,1)),null);
    }
    @Test void trackedLoopKeepsAdvancingAndSkipsHistoricalSounds() {
        var p=new EmotePlayer(emote(Emote.Loop.LOOP));p.seek(4.45);
        assertEquals(.45,p.time(0),1e-9);assertEquals(1,p.weight(0));assertTrue(p.sounds().isEmpty());
        p.tick();p.tick();assertEquals(.55,p.time(0),1e-9);
        assertEquals(List.of("test:new"),p.sounds().stream().map(Emote.Sound::id).toList());
        p.stop();for(int i=0;i<4;i++)p.tick();assertTrue(p.finished());
    }
    @Test void reloadRebindsRemoteDefinitionsAtCurrentElapsedTimeWithoutOldSounds() {
        var original=new EmotePlayer(emote(Emote.Loop.LOOP));original.seek(4.45);
        var rebound=original.rebind(emote(Emote.Loop.LOOP));
        assertNotSame(original,rebound);assertNotSame(original.emote(),rebound.emote());
        assertEquals(original.time(0),rebound.time(0));assertTrue(rebound.sounds().isEmpty());
        rebound.tick();rebound.tick();assertEquals(.55,rebound.time(0),1e-9);
    }
    @Test void geometryRejectsNonFiniteInflationBeforeRendering() {
        String json="""
            {"format_version":"1.12.0","minecraft:geometry":[{"description":{"texture_width":16,"texture_height":16},
             "bones":[{"name":"prop","cubes":[{"origin":[0,0,0],"size":[1,1,1],"uv":[0,0],"inflate":"NaN"}]}]}]}
            """;
        assertThrows(IllegalArgumentException.class,()->PropGeometry.parse(new java.io.StringReader(json),"test:texture"));
    }
    @Test void lateJoinSeeksPushupsLoopAndMeditateBreath() {
        var enter=new Emote("quarkos_emotes:pushups","Pushups","","pose",1.3,Emote.Loop.ONCE,true,"linear",Map.of(),List.of(),null);
        var push=new Emote("quarkos_emotes:pushups","Pushups","","pose",2,Emote.Loop.LOOP,true,"linear",Map.of(),List.of(),null).withChain(enter,null);
        var player=new EmotePlayer(push);player.seek(1.3+5.25);
        assertEquals("main",player.segment(0).name());
        assertEquals(1.25,player.time(0),1e-9);
        assertEquals(1,player.weight(0));
        var breath=new Emote("quarkos_emotes:meditate","Meditate","","pose",2,Emote.Loop.LOOP,true,"linear",Map.of(),List.of(),null);
        var meditate=new Emote("quarkos_emotes:meditate","Meditate","","pose",5.6,Emote.Loop.HOLD,true,"linear",Map.of(),List.of(),null).withChain(null,breath);
        var seated=new EmotePlayer(meditate);seated.seek(5.6+3.25);
        assertEquals("loop",seated.segment(0).name());
        assertEquals(1.25,seated.time(0),1e-9);
        assertEquals(EmotePlayer.State.PLAYING,seated.state());
        assertTrue(seated.sounds().isEmpty());
    }
    @Test void expiredOnceIsFinishedButLateHoldRetainsLastFrame() {
        var once=new EmotePlayer(emote(Emote.Loop.ONCE));once.seek(20);assertTrue(once.finished());assertTrue(once.sounds().isEmpty());
        var hold=new EmotePlayer(emote(Emote.Loop.HOLD));hold.seek(20);
        assertEquals(EmotePlayer.State.HOLDING,hold.state());assertEquals(2,hold.time(0));assertEquals(1,hold.weight(0));
        hold.stop();for(int i=0;i<4;i++)hold.tick();assertTrue(hold.finished());
        assertThrows(IllegalArgumentException.class,()->hold.seek(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->hold.seek(-1));
    }
}
