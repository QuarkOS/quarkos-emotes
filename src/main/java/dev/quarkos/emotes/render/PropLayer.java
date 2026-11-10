package dev.quarkos.emotes.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
//? if >=1.21.11 {
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
public final class PropLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    public PropLayer(AvatarRenderer<?> parent) { super(parent); }
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch) {
        if(state.isInvisible||state.isSpectator)return;
        var mc=Minecraft.getInstance();var entity=mc.level==null?null:mc.level.getEntity(state.id);
        if(!(entity instanceof net.minecraft.world.entity.LivingEntity living))return;
        GeometryProps.draw(state.id,state.ageInTicks-(float)Math.floor(state.ageInTicks),getParentModel(),pose,(cube,texture,stack)->
                collector.submitModelPart(cube,stack,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(texture),light,
                        net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(state,0),null));
        PropAttachment.draw(state.id,state.ageInTicks-(float)Math.floor(state.ageInTicks),getParentModel(),pose,(item,stack)->{
            var render=new net.minecraft.client.renderer.item.ItemStackRenderState();
            mc.getItemModelResolver().updateForLiving(render,item,ItemDisplayContext.FIXED,living);
            render.submit(stack,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);
        });
    }
}
//?} else {
/*import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
public final class PropLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public PropLayer(net.minecraft.client.renderer.entity.player.PlayerRenderer parent) { super(parent); }
    @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,AbstractClientPlayer player,float swing,float amount,float partial,float age,float yaw,float pitch) {
        if(player.isInvisible()||player.isSpectator())return;
        GeometryProps.draw(player.getId(),partial,getParentModel(),pose,(cube,texture,stack)->cube.render(stack,
                buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(texture)),light,
                net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(player,0)));
        PropAttachment.draw(player.getId(),partial,getParentModel(),pose,(item,stack)->Minecraft.getInstance().getItemRenderer()
                .renderStatic(item,ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,stack,buffer,player.level(),0));
    }
}
*///?}
