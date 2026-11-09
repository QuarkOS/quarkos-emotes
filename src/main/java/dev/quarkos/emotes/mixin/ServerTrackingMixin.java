package dev.quarkos.emotes.mixin;

import dev.quarkos.emotes.network.ServerSync;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerTrackingMixin {
    @Shadow @Final private Entity entity;
    @Inject(method="addPairing",at=@At("TAIL"))
    private void emotes$track(ServerPlayer viewer,CallbackInfo ci) { ServerSync.track(entity,viewer); }
    @Inject(method="removePairing",at=@At("TAIL"))
    private void emotes$untrack(ServerPlayer viewer,CallbackInfo ci) { ServerSync.untrack(entity,viewer); }
}
