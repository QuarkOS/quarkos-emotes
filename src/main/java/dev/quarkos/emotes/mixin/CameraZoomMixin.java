package dev.quarkos.emotes.mixin;

import dev.quarkos.emotes.ClientProofHooks;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Deterministic harness framing only; ordinary gameplay keeps vanilla's camera. */
@Mixin(Camera.class)
public abstract class CameraZoomMixin {
    @Shadow private Vec3 position;
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow protected abstract void setPosition(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="getMaxZoom",at=@At("RETURN"),cancellable=true)
    private void emotes$distance(float distance,CallbackInfoReturnable<Float> cir) {
        // Honor the portrait distance. Block clipping otherwise pulls a low
        // camera into the head when the body is on the ground.
        if(ClientProofHooks.visual()) cir.setReturnValue(ClientProofHooks.distance());
    }
    //? if >=26.3 {
    /*@Shadow private Entity entity;
    @Shadow private boolean detached;

    @Inject(method = "alignWithEntity(F)V", at = @At("TAIL"))
    private void emotes$frame(float partial, CallbackInfo ci) {
        frame(entity, detached, partial);
    }
    *///?} elif >=1.21.11 {
    @Inject(method="setup",at=@At("TAIL"))
    private void emotes$frame(net.minecraft.world.level.Level level,Entity entity,boolean detached,boolean reverse,float partial,CallbackInfo ci) {
        frame(entity,detached,partial);
    }
    //?} else {
    /*@Inject(method="setup",at=@At("TAIL"))
    private void emotes$frame(net.minecraft.world.level.BlockGetter level,Entity entity,boolean detached,boolean reverse,float partial,CallbackInfo ci) {
        frame(entity,detached,partial);
    }
    *///?}
    private void frame(Entity entity,boolean detached,float partial) {
        if(!ClientProofHooks.visual()||!detached||entity==null)return;
        Vec3 eye=entity.getEyePosition(partial),pos=position;
        double angle=Math.toRadians(ClientProofHooks.orbit()),dx=pos.x-eye.x,dz=pos.z-eye.z;
        double yaw=Math.toRadians(entity.getYRot()),forward=ClientProofHooks.forward();
        setPosition(eye.x+dx*Math.cos(angle)-dz*Math.sin(angle)-Math.sin(yaw)*forward,pos.y-ClientProofHooks.drop(),
                eye.z+dx*Math.sin(angle)+dz*Math.cos(angle)+Math.cos(yaw)*forward);
        setRotation(yRot+ClientProofHooks.orbit(),xRot+ClientProofHooks.pitch());
    }
}
