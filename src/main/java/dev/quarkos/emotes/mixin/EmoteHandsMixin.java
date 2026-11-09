package dev.quarkos.emotes.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.quarkos.emotes.EmotesClient;
import dev.quarkos.emotes.config.EmoteConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Render gestures in player axes instead of adding them to vanilla's item-holding wrist transform. */
@Mixin(ItemInHandRenderer.class)
public abstract class EmoteHandsMixin {
    private static boolean animated(LocalPlayer player) {
        return player!=null&&!player.isInvisible()&&EmoteConfig.get().firstPerson==EmoteConfig.FirstPerson.SHOW_ARMS
                &&EmotesClient.player(player.getUUID())!=null;
    }
    //? if >=1.21.11 {
    @Inject(method="renderHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void emotes$hands(float partial,PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,LocalPlayer player,int light,CallbackInfo ci) {
        if(!animated(player))return;
        var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        var skin=player.getSkin().body().texturePath();
        pose.pushPose();
        pose.translate(0,-.375,-1.15);pose.scale(-1,-1,1);
        renderer.renderRightHand(pose,collector,light,skin,player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE));
        renderer.renderLeftHand(pose,collector,light,skin,player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE));
        pose.popPose();ci.cancel();
    }
    //?} else {
    /*@Inject(method="renderHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void emotes$hands(float partial,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource.BufferSource buffer,LocalPlayer player,int light,CallbackInfo ci) {
        if(!animated(player))return;
        var renderer=(net.minecraft.client.renderer.entity.player.PlayerRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        pose.pushPose();
        pose.translate(0,-.375,-1.15);pose.scale(-1,-1,1);
        renderer.renderRightHand(pose,buffer,light,player);
        renderer.renderLeftHand(pose,buffer,light,player);
        pose.popPose();ci.cancel();
    }
    *///?}
}
