package dev.quarkos.emotes.animation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ShippedPoseTest {
    private static final String ROOT="/assets/quarkos_emotes/emotes/";
    private static Reader resource(String path) {
        return new InputStreamReader(Objects.requireNonNull(ShippedPoseTest.class.getResourceAsStream(path),path),StandardCharsets.UTF_8);
    }
    private static Emote load(String name,boolean folder) throws IOException {
        String base=(folder?name+"/":"")+name;
        JsonObject sidecar=null;
        if(folder)try(var reader=resource(ROOT+base+".quarkos.json")){sidecar=JsonParser.parseReader(reader).getAsJsonObject();}
        try(var reader=resource(ROOT+base+".animation.json")) {
            var parsed=new EmoteParser().parse(reader,"quarkos_emotes",base+".animation.json",sidecar);
            assertTrue(parsed.valid(),parsed.errors().toString());return parsed.emotes().getFirst();
        }
    }
    private static Map<String,Emote.Pose> rest() {
        var map=new LinkedHashMap<String,Emote.Pose>();
        for(String name:List.of("head","body","right_arm","left_arm","right_leg","left_leg")) {
            double x=name.startsWith("right")?-1.9:name.startsWith("left")?1.9:0;
            double y=name.endsWith("leg")?12:name.endsWith("arm")?2:0;
            map.put(name,new Emote.Pose(Vec3.ZERO,new Vec3(x,y,0),Vec3.ONE));
        }
        return map;
    }
    @Test void seatedLegsStayAboveFloorDuringEntryHoldAndBlendedExit() throws IOException {
        var e=load("sit_full",true);var player=new EmotePlayer(e);
        for(int frame=0;frame<=960;frame++) {
            double time=frame/240.0;player.freeze(time);
            // Includes every exit blend weight, not just authored full-weight poses.
            for(int blend=0;blend<=20;blend++) {
                var pose=PoseMath.flat(rest(),player.pose(0),blend/20.0);
                for(String leg:List.of("right_leg","left_leg")) {
                    var p=pose.get(leg);double angle=p.rotation().x();
                    for(double y:new double[]{0,12})for(double z:new double[]{-2,2}) {
                        double floor=24-p.position().y()-y*Math.cos(angle)+z*Math.sin(angle);
                        assertTrue(floor>=-1e-6,"Foot below floor at t="+time+", weight="+blend/20.0+": "+floor);
                    }
                }
                // Correcting the feet must carry all siblings together.
                assertEquals(pose.get("head").position().y(),pose.get("body").position().y(),1e-9);
                assertEquals(12+(player.pose(0).get("body").position().y()-player.pose(0).get("right_leg").position().y())*blend/20.0,
                        pose.get("right_leg").position().y()-pose.get("body").position().y(),1e-9);
            }
        }
    }
    @Test void bowMaintainsHipNeckAndShoulderConnectionsOnTheFlatRig() throws IOException {
        var e=load("bow",true);
        for(var key:e.bones().get("body").rotation().keys()) {
            double t=key.time(),angle=Math.toRadians(key.post().x());
            var body=e.bones().get("body").sample(t,e.easing());
            // The animator bends about the front hip edge at model [0,12,-2].
            assertEquals(12,-body.position().y()+12*Math.cos(angle)+2*Math.sin(angle),.002);
            assertEquals(-2,body.position().z()+12*Math.sin(angle)-2*Math.cos(angle),.002);
            assertEquals(body.position(),e.bones().get("head").sample(t,e.easing()).position());
            for(String arm:List.of("right_arm","left_arm")) {
                var shoulder=e.bones().get(arm).sample(t,e.easing()).position();
                assertEquals(-body.position().y()+2*Math.cos(angle),2-shoulder.y(),.002);
                assertEquals(body.position().z()+2*Math.sin(angle),shoulder.z(),.002);
            }
        }
    }
    @Test void shippedOneShotsReachTheirLastPoseAndFadeWithoutASnap() throws IOException {
        for(String name:List.of("wave","thinking","tableflip","facepalm","clap","bow","salute","laugh","shrug","cheer","point","dab","sit_full","stretch")) {
            var e=load(name,!name.equals("stretch"));var p=new EmotePlayer(e);
            while(!p.stopping())p.tick();
            assertEquals(e.length(),p.time(0),1e-8);
            assertTrue(p.weight(0)>.74,name+" must retain the pose at the natural endpoint");
            var finalPose=p.pose(0);double weight=p.weight(0);
            while(!p.finished()) {
                p.tick();assertEquals(finalPose,p.pose(.5),name+" last pose must stay fixed");
                assertTrue(p.weight(0)<=weight,name+" exit must fade toward the default pose");weight=p.weight(0);
            }
            assertEquals(0,p.weight(0),1e-8,name);
        }
    }
    @Test void allThirtyTwoShippedIdsAndNamesResolveFromFlatAndFolderFiles() throws IOException {
        Set<String> ids=new HashSet<>();JsonObject lang;
        try(var reader=resource("/assets/quarkos_emotes/lang/en_us.json")){lang=JsonParser.parseReader(reader).getAsJsonObject();}

        for(String name:List.of("wave","thinking","tableflip","facepalm","clap","bow","salute","laugh","dab","sit_full","groove","bigdance","shrug","point","cheer","stretch","yes","no","cry","rage","victory","heart_hands","wave_big","backflip","spin","worm","floss","tpose","zombie_walk","slow_clap","sleep","sit")) {
            var e=load(name,!name.equals("stretch"));
            assertEquals("quarkos_emotes:"+name,e.id());assertTrue(ids.add(e.id()));
            assertTrue(lang.has(e.name()),"Missing display name: "+e.name());
        }
        assertEquals(32,ids.size());
        try {
            var folder=java.nio.file.Path.of(Objects.requireNonNull(getClass().getResource(ROOT)).toURI());
            assertEquals(32,EmoteFiles.discover(folder).size(),"no unlisted or duplicate built-ins");
        } catch(java.net.URISyntaxException e) {throw new AssertionError(e);}
    }
}
