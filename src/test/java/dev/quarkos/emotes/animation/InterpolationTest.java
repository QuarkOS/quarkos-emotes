package dev.quarkos.emotes.animation;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class InterpolationTest {
    private Track.Key key(double t,double pre,double post,Track.Mode mode) { return new Track.Key(t,new Vec3(pre,0,0),new Vec3(post,0,0),mode); }
    @Test void linearBoundariesAndDiscontinuities() {
        var track=new Track(List.of(key(1,2,4,Track.Mode.LINEAR),key(3,8,12,Track.Mode.LINEAR)));
        assertEquals(2,track.sample(0,"linear",Vec3.ZERO).x());assertEquals(4,track.sample(1,"linear",Vec3.ZERO).x());
        assertEquals(6,track.sample(2,"linear",Vec3.ZERO).x());assertEquals(12,track.sample(3,"linear",Vec3.ZERO).x());
        assertEquals(12,track.sample(9,"linear",Vec3.ZERO).x());assertEquals(Vec3.ONE,new Track(List.of()).sample(0,"linear",Vec3.ONE));
    }
    @Test void stepAndCatmullRom() {
        var step=new Track(List.of(key(0,0,0,Track.Mode.STEP),key(1,8,8,Track.Mode.LINEAR)));
        assertEquals(0,step.sample(.99,"linear",Vec3.ZERO).x());assertEquals(8,step.sample(1,"linear",Vec3.ZERO).x());
        var cat=new Track(List.of(key(0,0,0,Track.Mode.CATMULLROM),key(1,2,2,Track.Mode.CATMULLROM),key(2,4,4,Track.Mode.CATMULLROM),key(3,6,6,Track.Mode.CATMULLROM)));
        assertEquals(3,cat.sample(1.5,"ease_in",Vec3.ZERO).x(),1e-9);
        assertEquals(0,cat.sample(0,"linear",Vec3.ZERO).x());assertEquals(6,cat.sample(3,"linear",Vec3.ZERO).x());
        assertEquals(2,cat.sample(1,"linear",Vec3.ZERO).x());
    }
    @Test void easingEndpointsAndShape() {
        for(String name:List.of("linear","ease_in","ease_out","ease_in_out","ease_in_cubic","ease_out_cubic","ease_in_out_cubic","ease_in_sine","ease_out_sine","ease_in_out_sine","ease_in_back","ease_out_back","ease_in_out_back")) {
            assertEquals(0,Easing.apply(name,0),1e-9,name);assertEquals(1,Easing.apply(name,1),1e-9,name);
        }
        assertEquals(.25,Easing.apply("ease_in",.5));assertEquals(.75,Easing.apply("ease_out",.5));
        assertEquals(.5,Easing.apply("ease_in_out_back",.5),1e-9);assertTrue(Easing.apply("ease_out_back",.7)>1);
        assertEquals(0,Easing.apply("linear",-1));assertEquals(1,Easing.apply("linear",2));
    }
    private Emote emote(Emote.Loop loop) { return new Emote("test:a","a","minecraft:stick","general",1,loop,true,"linear",Map.of(),List.of(new Emote.Sound(0,"minecraft:a",1,1),new Emote.Sound(.5,"minecraft:b",1,1)),null); }
    @Test void wrapHoldAndOnce() {
        assertEquals(.25,emote(Emote.Loop.LOOP).wrap(2.25));assertEquals(.75,emote(Emote.Loop.LOOP).wrap(-.25));
        assertEquals(0,emote(Emote.Loop.LOOP).wrap(1));assertEquals(1,emote(Emote.Loop.HOLD).wrap(99));
        var once=new EmotePlayer(emote(Emote.Loop.ONCE));for(int i=0;i<25;i++)once.tick();assertTrue(once.finished());
        var hold=new EmotePlayer(emote(Emote.Loop.HOLD));for(int i=0;i<30;i++)hold.tick();assertFalse(hold.finished());assertEquals(1,hold.time(0));
    }
    @Test void blendFreezeAndSoundCrossings() {
        var player=new EmotePlayer(emote(Emote.Loop.LOOP));assertEquals(0,player.weight(0));
        assertEquals(1,player.sounds().size());assertTrue(player.sounds().isEmpty());
        for(int i=0;i<10;i++)player.tick();assertEquals(1,player.weight(0),1e-9);
        // Floating-point tick accumulation may be infinitesimally below 0.5; the following tick crosses it.
        player.tick();assertEquals(1,player.sounds().size());assertTrue(player.sounds().isEmpty());
        for(int i=0;i<10;i++)player.tick();assertEquals(1,player.sounds().size());
        player.freeze(.4);assertEquals(.4,player.time(.8),1e-9);assertTrue(player.sounds().isEmpty());
        player.stop();for(int i=0;i<4;i++)player.tick();assertTrue(player.finished());assertEquals(0,player.weight(0),1e-9);
    }
}
