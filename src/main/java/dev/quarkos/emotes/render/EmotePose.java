package dev.quarkos.emotes.render;

import dev.quarkos.emotes.animation.*;
import dev.quarkos.emotes.EmotesClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
public final class EmotePose {
    public static void apply(HumanoidModel<?> model,int entityId,float partial) {
        var playback=EmotesClient.entity(entityId); if(playback==null) return;
        var pose=playback.pose(partial); float weight=(float)playback.weight(partial);
        double[] before=(dev.quarkos.emotes.ClientProofHooks.visual()||dev.quarkos.emotes.ClientProofHooks.multiplayer())?snapshot(model):null;
        var parts=java.util.Map.of("head",model.head,"body",model.body,"right_arm",model.rightArm,
                "left_arm",model.leftArm,"right_leg",model.rightLeg,"left_leg",model.leftLeg);
        var baseline=new java.util.LinkedHashMap<String,Emote.Pose>();
        parts.forEach((name,part)->baseline.put(name,playerBase(part,name,playback.emote(),weight)));
        PoseMath.flat(baseline,pose,weight).forEach((name,result)->set(parts.get(name),result));
        if(dev.quarkos.emotes.ClientProofHooks.visual())dev.quarkos.emotes.ClientProofHooks.rotation(entityId,model.body.xRot,model.body.yRot);
        if(before!=null)dev.quarkos.emotes.ClientProofHooks.record(entityId,changed(before,snapshot(model)));
    }
    private static double[] snapshot(HumanoidModel<?> m) {
        double[] values=new double[54];int i=0;
        for(var p:new ModelPart[]{m.head,m.body,m.rightArm,m.leftArm,m.rightLeg,m.leftLeg}) {
            values[i++]=p.xRot;values[i++]=p.yRot;values[i++]=p.zRot;
            values[i++]=p.x;values[i++]=p.y;values[i++]=p.z;
            values[i++]=p.xScale;values[i++]=p.yScale;values[i++]=p.zScale;
        } return values;
    }
    private static boolean changed(double[] before,double[] after) {
        for(int i=0;i<before.length;i++)if(Math.abs(before[i]-after[i])>.0001)return true;return false;
    }
    public static void apply(ModelPart part,Emote.Pose pose,float weight) {
        if(pose==null) return;
        set(part,PoseMath.additive(current(part),pose,weight));
    }
    public static void applyArm(ModelPart part,String bone,Emote emote,Emote.Pose pose,float weight) {
        if(pose!=null)set(part,PoseMath.additive(playerBase(part,bone,emote,weight),pose,weight));
    }
    private static Emote.Pose playerBase(ModelPart part,String bone,Emote emote,float weight) {
        // Initial pose survives animation offsets and identifies the baked model
        // in both MC versions, including the first-person arm render path.
        //? if >=1.21.11 {
        float initialY=part.getInitialPose().y();
        //?} else {
        /*float initialY=part.getInitialPose().y;
        *///?}
        return bone.endsWith("_arm")?PoseMath.armPivot(current(part),Math.abs(initialY-2.5f)<.001f,emote.armPivot(),weight):current(part);
    }
    private static Emote.Pose current(ModelPart part) {
        return new Emote.Pose(new Vec3(part.xRot,part.yRot,part.zRot),new Vec3(part.x,part.y,part.z),new Vec3(part.xScale,part.yScale,part.zScale));
    }
    private static void set(ModelPart part,Emote.Pose blended) {
        Vec3 r=blended.rotation(),p=blended.position(),s=blended.scale();
        part.xRot=(float)r.x(); part.yRot=(float)r.y(); part.zRot=(float)r.z();
        // Vanilla resets part positions before each setupAnim; offsets remain in model pixels.
        part.x=(float)p.x(); part.y=(float)p.y(); part.z=(float)p.z();
        part.xScale=(float)s.x(); part.yScale=(float)s.y(); part.zScale=(float)s.z();
    }
}
