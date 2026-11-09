package dev.quarkos.emotes.animation;

import java.util.*;

/** Independent sibling transforms. The baseline is the model's current vanilla pose. */
public final class PoseMath {
    private PoseMath() {}
    public static Emote.Pose additive(Emote.Pose base,Emote.Pose animation,double weight) {
        if(animation==null) return base;
        Vec3 r=BedrockAxes.rotation(animation.rotation()),p=BedrockAxes.position(animation.position()),s=animation.scale();
        return new Emote.Pose(add(base.rotation(),r,weight),add(base.position(),p,weight),
                new Vec3(base.scale().x()*(1+(s.x()-1)*weight),base.scale().y()*(1+(s.y()-1)*weight),base.scale().z()*(1+(s.z()-1)*weight)));
    }
    private static Vec3 add(Vec3 a,Vec3 b,double w) { return new Vec3(a.x()+b.x()*w,a.y()+b.y()*w,a.z()+b.z()*w); }
    public static Map<String,Emote.Pose> flat(Map<String,Emote.Pose> baseline,Map<String,Emote.Pose> animation,double weight) {
        Map<String,Emote.Pose> result=new LinkedHashMap<>();
        baseline.forEach((name,base)->result.put(name,additive(base,animation.get(name),weight)));
        return result;
    }
}
