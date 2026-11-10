package dev.quarkos.emotes.harness;

import dev.quarkos.emotes.*;
import dev.quarkos.emotes.config.EmoteConfig;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in live proof. Uses real connections, advancing playback and ordinary movement cancellation. */
public final class MultiplayerHarness {
    private static final Logger LOG=LoggerFactory.getLogger("quarkos_emotes");
    private static final String ADDRESS=System.getProperty("quarkos_emotes.joinServer","");
    private static final boolean VANILLA=Boolean.getBoolean("quarkos_emotes.vanilla");
    private static final boolean VIEWER=System.getProperty("quarkos_emotes.role","actor").equals("viewer");
    private static final String WAVE="quarkos_emotes:wave",DANCE="quarkos_emotes:bigdance";
    private static boolean connected,joined,done,summary,waveSeen,danceSeen,waveStopped,danceStopped,waveRendered,danceRendered;
    private static int ticks,age,pass,fail,waveAt,danceAt,finishAt;
    private static UUID actor;
    private static boolean localHandRendered;
    public static void recordHand(boolean changed) {if(active()&&VANILLA)localHandRendered|=changed;}
    private static Path waveShot,danceShot,hintShot;
    private static final long TIMEOUT_SCALE=timeoutScale();
    private static long timeoutScale() {
        try { return Math.max(1,Long.parseLong(System.getenv().getOrDefault("MP_TIMEOUT_SCALE","3"))); }
        catch(NumberFormatException e) { return 3; }
    }
    private static final long START=System.nanoTime();
    public static boolean active() { return !ADDRESS.isBlank(); }
    public static void record(int entity,boolean changed) {
        if(!active()||!VIEWER||!changed)return;
        var mc=Minecraft.getInstance();if(mc.level==null||actor==null)return;
        var remote=mc.level.getEntity(entity);if(remote==null||!remote.getUUID().equals(actor))return;
        var p=EmotesClient.player(actor);if(p==null||p.stopping())return;
        if(p.emote().id().equals(WAVE))waveRendered=true;
        if(p.emote().id().equals(DANCE))danceRendered=true;
    }
    private static void check(String name,boolean ok) {
        if(ok)pass++;else fail++;
        LOG.info("EMOTE_MP {}check={} result={}",VANILLA?"vanilla ":"",name,ok?"PASS":"FAIL");
    }
    public static void tick(Minecraft mc) {
        if(!active())return;
        ticks++;
        try {
            if(!done&&(System.nanoTime()-START>240_000_000_000L*TIMEOUT_SCALE||mc.screen instanceof DisconnectedScreen)) {
                check(mc.screen instanceof DisconnectedScreen?"no_disconnect":"timeout",false);done=true;
            }
            if(done) {
                mc.options.keyUp.setDown(false);
                if(!summary) {summary=true;finishAt=ticks;LOG.info("EMOTE_MP_SUMMARY pass={} fail={}",pass,fail);}
                if(ticks-finishAt>10)mc.stop();return;
            }
            mc.options.onboardAccessibility=false;mc.options.pauseOnLostFocus=false;
            if(mc.screen instanceof AccessibilityOnboardingScreen) {mc.options.save();mc.setScreen(new TitleScreen());return;}
            if(!connected&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null) {
                connected=true;mc.options.save();
                ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString(ADDRESS),new ServerData("Emote proof",ADDRESS,ServerData.Type.OTHER),false,null);return;
            }
            if(mc.player==null||mc.level==null||mc.screen!=null)return;
            if(!joined) {
                joined=true;LOG.info("EMOTE_MP joined role={} vanilla={} player={}",VIEWER?"viewer":"actor",VANILLA,mc.player.getName().getString());
                mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.bobView().set(false);mc.options.fov().set(54);
                EmoteConfig.get().firstPerson=EmoteConfig.FirstPerson.SHOW_ARMS;EmoteConfig.get().cancelOnMove=true;
            }
            if(VANILLA) { vanilla(mc);return; }
            var connection=mc.getConnection();
            var peer=connection.getOnlinePlayers().stream().filter(p->profileName(p.getProfile()).equals(VIEWER?"A":"B")).findFirst();
            if(peer.isEmpty()) {if(age>=80&&!VIEWER)actor(mc);return;}
            if(VIEWER) {actor=profileId(peer.get().getProfile());viewer(mc);}else actor(mc);
        } catch(Exception e) {LOG.error("EMOTE_MP failed",e);check("exception",false);done=true;}
    }
    private static String profileName(com.mojang.authlib.GameProfile profile) {
        //? if >=1.21.11 {
        return profile.name();
        //?} else {
        /*return profile.getName();
        *///?}
    }
    private static UUID profileId(com.mojang.authlib.GameProfile profile) {
        //? if >=1.21.11 {
        return profile.id();
        //?} else {
        /*return profile.getId();
        *///?}
    }
    private static void actor(Minecraft mc) {
        age++;
        // Let the viewer finish loading chunks and settle after server placement.
        if(age==80) {
            check("canSend",EmotesClient.canSend());EmotesClient.play(WAVE);LOG.info("EMOTE_MP sent id={}",WAVE);
            check("local_wave",EmotesClient.player(mc.player.getUUID())!=null);
        }
        if(age==145) {EmotesClient.play(DANCE);LOG.info("EMOTE_MP sent id={}",DANCE);}
        if(age==245) {mc.options.keyUp.setDown(true);LOG.info("EMOTE_MP move_to_cancel");}
        if(age==253) {
            mc.options.keyUp.setDown(false);var p=EmotesClient.player(mc.player.getUUID());
            check("movement_cancel",p==null||p.stopping());LOG.info("EMOTE_MP cancelled id={}",DANCE);
        }
        if(age>=285) {check("no_disconnect",mc.getConnection()!=null);done=true;}
    }
    private static void viewer(Minecraft mc) throws Exception {
        var entity=mc.level.getPlayerByUUID(actor);if(entity==null)return;
        // Server places B three blocks in front of A; first-person camera shows the real remote pose.
        mc.player.setYRot(180);mc.player.setXRot(8);
        var p=EmotesClient.player(actor);
        if(p!=null&&!p.stopping()) {
            String id=p.emote().id();
            if(id.equals(WAVE)&&!waveSeen) {
                waveSeen=true;waveAt=ticks;check("remote_wave",true);
                check("viewer_distance",Math.abs(mc.player.distanceTo(entity)-3)<.5);
                LOG.info("EMOTE_MP seen player=A id={} t={}",id,p.time(0));
            }
            if(id.equals(DANCE)&&!danceSeen) {
                danceSeen=true;danceAt=ticks;check("remote_bigdance",true);
                LOG.info("EMOTE_MP seen player=A id={} t={}",id,p.time(0));
            }
        }
        if(waveSeen&&waveShot==null&&ticks-waveAt>=10)waveShot=ClientHarness.screenshot("mp-viewer-wave");
        if(danceSeen&&danceShot==null&&ticks-danceAt>=30)danceShot=ClientHarness.screenshot("mp-viewer-bigdance");
        if(waveSeen&&!waveStopped&&(p==null||p.stopping()||!p.emote().id().equals(WAVE))) {
            waveStopped=true;check("remote_wave_stop",true);LOG.info("EMOTE_MP stopped player=A id={}",WAVE);
        }
        if(danceSeen&&!danceStopped&&(p==null||p.stopping())) {
            danceStopped=true;check("remote_move_stop",ticks-danceAt>=80&&ticks-danceAt<150*TIMEOUT_SCALE);LOG.info("EMOTE_MP stopped player=A id={}",DANCE);
            check("wave_rendered",waveRendered);check("bigdance_rendered",danceRendered);
            check("wave_screenshot",waveShot!=null&&Files.isRegularFile(waveShot));
            check("bigdance_screenshot",danceShot!=null&&Files.isRegularFile(danceShot));
            check("no_disconnect",mc.getConnection()!=null);done=true;
        }
    }
    private static void vanilla(Minecraft mc) throws Exception {
        age++;
        if(age==60) {
            check("canSend_false",!EmotesClient.canSend());EmotesClient.play(WAVE);
            check("local_playback",EmotesClient.player(mc.player.getUUID())!=null);
            check("hint_shown_once",EmotesClient.hintCount()==1);
            LOG.info("EMOTE_MP vanilla canSend={} local=true hints={}",EmotesClient.canSend(),EmotesClient.hintCount());
        }
        if(age==70)hintShot=ClientHarness.screenshot("mp-vanilla-hint");
        if(age==110) {EmotesClient.play(WAVE);check("hint_still_once",EmotesClient.hintCount()==1);}
        if(age==160) {
            check("no_disconnect",mc.getConnection()!=null);
            check("local_rendered",localHandRendered);
            check("hint_screenshot",hintShot!=null&&Files.isRegularFile(hintShot));done=true;
        }
    }
}
