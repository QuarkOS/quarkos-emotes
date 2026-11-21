package dev.quarkos.emotes.render;

import dev.quarkos.emotes.EmotesClient;
import dev.quarkos.emotes.animation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Textured box-UV props with optional flat player-bone parents. */
public final class GeometryProps {
    private static final Map<PropGeometry.Cube,Map<String,ModelPart>> CUBES=new HashMap<>();
    private static final Set<Identifier> LOCAL_TEXTURES=new HashSet<>();
    public interface Drawer { void draw(ModelPart cube,Identifier texture,PoseStack pose); }
    public static void invalidate() {
        CUBES.clear();
        var textures=Set.copyOf(LOCAL_TEXTURES);LOCAL_TEXTURES.clear();
        Minecraft.getInstance().execute(()->textures.forEach(id->Minecraft.getInstance().getTextureManager().release(id)));
    }
    public static String localTexture(String relative,Path path) throws IOException {
        // Encode the full relative UTF-8 path without collisions or resource-id-invalid characters.
        String name=HexFormat.of().formatHex(relative.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Identifier id=Identifier.fromNamespaceAndPath("quarkos_emotes","local_props/"+name);
        if(LOCAL_TEXTURES.contains(id))return id.toString();
        NativeImage pixels;
        try(InputStream input=Files.newInputStream(path)) {pixels=NativeImage.read(input);}
        LOCAL_TEXTURES.add(id);
        Minecraft.getInstance().execute(()-> {
            //? if >=1.21.11 {
            var texture=new DynamicTexture(()->"Quark's emote prop "+relative,pixels);
            //?} else {
            /*var texture=new DynamicTexture(pixels);
            *///?}
            Minecraft.getInstance().getTextureManager().register(id,texture);
        });
        return id.toString();
    }
    public static void draw(int entityId,float partial,net.minecraft.client.model.HumanoidModel<?> model,PoseStack pose,Drawer drawer) {
        var player=EmotesClient.entity(entityId);
        if(player==null||!player.propsVisible(partial))return;
        dev.quarkos.emotes.ClientProofHooks.propPass(entityId);
        // Preserve authored prop trajectories/pivots; scaling flip angles during exit could dip below the floor.
        double weight=1;
        for(var geo:player.emote().props())for(var bone:geo.bones()) {
            if(!geo.visible(bone,player,partial))continue;
            pose.pushPose();
            try {
                // LivingEntityRenderer has already applied body yaw and the model-space Y flip.
                // Its model origin is 24 pixels above the feet.
                pose.translate(0,1.5,0);
                transformBone(geo,bone,player,partial,weight,model,pose,new HashSet<>());
                for(var cube:bone.cubes()) {
                    pose.pushPose();
                    try {
                        pivot(pose,cube.pivot(),cube.rotation(),Vec3.ZERO,Vec3.ONE);
                        dev.quarkos.emotes.ClientProofHooks.prop(entityId,bone.name());
                        drawer.draw(model(cube,geo),Identifier.parse(geo.texture()),pose);
                    } finally {pose.popPose();}
                }
            } finally {pose.popPose();}
        }
    }
    private static void transformBone(PropGeometry geo,PropGeometry.Bone bone,EmotePlayer player,float partial,double weight,net.minecraft.client.model.HumanoidModel<?> model,PoseStack pose,Set<String> visited) {
        if(!visited.add(bone.name()))throw new IllegalArgumentException("Geometry parent cycle");
        if(bone.parent()!=null) {
            var part=switch(bone.parent()) {
                case "head" -> model.head;case "body" -> model.body;
                case "right_arm" -> model.rightArm;case "left_arm" -> model.leftArm;
                case "right_leg" -> model.rightLeg;case "left_leg" -> model.leftLeg;default -> null;
            };
            if(part!=null) {
                // Inherit the actual final flat limb, including entry/exit weight,
                // authored position, Steve/Alex pivot policy and vanilla baseline.
                var initial=part.getInitialPose();
                //? if >=1.21.11 {
                Vec3 rest=new Vec3(initial.x(),initial.y(),initial.z());
                //?} else {
                /*Vec3 rest=new Vec3(initial.x,initial.y,initial.z);
                *///?}
                if(bone.parent().endsWith("_arm")&&player.emote().armPivot()==Emote.ArmPivot.STEVE)
                    rest=new Vec3(rest.x(),2,rest.z());
                apply(pose,PropTransform.parent(rest,new Emote.Pose(new Vec3(part.xRot,part.yRot,part.zRot),
                        new Vec3(part.x,part.y,part.z),new Vec3(part.xScale,part.yScale,part.zScale))));
            } else {
                var parent=geo.bones().stream().filter(b->b.name().equals(bone.parent())).findFirst().orElseThrow();
                transformBone(geo,parent,player,partial,weight,model,pose,visited);
            }
        }
        var sample=bone.sample(player,partial);
        Vec3 r=sample.rotation(),p=sample.position(),s=sample.scale(),rest=bone.rotation();
        pivot(pose,bone.pivot(),new Vec3(rest.x()+r.x()*weight,rest.y()+r.y()*weight,rest.z()+r.z()*weight),
                new Vec3(p.x()*weight,p.y()*weight,p.z()*weight),new Vec3(1+(s.x()-1)*weight,1+(s.y()-1)*weight,1+(s.z()-1)*weight));
    }
    private static void pivot(PoseStack pose,Vec3 pivot,Vec3 rotation,Vec3 position,Vec3 scale) {
        apply(pose,PropTransform.geometry(pivot,rotation,position,scale));
    }
    private static void apply(PoseStack pose,PropTransform transform) {
        Vec3 p=transform.pivot(),d=transform.position(),r=transform.rotation(),scale=transform.scale();
        pose.translate((p.x()+d.x())/16,(p.y()+d.y())/16,(p.z()+d.z())/16);
        pose.mulPose(Axis.ZP.rotation((float)r.z()));pose.mulPose(Axis.YP.rotation((float)r.y()));pose.mulPose(Axis.XP.rotation((float)r.x()));
        pose.scale((float)scale.x(),(float)scale.y(),(float)scale.z());
        pose.translate(-p.x()/16,-p.y()/16,-p.z()/16);
    }

    private static ModelPart model(PropGeometry.Cube c,PropGeometry geo) {
        return CUBES.computeIfAbsent(c,k->new HashMap<>()).computeIfAbsent(geo.textureWidth()+"x"+geo.textureHeight(),k->{
            Vec3 o=c.origin(),s=c.size();float inflate=(float)c.inflate();
            var cube=new ModelPart.Cube(c.u(),c.v(),(float)o.x(),(float)-(o.y()+s.y()),(float)o.z(),
                    (float)s.x(),(float)s.y(),(float)s.z(),inflate,inflate,inflate,c.mirror(),geo.textureWidth(),geo.textureHeight(),EnumSet.allOf(Direction.class));
            return new ModelPart(List.of(cube),Map.of());
        });
    }
}
