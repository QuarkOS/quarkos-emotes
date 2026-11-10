package dev.quarkos.emotes.mixin;

import dev.quarkos.emotes.ClientProofHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.11 {
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;
//?} else {
/*import net.minecraft.client.resources.PlayerSkin;
*///?}

/** Opt-in visual proof only. Both the texture and dispatcher model type change. */
@Mixin(AbstractClientPlayer.class)
public abstract class HarnessSkinMixin {
    @Inject(method="getSkin",at=@At("HEAD"),cancellable=true)
    private void emotes$skin(CallbackInfoReturnable<PlayerSkin> ci) {
        if(!ClientProofHooks.visual()||(Object)this!=Minecraft.getInstance().player)return;
        String model=ClientProofHooks.model();if(model.isEmpty())return;
        boolean slim=model.equals("alex");
        //? if >=1.21.11 {
        ci.setReturnValue(new PlayerSkin(new net.minecraft.core.ClientAsset.ResourceTexture(
                Identifier.withDefaultNamespace("entity/player/"+(slim?"slim/alex":"wide/steve"))),
                null,null,slim?PlayerModelType.SLIM:PlayerModelType.WIDE,true));
        //?} else {
        /*ci.setReturnValue(new PlayerSkin(Identifier.withDefaultNamespace("textures/entity/player/"+(slim?"slim/alex":"wide/steve")+".png"),
                null,null,null,slim?PlayerSkin.Model.SLIM:PlayerSkin.Model.WIDE,true));
        *///?}
    }
}
