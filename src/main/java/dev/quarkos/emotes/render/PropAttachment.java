package dev.quarkos.emotes.render;

import dev.quarkos.emotes.EmotesClient;
import dev.quarkos.emotes.animation.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;
import java.io.*;
public final class PropAttachment {
    private static final Map<String,JsonArray> MODELS=new HashMap<>();
    public static void invalidate() { MODELS.clear(); }
    public interface Drawer { void draw(ItemStack item,PoseStack pose); }
    public static void draw(int entityId,float partial,HumanoidModel<?> model,PoseStack pose,Drawer drawer) {
        var playback=EmotesClient.entity(entityId);if(playback==null||!playback.propsVisible(partial)||playback.emote().prop()==null||!playback.emote().props().isEmpty())return;
        var prop=playback.emote().prop();
        pose.pushPose();
        try {
            String bone=prop.has("bone")?BedrockAxes.bone(prop.get("bone").getAsString()):"prop";
            var parent=switch(bone) {
                case "head" -> model.head;case "body" -> model.body;case "right_arm" -> model.rightArm;case "left_arm" -> model.leftArm;
                case "right_leg" -> model.rightLeg;case "left_leg" -> model.leftLeg;default -> null;
            };
            if(parent!=null) { parent.translateAndRotate(pose);pose.translate(0,.6,0); }
            else {
                // Standalone prop bones use Bedrock's feet-at-Y=0 coordinate space.
                pose.translate(0,1.5,0);
                var transform=playback.pose(partial).get(bone);
                if(transform!=null) {
                    Vec3 p=BedrockAxes.position(transform.position()),r=BedrockAxes.rotation(transform.rotation()),s=transform.scale();
                    pose.translate(p.x()/16,p.y()/16,p.z()/16);
                    pose.mulPose(Axis.ZP.rotation((float)r.z()));pose.mulPose(Axis.YP.rotation((float)r.y()));pose.mulPose(Axis.XP.rotation((float)r.x()));
                    pose.scale((float)s.x(),(float)s.y(),(float)s.z());
                }
            }
            if(prop.has("item")) {
                pose.mulPose(Axis.XP.rotationDegrees(180));drawer.draw(stack(prop.get("item").getAsString()),pose);
            } else {
                JsonArray cubes=prop.has("cubes")?prop.getAsJsonArray("cubes"):new JsonArray();
                if(prop.has("model")&&prop.get("model").isJsonArray()) cubes=prop.getAsJsonArray("model");
                if(prop.has("model")&&prop.get("model").isJsonPrimitive()) {
                    cubes=MODELS.computeIfAbsent(prop.get("model").getAsString(),PropAttachment::loadModel);
                }
                for(var cube:cubes) {
                    var c=cube.getAsJsonObject();var o=vector(c,"origin",new double[]{0,0,0});var s=vector(c,"size",new double[]{16,16,16});
                    pose.pushPose();
                    try {
                        var pivot=vector(c,"pivot",new double[]{0,0,0});var r=vector(c,"rotation",new double[]{0,0,0});
                        pose.translate(-pivot[0]/16,-pivot[1]/16,pivot[2]/16);
                        pose.mulPose(Axis.ZP.rotationDegrees((float)-r[2]));pose.mulPose(Axis.YP.rotationDegrees((float)-r[1]));pose.mulPose(Axis.XP.rotationDegrees((float)r[0]));
                        pose.translate(pivot[0]/16,pivot[1]/16,-pivot[2]/16);
                        pose.translate(-(o[0]+s[0]/2)/16,-(o[1]+s[1]/2)/16,(o[2]+s[2]/2)/16);
                        pose.scale((float)s[0]/16,(float)s[1]/16,(float)s[2]/16);pose.mulPose(Axis.XP.rotationDegrees(180));
                        drawer.draw(stack(c.has("item")?c.get("item").getAsString():"minecraft:oak_planks"),pose);
                    } finally {pose.popPose();}
                }
            }
        } catch(RuntimeException ignored) {
            // An optional malformed prop cannot interrupt the entity render pass.
        } finally {pose.popPose();}
    }
    private static JsonArray loadModel(String name) {
        JsonArray cubes=new JsonArray(); Identifier id=Identifier.tryParse(name);if(id==null)return cubes;
        var resource=Minecraft.getInstance().getResourceManager().getResource(id);
        if(resource.isPresent()) try(Reader reader=resource.get().openAsReader()) {
            var root=JsonParser.parseReader(reader).getAsJsonObject();
            if(root.has("minecraft:geometry")) for(var geo:root.getAsJsonArray("minecraft:geometry")) {
                var bones=geo.getAsJsonObject().getAsJsonArray("bones");if(bones==null)continue;
                for(var b:bones) {var obj=b.getAsJsonObject();if(obj.has("cubes"))for(var cube:obj.getAsJsonArray("cubes"))cubes.add(cube);}
            }
        } catch(Exception e) {org.slf4j.LoggerFactory.getLogger("quarkos_emotes").warn("Could not load prop model {}",name,e);}
        return cubes;
    }
    private static double[] vector(JsonObject o,String name,double[] defaults) {
        if(!o.has(name))return defaults;var a=o.getAsJsonArray(name);return new double[]{a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble()};
    }
    private static ItemStack stack(String name) {
        Identifier id=Identifier.tryParse(name);if(id==null)return ItemStack.EMPTY;
        //? if >=1.21.11 {
        var item=BuiltInRegistries.ITEM.getValue(id);
        //?} else {
        /*var item=BuiltInRegistries.ITEM.get(id);
        *///?}
        return item==null?ItemStack.EMPTY:new ItemStack(item);
    }
}
