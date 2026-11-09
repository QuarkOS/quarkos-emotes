package dev.quarkos.emotes.mixin;
import dev.quarkos.emotes.EmotesClient;
import dev.quarkos.emotes.config.EmoteConfig;
import dev.quarkos.emotes.render.EmotePose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class FirstPersonMixin {
    //? if >=1.21.11 {
    @Inject(method="renderHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
    private void emotes$arm(PoseStack stack,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,net.minecraft.resources.Identifier skin,ModelPart arm,boolean sleeve,CallbackInfo ci) {
        var model=((AvatarRenderer<?>)(Object)this).getModel();apply(arm,arm==model.rightArm?"right_arm":"left_arm");
    }
    //?} else {
    /*@Inject(method="renderHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",ordinal=0))
    private void emotes$arm(PoseStack stack,net.minecraft.client.renderer.MultiBufferSource buffer,int light,net.minecraft.client.player.AbstractClientPlayer player,ModelPart arm,ModelPart sleeve,CallbackInfo ci) {
        var model=((net.minecraft.client.renderer.entity.player.PlayerRenderer)(Object)this).getModel();
        if(EmoteConfig.get().firstPerson==EmoteConfig.FirstPerson.SHOW_ARMS&&EmotesClient.player(player.getUUID())!=null) {
            arm.resetPose();apply(arm,arm==model.rightArm?"right_arm":"left_arm");
        }
    }
    @Inject(method="renderHand",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",ordinal=1))
    private void emotes$sleeve(PoseStack stack,net.minecraft.client.renderer.MultiBufferSource buffer,int light,net.minecraft.client.player.AbstractClientPlayer player,ModelPart arm,ModelPart sleeve,CallbackInfo ci) {
        if(EmoteConfig.get().firstPerson==EmoteConfig.FirstPerson.SHOW_ARMS&&EmotesClient.player(player.getUUID())!=null) sleeve.copyFrom(arm);
    }
    *///?}
    private static void apply(ModelPart arm,String bone) {
        var mc=Minecraft.getInstance();if(mc.player==null||EmoteConfig.get().firstPerson!=EmoteConfig.FirstPerson.SHOW_ARMS)return;
        var p=EmotesClient.player(mc.player.getUUID());if(p!=null)EmotePose.apply(arm,p.pose(.5).get(bone),(float)p.weight(.5));
    }
}
