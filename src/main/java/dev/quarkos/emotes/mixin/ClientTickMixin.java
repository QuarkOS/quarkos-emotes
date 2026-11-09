package dev.quarkos.emotes.mixin;
import dev.quarkos.emotes.EmotesClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class ClientTickMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void emotes$tick(CallbackInfo ci) { EmotesClient.tick((Minecraft)(Object)this); }
}
