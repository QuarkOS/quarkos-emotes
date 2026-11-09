package dev.quarkos.emotes.network;
//? if fabric {
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;
public final class FabricNetwork {
    public record Request(String id) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(Identifier.fromNamespaceAndPath("quarkos_emotes","request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.id,128),b->new Request(b.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Broadcast(UUID player,String id) implements CustomPacketPayload {
        public static final Type<Broadcast> TYPE=new Type<>(Identifier.fromNamespaceAndPath("quarkos_emotes","play"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Broadcast> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.player);b.writeUtf(p.id,128);},b->new Broadcast(b.readUUID(),b.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static final ProtocolGuard GUARD=new ProtocolGuard();
    public static void init() {
        PayloadTypeRegistry.playC2S().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.playS2C().register(Broadcast.TYPE,Broadcast.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(packet,context)-> {
            var source=context.player();
            if(!GUARD.accept(source,packet.id(),System.nanoTime())) return;
            var broadcast=new Broadcast(source.getUUID(),packet.id());
            Set<net.minecraft.server.level.ServerPlayer> targets=new HashSet<>(PlayerLookup.tracking(source)); targets.add(source);
            for(var target:targets) if(ServerPlayNetworking.canSend(target,Broadcast.TYPE)) ServerPlayNetworking.send(target,broadcast);
        });
    }
}
//?}
