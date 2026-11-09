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
    private static boolean queued,moved,summary;
    private static List<Shot> shots=List.of();
    private static Path pending;
    public static boolean active() { return Boolean.getBoolean("quarkos_emotes.harness"); }
    public static void record(int entity,boolean changed) {
        var mc=Minecraft.getInstance(); if(active()&&mc.player!=null&&mc.player.getId()==entity) moved|=changed;
    }
    public static void tick(Minecraft mc) {
        if(!active()) return;
        ticks++;
        try {
            if(ticks>20*240) { LoggerFactory.getLogger("quarkos_emotes").error("EMOTE_HARNESS timeout stage={}",stage);stage=Stage.STOP; }
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
            if(!only.contains("")&&!only.contains(name)&&!only.contains(e.id())&&!only.contains("emote-"+name)) continue;
            list.add(new Shot("emote-"+name,e.id(),CameraType.THIRD_PERSON_FRONT,""));
            if(name.equals("table_flip")||name.startsWith("dance_")) {
                list.add(new Shot("emote-"+name+"-back",e.id(),CameraType.THIRD_PERSON_BACK,""));
                list.add(new Shot("emote-"+name+"-side",e.id(),CameraType.THIRD_PERSON_FRONT,"side"));
            }
        }
        String wave=EmoteResources.REGISTRY.all().stream().filter(e->e.id().endsWith(":wave")).map(Emote::id).findFirst()
                .orElse(EmoteResources.REGISTRY.all().isEmpty()?"":EmoteResources.REGISTRY.all().getFirst().id());
        for(String ui:List.of("ui-wheel","ui-wheel-hover","ui-config","fp-wave")) if(only.contains("")||only.contains(ui))
            list.add(new Shot(ui,wave,ui.equals("fp-wave")?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT,ui));
        return list;
    }
    private static void anchor(Minecraft mc,boolean side) {
        var p=mc.player; if(p==null) return;
        p.setNoGravity(true);p.setDeltaMovement(0,0,0);p.setPos(.5,-59,.5);
        p.xo=.5;p.yo=-59;p.zo=.5;float yaw=side?90:180;
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
        var shot=shots.get(index);anchor(mc,shot.ui().equals("side"));
        if(wait==0) {
            moved=false;queued=false;mc.setScreen(null); EmoteConfig.get().firstPerson=EmoteConfig.FirstPerson.SHOW_ARMS;
            EmotesClient.preview(shot.id());var p=EmotesClient.player(mc.player.getUUID());if(p!=null)p.freeze(p.emote().length()*.4);
            mc.options.setCameraType(shot.camera());mc.options.hideGui=true;
            if(shot.ui().startsWith("ui-wheel")) { var wheel=new EmoteWheelScreen();mc.setScreen(wheel);if(shot.ui().endsWith("hover"))wheel.harnessHover=0; }
            if(shot.ui().equals("ui-config")) mc.setScreen(new EmoteConfigScreen(null));
        }
        wait++;
        if(wait<35) return;
        Path path=directory().resolve(shot.name()+".png");
        if(!queued) { Files.createDirectories(path.getParent());Files.deleteIfExists(path);pending=path;queued=true;return; }
        if(!Files.isRegularFile(path)) { if(wait>300) throw new IllegalStateException("Screenshot not written: "+path);return; }
        if(shot.name().startsWith("emote-")&&!shot.name().endsWith("-back")&&!shot.name().endsWith("-side")) {
            boolean ok=moved;if(ok)pass++;else fail++;
            LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_CHECK name={} result={} detail={}",shot.id(),ok?"PASS":"FAIL",ok?"rendered player bone changed":"rendered pose equals idle or model hook was not reached");
        }
        index++;wait=0;ticks=0;
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
