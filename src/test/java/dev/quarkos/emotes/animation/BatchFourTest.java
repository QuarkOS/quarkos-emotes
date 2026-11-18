package dev.quarkos.emotes.animation;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BatchFourTest {
    @TempDir Path folder;
    private Reader resource(String file) {
        var in=getClass().getResourceAsStream("/assets/quarkos_emotes/emotes/"+file);
        return in==null?null:new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8);
    }
    private Emote shipped(String name) throws IOException {
        String file=name+"/"+name+".animation.json";
        try(var r=resource(file);var side=resource(name+"/"+name+".quarkos.json")) {
            var parsed=new EmoteParser().parse(r,"quarkos_emotes",file,JsonParser.parseReader(side).getAsJsonObject());
            assertTrue(parsed.valid(),parsed.errors().toString());
            return EmoteFiles.chained(parsed.emotes().getFirst(),file,this::resource);
        }
    }
    private Emote clip(String name,double length,String mode) {
        String json="{\"animations\":{\"animation.emote."+name+"\":{\"animation_length\":"+length+",\"loop\":"+mode+",\"bones\":{\"body\":{\"rotation\":[90,0,0]}}}}}";
        return new EmoteParser().parse(new StringReader(json),"test",name+".animation.json",null).emotes().getFirst();
    }
    @Test void strictDiscoveryFixtureIgnoresAllExtrasAndArchives() throws IOException {
        var expected=Set.of("sit/sit.animation.json","nested/wave/wave.animation.json","flat.animation.json","legacy.json");
        var paths=new HashSet<>(expected);
        paths.addAll(List.of("sit/sit_enter.animation.json","sit/sit_loop.animation.json","sit/sit_v1.animation.json",
                "sit/sit_v2.animation.json","sit/clipcheck.json","sit/sit.quarkos.json","sit/prop.geo.json","sit/prop.png",
                "sit/proof.gif","sit/author.bbmodel","_archive/sit/sit.animation.json","sit/_old/old/old.animation.json"));
        for(String path:paths) {var p=folder.resolve(path);Files.createDirectories(p.getParent());Files.writeString(p,"{}");}
        assertEquals(expected,new HashSet<>(EmoteFiles.discover(folder).stream().map(folder::relativize).map(p->p.toString().replace('\\','/')).toList()));
        for(String path:paths)assertEquals(expected.contains(path),EmoteFiles.animation("emotes/"+path),path);
    }
    @Test void optionalFilesAttachOnlyToFolderMainAndHoldLoop() throws IOException {
        var main=clip("test",1,"false");
        assertNull(EmoteFiles.chained(main,"test.animation.json",p->{fail("flat file has no chain");return null;}).enter());
        var absent=EmoteFiles.chained(main,"test/test.animation.json",p->null);
        assertNull(absent.enter());assertNull(absent.followLoop());
        String entry="{\"animations\":{\"entry\":{\"animation_length\":0.5,\"loop\":false}}}";
        var present=EmoteFiles.chained(main,"test/test.animation.json",p->p.contains("_enter")?new StringReader(entry):null);
        assertEquals(.5,present.entryLength());assertEquals("test:test",present.id());
        assertThrows(IOException.class,()->EmoteFiles.chained(main,"test/test.animation.json",p->new StringReader(entry.replace("false","true"))));
        assertNull(main.withChain(null,clip("loop",1,"true")).followLoop());
    }
    @Test void batchFourModesLengthsAndPivots() throws IOException {
        var names=List.of("jump_hype","fishing_cast","sword_salute","guitar","drums","pushups","default_dance","kneel","lay_down","meditate");
        double[] lengths={2,2.8,2.4,2,2,2,4,1.6,2.6,5.6};
        for(int i=0;i<names.size();i++) {
            String name=names.get(i);var e=shipped(name);assertEquals(lengths[i],e.length());
            assertEquals(i<3?Emote.Loop.ONCE:i<7?Emote.Loop.LOOP:Emote.Loop.HOLD,e.loop());
            assertEquals(Set.of("lay_down","pushups","sword_salute").contains(name)?Emote.ArmPivot.STEVE:Emote.ArmPivot.MODEL,e.armPivot());
            assertEquals("quarkos_emotes:"+name,e.id());assertTrue(e.cancelOnMove());
        }
    }
    private void samePose(Map<String,Emote.Pose> a,Map<String,Emote.Pose> b,double tolerance) {
        assertEquals(a.keySet(),b.keySet());
        for(String bone:a.keySet()) {
            var x=a.get(bone);var y=b.get(bone);
            for(var pair:List.of(List.of(x.rotation(),y.rotation()),List.of(x.position(),y.position()),List.of(x.scale(),y.scale()))) {
                assertEquals(pair.get(0).x(),pair.get(1).x(),tolerance,bone);
                assertEquals(pair.get(0).y(),pair.get(1).y(),tolerance,bone);
                assertEquals(pair.get(0).z(),pair.get(1).z(),tolerance,bone);
            }
        }
    }
    @Test void enterHandoffIsContinuousAtFullWeightAndLoopsWithoutReentering() throws IOException {
        var e=shipped("pushups");assertEquals(1.3,e.entryLength());
        var p=new EmotePlayer(e);p.seek(1.3-1e-7);assertEquals("enter",p.segment(0).name());var before=p.pose(0);
        p.seek(1.3);assertEquals("main",p.segment(0).name());assertEquals(0,p.time(0));assertEquals(1,p.weight(0));
        samePose(before,p.pose(0),.001);
        var seam=p.pose(0);p.seek(3.3);samePose(seam,p.pose(0),.001);assertEquals("main",p.segment(0).name());
        p.seek(1.25);p.tick();assertEquals("main",p.segment(0).name());
    }
    @Test void heldMainHandsOffToBreathingLoopAtFullWeight() throws IOException {
        var e=shipped("meditate");var p=new EmotePlayer(e);p.seek(5.6-1e-7);var before=p.pose(0);
        assertEquals("main",p.segment(0).name());p.seek(5.6);
        assertEquals("loop",p.segment(0).name());assertEquals(EmotePlayer.State.PLAYING,p.state());assertEquals(1,p.weight(0));
        samePose(before,p.pose(0),.001);var exhale=p.pose(0);p.seek(6.6);
        assertNotEquals(exhale,p.pose(0));p.seek(7.6);samePose(exhale,p.pose(0),.001);
    }
    @Test void lateJoinAndReloadUseTotalElapsedForCorrectSegmentAndPhase() throws IOException {
        for(String name:List.of("pushups","meditate")) {
            var e=shipped(name);double start=name.equals("pushups")?1.3:5.6;
            var p=new EmotePlayer(e);p.seek(start+12.75);
            assertEquals(.75,p.time(0),1e-8);assertEquals(name.equals("pushups")?"main":"loop",p.segment(0).name());
            var rebound=p.rebind(e.withProps(List.of()));samePose(p.pose(0),rebound.pose(0),0);
            assertTrue(p.sounds().isEmpty());p.tick();assertEquals(.8,p.time(0),1e-8);
            p.seek(.25);assertEquals(name.equals("pushups")?"enter":"main",p.segment(0).name());
        }
    }
    @Test void cancelFreezesAndFadesAtEveryStageIncludingEarlyEntryAndHeldPose() throws IOException {
        for(String name:List.of("pushups","meditate","kneel","lay_down"))for(double t:new double[]{.05,.5,1.5,6.75,20}) {
            var p=new EmotePlayer(shipped(name));p.seek(t);var pose=p.pose(0);double w=p.weight(0);p.stop();
            assertFalse(p.propsVisible(0));
            for(int i=1;i<=4;i++) {
                p.tick();samePose(pose,p.pose(.9),0);assertEquals(w*(1-i/4.0),p.weight(0),1e-8);
                assertEquals(i==4,p.finished());
            }
        }
    }
    private Emote withSound(Emote clip,String id) {
        return new Emote(clip.id(),clip.name(),clip.icon(),clip.category(),clip.length(),clip.loop(),true,clip.easing(),clip.bones(),
                List.of(new Emote.Sound(.1,id,1,1)),null);
    }
    @Test void chainSoundsBelongToTheirOwnSegmentAndLateJoinSkipsHistory() {
        var enter=withSound(clip("entry",.2,"false"),"test:enter");
        var main=withSound(clip("main",.2,"true"),"test:main").withChain(enter,null);
        var p=new EmotePlayer(main);
        p.tick();assertTrue(p.sounds().isEmpty());p.tick();assertEquals("test:enter",p.sounds().getFirst().id());
        for(int i=0;i<4;i++) {p.tick();var due=p.sounds();if(!due.isEmpty())assertEquals("test:main",due.getFirst().id());}
        p.seek(10.25);assertTrue(p.sounds().isEmpty());p.tick();assertEquals("test:main",p.sounds().getFirst().id());
        var hold=withSound(clip("hold",.2,"\"hold_on_last_frame\""),"test:hold")
                .withChain(null,withSound(clip("breath",.2,"true"),"test:breath"));
        p=new EmotePlayer(hold);p.seek(.25);p.tick();assertEquals("test:breath",p.sounds().getFirst().id());
    }
    @Test void totalDurationIncludesEntryOnceAndNeverExpiresForLoopsOrHolds() throws IOException {
        var e=clip("once",2,"false").withChain(clip("enter",1,"false"),null);
        assertEquals(3,e.totalLength());var p=new EmotePlayer(e);p.seek(2.9);assertFalse(p.stopping());
        p.tick();p.tick();p.tick();assertTrue(p.stopping());assertEquals(2,p.time(0));
        for(int i=0;i<4;i++)p.tick();assertTrue(p.finished());
        for(String name:List.of("pushups","meditate","kneel","lay_down")) {
            var infinite=shipped(name);assertEquals(Double.POSITIVE_INFINITY,infinite.totalLength());
            p=new EmotePlayer(infinite);p.seek(10000);assertFalse(p.stopping());assertEquals(1,p.weight(0));
        }
    }
    @Test void playerParentAndOwnTransformComposeAroundAbsolutePivots() {
        var rest=new Vec3(-5,2,0);
        // Right arm rotates 90 degrees around X, moves [2,-3,4], scales by 2 on Y.
        var parent=PropTransform.parent(rest,new Emote.Pose(new Vec3(Math.PI/2,0,0),new Vec3(-3,-1,4),new Vec3(1,2,1)));
        var own=PropTransform.geometry(new Vec3(-5,22,0),new Vec3(0,0,90),new Vec3(1,2,3),Vec3.ONE);
        Vec3 point=new Vec3(-4,-22,0);
        assertEquals(new Vec3(-2,-28,2),rounded(parent.apply(own.apply(point))));
        var idle=PropTransform.parent(rest,new Emote.Pose(Vec3.ZERO,rest,Vec3.ONE));assertEquals(point,idle.apply(point));
        // The body is a sibling: it does not participate in the right-arm chain.
        var body=PropTransform.geometry(new Vec3(0,24,0),new Vec3(70,0,0),new Vec3(0,10,0),Vec3.ONE);
        assertNotEquals(rounded(parent.apply(own.apply(point))),rounded(body.apply(parent.apply(own.apply(point)))));
    }
    private Vec3 rounded(Vec3 v) {return new Vec3(Math.rint(v.x()),Math.rint(v.y()),Math.rint(v.z()));}
    @Test void syntheticParentGeometryAcceptsPlayerOrPropParentsAndRejectsUnknownAndCycles() {
        String json="{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"texture_width\":16,\"texture_height\":16},\"bones\":[{\"name\":\"sword\",\"parent\":\"right_arm\",\"pivot\":[-5,22,0]},{\"name\":\"tip\",\"parent\":\"sword\"}]}]}";
        var geo=PropGeometry.parse(new StringReader(json),"test:synthetic.png").getFirst();
        assertEquals("right_arm",geo.bones().getFirst().parent());assertEquals("sword",geo.bones().getLast().parent());
        assertThrows(IllegalArgumentException.class,()->PropGeometry.parse(new StringReader(json.replace("right_arm","unknown")),"test:synthetic.png"));
        assertThrows(IllegalArgumentException.class,()->PropGeometry.parse(new StringReader(json.replace("right_arm","tip")),"test:synthetic.png"));
    }
    @Test void deliveredSwordUsesFlatCopiedArmChannelsAndHidesOnCancel() throws IOException {
        try(var r=resource("sword_salute/sword_prop.geo.json")) {
            var geo=PropGeometry.parse(r,"test:sword_prop.png").getFirst();var bone=geo.bones().getFirst();
            assertNull(bone.parent());assertEquals(new Vec3(-5,22,0),bone.pivot());
            var p=new EmotePlayer(shipped("sword_salute").withProps(List.of(geo)));p.freeze(.8);
            assertEquals(p.pose(0).get("right_arm").rotation(),bone.sample(p,0).rotation());
            assertEquals(p.pose(0).get("right_arm").position(),bone.sample(p,0).position());
            assertTrue(p.propsVisible(0));p.stop();assertFalse(p.propsVisible(0));
        }
    }
}
