package dev.quarkos.emotes.animation;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
public record Track(List<Key> keys) {
    public enum Mode { LINEAR, STEP, CATMULLROM }
    public record Key(double time, Vec3 pre, Vec3 post, Mode mode) {}
    public Track {
        // Stable order preserves the incoming value and final outgoing value
        // when distinct JSON timestamps (e.g. 0.25 and 0.250) parse equally.
        var merged=new ArrayList<Key>();
        for(var key:keys.stream().sorted(Comparator.comparingDouble(Key::time)).toList()) {
            if(!merged.isEmpty()&&merged.getLast().time()==key.time()) {
                var first=merged.removeLast();
                merged.add(new Key(key.time(),first.pre(),key.post(),key.mode()));
            } else merged.add(key);
        }
        keys=List.copyOf(merged);
    }
    public Vec3 sample(double time, String easing, Vec3 fallback) {
        return sample(time,easing,fallback,false);
    }
    public Vec3 sample(double time, String easing, Vec3 fallback, boolean loop) {
        if (keys.isEmpty()) return fallback;
        if (time < keys.getFirst().time()) return keys.getFirst().pre();
        for (int i=0;i<keys.size()-1;i++) {
            Key a=keys.get(i), b=keys.get(i+1);
            if (time >= b.time()) continue;
            if (time == a.time()) return a.post();
            double t=(time-a.time())/(b.time()-a.time());
            return switch (a.mode()) {
                case STEP -> a.post();
                case LINEAR -> a.post().lerp(b.pre(), Easing.apply(easing,t));
                // Blockbench 5.2.2: the seam key is duplicated, so skip it when
                // looking across a looping track's endpoints.
                case CATMULLROM -> Vec3.catmull(!a.pre().equals(a.post()) ? a.post() :
                        keys.get(i==0 && loop && keys.size()>2 ? keys.size()-2 : Math.max(0,i-1)).post(),a.post(),b.pre(),
                        !b.pre().equals(b.post()) ? b.pre() :
                        keys.get(i+2==keys.size() && loop && keys.size()>2 ? 1 : Math.min(keys.size()-1,i+2)).pre(),t);
            };
        }
        return keys.getLast().post();
    }
}
