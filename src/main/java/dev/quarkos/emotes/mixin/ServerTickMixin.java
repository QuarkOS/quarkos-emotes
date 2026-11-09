package dev.quarkos.emotes.mixin;

import dev.quarkos.emotes.network.ServerSync;
import net.minecraft.server.MinecraftServer;
import java.util.function.BooleanSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class ServerTickMixin {
    @Inject(method="stopServer",at=@At("TAIL"))
    private void emotes$shutdown(CallbackInfo ci) { ServerSync.shutdown(); }
    @Inject(method="tickChildren",at=@At("TAIL"))
    private void emotes$serverTick(BooleanSupplier time,CallbackInfo ci) { ServerSync.tick((MinecraftServer)(Object)this); }
}
