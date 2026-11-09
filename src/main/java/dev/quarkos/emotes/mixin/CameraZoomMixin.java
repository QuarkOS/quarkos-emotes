package dev.quarkos.emotes.mixin;

import dev.quarkos.emotes.ClientProofHooks;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Deterministic harness framing only; ordinary gameplay keeps vanilla's camera. */
@Mixin(Camera.class)
public abstract class CameraZoomMixin {
    @Shadow private Vec3 position;
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow protected abstract void setPosition(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @ModifyVariable(method="getMaxZoom",at=@At("HEAD"),argsOnly=true)
    private float emotes$distance(float distance) { return ClientProofHooks.visual()?ClientProofHooks.distance():distance; }
    //? if >=1.21.11 {
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
        setRotation(yRot+ClientProofHooks.orbit(),xRot);
    }
}
