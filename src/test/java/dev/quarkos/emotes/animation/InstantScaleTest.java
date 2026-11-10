package dev.quarkos.emotes.animation;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InstantScaleTest {
    private Emote parse(String scale) {
        var result=new EmoteParser().parse(new StringReader("{\"animations\":{\"test\":{\"animation_length\":3,\"bones\":{\"sword\":{\"scale\":"+scale+"}}}}}"),"test");
        assertTrue(result.valid(),result.errors().toString());return result.emotes().getFirst();
    }
    private Reader resource(String path) {
        return new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/assets/quarkos_emotes/emotes/"+path)),java.nio.charset.StandardCharsets.UTF_8);
    }
    private Emote sword() throws IOException {
        try(var r=resource("sword_salute/sword_salute.animation.json");var side=resource("sword_salute/sword_salute.quarkos.json")) {
            return new EmoteParser().parse(r,"quarkos_emotes","sword_salute.animation.json",JsonParser.parseReader(side).getAsJsonObject()).emotes().getFirst();
        }
    }
    private PropGeometry geometry() throws IOException {
        try(var r=resource("sword_salute/sword_prop.geo.json")) {return PropGeometry.parse(r,"test:sword.png").getFirst();}
    }
    @Test void scalarAndVectorScaleConstantsAndKeys() {
        for(String value:List.of("2","[2,2,2]","{\"0\":2}","{\"0\":{\"pre\":0,\"post\":2}}"))
            assertEquals(new Vec3(2,2,2),new EmotePlayer(parse(value)).pose(0).get("sword").scale());
    }
    @Test void prePostSwitchesAtExactInstantWithoutARampForEveryInterpolationMode() {
        for(String mode:List.of("linear","catmullrom","step")) {
            var track=parse("{\"0\":{\"post\":0,\"lerp_mode\":\""+mode+"\"},\"0.25\":{\"pre\":0,\"post\":1,\"lerp_mode\":\""+mode+"\"},\"2.2\":{\"pre\":1,\"post\":0}}").bones().get("sword").scale();
            assertEquals(Vec3.ZERO,track.sample(Math.nextDown(.25),"linear",Vec3.ONE));
            assertEquals(Vec3.ZERO,track.sample(.125,"linear",Vec3.ONE));
            assertEquals(Vec3.ONE,track.sample(.25,"linear",Vec3.ZERO));
            assertEquals(Vec3.ONE,track.sample(Math.nextDown(2.2),"linear",Vec3.ZERO));
            assertEquals(Vec3.ONE,track.sample(1,"linear",Vec3.ZERO));
            assertEquals(Vec3.ZERO,track.sample(2.2,"linear",Vec3.ONE));
        }
    }
    @Test void stepHoldsPreviousValueUntilExactKeyIncludingPartialTicks() {
        var p=new EmotePlayer(parse("{\"0\":{\"post\":0,\"lerp_mode\":\"step\"},\"0.225\":1}"));
        for(int i=0;i<4;i++)p.tick();
        assertEquals(Vec3.ZERO,p.pose(.499).get("sword").scale());
        assertEquals(Vec3.ONE,p.pose(.5).get("sword").scale());
        assertEquals(Vec3.ONE,p.pose(.501).get("sword").scale());
        var e=p.emote();
        var loop=new Emote(e.id(),e.name(),e.icon(),e.category(),e.length(),Emote.Loop.LOOP,true,e.easing(),e.bones(),List.of(),null);
        p=new EmotePlayer(loop);p.seek(.2);
        assertEquals(Vec3.ZERO,p.pose(.499).get("sword").scale());
        assertEquals(Vec3.ONE,p.pose(.5).get("sword").scale());
    }
    @Test void duplicateParsedTimesKeepFirstIncomingAndLastOutgoingValues() {
        var track=parse("{\"0\":0,\"0.25\":0,\"0.250\":{\"post\":1,\"lerp_mode\":\"step\"},\"1\":2}").bones().get("sword").scale();
        assertEquals(3,track.keys().size());
        assertEquals(Vec3.ZERO,track.sample(.249,"linear",Vec3.ONE));
        assertEquals(Vec3.ONE,track.sample(.25,"linear",Vec3.ZERO));
        assertEquals(Vec3.ONE,track.sample(.75,"linear",Vec3.ZERO));
        assertEquals(new Vec3(2,2,2),track.sample(1,"linear",Vec3.ZERO));
    }
    @Test void shippedSwordPopsOnTicksFiveAndFortyFourAndBetweenTicks() throws IOException {
        var geo=geometry();var bone=geo.bones().getFirst();var p=new EmotePlayer(sword().withProps(List.of(geo)));
        for(int tick=0;tick<=48;tick++) {
            boolean visible=tick>=5&&tick<44;
            assertEquals(visible,geo.visible(bone,p,0),"tick "+tick);
            assertEquals(visible,geo.visible(bone,p,.999),"partial tick "+tick);
            assertEquals(tick+1>=5&&tick+1<44,geo.visible(bone,p,1),"next tick boundary "+tick);
            p.tick();
        }
        p=new EmotePlayer(sword());p.seek(.249);
        assertFalse(geo.visible(bone,p,.019));assertTrue(geo.visible(bone,p,.021));
        p.seek(2.199);assertTrue(geo.visible(bone,p,.019));assertFalse(geo.visible(bone,p,.021));
        p.seek(2.15);assertFalse(geo.visible(bone,p,1));
        p.seek(2.175);assertFalse(geo.visible(bone,p,.5));
        p.seek(.8);assertTrue(geo.visible(bone,p,0));p.stop();assertFalse(geo.visible(bone,p,0));
    }
    @Test void nearZeroAnyAxisHidesAndMirroringDoesNot() {
        for(Vec3 s:List.of(Vec3.ZERO,new Vec3(1,0,1),new Vec3(1,1,-.00001),new Vec3(.000099,1,1)))assertTrue(PropGeometry.hidden(s));
        assertFalse(PropGeometry.hidden(new Vec3(-1,1,1)));assertFalse(PropGeometry.hidden(new Vec3(.0001,1,1)));
    }
    @Test void ancestorScaleHidesPropChildrenAndUnkeyedPlayerParentStaysVisible() {
        var parent=new PropGeometry.Bone("sword",null,Vec3.ZERO,Vec3.ZERO,List.of());
        var child=new PropGeometry.Bone("tip","sword",Vec3.ZERO,Vec3.ZERO,List.of());
        var attached=new PropGeometry.Bone("attached","right_arm",Vec3.ZERO,Vec3.ZERO,List.of());
        var geo=new PropGeometry("test:prop.png",16,16,List.of(parent,child,attached));
        var p=new EmotePlayer(parse("0"));p.seek(.5);
        assertFalse(geo.visible(child,p,0));assertTrue(geo.visible(attached,p,0));
        var e=parse("1");var bones=new HashMap<>(e.bones());bones.put("right_arm",parse("0").bones().get("sword"));
        e=new Emote(e.id(),e.name(),e.icon(),e.category(),e.length(),e.loop(),true,e.easing(),bones,List.of(),null);
        p=new EmotePlayer(e);p.seek(.5);assertFalse(geo.visible(attached,p,0));
        assertEquals(Vec3.ONE,new EmotePlayer(parse("1")).pose(0).get("sword").scale());
    }
    @Test void shippedSwordCopiesArmAtItsRootPivotForBothRigs() throws IOException {
        var e=sword();assertEquals(Emote.ArmPivot.STEVE,e.armPivot());var geo=geometry();var bone=geo.bones().getFirst();
        assertNull(bone.parent());assertEquals(new Vec3(-5,22,0),bone.pivot());
        assertEquals(e.bones().get("right_arm").rotation(),e.bones().get("sword").rotation());
        assertEquals(e.bones().get("right_arm").position(),e.bones().get("sword").position());
        for(boolean slim:List.of(false,true))for(int frame=25;frame<220;frame++) {
            double time=frame/100.0;var a=e.bones().get("right_arm").sample(time,e.easing());var s=e.bones().get("sword").sample(time,e.easing());
            var base=PoseMath.armPivot(new Emote.Pose(Vec3.ZERO,new Vec3(-5,slim?2.5:2,0),Vec3.ONE),slim,e.armPivot(),1);
            assertEquals(2,base.position().y());
            var arm=PropTransform.geometry(bone.pivot(),a.rotation(),a.position(),Vec3.ONE);
            var sword=PropTransform.geometry(bone.pivot(),s.rotation(),s.position(),s.scale());
            var grip=new Vec3(slim?-5.5:-6,-13,0);
            assertEquals(arm.apply(grip),sword.apply(grip),"hand/grip at "+time+" slim="+slim);
        }
    }
}
