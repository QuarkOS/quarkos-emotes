package dev.quarkos.emotes;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quarkos.emotes.animation.*;
import dev.quarkos.emotes.config.*;
import dev.quarkos.emotes.harness.ClientHarness;
import net.minecraft.client.*;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import java.util.*;
import org.lwjgl.glfw.GLFW;
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
    private static final Map<UUID,EmotePlayer> PLAYERS=new HashMap<>();
    private static boolean ready,hinted;
    private static Object level;
    private static CameraType savedCamera;
    private static net.minecraft.world.phys.Vec3 lastPosition;
    private static java.util.function.Function<String,Boolean> sender=id->false;
    public static String selected="";
    public static void sender(java.util.function.Function<String,Boolean> s) { sender=s; }
    public static EmotePlayer player(UUID id) { return PLAYERS.get(id); }
    public static EmotePlayer entity(int id) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null) return null;
        var entity=mc.level.getEntity(id);
        if(entity==null || (!EmoteConfig.get().showOthers&&entity!=mc.player)) return null;
        if(entity==mc.player&&!(mc.screen instanceof EmoteConfigScreen)&&mc.options.getCameraType().isFirstPerson()&&EmoteConfig.get().firstPerson==EmoteConfig.FirstPerson.NONE) return null;
        return PLAYERS.get(entity.getUUID());
    }
    public static void receive(UUID uuid,String id) {
        Minecraft mc=Minecraft.getInstance();
        // Local playback starts immediately and must not restart on the server echo.
        if(mc.player!=null&&mc.player.getUUID().equals(uuid)) return;
        if(id.isEmpty()) { var p=PLAYERS.get(uuid); if(p!=null) p.stop(); }
        else { var e=EmoteResources.REGISTRY.get(id); if(e!=null) PLAYERS.put(uuid,new EmotePlayer(e)); }
    }
    public static void play(String id) {
        Minecraft mc=Minecraft.getInstance(); if(mc.player==null) return;
        var emote=EmoteResources.REGISTRY.get(id); if(emote==null) return;
        selected=id; PLAYERS.put(mc.player.getUUID(),new EmotePlayer(emote));
        if(savedCamera==null&&EmoteConfig.get().firstPerson==EmoteConfig.FirstPerson.THIRD_PERSON_CAMERA&&mc.options.getCameraType().isFirstPerson()) {
            savedCamera=mc.options.getCameraType(); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        lastPosition=mc.player.position();
        boolean shared=sender.apply(id);
        if(!shared&&!mc.hasSingleplayerServer()&&!hinted) {
            hinted=true; mc.player.displayClientMessage(Component.translatable("text.quarkos_emotes.local_only"),false);
        }
    }
    public static void preview(String id) {
        Minecraft mc=Minecraft.getInstance(); var emote=EmoteResources.REGISTRY.get(id);
        if(mc.player!=null&&emote!=null) PLAYERS.put(mc.player.getUUID(),new EmotePlayer(emote));
    }
    public static void stop() {
        Minecraft mc=Minecraft.getInstance(); if(mc.player==null) return;
        var p=PLAYERS.get(mc.player.getUUID()); if(p!=null&&!p.stopping()) { p.stop(); sender.apply(""); }
    }
    private static void restoreCamera(Minecraft mc) { if(savedCamera!=null) { mc.options.setCameraType(savedCamera); savedCamera=null; } }
    public static void tick(Minecraft mc) {
        if(!ready) { ready=true; EmoteConfig.load(mc.gameDirectory.toPath()); }
        if(level!=mc.level) { level=mc.level; PLAYERS.clear(); hinted=false; lastPosition=null; restoreCamera(mc); }
        if(mc.player!=null) {
            while(OPEN.consumeClick()) {
                if(mc.screen==null) { stop(); mc.setScreen(new EmoteWheelScreen()); }
            }
            for(int i=0;i<FAVORITES.length;i++) while(FAVORITES[i].consumeClick()) {
                if(mc.screen==null&&i<EmoteConfig.get().favorites.size()) play(EmoteConfig.get().favorites.get(i));
            }
            var local=PLAYERS.get(mc.player.getUUID());
            if(local!=null&&!ClientHarness.active()) {
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
            var entry=it.next(); var p=entry.getValue(); p.tick();
            if(EmoteConfig.get().playSounds&&mc.level!=null&&(EmoteConfig.get().showOthers||mc.player!=null&&entry.getKey().equals(mc.player.getUUID()))) {
                var entity=mc.level.getPlayerByUUID(entry.getKey());
                for(var s:p.sounds()) if(entity!=null) {
                    Identifier id=Identifier.tryParse(s.id()); if(id!=null) mc.getSoundManager().play(new SimpleSoundInstance(
                            id,SoundSource.PLAYERS,Math.clamp(s.volume()*EmoteConfig.get().soundVolume,0,4),Math.clamp(s.pitch(),.1f,4),
                            RandomSource.create(),false,0,SoundInstance.Attenuation.LINEAR,entity.getX(),entity.getY(),entity.getZ(),false));
                }
            } else p.sounds();
            if(p.finished()) it.remove();
        }
        if(mc.player==null||!PLAYERS.containsKey(mc.player.getUUID())) restoreCamera(mc);
        ClientHarness.tick(mc);
    }
}
