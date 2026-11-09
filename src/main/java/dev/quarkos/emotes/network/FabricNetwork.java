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
        public static final Type<Request> TYPE=new Type<>(Identifier.fromNamespaceAndPath("quarkos_emotes","request_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.id,128),b->new Request(b.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Broadcast(UUID player,String id,int elapsed) implements CustomPacketPayload {
        public static final Type<Broadcast> TYPE=new Type<>(Identifier.fromNamespaceAndPath("quarkos_emotes","play_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Broadcast> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.player);b.writeUtf(p.id,128);b.writeVarInt(p.elapsed);},b->new Broadcast(b.readUUID(),b.readUtf(128),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void init() {
        if(ServerSync.disabled())return;
        PayloadTypeRegistry.playC2S().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.playS2C().register(Broadcast.TYPE,Broadcast.CODEC);
        ServerSync.sender=(target,player,id,elapsed)-> {
            if(ServerPlayNetworking.canSend(target,Broadcast.TYPE))ServerPlayNetworking.send(target,new Broadcast(player,id,elapsed));
        };
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(packet,context)->
                context.server().execute(()->ServerSync.request(context.player(),packet.id())));
    }
}
//?}
