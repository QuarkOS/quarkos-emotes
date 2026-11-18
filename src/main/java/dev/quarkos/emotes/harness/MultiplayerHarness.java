package dev.quarkos.emotes.harness;

import dev.quarkos.emotes.*;
import dev.quarkos.emotes.config.EmoteConfig;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in live proof. Uses real connections, advancing playback and ordinary movement cancellation. */
public final class MultiplayerHarness {
    private static final Logger LOG=LoggerFactory.getLogger("quarkos_emotes");
    private static final String ADDRESS=System.getProperty("quarkos_emotes.joinServer","");
    private static final boolean VANILLA=Boolean.getBoolean("quarkos_emotes.vanilla");
    private static final boolean LATE=Boolean.getBoolean("quarkos_emotes.late");
    private static final boolean LIFE=Boolean.getBoolean("quarkos_emotes.life");
    private static final boolean VIEWER=System.getProperty("quarkos_emotes.role","actor").equals("viewer");
    private static final String WAVE="quarkos_emotes:wave",DANCE="quarkos_emotes:bigdance";
    private static boolean connected,joined,done,summary,waveSeen,danceSeen,waveStopped,danceStopped,waveRendered,danceRendered;
    private static int ticks,age,pass,fail,waveAt,danceAt,finishAt,viewerHold,viewerGone;
    private static int hold,awayHold,seenHold;
    private static boolean reconnecting,connectSent,reloadStarted,reloadDone;
    private static Object markedLevel;
    private static double reloadTime;
    private enum AStep { DANCE, KILL, AFTER_RESPAWN, TO_END, FROM_END, AFTER_DIM, DISCONNECT, AFTER_RECONNECT, WAIT_VIEWER_RELOAD, ACTOR_RELOAD, FINAL, FINISH }
    private enum VStep { DANCE, IDLE1, WAVE1, AWAY, IDLE2, WAVE2, GONE, IDLE3, WAVE3, RELOAD, CONTINUED, IDLE4, WAVE4, FINISH }
    private static AStep astep=AStep.DANCE;
    private static VStep vstep=VStep.DANCE;
    private static Path lifeShot,multiShot;
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
    /** Lifecycle viewer frames hide the HUD, chat and toasts. */
    public static boolean cleanShot() { return LIFE&&VIEWER; }
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
            boolean planned=LIFE&&!VIEWER&&reconnecting;
            if(!done&&(System.nanoTime()-START>240_000_000_000L*TIMEOUT_SCALE||(mc.screen instanceof DisconnectedScreen&&!planned))) {
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
            if(LIFE&&!VIEWER&&reconnecting) { actorReconnect(mc);return; }
            if(mc.player==null||mc.level==null)return;
            if(LIFE&&mc.screen instanceof DeathScreen) {
                if(mc.getConnection()!=null) mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                return;
            }
            if(mc.screen!=null)return;
            if(!joined) {
                joined=true;LOG.info("EMOTE_MP joined role={} vanilla={} player={}",VIEWER?"viewer":"actor",VANILLA,mc.player.getName().getString());
                mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.bobView().set(false);mc.options.fov().set(54);
                EmoteConfig.get().firstPerson=EmoteConfig.FirstPerson.SHOW_ARMS;EmoteConfig.get().cancelOnMove=true;
            }
            if(VANILLA) { vanilla(mc);return; }
            if(LIFE) { if(VIEWER)viewerLife(mc);else actorLife(mc);return; }
            if(LATE&&!VIEWER) { actorLate(mc);return; }
            var connection=mc.getConnection();
            var peer=connection.getOnlinePlayers().stream().filter(p->profileName(p.getProfile()).equals(VIEWER?"A":"B")).findFirst();
            if(peer.isEmpty()) {if(age>=80&&!VIEWER)actor(mc);return;}
            if(VIEWER) {actor=profileId(peer.get().getProfile());if(LATE)viewerLate(mc);else viewer(mc);}else actor(mc);
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
    private static void actorLate(Minecraft mc) {
        age++;
        if(age==80) {
            check("canSend",EmotesClient.canSend());EmotesClient.play(DANCE);LOG.info("EMOTE_MP sent id={}",DANCE);
            check("local_bigdance",EmotesClient.player(mc.player.getUUID())!=null);
        }
        var connection=mc.getConnection();
        boolean viewerOnline=connection!=null&&connection.getOnlinePlayers().stream().anyMatch(p->profileName(p.getProfile()).equals("B"));
        // Count a real visit. A one-tick tab-list gap does not end the hold, and
        // the viewer is allowed to leave once it has the caught-up screenshot.
        if(viewerOnline) { viewerHold++; viewerGone=0; }
        else if(viewerHold>0) viewerGone++;
        if(viewerHold>=300||(viewerHold>=40&&viewerGone>=40)) {
            check("saw_viewer",true);check("no_disconnect",mc.getConnection()!=null);
            LOG.info("EMOTE_MP late_hold_done ticks={} gone={}",viewerHold,viewerGone);done=true;
        }
    }
    private static void viewerLate(Minecraft mc) throws Exception {
        var entity=mc.level.getPlayerByUUID(actor);if(entity==null)return;
        mc.player.setYRot(180);mc.player.setXRot(12);
        var playback=EmotesClient.player(actor);
        if(playback!=null&&!playback.stopping()&&playback.emote().id().equals(DANCE)&&!danceSeen) {
            danceSeen=true;danceAt=ticks;int elapsed=EmotesClient.remoteElapsed(actor);
            check("remote_bigdance",true);
            check("late_elapsed",elapsed>=1500);
            check("viewer_distance",Math.abs(mc.player.distanceTo(entity)-3)<1.25);
            LOG.info("EMOTE_MP late player=A id={} phase={} elapsedMs={} distance={}",DANCE,playback.time(0),elapsed,mc.player.distanceTo(entity));
        }
        if(danceSeen&&danceShot==null&&ticks-danceAt>=20)danceShot=ClientHarness.screenshot("mp-viewer-late-bigdance");
        if(danceSeen&&danceRendered&&danceShot!=null&&Files.isRegularFile(danceShot)) {
            check("bigdance_rendered",true);check("late_screenshot",true);check("no_disconnect",mc.getConnection()!=null);done=true;
        }
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
    private static void command(Minecraft mc,String command) {
        if(mc.getConnection()!=null) mc.getConnection().sendCommand(command);
        LOG.info("EMOTE_MP command {}",command);
    }
    private static void actorReconnect(Minecraft mc) {
        if(mc.player!=null&&mc.level!=null&&mc.screen==null) {
            reconnecting=false;connectSent=false;astep=AStep.AFTER_RECONNECT;hold=0;
            LOG.info("EMOTE_MP life reconnected");return;
        }
        // A connecting or terrain screen has to stay up. Replacing it aborts the join.
        if(mc.screen instanceof DisconnectedScreen) { connectSent=false;mc.setScreen(new TitleScreen());return; }
        if(mc.screen==null) { mc.setScreen(new TitleScreen());return; }
        if(mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&!connectSent) {
            connectSent=true;
            ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString(ADDRESS),new ServerData("Emote proof",ADDRESS,ServerData.Type.OTHER),false,null);
        }
    }
    private static void actorLife(Minecraft mc) {
        hold++;
        switch(astep) {
            case DANCE -> {
                if(hold==40) {
                    check("canSend",EmotesClient.canSend());EmotesClient.play(DANCE);
                    check("local_dance",EmotesClient.player(mc.player.getUUID())!=null);
                    LOG.info("EMOTE_MP life dance");
                }
                if(hold==120) { command(mc,"spawnpoint @s 0 -59 0");command(mc,"kill");astep=AStep.KILL;hold=0; }
            }
            case KILL -> {
                if(mc.player.isAlive()&&hold==8) command(mc,"tp @s 0.5 -60 0.5");
                if(mc.player.isAlive()&&EmotesClient.player(mc.player.getUUID())==null&&hold>40) {
                    check("respawned",true);LOG.info("EMOTE_MP life respawned");astep=AStep.AFTER_RESPAWN;hold=0;
                } else if(hold>400) { check("respawned",false);done=true; }
            }
            case AFTER_RESPAWN -> {
                if(hold==30) EmotesClient.play(WAVE);
                if(hold==110) { markedLevel=mc.level;command(mc,"execute in minecraft:the_end run tp @s 100 80 0");astep=AStep.TO_END;hold=0; }
            }
            case TO_END -> {
                if(mc.level!=markedLevel&&hold>15) {
                    check("changed_dimension",true);LOG.info("EMOTE_MP life dimension=end");
                    markedLevel=mc.level;command(mc,"execute in minecraft:overworld run tp @s 0.5 -60 0.5");astep=AStep.FROM_END;hold=0;
                } else if(hold>1200) { check("changed_dimension",false);done=true; }
            }
            case FROM_END -> {
                if(mc.level!=markedLevel&&hold>30) {
                    check("returned",mc.player.isAlive()&&EmotesClient.player(mc.player.getUUID())==null);
                    LOG.info("EMOTE_MP life dimension=overworld");astep=AStep.AFTER_DIM;hold=0;
                } else if(hold>1200) { check("returned",false);done=true; }
            }
            case AFTER_DIM -> {
                // Stay idle long enough for the viewer to record a clear pose, then wave, then leave.
                if(hold==80) EmotesClient.play(WAVE);
                if(hold==200) {
                    reconnecting=true;LOG.info("EMOTE_MP life disconnect");
                    mc.getConnection().getConnection().disconnect(Component.literal("proof reconnect"));
                    astep=AStep.DISCONNECT;hold=0;
                }
            }
            case DISCONNECT -> { }
            case AFTER_RECONNECT -> {
                if(hold==20) check("reconnected",mc.getConnection()!=null&&EmotesClient.canSend());
                // Idle long enough for the viewer to see the player gone and back with no pose.
                if(hold==100) {
                    command(mc,"tp @s 0.5 -60 0.5");EmotesClient.play(DANCE);astep=AStep.WAIT_VIEWER_RELOAD;hold=0;
                }
            }
            case WAIT_VIEWER_RELOAD -> {
                // Big dance loops, so it is still playing when the viewer's reload finishes.
                if(hold==1400) {
                    LOG.info("EMOTE_MP life actor_reload");reloadDone=false;
                    mc.reloadResourcePacks().thenRun(()->mc.execute(()->reloadDone=true));
                    astep=AStep.ACTOR_RELOAD;hold=0;
                }
            }
            case ACTOR_RELOAD -> {
                if(reloadDone&&EmotesClient.player(mc.player.getUUID())==null&&hold>10) {
                    check("reloaded",true);astep=AStep.FINAL;hold=0;
                } else if(hold>800) { check("reloaded",false);done=true; }
            }
            case FINAL -> {
                if(hold==60) EmotesClient.play(WAVE);
                if(hold==180) { check("no_disconnect",mc.getConnection()!=null);done=true; }
            }
            case FINISH -> { }
        }
    }
    private static boolean file(Path path) { return path!=null&&Files.isRegularFile(path); }
    private static void shot(String name) throws Exception { if(lifeShot==null) lifeShot=ClientHarness.screenshot(name); }
    private static void viewerLife(Minecraft mc) throws Exception {
        resolveActor(mc);
        mc.player.setYRot(180);mc.player.setXRot(8);
        //? if >=26.3 {
        /*ClientHarness.setHudHidden(mc, true);
        *///?} else {
        mc.options.hideGui=true;
        //?}
        var entity=actor==null?null:mc.level.getPlayerByUUID(actor);
        var playback=actor==null?null:EmotesClient.player(actor);
        boolean dancing=playback!=null&&!playback.stopping()&&playback.emote().id().equals(DANCE);
        boolean waving=playback!=null&&!playback.stopping()&&playback.emote().id().equals(WAVE);
        boolean idle=entity!=null&&entity.isAlive()&&playback==null;
        switch(vstep) {
            case DANCE -> {
                if(!(dancing&&danceRendered)) { seenHold=0;return; }
                if(++seenHold<30) return;
                if(lifeShot==null) { shot("life-playing");return; }
                if(!file(lifeShot)) return;
                if(multiShot==null) { multiShot=ClientHarness.screenshot("store-multiplayer");return; }
                if(!file(multiShot)) return;
                check("saw_dance",true);check("playing_shot",true);
                LOG.info("EMOTE_MP life saw_dance");lifeShot=null;vstep=VStep.IDLE1;awayHold=0;
            }
            case IDLE1 -> {
                if(!idle) { awayHold=0;return; }
                if(++awayHold<20) return;
                shot("life-respawn-idle");if(!file(lifeShot)) return;
                check("respawn_cleared",true);waveRendered=false;lifeShot=null;vstep=VStep.WAVE1;awayHold=0;
            }
            case WAVE1 -> {
                if(!(waving&&waveRendered)) return;
                shot("life-respawn-recovered");if(!file(lifeShot)) return;
                check("respawn_recovered",true);lifeShot=null;vstep=VStep.AWAY;awayHold=0;
            }
            case AWAY -> {
                if(entity!=null) { awayHold=0;return; }
                if(++awayHold>=15) { LOG.info("EMOTE_MP life dimension_left");vstep=VStep.IDLE2;awayHold=0; }
            }
            case IDLE2 -> {
                if(!idle) { awayHold=0;return; }
                if(++awayHold<20) return;
                shot("life-dimension-idle");if(!file(lifeShot)) return;
                check("dimension_cleared",true);waveRendered=false;lifeShot=null;vstep=VStep.WAVE2;
            }
            case WAVE2 -> {
                if(!(waving&&waveRendered)) return;
                shot("life-dimension-recovered");if(!file(lifeShot)) return;
                check("dimension_recovered",true);lifeShot=null;vstep=VStep.GONE;awayHold=0;
            }
            case GONE -> {
                // A fast reconnect can put the player back before 15 ticks. One missing tick is enough.
                if(entity==null) { awayHold=1;return; }
                if(awayHold>=1) { LOG.info("EMOTE_MP life actor_left");vstep=VStep.IDLE3;awayHold=0; }
            }
            case IDLE3 -> {
                if(!idle) { awayHold=0;return; }
                if(++awayHold<25) return;
                shot("life-disconnect-idle");if(!file(lifeShot)) return;
                check("disconnect_cleared",true);waveRendered=false;lifeShot=null;vstep=VStep.WAVE3;
            }
            case WAVE3 -> {
                if(!(dancing&&danceRendered)) return;
                shot("life-reconnect-recovered");if(!file(lifeShot)) return;
                check("reconnect_recovered",true);lifeShot=null;
                reloadTime=playback.time(0);danceRendered=false;reloadDone=false;reloadStarted=true;
                LOG.info("EMOTE_MP life viewer_reload phase={}",reloadTime);
                mc.reloadResourcePacks().thenRun(()->mc.execute(()->reloadDone=true));
                vstep=VStep.RELOAD;
            }
            case RELOAD -> { if(reloadDone&&mc.getOverlay()==null) { vstep=VStep.CONTINUED;awayHold=0; } }
            case CONTINUED -> {
                // The reload splash stays on the framebuffer for a few frames after the future completes.
                if(mc.getOverlay()!=null) { awayHold=0;return; }
                if(++awayHold<40) return;
                boolean moved=playback!=null&&!playback.stopping()&&playback.emote().id().equals(DANCE)
                        &&(Math.abs(playback.time(0)-reloadTime)>.3||playback.time(0)<reloadTime);
                if(!(moved&&danceRendered)) { if(awayHold>400) { check("reload_continued",false);done=true; } return; }
                shot("life-reload-continued");if(!file(lifeShot)) return;
                check("reload_continued",true);lifeShot=null;vstep=VStep.IDLE4;awayHold=0;
            }
            case IDLE4 -> {
                if(!idle) { awayHold=0;return; }
                if(++awayHold<15) return;
                shot("life-reload-idle");if(!file(lifeShot)) return;
                check("reload_cleared",true);waveRendered=false;lifeShot=null;vstep=VStep.WAVE4;
            }
            case WAVE4 -> {
                if(!(waving&&waveRendered)) return;
                shot("life-reload-recovered");if(!file(lifeShot)) return;
                check("reload_recovered",true);check("no_disconnect",mc.getConnection()!=null);
                LOG.info("EMOTE_MP life viewer_done");done=true;vstep=VStep.FINISH;
            }
            case FINISH -> { }
        }
    }
    private static void resolveActor(Minecraft mc) {
        var connection=mc.getConnection();if(connection==null)return;
        connection.getOnlinePlayers().stream().filter(p->profileName(p.getProfile()).equals("A")).findFirst()
                .ifPresent(p->actor=profileId(p.getProfile()));
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
