package dev.quarkos.emotes.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.quarkos.emotes.config.ui.EmotePreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the orbit of the inventory preview currently being rendered. */
@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
public abstract class PreviewOrbitMixin {
    //? if >=1.21.11 {
    @Inject(method="setupRotations",at=@At("TAIL"))
    private void emotes$previewOrbit(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,PoseStack pose,float bob,float bodyYaw,CallbackInfo ci) {
        orbit(pose);
    }
    //?} else {
    /*@Inject(method="setupRotations",at=@At("TAIL"))
    private void emotes$previewOrbit(net.minecraft.world.entity.LivingEntity entity,PoseStack pose,float age,float bodyYaw,float partial,float scale,CallbackInfo ci) {
        orbit(pose);
    }
    *///?}
    private static void orbit(PoseStack pose) {
        float yaw = EmotePreview.orbit();
        if (yaw != 0) pose.mulPose(Axis.YP.rotationDegrees(yaw));
    }
}
