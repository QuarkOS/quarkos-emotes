package dev.quarkos.emotes.animation;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
public record Emote(String id, String name, String icon, String category, double length, Loop loop,
                    boolean cancelOnMove, String easing, Map<String, Bone> bones, List<Sound> sounds, JsonObject prop) {
    public enum Loop { ONCE, LOOP, HOLD }
    public record Sound(double time, String id, float volume, float pitch) {}
    public record Bone(Track rotation, Track position, Track scale) {
        public Pose sample(double time, String easing) {
            return new Pose(rotation.sample(time,easing,Vec3.ZERO),position.sample(time,easing,Vec3.ZERO),scale.sample(time,easing,Vec3.ONE));
        }
    }
    public record Pose(Vec3 rotation, Vec3 position, Vec3 scale) {
        public boolean moved() { return rotation.magnitude()+position.magnitude()+(Math.abs(scale.x()-1)+Math.abs(scale.y()-1)+Math.abs(scale.z()-1))>0.000001; }
    }
    public Emote { bones=Map.copyOf(bones); sounds=List.copyOf(sounds); prop=prop==null ? null : prop.deepCopy(); }
    public double wrap(double time) { return loop==Loop.LOOP && length>0 ? ((time%length)+length)%length : Math.max(0,Math.min(length,time)); }
}
