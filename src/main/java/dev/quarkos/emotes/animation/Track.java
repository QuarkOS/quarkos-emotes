package dev.quarkos.emotes.animation;

import java.util.List;
import java.util.Comparator;
public record Track(List<Key> keys) {
    public enum Mode { LINEAR, STEP, CATMULLROM }
    public record Key(double time, Vec3 pre, Vec3 post, Mode mode) {}
    public Track { keys = keys.stream().sorted(Comparator.comparingDouble(Key::time)).toList(); }
    public Vec3 sample(double time, String easing, Vec3 fallback) {
        if (keys.isEmpty()) return fallback;
        if (time < keys.getFirst().time()) return keys.getFirst().pre();
        for (int i=0;i<keys.size()-1;i++) {
            Key a=keys.get(i), b=keys.get(i+1);
            if (time >= b.time()) continue;
            double t=(time-a.time())/(b.time()-a.time());
            return switch (a.mode()) {
                case STEP -> a.post();
                case LINEAR -> a.post().lerp(b.pre(), Easing.apply(easing,t));
                case CATMULLROM -> Vec3.catmull(keys.get(Math.max(0,i-1)).post(),a.post(),b.pre(),
                        keys.get(Math.min(keys.size()-1,i+2)).pre(),t);
            };
        }
        return keys.getLast().post();
    }
}
