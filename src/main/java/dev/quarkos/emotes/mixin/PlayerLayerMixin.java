package dev.quarkos.emotes.mixin;
import dev.quarkos.emotes.render.PropLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class PlayerLayerMixin {
    @Inject(method="<init>",at=@At("TAIL"))
    private void emotes$layer(EntityRendererProvider.Context context,boolean slim,CallbackInfo ci) {
        //? if >=1.21.11 {
        ((LayerAccess)this).emotes$addLayer(new PropLayer((AvatarRenderer<?>)(Object)this));
        //?} else {
        /*((LayerAccess)this).emotes$addLayer(new PropLayer((net.minecraft.client.renderer.entity.player.PlayerRenderer)(Object)this));
        *///?}
    }
}
