package dev.quarkos.emotes.render;

import dev.quarkos.emotes.animation.*;
import dev.quarkos.emotes.EmotesClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
public final class EmotePose {
    public static void apply(HumanoidModel<?> model,int entityId,float partial) {
        var playback=EmotesClient.entity(entityId); if(playback==null) return;
        var pose=playback.pose(partial); float weight=(float)playback.weight(partial);
        double[] before=dev.quarkos.emotes.harness.ClientHarness.active()?snapshot(model):null;
        apply(model.head,pose.get("head"),weight); apply(model.body,pose.get("body"),weight);
        apply(model.rightArm,pose.get("right_arm"),weight); apply(model.leftArm,pose.get("left_arm"),weight);
        apply(model.rightLeg,pose.get("right_leg"),weight); apply(model.leftLeg,pose.get("left_leg"),weight);
        if(before!=null)dev.quarkos.emotes.harness.ClientHarness.record(entityId,changed(before,snapshot(model)));
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
        Vec3 r=BedrockAxes.rotation(pose.rotation()), p=BedrockAxes.position(pose.position()), s=pose.scale();
        part.xRot+=(float)(r.x()-part.xRot)*weight; part.yRot+=(float)(r.y()-part.yRot)*weight; part.zRot+=(float)(r.z()-part.zRot)*weight;
        // Vanilla resets part positions before each setupAnim; offsets remain in model pixels.
        part.x+=(float)p.x()*weight; part.y+=(float)p.y()*weight; part.z+=(float)p.z()*weight;
        part.xScale=1+(float)(s.x()-1)*weight; part.yScale=1+(float)(s.y()-1)*weight; part.zScale=1+(float)(s.z()-1)*weight;
    }
}
