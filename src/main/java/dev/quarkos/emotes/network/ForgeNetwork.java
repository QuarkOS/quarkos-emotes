package dev.quarkos.emotes.network;
//? if forge {
import net.minecraftforge.network.*;
import net.minecraft.network.protocol.PacketFlow;
import java.util.*;
public final class ForgeNetwork {
    public record Request(String id) {}
    public record Broadcast(UUID player,String id,int elapsed) {}
    public static SimpleChannel CHANNEL;
    @FunctionalInterface public interface Receiver { void accept(UUID player,String id,int elapsed); }
    public static Receiver receiver=(uuid,id,elapsed)->{};
    public static void init() {
        if(ServerSync.disabled())return;
        CHANNEL=ChannelBuilder.named("quarkos_emotes:main").networkProtocolVersion(2).optional().simpleChannel();
        CHANNEL.messageBuilder(Request.class,0).direction(PacketFlow.SERVERBOUND)
                .encoder((p,b)->b.writeUtf(p.id(),128)).decoder(b->new Request(b.readUtf(128)))
                .consumerMainThread((packet,context)-> {
                    var source=context.getSender();if(source!=null)ServerSync.request(source,packet.id());
                }).add();
        CHANNEL.messageBuilder(Broadcast.class,1).direction(PacketFlow.CLIENTBOUND)
                .encoder((p,b)->{b.writeUUID(p.player());b.writeUtf(p.id(),128);b.writeVarInt(p.elapsed());})
                .decoder(b->new Broadcast(b.readUUID(),b.readUtf(128),b.readVarInt()))
                .consumerMainThread((p,context)->receiver.accept(p.player(),p.id(),p.elapsed())).add();
        CHANNEL.build();
        ServerSync.sender=(target,player,id,elapsed)-> {
            if(CHANNEL.isRemotePresent(target.connection.getConnection()))
                CHANNEL.send(new Broadcast(player,id,elapsed),PacketDistributor.PLAYER.with(target));
        };
    }
}
//?}
