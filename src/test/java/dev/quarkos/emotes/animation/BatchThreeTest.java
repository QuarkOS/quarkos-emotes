package dev.quarkos.emotes.animation;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BatchThreeTest {
    private Emote shipped(String name) throws IOException {
        String path="/assets/quarkos_emotes/emotes/"+name+"/"+name;
        try(var animation=reader(path+".animation.json");var metadata=reader(path+".quarkos.json")) {
            var parsed=new EmoteParser().parse(animation,"quarkos_emotes",name+".animation.json",JsonParser.parseReader(metadata).getAsJsonObject());
            assertTrue(parsed.valid(),parsed.errors().toString());assertTrue(parsed.warnings().isEmpty());
            return parsed.emotes().getFirst();
        }
    }
    private Reader reader(String path) {
        return new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(path),path),StandardCharsets.UTF_8);
    }
    private Emote.Pose rest(double y) {return new Emote.Pose(Vec3.ZERO,new Vec3(-5,y,0),Vec3.ONE);}

    @Test void batchThreeHasExpectedLengthsModesAndSteveOrbitFlags() throws IOException {
        var lengths=Map.ofEntries(Map.entry("yes",1.6),Map.entry("no",1.6),Map.entry("cry",2.6),Map.entry("rage",2.0),
                Map.entry("victory",1.8),Map.entry("heart_hands",2.0),Map.entry("wave_big",2.4),Map.entry("backflip",1.6),
                Map.entry("spin",1.6),Map.entry("worm",3.2),Map.entry("floss",2.0),Map.entry("tpose",1.2),
                Map.entry("zombie_walk",2.0),Map.entry("slow_clap",3.0),Map.entry("sleep",2.2),Map.entry("sit",1.1));
        for(var entry:lengths.entrySet()) {
            String name=entry.getKey();var e=shipped(name);
            assertEquals("quarkos_emotes:"+name,e.id());assertEquals(entry.getValue(),e.length());assertTrue(e.cancelOnMove());
            assertEquals(Set.of("sit","sleep").contains(name)?Emote.Loop.HOLD:
                    Set.of("floss","zombie_walk").contains(name)?Emote.Loop.LOOP:Emote.Loop.ONCE,e.loop());
            assertEquals(Set.of("backflip","spin","worm","sleep").contains(name)?Emote.ArmPivot.STEVE:Emote.ArmPivot.MODEL,e.armPivot());
            assertEquals(e.armPivot(),e.withProps(List.of()).armPivot());
            for(var b:e.bones().values())for(var t:List.of(b.rotation(),b.position(),b.scale()))for(var k:t.keys())
                assertEquals(Math.rint(k.time()*20),k.time()*20,1e-8);
        }
    }
    @Test void shippedFullTurnsSurviveParserSamplingEulerApplicationAndBlends() throws IOException {
        for(String name:List.of("backflip","spin")) {
            var e=shipped(name);boolean flip=name.equals("backflip");var track=e.bones().get("body").rotation();
            assertEquals(flip?-360:360,axis(track.keys().getLast().post(),flip),1e-6,"parser must retain complete turns");
            var player=new EmotePlayer(e);player.freeze(flip?.8:.9);
            assertEquals(flip?-180:270.5,axis(player.pose(0).get("body").rotation(),flip),.001);
            player.freeze(flip?.825:.925);
            double between=axis(player.pose(0).get("body").rotation(),flip);
            assertTrue(flip?between < -180 && between > -210:between > 270 && between < 303);
            player.freeze(e.length());
            var baseline=Map.of("body",rest(0));
            assertEquals(Math.toRadians(flip?-360:360),axis(PoseMath.flat(baseline,player.pose(0),1).get("body").rotation(),flip),1e-9);
            assertEquals(Math.toRadians(flip?-180:180),axis(PoseMath.flat(baseline,player.pose(0),.5).get("body").rotation(),flip),1e-9,"entry is componentwise Euler");
            player.stop();var frozen=player.pose(0);player.tick();player.tick();
            assertEquals(frozen,player.pose(.5));assertEquals(.5,player.weight(0),1e-9);
            assertEquals(Math.toRadians(flip?-180:180),axis(PoseMath.flat(baseline,player.pose(0),player.weight(0)).get("body").rotation(),flip),1e-9,"exit must not wrap");
        }
    }
    private double axis(Vec3 v,boolean flip) {return flip?v.x():v.y();}

    @Test void everyInterpolationModeUsesRawComponentsAcrossAFullTurn() {
        for(var mode:Track.Mode.values()) {
            var t=new Track(List.of(new Track.Key(0,Vec3.ZERO,Vec3.ZERO,mode),
                    new Track.Key(1,new Vec3(-360,360,720),new Vec3(-360,360,720),mode)));
            assertEquals(mode==Track.Mode.STEP?Vec3.ZERO:new Vec3(-180,180,360),t.sample(.5,"linear",Vec3.ZERO));
        }
    }
    @Test void pivotShiftIsTemporaryWeightedAndOnlyForSlimSteveOrbits() throws IOException {
        var slim=rest(2.5);var wide=rest(2);
        for(double weight:new double[]{0,.25,.5,1}) {
            var corrected=PoseMath.armPivot(slim,true,Emote.ArmPivot.STEVE,weight);
            assertEquals(2.5-.5*weight,corrected.position().y());
            assertEquals(slim,PoseMath.armPivot(slim,true,Emote.ArmPivot.MODEL,weight));
            assertEquals(wide,PoseMath.armPivot(wide,false,Emote.ArmPivot.STEVE,weight));
        }
        for(String name:List.of("backflip","spin","worm","sleep")) {
            var e=shipped(name);var p=new EmotePlayer(e);p.freeze(name.equals("backflip")?.8:name.equals("worm")?1.25:e.length());
            var arm=p.pose(0).get("right_arm");
            assertEquals(PoseMath.additive(wide,arm,1),PoseMath.additive(PoseMath.armPivot(slim,true,e.armPivot(),1),arm,1),name+" uses exactly the authoring pivot");
            p.stop();for(int i=0;i<4;i++)p.tick();
            assertTrue(p.finished());assertEquals(0,p.weight(0),1e-9);
            var restored=PoseMath.additive(PoseMath.armPivot(slim,true,e.armPivot(),p.weight(0)),arm,p.weight(0));
            assertEquals(slim.position().y(),restored.position().y(),1e-9);
            assertEquals(0,restored.rotation().magnitude(),1e-9);
        }
        assertEquals(2.5,slim.position().y(),"baseline remains unchanged");
        // The original pivot mismatch under X rotation is |(R-I)*(0,.5,0)|.
        assertEquals(Math.sqrt(.5),Math.hypot(.5*(Math.cos(Math.PI/2)-1),.5*Math.sin(Math.PI/2)),1e-9);
        assertEquals(1,Math.hypot(.5*(Math.cos(Math.PI)-1),.5*Math.sin(Math.PI)),1e-9);
    }
    @Test void armPivotDefaultsToModelAndRejectsUnknownValues() {
        String json="{\"animations\":{\"animation.emote.test\":{\"animation_length\":1}}}";
        var parser=new EmoteParser();
        assertEquals(Emote.ArmPivot.MODEL,parser.parse(new StringReader(json),"test").emotes().getFirst().armPivot());
        var bad=JsonParser.parseString("{\"arm_pivot\":\"alex\"}").getAsJsonObject();
        assertFalse(parser.parse(new StringReader(json),"test","test.animation.json",bad).valid());
    }
    @Test void shippedSitAndSleepHoldUntilCancelThenFadeForPointTwoSeconds() throws IOException {
        for(String name:List.of("sit","sleep")) {
            var p=new EmotePlayer(shipped(name));for(int i=0;i<400;i++)p.tick();
            assertEquals(EmotePlayer.State.HOLDING,p.state());assertEquals(p.emote().length(),p.time(.5));assertEquals(1,p.weight(0));
            if(name.equals("sleep"))assertEquals(-90,p.pose(0).get("body").rotation().x(),.001,"lying down");
            else assertTrue(p.pose(0).get("right_leg").rotation().x()<-80,"seated");
            var held=p.pose(0);p.stop();
            for(int i=1;i<=4;i++) {
                p.tick();assertEquals(held,p.pose(.5));assertEquals(1-i/4.0,p.weight(0),1e-9);
                assertEquals(i==4,p.finished());
            }
        }
    }
}
