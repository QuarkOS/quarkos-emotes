package dev.quarkos.emotes.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.*;
import org.slf4j.LoggerFactory;

/** Called only on the server thread, by packet handlers and vanilla tracking hooks. */
public final class ServerSync {
    @FunctionalInterface public interface Sender { void send(ServerPlayer target,UUID player,String id,int elapsed); }
    public static Sender sender=(target,player,id,elapsed)->{};
    private static final ProtocolGuard GUARD=new ProtocolGuard();
    private static final PlaybackState STATE=new PlaybackState();
    private static final Map<ServerPlayer,Set<ServerPlayer>> TRACKERS=new IdentityHashMap<>();
    private static final Map<ServerPlayer,Object> LEVELS=new IdentityHashMap<>();
    public static boolean disabled() { return Boolean.getBoolean("quarkos_emotes.disableServer"); }
    private static boolean proof() { return Boolean.getBoolean("quarkos_emotes.serverHarness"); }
    public static void request(ServerPlayer source,String id) {
        if(disabled()||!GUARD.accept(source,id,System.nanoTime()))return;
        // Respawn can replace an entity with the same UUID before the next tick.
        LEVELS.keySet().removeIf(old->{
            if(old!=source&&old.getUUID().equals(source.getUUID())) {GUARD.remove(old);TRACKERS.remove(old);return true;}return false;
        });
        STATE.update(source.getUUID(),id,System.nanoTime());LEVELS.put(source,source.level());
        broadcast(source,id,0);
    }
    private static void broadcast(ServerPlayer source,String id,int elapsed) {
        var targets=new HashSet<>(TRACKERS.getOrDefault(source,Set.of()));targets.add(source);
        for(var target:targets)sender.send(target,source.getUUID(),id,elapsed);
        if(proof())LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_SERVER {} player={} id={}",id.isEmpty()?"stop":"play",source.getName().getString(),id);
    }
    public static void track(Entity entity,ServerPlayer target) {
        if(disabled()||!(entity instanceof ServerPlayer source))return;
        TRACKERS.computeIfAbsent(source,p->Collections.newSetFromMap(new IdentityHashMap<>())).add(target);
        var active=STATE.get(source.getUUID());
        if(active!=null&&LEVELS.get(source)==source.level()) {
            int elapsed=active.elapsed(System.nanoTime());sender.send(target,source.getUUID(),active.id(),elapsed);
            if(proof())LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_SERVER track player={} viewer={} id={} elapsed={}",source.getName().getString(),target.getName().getString(),active.id(),elapsed);
        }
    }
    public static void untrack(Entity entity,ServerPlayer target) {
        if(entity instanceof ServerPlayer source) {var targets=TRACKERS.get(source);if(targets!=null)targets.remove(target);}
    }
    public static void shutdown() {
        for(var source:LEVELS.keySet()) {STATE.remove(source.getUUID());GUARD.remove(source);}
        LEVELS.clear();TRACKERS.clear();
    }
    public static void tick(MinecraftServer server) {
        ServerProofHooks.tick(server);
        if(disabled())return;
        Set<ServerPlayer> online=new HashSet<>(server.getPlayerList().getPlayers());
        var it=LEVELS.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next();var source=entry.getKey();
            if(!online.contains(source)||!source.isAlive()||entry.getValue()!=source.level()) {
                broadcast(source,"",0);STATE.remove(source.getUUID());GUARD.remove(source);it.remove();TRACKERS.remove(source);
            }
        }
        TRACKERS.keySet().removeIf(p->!online.contains(p));
        TRACKERS.values().forEach(targets->targets.removeIf(p->!online.contains(p)));
    }
}
