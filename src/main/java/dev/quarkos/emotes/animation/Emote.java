package dev.quarkos.emotes.animation;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
public record Emote(String id, String name, String icon, String category, double length, Loop loop,
                    boolean cancelOnMove, String easing, Map<String, Bone> bones, List<Sound> sounds, JsonObject prop, List<PropGeometry> props, ArmPivot armPivot, Emote enter, Emote followLoop) {
    public Emote(String id, String name, String icon, String category, double length, Loop loop,
                 boolean cancelOnMove, String easing, Map<String,Bone> bones, List<Sound> sounds, JsonObject prop,
                 List<PropGeometry> props, ArmPivot armPivot) {
        this(id,name,icon,category,length,loop,cancelOnMove,easing,bones,sounds,prop,props,armPivot,null,null);
    }
    public Emote withChain(Emote entry, Emote repeat) {
        return new Emote(id,name,icon,category,length,loop,cancelOnMove,easing,bones,sounds,prop,props,armPivot,entry,
                loop==Loop.HOLD?repeat:null);
    }
    public double entryLength() { return enter==null?0:enter.length(); }
    /** Timeline duration, excluding the exit fade. Holds and repeating chains never expire. */
    public double totalLength() { return loop==Loop.ONCE?entryLength()+length:Double.POSITIVE_INFINITY; }
    public record Segment(String name, Emote animation, double time) {}
    public Segment segment(double elapsed) {
        double t=Math.max(0,elapsed);
        if(enter!=null && t<enter.length()-1e-9) return new Segment("enter",enter,t);
        t=Math.max(0,t-entryLength());
        if(loop==Loop.HOLD && followLoop!=null && t>=length-1e-9)
            return new Segment("loop",followLoop,followLoop.wrap(Math.max(0,t-length)));
        return new Segment("main",this,wrap(t));
    }
    public Emote(String id, String name, String icon, String category, double length, Loop loop,
                 boolean cancelOnMove, String easing, Map<String,Bone> bones, List<Sound> sounds, JsonObject prop, List<PropGeometry> props) {
        this(id,name,icon,category,length,loop,cancelOnMove,easing,bones,sounds,prop,props,ArmPivot.MODEL);
    }
    public Emote(String id, String name, String icon, String category, double length, Loop loop,
                 boolean cancelOnMove, String easing, Map<String,Bone> bones, List<Sound> sounds, JsonObject prop) {
        this(id,name,icon,category,length,loop,cancelOnMove,easing,bones,sounds,prop,List.of());
    }
    public Emote withProps(List<PropGeometry> geometries) {
        return new Emote(id,name,icon,category,length,loop,cancelOnMove,easing,bones,sounds,prop,geometries,armPivot,enter,followLoop);
    }
    public enum Loop { ONCE, LOOP, HOLD }
    public enum ArmPivot { MODEL, STEVE }
    public record Sound(double time, String id, float volume, float pitch) {}
    public record Bone(Track rotation, Track position, Track scale) {
        public Pose sample(double time, String easing) {
            return sample(time,easing,false);
        }
        public Pose sample(double time, String easing, boolean loop) {
            return new Pose(rotation.sample(time,easing,Vec3.ZERO,loop),position.sample(time,easing,Vec3.ZERO,loop),scale.sample(time,easing,Vec3.ONE,loop));
        }
    }
    public record Pose(Vec3 rotation, Vec3 position, Vec3 scale) {
        public boolean moved() { return rotation.magnitude()+position.magnitude()+(Math.abs(scale.x()-1)+Math.abs(scale.y()-1)+Math.abs(scale.z()-1))>0.000001; }
    }
    public Emote { props=List.copyOf(props); bones=Map.copyOf(bones); sounds=List.copyOf(sounds); prop=prop==null ? null : prop.deepCopy(); }
    public double wrap(double time) {
        if(loop!=Loop.LOOP||length<=0)return Math.max(0,Math.min(length,time));
        double phase=time%length;
        return phase<0?phase+length:phase;
    }
}
