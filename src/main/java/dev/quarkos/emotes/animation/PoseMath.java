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
        // A shared translation moves the whole rig. Blending its
        // translation and leg angles independently can sweep the feet below the
        // floor (even when every authored key is clear). Lift all siblings by
        // the same amount so their relative positions remain intact.
        var leg=animation.get("right_leg");
        // Torso/shoulder pivot compensation can add different Y offsets above
        // the shared root (Blockato's sit). Matching leg offsets still identify
        // the common drop; carry the correction through every sibling.
        if(leg!=null&&baseline.size()==6&&baseline.keySet().stream().allMatch(animation::containsKey)
                &&Math.abs(animation.get("left_leg").position().y()-leg.position().y())<1e-6) {
            double lift=Math.max(0,Math.max(foot(result.get("right_leg")),foot(result.get("left_leg")))
                    -Math.max(foot(baseline.get("right_leg")),foot(baseline.get("left_leg"))));
            if(lift>0)result.replaceAll((name,p)->new Emote.Pose(p.rotation(),
                    new Vec3(p.position().x(),p.position().y()-lift,p.position().z()),p.scale()));
        }
        return result;
    }
    private static double foot(Emote.Pose p) {
        Vec3 r=p.rotation(),s=p.scale();double max=Double.NEGATIVE_INFINITY;
        for(double x:new double[]{-2*s.x(),2*s.x()})for(double y:new double[]{0,12*s.y()})for(double z:new double[]{-2*s.z(),2*s.z()}) {
            double ry=y*Math.cos(r.x())-z*Math.sin(r.x()),rz=y*Math.sin(r.x())+z*Math.cos(r.x());
            double rx=x*Math.cos(r.y())+rz*Math.sin(r.y());
            max=Math.max(max,p.position().y()+rx*Math.sin(r.z())+ry*Math.cos(r.z()));
        }
        return max;
    }
}
