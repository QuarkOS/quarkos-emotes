package dev.quarkos.emotes.network;
//? if forge {
import net.minecraftforge.network.*;
import net.minecraft.network.protocol.PacketFlow;
import java.util.*;
import java.util.function.BiConsumer;
public final class ForgeNetwork {
    public record Request(String id) {}
    public record Broadcast(UUID player,String id) {}
    public static final SimpleChannel CHANNEL=ChannelBuilder.named("quarkos_emotes:main").networkProtocolVersion(1).optional().simpleChannel();
    private static final ProtocolGuard GUARD=new ProtocolGuard();
    public static BiConsumer<UUID,String> receiver=(uuid,id)->{};
    public static void init() {
        CHANNEL.messageBuilder(Request.class,0).direction(PacketFlow.SERVERBOUND)
                .encoder((p,b)->b.writeUtf(p.id(),128)).decoder(b->new Request(b.readUtf(128)))
                .consumerMainThread((packet,context)-> {
                    var source=context.getSender();
                    if(source==null||!GUARD.accept(source,packet.id(),System.nanoTime())) return;
                    var broadcast=new Broadcast(source.getUUID(),packet.id());
                    // Chunk viewers match the server's tracking area; omit peers without our optional channel.
                    //? if >=1.21.11 {
                    var world=source.level();
                    //?} else {
                    /*var world=source.serverLevel();
                    *///?}
                    Set<net.minecraft.server.level.ServerPlayer> targets=new HashSet<>(world.getChunkSource().chunkMap.getPlayers(source.chunkPosition(),false));
                    targets.add(source);
                    for(var target:targets) if(CHANNEL.isRemotePresent(target.connection.getConnection()))
                        CHANNEL.send(broadcast,PacketDistributor.PLAYER.with(target));
                }).add();
        CHANNEL.messageBuilder(Broadcast.class,1).direction(PacketFlow.CLIENTBOUND)
                .encoder((p,b)->{b.writeUUID(p.player());b.writeUtf(p.id(),128);})
                .decoder(b->new Broadcast(b.readUUID(),b.readUtf(128)))
                .consumerMainThread((p,context)->receiver.accept(p.player(),p.id())).add();
        CHANNEL.build();
    }
}
//?}
