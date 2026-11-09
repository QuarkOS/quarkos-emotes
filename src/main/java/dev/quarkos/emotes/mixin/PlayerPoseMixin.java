package dev.quarkos.emotes.mixin;

import dev.quarkos.emotes.render.EmotePose;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.11 {
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
@Mixin(PlayerModel.class)
public abstract class PlayerPoseMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("TAIL"))
    private void emotes$pose(AvatarRenderState state,CallbackInfo ci) {
        PlayerModel model=(PlayerModel)(Object)this;
        EmotePose.apply(model,state.id,state.ageInTicks-(float)Math.floor(state.ageInTicks));
        // Modern outer layers are child parts and inherit transforms; copying would apply them twice.
    }
}
//?} else {
/*import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
@Mixin(PlayerModel.class)
public abstract class PlayerPoseMixin {
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("HEAD"))
    private void emotes$reset(LivingEntity entity,float swing,float amount,float age,float yaw,float pitch,CallbackInfo ci) {
        PlayerModel<?> m=(PlayerModel<?>)(Object)this;
        for(var p:new net.minecraft.client.model.geom.ModelPart[]{m.head,m.hat,m.body,m.rightArm,m.leftArm,m.rightLeg,m.leftLeg,m.jacket,m.rightSleeve,m.leftSleeve,m.rightPants,m.leftPants}) p.resetPose();
    }
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void emotes$pose(LivingEntity entity,float limbSwing,float limbAmount,float age,float yaw,float pitch,CallbackInfo ci) {
        PlayerModel<?> model=(PlayerModel<?>)(Object)this;
        EmotePose.apply(model,entity.getId(),age-(float)Math.floor(age));
        model.hat.copyFrom(model.head); model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm); model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg); model.leftPants.copyFrom(model.leftLeg);
    }
}
*///?}
