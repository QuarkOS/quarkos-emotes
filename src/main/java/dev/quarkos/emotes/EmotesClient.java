package dev.quarkos.emotes;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.emotes.animation.*;
import dev.quarkos.emotes.config.*;
import dev.quarkos.emotes.config.ui.EmotePreview;
import dev.quarkos.emotes.network.PlaybackState;
import net.minecraft.client.*;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import java.util.*;
//? if <26.3 {
import org.lwjgl.glfw.GLFW;
//?}
public final class EmotesClient {
    public static final String MOD_ID="quarkos_emotes";
    //? if >=1.21.11 {
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID,"emotes"));
    //?} else {
    /*private static final String CATEGORY="key.categories.quarkos_emotes";
    *///?}
    public static final KeyMapping OPEN=new KeyMapping("key.quarkos_emotes.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_B,CATEGORY);
    public static final KeyMapping[] FAVORITES=java.util.stream.IntStream.range(1,5).mapToObj(i->
            new KeyMapping("key.quarkos_emotes.favorite_"+i,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,CATEGORY)).toArray(KeyMapping[]::new);
    public static final KeyMapping[] QUICK_SLOTS=java.util.stream.IntStream.rangeClosed(1,EmoteConfig.SLOT_COUNT).mapToObj(i->
            new KeyMapping("key.quarkos_emotes.slot_"+i,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,CATEGORY)).toArray(KeyMapping[]::new);
    private static final Map<UUID,EmotePlayer> PLAYERS=new HashMap<>();
    private static final Map<UUID,Object> ENTITIES=new HashMap<>();
    private record RemoteCue(String id,int elapsed) {}
    /** Cues that arrived before the client world existed, plus unknown ids waiting on a reload. */
    private static final Map<UUID,RemoteCue> PENDING=new HashMap<>();
    private static final Map<UUID,Integer> REMOTE_ELAPSED=new HashMap<>();
    private static final Map<UUID,Integer> MISSING=new HashMap<>();
    public static int remoteElapsed(UUID id) { return REMOTE_ELAPSED.getOrDefault(id,-1); }
    private static boolean ready,hinted;
    private static Object level,localEntity,connection;
    private static boolean sharedLocal;
    private static int hintCount;
    private static java.util.function.BooleanSupplier capability=()->false;
    public static void capability(java.util.function.BooleanSupplier c) { capability=c; }
    public static boolean canSend() { return capability.getAsBoolean(); }
    public static int hintCount() { return hintCount; }
    private static CameraType savedCamera;
    private static net.minecraft.world.phys.Vec3 lastPosition;
    private static java.util.function.Function<String,Boolean> sender=id->false;
    public static String selected="";
    public static void sender(java.util.function.Function<String,Boolean> s) { sender=s; }
    public static EmotePlayer player(UUID id) { return PLAYERS.get(id); }
    public static EmotePlayer entity(int id) {
        var preview=EmotePreview.playback(id);
        if(preview!=null)return preview;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null) return null;
        var entity=mc.level.getEntity(id);
        if(entity==null || (!EmoteConfig.get().showOthers&&entity!=mc.player)) return null;
        if(entity==mc.player&&mc.options.getCameraType().isFirstPerson()&&EmoteConfig.get().firstPerson==EmoteConfig.FirstPerson.NONE) return null;
        return PLAYERS.get(entity.getUUID());
    }
    public static void receive(UUID uuid,String id) { receive(uuid,id,0); }
    public static void receive(UUID uuid,String id,int elapsed) {
        Minecraft mc=Minecraft.getInstance();
        if(!mc.isSameThread()) { mc.execute(()->receive(uuid,id,elapsed));return; }
        if(uuid==null||id==null||!PlaybackState.validElapsed(elapsed))return;
        // Local playback starts immediately and must not restart on the server echo.
        if(mc.player!=null&&mc.player.getUUID().equals(uuid))return;
        if(mc.level==null) { PENDING.put(uuid,new RemoteCue(id,elapsed));return; }
        applyCue(mc,uuid,id,elapsed);
    }
    private static void applyCue(Minecraft mc,UUID uuid,String id,int elapsed) {
        REMOTE_ELAPSED.put(uuid,elapsed);
        if(id.isEmpty()) { var p=PLAYERS.get(uuid);if(p!=null)p.stop();PENDING.remove(uuid);return; }
        var emote=EmoteResources.REGISTRY.get(id);
        if(emote==null) { PENDING.put(uuid,new RemoteCue(id,elapsed));return; }
        var playback=new EmotePlayer(emote);playback.seek(elapsed/1000.0);
        PLAYERS.put(uuid,playback);ENTITIES.put(uuid,mc.level.getPlayerByUUID(uuid));MISSING.remove(uuid);PENDING.remove(uuid);
        org.slf4j.LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_REMOTE apply id={} elapsedMs={} phase={}",id,elapsed,playback.time(0));
    }
    public static void play(String id) {
        Minecraft mc=Minecraft.getInstance();synchronizeWorld(mc);if(mc.player==null)return;
        var emote=EmoteResources.REGISTRY.get(id); if(emote==null) return;
        selected=id; PLAYERS.put(mc.player.getUUID(),new EmotePlayer(emote));
        if(savedCamera==null&&EmoteConfig.get().firstPerson==EmoteConfig.FirstPerson.THIRD_PERSON_CAMERA&&mc.options.getCameraType().isFirstPerson()) {
            savedCamera=mc.options.getCameraType(); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        lastPosition=mc.player.position();
        boolean shared=sender.apply(id);sharedLocal=shared;
        if(!shared&&!mc.hasSingleplayerServer()&&!hinted) {
            hinted=true;
            hintCount++;
            //? if >=26.1 {
            /*mc.player.sendSystemMessage(Component.translatable("text.quarkos_emotes.local_only"));
            *///?} else {
            mc.player.displayClientMessage(Component.translatable("text.quarkos_emotes.local_only"),false);
            //?}
        }
    }
    public static void preview(String id) {
        Minecraft mc=Minecraft.getInstance();synchronizeWorld(mc);var emote=EmoteResources.REGISTRY.get(id);
        if(mc.player!=null&&emote!=null) {
            if(sharedLocal) {sender.apply("");sharedLocal=false;}
            restoreCamera(mc);PLAYERS.put(mc.player.getUUID(),new EmotePlayer(emote));
        }
    }
    public static void stop() {
        Minecraft mc=Minecraft.getInstance(); if(mc.player==null) return;
        var p=PLAYERS.get(mc.player.getUUID());if(p!=null)p.stop();
        if(sharedLocal) {sender.apply("");sharedLocal=false;}
    }
    public static void reload() {
        Minecraft mc=Minecraft.getInstance();
        if(!mc.isSameThread()) {mc.execute(EmotesClient::reload);return;}
        stop();
        if(mc.player!=null) {PLAYERS.remove(mc.player.getUUID());ENTITIES.remove(mc.player.getUUID());}
        restoreCamera(mc);
    }
    public static void resourcesReloaded() {
        var it=PLAYERS.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next();var replacement=EmoteResources.REGISTRY.get(entry.getValue().emote().id());
            if(replacement==null||entry.getValue().stopping()) {it.remove();ENTITIES.remove(entry.getKey());}
            else entry.setValue(entry.getValue().rebind(replacement));
        }
    }
    private static void restoreCamera(Minecraft mc) { if(savedCamera!=null) { mc.options.setCameraType(savedCamera); savedCamera=null; } }
    private static void synchronizeWorld(Minecraft mc) {
        boolean sameConnection=connection==mc.getConnection();
        if(!sameConnection) {
            // Keep a cue that raced ahead of the first connection. A later session starts clean.
            if(!dev.quarkos.emotes.network.RemoteApply.retainCueOnConnectionChange(connection!=null)) {PENDING.clear();REMOTE_ELAPSED.clear();}
            connection=mc.getConnection();hinted=false;hintCount=0;
        }
        if(level!=mc.level||localEntity!=mc.player) {
            if(sharedLocal&&sameConnection&&mc.getConnection()!=null)sender.apply("");sharedLocal=false;
            // Keep a tracking cue across the first attach and across a local
            // player-object swap. Only a real level change starts a new session.
            boolean drop=dev.quarkos.emotes.network.RemoteApply.dropRemoteCues(level!=null,level!=mc.level);
            if(drop) {PLAYERS.clear();ENTITIES.clear();MISSING.clear();}
            else if(localEntity!=null&&localEntity!=mc.player) {
                var id=((net.minecraft.world.entity.player.Player)localEntity).getUUID();
                PLAYERS.remove(id);ENTITIES.remove(id);MISSING.remove(id);
            }
            level=mc.level;localEntity=mc.player;lastPosition=null;
            if(drop) restoreCamera(mc);
        }
    }
    private static void flushPending(Minecraft mc) {
        if(mc.level==null||PENDING.isEmpty())return;
        for(var entry:List.copyOf(PENDING.entrySet())) applyCue(mc,entry.getKey(),entry.getValue().id(),entry.getValue().elapsed());
    }
    public static void tick(Minecraft mc) {
        if(!ready) { ready=true; EmoteConfig.load(mc.gameDirectory.toPath()); }
        synchronizeWorld(mc);
        flushPending(mc);
        if(mc.screen!=null&&!ClientProofHooks.visual()) {stop();restoreCamera(mc);}
        if(mc.player!=null&&!mc.player.isAlive())restoreCamera(mc);
        if(mc.player!=null) {
            boolean activated=false;
            while(OPEN.consumeClick()) {
                if(mc.screen==null&&!activated) {stop();mc.setScreen(new EmoteWheelScreen(OPEN.isDown()));activated=true;}
            }
            for(int i=0;i<FAVORITES.length;i++) while(FAVORITES[i].consumeClick()) {
                if(mc.screen==null&&!activated&&i<EmoteConfig.get().favorites.size()) {play(EmoteConfig.get().favorites.get(i));activated=true;}
            }
            for(int i=0;i<QUICK_SLOTS.length;i++) while(QUICK_SLOTS[i].consumeClick()) {
                String id=EmoteConfig.get().slot(i);
                if(mc.screen==null&&!activated&&EmoteResources.REGISTRY.get(id)!=null) {play(id);activated=true;}
            }
            var local=PLAYERS.get(mc.player.getUUID());
            if(local!=null&&!ClientProofHooks.visual()) {
                var o=mc.options;
                boolean moving=o.keyUp.isDown()||o.keyDown.isDown()||o.keyLeft.isDown()||o.keyRight.isDown()
                        ||(lastPosition!=null&&mc.player.position().distanceToSqr(lastPosition)>.0001);
                if((EmoteConfig.get().cancelOnMove&&local.emote().cancelOnMove()&&moving)
                        ||o.keyJump.isDown()||o.keyAttack.isDown()||mc.player.swinging||mc.player.hurtTime>0||!mc.player.isAlive()) stop();
            }
            lastPosition=mc.player.position();
        }
        var it=PLAYERS.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next();var p=entry.getValue();
            var entity=mc.level==null?null:mc.level.getPlayerByUUID(entry.getKey());
            if(entity==null) {
                int waited=MISSING.merge(entry.getKey(),1,Integer::sum);
                if(!dev.quarkos.emotes.network.RemoteApply.keepWhileEntityMissing(waited)) {
                    it.remove();ENTITIES.remove(entry.getKey());MISSING.remove(entry.getKey());continue;
                }
                // Keep the phase moving so a late joiner is not stuck at the packet's instant.
                p.tick();p.sounds();
                if(p.finished()) {it.remove();ENTITIES.remove(entry.getKey());MISSING.remove(entry.getKey());}
                continue;
            }
            MISSING.remove(entry.getKey());
            Object previous=ENTITIES.put(entry.getKey(),entity);
            if(dev.quarkos.emotes.network.RemoteApply.dropOnEntityReplacement(previous!=null,previous==entity,entity.isAlive())) {
                it.remove();ENTITIES.remove(entry.getKey());continue;
            }
            p.tick();
            if(mc.player!=null&&entry.getKey().equals(mc.player.getUUID())&&p.stopping()&&sharedLocal) {
                sender.apply("");sharedLocal=false;
            }
            if(EmoteConfig.get().playSounds&&mc.level!=null&&(EmoteConfig.get().showOthers||mc.player!=null&&entry.getKey().equals(mc.player.getUUID()))) {
                for(var s:p.sounds()) if(entity!=null) {
                    Identifier id=Identifier.tryParse(s.id()); if(id!=null) mc.getSoundManager().play(new SimpleSoundInstance(
                            id,SoundSource.PLAYERS,Math.clamp(s.volume()*EmoteConfig.get().soundVolume,0,4),Math.clamp(s.pitch(),.1f,4),
                            RandomSource.create(),false,0,SoundInstance.Attenuation.LINEAR,entity.getX(),entity.getY(),entity.getZ(),false));
                }
            } else p.sounds();
            if(p.finished()) {it.remove();ENTITIES.remove(entry.getKey());}
        }
        if(mc.player==null||!PLAYERS.containsKey(mc.player.getUUID())) restoreCamera(mc);
        ClientProofHooks.tick(mc);
    }
}
