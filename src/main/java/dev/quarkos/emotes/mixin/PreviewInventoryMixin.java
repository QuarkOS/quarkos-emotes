package dev.quarkos.emotes.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;

//? if >=1.21.11 {
import dev.quarkos.emotes.config.ui.EmotePreview;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}

@Mixin(InventoryScreen.class)
public abstract class PreviewInventoryMixin {
    //? if >=1.21.11 {
    //? if >=26.1 {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)"
            + "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
    *///?} else {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    //?}
    private static void emotes$capturePreview(LivingEntity entity, CallbackInfoReturnable<EntityRenderState> cir) {
        EmotePreview.capture(entity.getId(), cir.getReturnValue());
    }
    //?}
}
