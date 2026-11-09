package dev.quarkos.emotes.mixin;
import dev.quarkos.emotes.ClientProofHooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class GuiCaptureMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void emotes$capture(DeltaTracker tracker,boolean renderLevel,CallbackInfo ci) { ClientProofHooks.capture(); }
}
