package dev.quarkos.emotes.animation;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnimatorContractTest {
    @TempDir Path directory;
    private static final String BASE="/assets/quarkos_emotes/emotes/";
    private Reader resource(String name) {
        var stream=getClass().getResourceAsStream(BASE+name);
        assertNotNull(stream,name);return new InputStreamReader(stream,StandardCharsets.UTF_8);
    }
    private Emote shipped(String name) throws IOException {
        try(var reader=resource(name+"/"+name+".animation.json");var meta=resource(name+"/"+name+".quarkos.json")) {
            var result=new EmoteParser().parse(reader,"quarkos_emotes",name+".animation.json",JsonParser.parseReader(meta).getAsJsonObject());
            assertTrue(result.valid(),result.errors().toString());assertTrue(result.warnings().isEmpty());
            assertEquals(1,result.emotes().size());return result.emotes().getFirst();
        }
    }
    @Test void parsesAllFiveShippedAnimatorExports() throws IOException {
        for(var entry:Map.of("wave",2.0,"thinking",3.0,"tableflip",2.4,"facepalm",2.4,"clap",2.4).entrySet()) {
            String name=entry.getKey();var emote=shipped(name);
            assertEquals("quarkos_emotes:"+name,emote.id());assertEquals(entry.getValue(),emote.length());
            assertEquals(Emote.Loop.ONCE,emote.loop());assertEquals("emote.quarkos_emotes."+name,emote.name());
            assertTrue(emote.cancelOnMove());
            var expected=new HashSet<>(Set.of("head","body","right_arm","left_arm"));
            if(name.equals("tableflip"))expected.add("table");
            assertEquals(expected,emote.bones().keySet());
            for(var bone:emote.bones().values())for(var track:List.of(bone.rotation(),bone.position(),bone.scale()))for(var key:track.keys()) {
                assertEquals(Math.rint(key.time()*20),key.time()*20,1e-8,"20 fps grid");
                assertTrue(key.time()<=emote.length());
            }
            try(var reader=resource(name+"/"+name+".animation.json")) {
                var document=JsonParser.parseReader(reader).getAsJsonObject();
                assertFalse(document.has("quarkos"));
                assertEquals(Set.of("animation.emote."+name),document.getAsJsonObject("animations").keySet());
            }
        }
    }
    @Test void batchTwoDurationsLoopsIdsGridAndOriginalAnimationNames() throws IOException {
        var lengths=Map.of("groove",2.0,"bigdance",4.0,"bow",2.4,"salute",2.0,"laugh",2.4,
                "shrug",1.6,"cheer",2.4,"point",2.0,"dab",1.6,"sit_full",4.0);
        for(var entry:lengths.entrySet()) {
            String name=entry.getKey();var e=shipped(name);
            assertEquals("quarkos_emotes:"+name,e.id());assertEquals(entry.getValue(),e.length());
            assertEquals(Set.of("groove","bigdance").contains(name)?Emote.Loop.LOOP:Emote.Loop.ONCE,e.loop());
            assertTrue(e.cancelOnMove());
            try(var reader=resource(name+"/"+name+".animation.json")) {
                var root=JsonParser.parseReader(reader).getAsJsonObject();assertFalse(root.has("quarkos"));
                assertEquals(Set.of("animation.emote."+(name.equals("sit_full")?"sit":name)),root.getAsJsonObject("animations").keySet());
            }
            for(var bone:e.bones().values())for(var track:List.of(bone.rotation(),bone.position(),bone.scale()))for(var key:track.keys())
                assertEquals(Math.rint(key.time()*20),key.time()*20,1e-8,"20 fps grid");
        }
    }
    @Test void shippedDancesHaveNoPoseJumpAndContinuousVelocityAtWrap() throws IOException {
        for(String name:List.of("groove","bigdance")) {
            var e=shipped(name);var p=new EmotePlayer(e);double h=1e-7;
            p.freeze(e.length()-h);var before=p.pose(0);
            p.freeze(e.length());var seam=p.pose(0);
            p.freeze(e.length()+h);var after=p.pose(0);
            assertEquals(0,p.time(0)-h,1e-12);
            for(String bone:seam.keySet())for(int channel=0;channel<3;channel++) {
                var a=component(before.get(bone),channel);var b=component(seam.get(bone),channel);var c=component(after.get(bone),channel);
                for(int axis=0;axis<3;axis++) {
                    double av=axis(a,axis),bv=axis(b,axis),cv=axis(c,axis);
                    assertEquals(av,cv,.001,name+" pose jump: "+bone);
                    assertEquals((bv-av)/h,(cv-bv)/h,.01,name+" seam velocity: "+bone);
                }
            }
        }
    }
    private Vec3 component(Emote.Pose p,int i) { return switch(i) { case 0 -> p.rotation();case 1 -> p.position();default -> p.scale(); }; }
    private double axis(Vec3 v,int i) { return switch(i) { case 0 -> v.x();case 1 -> v.y();default -> v.z(); }; }
    @Test void geometryCubesUvPivotsAndRotation() {
        var geometries=PropGeometry.parse(new StringReader("""
            {"format_version":"1.12.0","minecraft:geometry":[{"description":{"texture_width":64,"texture_height":32},"bones":[
            {"name":"Table.Custom","pivot":[1,2,-20],"rotation":[10,20,30],"cubes":[
            {"origin":[-8,11,-20],"size":[16,2,12],"uv":[4,8],"pivot":[2,3,4],"rotation":[5,6,7],"mirror":true,"inflate":0.25}]}]}]}
            """),"test:texture.png");
        assertEquals(1,geometries.size());var geo=geometries.getFirst();assertEquals(64,geo.textureWidth());assertEquals(32,geo.textureHeight());
        assertEquals("test:texture.png",geo.texture());var bone=geo.bones().getFirst();
        assertEquals("Table.Custom",bone.name());assertEquals(new Vec3(1,2,-20),bone.pivot());assertEquals(new Vec3(10,20,30),bone.rotation());
        var cube=bone.cubes().getFirst();assertEquals(new Vec3(-8,11,-20),cube.origin());assertEquals(new Vec3(16,2,12),cube.size());
        assertEquals(4,cube.u());assertEquals(8,cube.v());assertEquals(new Vec3(2,3,4),cube.pivot());assertEquals(new Vec3(5,6,7),cube.rotation());
        assertTrue(cube.mirror());assertEquals(.25,cube.inflate());
        var emote=new EmoteParser().parse(new StringReader("""
            {"animations":{"animation.emote.custom":{"animation_length":1,"bones":{"Table.Custom":{"rotation":[25,0,0]}}}}}
            """),"test","custom.animation.json",null).emotes().getFirst();
        assertEquals(new Vec3(25,0,0),bone.sample(new EmotePlayer(emote),0).rotation());
        assertFalse(emote.bones().containsKey("table.custom"));
    }
    @Test void bindsByGeoBoneNameAndTableStandsInFrontAtFeet() throws IOException {
        PropGeometry geo;
        try(var reader=resource("tableflip/table_prop.geo.json")) {geo=PropGeometry.parse(reader,"test:table_prop.png").getFirst();}
        assertEquals(64,geo.textureWidth());assertEquals(64,geo.textureHeight());
        var bone=geo.bones().getFirst();assertEquals("table",bone.name());assertEquals(new Vec3(0,0,-20),bone.pivot());assertEquals(5,bone.cubes().size());
        var player=new EmotePlayer(shipped("tableflip").withProps(List.of(geo)));player.freeze(.75);
        var sample=bone.sample(player,0);
        assertEquals(new Vec3(110,0,-6),sample.rotation());assertEquals(new Vec3(0,12,-3),sample.position());
        assertFalse(player.emote().bones().containsKey("table_prop"));
        assertEquals(0,geo.bones().stream().flatMap(b->b.cubes().stream()).mapToDouble(c->c.origin().y()).min().orElseThrow());
        for(var cube:bone.cubes()) {
            assertTrue(cube.origin().z()+cube.size().z()<0,"front is negative Z");
            assertEquals(-cube.origin().y(),BedrockAxes.geometry(cube.origin()).y());
        }
        assertTrue(player.propsVisible(0));player.stop();assertFalse(player.propsVisible(0));
        for(int i=0;i<4;i++)player.tick();assertTrue(player.finished());assertEquals(0,player.weight(0));
        var natural=new EmotePlayer(player.emote());assertFalse(natural.propsVisible(0));
        natural.tick();assertTrue(natural.propsVisible(0));
        for(int i=0;i<50;i++)natural.tick();assertTrue(natural.stopping());assertFalse(natural.propsVisible(0));
    }
    @Test void flatBodyPoseDoesNotCarryOtherPlayerBones() {
        var rest=new Emote.Pose(Vec3.ZERO,new Vec3(0,0,0),Vec3.ONE);
        var head=new Emote.Pose(new Vec3(.1,.2,.3),new Vec3(0,0,0),Vec3.ONE);
        var arm=new Emote.Pose(Vec3.ZERO,new Vec3(-5,2,0),Vec3.ONE);
        var baseline=Map.of("body",rest,"head",head,"right_arm",arm);
        var animation=Map.of("body",new Emote.Pose(new Vec3(35,20,10),new Vec3(4,6,2),Vec3.ONE));
        var posed=PoseMath.flat(baseline,animation,1);
        assertEquals(head,posed.get("head"));assertEquals(arm,posed.get("right_arm"));
        assertEquals(new Vec3(4,-6,2),posed.get("body").position());assertEquals(Math.toRadians(35),posed.get("body").rotation().x());
        assertEquals(rest,baseline.get("body"));
    }
    @Test void offsetsAndRotationsAddToModelDefaultsIncludingSlimPivot() {
        var animation=new Emote.Pose(new Vec3(20,30,40),new Vec3(-1.5,.5,2),Vec3.ONE);
        for(double shoulderY:new double[]{2,2.5}) {
            var base=new Emote.Pose(new Vec3(.1,.2,.3),new Vec3(-5,shoulderY,0),Vec3.ONE);
            var pose=PoseMath.additive(base,animation,.5);
            assertEquals(-5.75,pose.position().x());assertEquals(shoulderY-.25,pose.position().y());assertEquals(1,pose.position().z());
            assertEquals(.1+Math.toRadians(10),pose.rotation().x(),1e-9);
            assertEquals(.2+Math.toRadians(15),pose.rotation().y(),1e-9);assertEquals(.3+Math.toRadians(20),pose.rotation().z(),1e-9);
            assertEquals(base,PoseMath.additive(base,animation,0));
        }
    }
    @Test void folderAndFlatDiscoveryAndFilenameIds() throws IOException {
        for(String name:List.of("wave/wave.animation.json","legacy.json","flat.animation.json","wave/wave.quarkos.json","wave/table_prop.geo.json","wave/ignored.json","wave/table_prop.png")) {
            var file=directory.resolve(name);Files.createDirectories(file.getParent());Files.writeString(file,"{}");
        }
        assertEquals(Set.of("wave/wave.animation.json","legacy.json","flat.animation.json"),EmoteFiles.discover(directory).stream()
                .map(directory::relativize).map(Path::toString).collect(java.util.stream.Collectors.toSet()));
        assertTrue(EmoteFiles.animation("emotes/wave/wave.animation.json"));assertFalse(EmoteFiles.animation("emotes/wave/wave.quarkos.json"));
        String json="{\"animations\":{\"animation.emote.unrelated\":{\"animation_length\":1}}}";
        for(String file:List.of("emotes/my_dance.animation.json","my_dance.json")) {
            var e=new EmoteParser().parse(new StringReader(json),"test",file,null).emotes().getFirst();
            assertEquals("test:my_dance",e.id());assertEquals("My Dance",e.name());
        }
    }
    @Test void sidecarMetadataAndInlinePrecedence() {
        var side=JsonParser.parseString("""
            {"name":"emote.test.wave","icon":"minecraft:diamond","category":"reaction","cancel_on_move":false,
            "sound":{"id":"minecraft:block.wood.hit","at":0.5,"volume":0.6,"pitch":1.2}}
            """).getAsJsonObject();
        String plain="{\"animations\":{\"animation.emote.wave\":{\"animation_length\":1}}}";
        var e=new EmoteParser().parse(new StringReader(plain),"test","wave.animation.json",side).emotes().getFirst();
        assertEquals("test:wave",e.id());assertEquals("emote.test.wave",e.name());assertEquals("minecraft:diamond",e.icon());
        assertEquals("reaction",e.category());assertFalse(e.cancelOnMove());assertEquals(1,e.sounds().size());
        assertEquals(.5,e.sounds().getFirst().time());assertEquals(.6,e.sounds().getFirst().volume(),1e-6);assertEquals(1.2,e.sounds().getFirst().pitch(),1e-6);
        for(String inline:List.of(
                "{\"quarkos\":{\"name\":\"Root name\"},\"animations\":{\"animation.wave\":{\"animation_length\":1}}}",
                "{\"animations\":{\"animation.wave\":{\"animation_length\":1,\"quarkos\":{\"name\":\"Root name\"}}}}")) {
            assertEquals("Root name",new EmoteParser().parse(new StringReader(inline),"test","wave.json",side).emotes().getFirst().name());
        }
    }
}
