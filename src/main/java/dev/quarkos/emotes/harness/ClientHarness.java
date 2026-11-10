package dev.quarkos.emotes.harness;

import dev.quarkos.emotes.*;
import dev.quarkos.emotes.animation.*;
import dev.quarkos.emotes.config.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.*;
import java.util.*;
import org.slf4j.LoggerFactory;
/** Opt-in deterministic visual check; no network services or external content are required. */
public final class ClientHarness {
    private enum Stage { TITLE, CREATE, WORLD, SHOTS, STOP }
    private record Shot(String name,String id,CameraType camera,String ui) {}
    private static Stage stage=Stage.TITLE;
    private static int ticks,index,wait,pass,fail;
    private static boolean queued,moved,summary,optionsReady,handMoved;
    private static boolean restoringCamera;
    private static int restoreTicks;
    private static double bodyX,bodyY;
    public static String model="steve";
    private static List<Shot> shots=List.of();
    private static Path pending;
    public static float portraitDistance=2.85f,portraitDrop=.65f,portraitOrbit,portraitForward;
    private static final int GUI_SCALE=Math.max(0,Math.min(8,Integer.getInteger("quarkos_emotes.guiScale",0)));
    /** Authored key times in seconds: contact/gesture peaks, not a fraction of clip length. */
    private static double poseTime(Emote e) {
        String name=e.id().substring(e.id().indexOf(':')+1);
        double time=switch(name) {
            case "wave" -> .5;
            case "thinking" -> 1.2;
            case "facepalm" -> .65;
            case "clap" -> .6;
            case "tableflip" -> .75;
            case "bow" -> 1.0;
            case "salute" -> .8;
            case "laugh" -> .65;
            case "dab" -> .8;
            case "sit_full" -> 2.0;
            case "groove" -> .625;
            case "bigdance" -> 1.5;
            case "shrug" -> .7;
            case "point" -> 1.0;
            case "cheer" -> .75;
            case "stretch" -> 1.15;
            case "yes", "no", "rage", "victory" -> .35;
            case "cry" -> .95;
            case "heart_hands" -> .8;
            case "wave_big", "slow_clap" -> .7;
            case "backflip" -> .8;
            case "spin" -> .9;
            case "worm" -> 1.25;
            case "floss", "zombie_walk" -> .25;
            case "tpose" -> .5;
            case "sit", "sleep" -> e.length();
            default -> Math.min(1,e.length()/2);
        };
        return Math.min(time,e.length());
    }
    public static void rotation(int entity,float x,float y) {
        var mc=Minecraft.getInstance();
        if(active()&&mc.player!=null&&mc.player.getId()==entity) {
            bodyX=Math.max(bodyX,Math.abs(Math.toDegrees(x)));bodyY=Math.max(bodyY,Math.abs(Math.toDegrees(y)));
        }
    }
    private static void check(String name,boolean ok,String detail) {
        if(ok)pass++;else fail++;
        LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_CHECK name={} result={} detail={}",name,ok?"PASS":"FAIL",detail);
    }
    public static boolean active() { return Boolean.getBoolean("quarkos_emotes.harness"); }
    public static void recordHand(boolean changed) { MultiplayerHarness.recordHand(changed);if(active())handMoved|=changed; }
    public static void record(int entity,boolean changed) {
        MultiplayerHarness.record(entity,changed);
        var mc=Minecraft.getInstance(); if(active()&&mc.player!=null&&mc.player.getId()==entity) moved|=changed;
    }
    public static void tick(Minecraft mc) {
        if(!active()) return;
        ticks++;
        try {
            if(!optionsReady) {
                optionsReady=true;mc.options.onboardAccessibility=false;mc.options.guiScale().set(GUI_SCALE);
                mc.options.save();mc.resizeDisplay();
            }
            if(mc.screen instanceof AccessibilityOnboardingScreen) {
                mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());stage=Stage.TITLE;ticks=0;return;
            }
            if(stage!=Stage.STOP&&ticks>20*240) {
                fail++;LoggerFactory.getLogger("quarkos_emotes").error("EMOTE_HARNESS timeout stage={}",stage);stage=Stage.STOP;wait=0;
            }
            switch(stage) {
                case TITLE -> { if(mc.getOverlay()==null&&mc.screen instanceof TitleScreen) { stage=Stage.CREATE;ticks=0; } }
                case CREATE -> create(mc);
                case WORLD -> {
                    if(mc.screen instanceof ConfirmScreen) pressFirst(mc.screen);
                    if(mc.level!=null&&mc.player!=null&&mc.screen==null) {
                        if(++wait<60) return;
                        shots=buildShots(); stage=Stage.SHOTS; ticks=0; wait=0;
                        mc.options.hideGui=true; mc.options.bobView().set(false); mc.options.fov().set(54);
                        mc.options.pauseOnLostFocus=false;
                        var server=mc.getSingleplayerServer();
                        if(server!=null) server.execute(()->{server.overworld().setDayTime(6000);server.overworld().setWeatherParameters(0,600000,false,false);});
                    }
                }
                case SHOTS -> shoot(mc);
                case STOP -> {
                    if(!summary) {summary=true;LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_SUMMARY pass={} fail={}",pass,fail);}
                    if(wait++>10)mc.stop();
                }
            }
        } catch(Exception e) { LoggerFactory.getLogger("quarkos_emotes").error("EMOTE_HARNESS failed",e);fail++;stage=Stage.STOP;wait=0; }
    }
    private static void create(Minecraft mc) {
        if(mc.screen instanceof ConfirmScreen) { pressFirst(mc.screen);return; }
        if(!(mc.screen instanceof CreateWorldScreen create)) {
            if(ticks==1) {
                //? if >=1.21.11 {
                CreateWorldScreen.openFresh(mc,()->mc.setScreen(new TitleScreen()));
                //?} else {
                /*CreateWorldScreen.openFresh(mc,new TitleScreen());
                *///?}
            }
            return;
        }
        var ui=create.getUiState(); if(ui.getNormalPresetList().isEmpty()) return;
        ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);ui.setAllowCommands(true);ui.setName("QuarkOS Emotes harness");
        ui.getNormalPresetList().stream().filter(e->e.preset().is(WorldPresets.FLAT)).findFirst().ifPresent(ui::setWorldType);
        for(var child:create.children()) if(child instanceof Button b&&b.getMessage().getString().equals(Component.translatable("selectWorld.create").getString())) {
            press(b);stage=Stage.WORLD;ticks=0;return;
        }
    }
    private static List<Shot> buildShots() {
        List<Shot> list=new ArrayList<>();
        if(EmoteResources.REGISTRY.all().isEmpty()) {
            fail++;LoggerFactory.getLogger("quarkos_emotes").error("EMOTE_CHECK name=registry result=FAIL detail=no_emotes_loaded");
        }
        Set<String> only=new HashSet<>(Arrays.asList(System.getProperty("quarkos_emotes.only","").split(",")));
        for(var e:EmoteResources.REGISTRY.all()) {
            String name=e.id().split(":",2)[1].replace('/','_');
            boolean all=only.contains("")||only.contains(name)||only.contains(e.id())||only.contains("emote-"+name);
            if(all) list.add(new Shot("emote-"+name,e.id(),CameraType.THIRD_PERSON_FRONT,""));
            if(all||only.contains("emote-"+name+"-34")) list.add(new Shot("emote-"+name+"-34",e.id(),CameraType.THIRD_PERSON_FRONT,"34"));
            if(Set.of("backflip","worm","floss").contains(name))for(String model:List.of("steve","alex")) {
                String variant="emote-"+name+"-"+model;
                if(all||only.contains(variant))list.add(new Shot(variant,e.id(),CameraType.THIRD_PERSON_FRONT,"34"));
            }
            if(name.equals("spin")&&(all||only.contains("emote-spin-90")))
                list.add(new Shot("emote-spin-90",e.id(),CameraType.THIRD_PERSON_FRONT,"90"));
            if(Set.of("tableflip","groove","bigdance","backflip","worm").contains(name)) {
                if(!Set.of("backflip","worm").contains(name)&&(all||only.contains("emote-"+name+"-back")))
                    list.add(new Shot("emote-"+name+"-back",e.id(),CameraType.THIRD_PERSON_BACK,""));
                if(all||only.contains("emote-"+name+"-side")) list.add(new Shot("emote-"+name+"-side",e.id(),CameraType.THIRD_PERSON_FRONT,"side"));
            }
        }
        String wave=EmoteResources.REGISTRY.all().stream().filter(e->e.id().endsWith(":wave")).map(Emote::id).findFirst()
                .orElse(EmoteResources.REGISTRY.all().isEmpty()?"":EmoteResources.REGISTRY.all().getFirst().id());
        for(String ui:List.of("ui-wheel","ui-wheel-hover","ui-wheel-s"+GUI_SCALE,"ui-config","fp-wave","fp-wave-camera","fp-sleep","fp-sleep-camera")) if(only.contains("")||only.contains(ui)||(ui.startsWith("ui-wheel-s")&&only.contains("ui-wheel")))
            list.add(new Shot(ui,ui.startsWith("fp-sleep")?"quarkos_emotes:sleep":wave,
                    ui.equals("fp-wave")||ui.equals("fp-sleep")?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT,ui));
        if(list.isEmpty())throw new IllegalArgumentException("No screenshots match quarkos_emotes.only");
        return list;
    }
    private static void anchor(Minecraft mc) {
        var p=mc.player; if(p==null) return;
        p.setNoGravity(true);p.setDeltaMovement(0,0,0);p.setPos(.5,-59,.5);
        p.xo=.5;p.yo=-59;p.zo=.5;float yaw=180;
        p.setYRot(yaw);p.yRotO=yaw;p.setYHeadRot(180);p.yHeadRotO=180;p.setYBodyRot(180);p.yBodyRotO=180;p.setXRot(0);p.xRotO=0;
        var server=mc.getSingleplayerServer();
        if(server!=null&&ticks%20==0) server.execute(()-> {
            var sp=server.getPlayerList().getPlayer(p.getUUID());if(sp!=null) {sp.setNoGravity(true);sp.setPos(.5,-59,.5);}
        });
    }
    private static void shoot(Minecraft mc) throws Exception {
        if(index>=shots.size()) {
            stage=Stage.STOP;wait=0;return;
        }
        var shot=shots.get(index);anchor(mc);
        if(restoringCamera) {
            if(++restoreTicks<6)return;
            check(shot.name()+"-restore",mc.options.getCameraType()==CameraType.FIRST_PERSON&&EmotesClient.player(mc.player.getUUID())==null,
                    "camera_restored_after_0.2s_cancel_blend");
            restoringCamera=false;index++;wait=0;ticks=0;return;
        }
        if(wait==0) {
            moved=false;handMoved=false;queued=false;bodyX=bodyY=0;mc.setScreen(null);
            model=shot.name().endsWith("-alex")?"alex":"steve";
            portraitOrbit=shot.ui().equals("side")?90:shot.ui().equals("34")?35:0;
            portraitDistance=2.85f;portraitDrop=.65f;portraitForward=0;
            if(shot.id().endsWith(":tableflip")) {
                portraitDistance=portraitOrbit==0?3.1f:2.85f;portraitDrop=.8f;
                if(portraitOrbit!=0)portraitForward=.55f;
            }
            if(shot.id().endsWith(":sit_full")||shot.id().endsWith(":sit")) { portraitDistance=2.25f;portraitDrop=1.05f; }
            if(shot.id().endsWith(":backflip")) { portraitDistance=3.3f;portraitDrop=0; }
            if(shot.id().endsWith(":worm")||shot.id().endsWith(":sleep")) { portraitDistance=3.0f;portraitDrop=1.3f; }
            boolean cameraShot=shot.ui().endsWith("-camera");
            EmoteConfig.get().firstPerson=cameraShot?EmoteConfig.FirstPerson.THIRD_PERSON_CAMERA:EmoteConfig.FirstPerson.SHOW_ARMS;
            mc.options.setCameraType(shot.camera());
            if(cameraShot) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);EmotesClient.play(shot.id());
                if(mc.options.getCameraType()!=CameraType.THIRD_PERSON_BACK) throw new IllegalStateException("First-person camera mode did not switch");
            } else EmotesClient.preview(shot.id());
            var p=EmotesClient.player(mc.player.getUUID());if(p!=null)p.freeze(shot.ui().equals("90")?.63:
                    p.emote().loop()==Emote.Loop.HOLD?p.emote().length()+1:poseTime(p.emote()));
            // F1 also hides the first-person hands. Keep the normal HUD for that evidence shot.
            mc.options.hideGui=!(shot.ui().equals("fp-wave")||shot.ui().equals("fp-sleep"));
            if(shot.ui().startsWith("ui-wheel")) { var wheel=new EmoteWheelScreen();mc.setScreen(wheel);if(shot.ui().endsWith("hover"))wheel.harnessHover=0; }
            if(shot.ui().equals("ui-config")) {
                mc.setScreen(new EmoteConfigScreen(null));
                // init() starts the preview; freeze that new instance as well.
                p=EmotesClient.player(mc.player.getUUID());if(p!=null)p.freeze(poseTime(p.emote()));
            }
            LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_SHOT name={} time={} camera={} distance={} orbit={} guiScale={} effectiveScale={}",
                    shot.name(),p==null?0:p.time(0),mc.options.getCameraType(),portraitDistance,portraitOrbit,GUI_SCALE,mc.getWindow().getGuiScale());
        }
        wait++;
        if(wait<35) return;
        Path path=directory().resolve(shot.name()+".png");
        if(!queued) { Files.createDirectories(path.getParent());Files.deleteIfExists(path);pending=path;queued=true;return; }
        if(!Files.isRegularFile(path)) { if(wait>300) throw new IllegalStateException("Screenshot not written: "+path);return; }
        if(shot.name().equals("emote-backflip")||shot.name().equals("emote-spin"))
            check(shot.name()+"-full-turn",shot.id().endsWith(":backflip")?bodyX>150:bodyY>150,
                    "rendered_body_degrees_x="+bodyX+"_y="+bodyY);
        if(shot.name().endsWith("-steve")||shot.name().endsWith("-alex")) {
            //? if >=1.21.11 {
            boolean slim=mc.player.getSkin().model()==net.minecraft.world.entity.player.PlayerModelType.SLIM;
            //?} else {
            /*boolean slim=mc.player.getSkin().model()==net.minecraft.client.resources.PlayerSkin.Model.SLIM;
            *///?}
            check(shot.name()+"-model",slim==model.equals("alex"),"forced_skin_and_model="+model);
        }
        var playback=EmotesClient.player(mc.player.getUUID());
        if(shot.id().equals("quarkos_emotes:sit")||shot.id().equals("quarkos_emotes:sleep"))
            check(shot.name()+"-hold",playback!=null&&playback.state()==EmotePlayer.State.HOLDING&&playback.weight(0)==1,
                    "held_final_pose_survives_anchor");
        if(shot.name().startsWith("emote-")&&!shot.name().endsWith("-back")&&!shot.name().endsWith("-side")&&!shot.name().endsWith("-34")) {
            boolean ok=moved;if(ok)pass++;else fail++;
            LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_CHECK name={} result={} detail={}",shot.id(),ok?"PASS":"FAIL",ok?"rendered player bone changed":"rendered pose equals idle or model hook was not reached");
        }
        if(shot.ui().equals("fp-wave")||shot.ui().equals("fp-sleep")) {
            if(handMoved)pass++;else fail++;
            LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_CHECK name={} result={} detail=animated_hand_render_hook",shot.name(),handMoved?"PASS":"FAIL");
        }
        if(shot.ui().endsWith("-camera")) {
            boolean ok=mc.options.getCameraType()==CameraType.THIRD_PERSON_BACK&&moved;
            if(ok)pass++;else fail++;
            LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_CHECK name={} result={} detail=camera_switched_and_pose_rendered",shot.name(),ok?"PASS":"FAIL");
            EmotesClient.stop();
            restoringCamera=true;restoreTicks=0;return;
        }
        index++;wait=0;ticks=0;
    }
    public static Path screenshot(String name) throws java.io.IOException {
        Path path=directory().resolve(name+".png");Files.createDirectories(path.getParent());Files.deleteIfExists(path);pending=path;return path;
    }
    private static Path directory() { return Path.of(System.getProperty("quarkos_emotes.shots","build/shots")); }
    /** At GameRenderer TAIL: includes deferred GUI and the fully rendered world. */
    public static void capture() {
        if(pending==null) return;Path path=pending;pending=null;Minecraft mc=Minecraft.getInstance();
        //? if >=1.21.11 {
        var target=mc.getMainRenderTarget();var color=target.getColorTexture();
        if(!(color instanceof com.mojang.blaze3d.opengl.GlTexture texture)) { Screenshot.takeScreenshot(target,img->write(path,img));return; }
        int width=target.width,height=target.height;
        org.lwjgl.opengl.GL11.glFinish();int old=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,texture.glId());
        java.nio.ByteBuffer buffer=org.lwjgl.system.MemoryUtil.memAlloc(width*height*4);
        NativeImage img=new NativeImage(width,height,false);
        try {
            org.lwjgl.opengl.GL11.glGetTexImage(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,0,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,buffer);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                int i=((height-1-y)*width+x)*4;
                img.setPixel(x,y,((buffer.get(i+3)&255)<<24)|((buffer.get(i)&255)<<16)|((buffer.get(i+1)&255)<<8)|(buffer.get(i+2)&255));
            }
            write(path,img);
        } finally { org.lwjgl.system.MemoryUtil.memFree(buffer);org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,old); }
        //?} else {
        /*write(path,Screenshot.takeScreenshot(mc.getMainRenderTarget()));
        *///?}
    }
    private static void write(Path path,NativeImage img) {
        try { img.writeToFile(path); }catch(Exception e) { LoggerFactory.getLogger("quarkos_emotes").error("Screenshot failed",e); }finally {img.close();}
    }
    private static void pressFirst(Screen screen) { for(var child:screen.children())if(child instanceof Button b){press(b);return;} }
    private static void press(Button b) {
        //? if >=1.21.11 {
        b.onPress(new net.minecraft.client.input.InputWithModifiers(){public int input(){return 0;}public int modifiers(){return 0;}});
        //?} else {
        /*b.onPress();
        *///?}
    }
}
