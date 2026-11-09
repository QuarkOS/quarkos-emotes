package dev.quarkos.emotes.animation;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
class EmoteParserTest {
    private EmoteParser.Result parse(String animation) {
        return new EmoteParser().parse(new StringReader("{\"format_version\":\"1.8.0\",\"animations\":{\"animation.wave\":"+animation+"}}"),"test");
    }
    @Test void constantVectorsAndAliases() {
        var result=parse("""
            {"animation_length":2,"bones":{"RightArm":{"rotation":[-90,0,0]},"ToRSo":{"position":[1,2,3]},"LeftLeg":{"scale":[2,2,2]},"waist":{}}}
            """);
        assertTrue(result.valid(),result.errors().toString());var e=result.emotes().getFirst();
        assertEquals("test:wave",e.id());assertEquals(-90,e.bones().get("right_arm").sample(1,"linear").rotation().x());
        assertTrue(e.bones().containsKey("body"));assertEquals(2,e.bones().get("left_leg").sample(1,"linear").scale().y());
        assertEquals("left_arm",BedrockAxes.bone("leftArm"));assertEquals("right_leg",BedrockAxes.bone("Right_Leg"));
        assertEquals("body",BedrockAxes.bone("ROOT"));
    }
    @Test void prePostAndModes() {
        var e=parse("""
            {"bones":{"head":{"rotation":{"0":{"pre":[1,0,0],"post":[2,0,0],"lerp_mode":"step"},"1":{"pre":[9,0,0],"post":[12,0,0],"lerp_mode":"catmullrom"},"2":[15,0,0]}}}}
            """).emotes().getFirst();
        var t=e.bones().get("head").rotation();
        assertEquals(2,t.sample(.5,"linear",Vec3.ZERO).x());assertEquals(12,t.sample(1,"linear",Vec3.ZERO).x());
        assertEquals(Track.Mode.CATMULLROM,t.keys().get(1).mode());assertEquals(2,e.length());
    }
    @Test void loopsSoundsAndMetadata() {
        var r=parse("""
            {"animation_length":3,"loop":"hold_on_last_frame","bones":{},
             "sound_effects":{"0.5":{"effect":"minecraft:entity.player.levelup"},"1":[{"effect":"minecraft:a"},{"effect":"minecraft:b"}]},
             "quarkos":{"id":"custom:dance","name":"Dance","category":"party","icon":"minecraft:diamond","cancel_on_move":false,"easing":"ease_out", "sound":{"id":"minecraft:c","at":2,"volume":0.4,"pitch":1.2},"prop":{"item":"minecraft:stick","bone":"right_arm"}}}
            """);
        assertTrue(r.valid());var e=r.emotes().getFirst();assertEquals(Emote.Loop.HOLD,e.loop());
        assertEquals("custom:dance",e.id());assertEquals("Dance",e.name());assertEquals("party",e.category());assertFalse(e.cancelOnMove());
        assertEquals(4,e.sounds().size());assertEquals(.5,e.sounds().getFirst().time());assertEquals(.4,e.sounds().getLast().volume(),1e-6);
        assertEquals("minecraft:stick",e.prop().get("item").getAsString());
        assertEquals(Emote.Loop.LOOP,parse("{\"loop\":true,\"animation_length\":1}").emotes().getFirst().loop());
        assertEquals(Emote.Loop.ONCE,parse("{\"loop\":false,\"animation_length\":1}").emotes().getFirst().loop());
    }
    @Test void unsupportedMolangWarnsAndUsesZero() {
        var r=parse("""
            {"bones":{"head":{"rotation":["90","math.pi","query.life_time * 20"]}}}
            """);
        assertTrue(r.valid());assertFalse(r.warnings().isEmpty());var v=r.emotes().getFirst().bones().get("head").sample(0,"linear").rotation();
        assertEquals(new Vec3(90,Math.PI,0),v);
    }
    @Test void malformedDoesNotThrow() {
        for(String json:new String[]{"[]","null","{","{}","{\"animations\":3}","{\"animations\":{\"bad\":{\"bones\":{\"head\":{\"rotation\":[1,2]}}}}}"}) {
            var r=assertDoesNotThrow(()->new EmoteParser().parse(new StringReader(json),"test"));assertFalse(r.valid(),json);
        }
        assertFalse(parse("{\"animation_length\":-1,\"bones\":{\"head\":{\"rotation\":{\"NaN\":[0,0,0]}}}}").valid());
    }
    @Test void multipleAnimationsKeepValidEntriesAndOverrideInOrder() {
        var r=new EmoteParser().parse(new StringReader("""
            {"animations":{"animation.a":{"animation_length":1},"animation.b":{"animation_length":2},"bad":null}}
            """),"test");
        assertEquals(2,r.emotes().size());assertFalse(r.valid());
        var later=parse("{\"animation_length\":4,\"quarkos\":{\"id\":\"test:a\"}}").emotes().getFirst();
        var registry=new EmoteRegistry();var definitions=new java.util.ArrayList<>(r.emotes());definitions.add(later);registry.replace(definitions);
        assertEquals(2,registry.all().size());assertEquals(4,registry.get("test:a").length());assertEquals(2,registry.get("test:b").length());
    }
}
