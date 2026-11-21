package dev.quarkos.emotes.harness;

import dev.quarkos.emotes.*;
import dev.quarkos.emotes.animation.*;
import dev.quarkos.emotes.config.*;
import dev.quarkos.emotes.render.ShotFraming;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.tutorial.TutorialSteps;
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
    private record UiShot(String name, int width, int height, int scale) {}
    private record UiFixture(List<String> favorites, List<String> slots, boolean favoritesOnly) {}
    private static final List<UiShot> UI_SHOTS = List.of(
            new UiShot("ui-wheel", 1920, 1080, 2),
            new UiShot("ui-wheel-scale4", 1920, 1080, 4),
            new UiShot("ui-grid", 1920, 1080, 2),
            new UiShot("ui-grid-search", 1920, 1080, 2),
            new UiShot("ui-settings", 1920, 1080, 2),
            new UiShot("ui-settings-scale4", 1920, 1080, 4),
            new UiShot("ui-grid-small", 960, 540, 2));
    private static UiFixture uiFixture;
    private static Stage stage=Stage.TITLE;
    private static int ticks,index,wait,pass,fail;
    private static boolean queued,moved,summary,optionsReady,handMoved;
    private static boolean restoringCamera,chainSwitched;
    private static String chainFrom;
    private static final Set<String> renderedProps=new HashSet<>();
    private static boolean propPass;
    private static final boolean QUICK=Boolean.getBoolean("quarkos_emotes.quick");
    private static int restoreTicks;
    private static double bodyX,bodyY;
    public static String model="steve";
    private static List<Shot> shots=List.of();
    private static Path pending;
    private static volatile Path written;
    public static float portraitDistance=3.25f,portraitDrop=.62f,portraitOrbit,portraitForward,portraitPitch;
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
            case "sit", "sleep", "kneel", "lay_down" -> e.length();
            case "jump_hype" -> 1.4;
            case "fishing_cast" -> .6;
            case "sword_salute" -> .8;
            case "guitar" -> .5;
            case "drums" -> 1.75;
            case "default_dance" -> 3.25;
            case "pushups" -> e.entryLength()+.5;
            case "meditate" -> e.length()+1;
            default -> Math.min(1,e.length()/2);
        };
        return time;
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
    public static void prop(int entity,String bone) {
        var mc=Minecraft.getInstance();
        if(active()&&mc.player!=null&&entity==mc.player.getId())renderedProps.add(bone);
    }
    public static void propPass(int entity) {
        var mc=Minecraft.getInstance();
        if(active()&&mc.player!=null&&entity==mc.player.getId())propPass=true;
    }
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
                // This also saves options and clears an active tutorial step.
                mc.getTutorial().setStep(TutorialSteps.NONE);
                mc.resizeDisplay();
            }
            if(mc.screen instanceof AccessibilityOnboardingScreen) {
                mc.options.onboardAccessibility=false;mc.options.save();mc.setScreen(new TitleScreen());stage=Stage.TITLE;ticks=0;return;
            }
            if(stage!=Stage.STOP&&ticks>20*240) {
                restoreUiFixture();
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
                        //? if >=26.3 {
                        /*setHudHidden(mc, true);
                        *///?} else {
                        mc.options.hideGui=true;
                        //?}
                        mc.options.bobView().set(false);
                        mc.options.fov().set(54);
                        mc.options.pauseOnLostFocus=false;
                        var server=mc.getSingleplayerServer();
                        //? if >=26.3 {
                        /*if (server != null) server.execute(() -> {
                            ServerHarness.setNoon(server);
                            server.setWeatherParameters(0, 600000, false, false);
                        });
                        *///?} else {
                        if(server!=null) server.execute(()->{server.overworld().setDayTime(6000);server.overworld().setWeatherParameters(0,600000,false,false);});
                        //?}
                    }
                }
                case SHOTS -> shoot(mc);
                case STOP -> {
                    if(!summary) {summary=true;LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_SUMMARY pass={} fail={}",pass,fail);}
                    if(wait++>10)mc.stop();
                }
            }
        } catch(Exception e) {
            restoreUiFixture();
            LoggerFactory.getLogger("quarkos_emotes").error("EMOTE_HARNESS failed",e);
            fail++;stage=Stage.STOP;wait=0;
        }
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
        ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);ui.setAllowCommands(true);ui.setName("Quark's Emotes harness");
        ui.getNormalPresetList().stream().filter(e->e.preset().is(WorldPresets.FLAT)).findFirst().ifPresent(ui::setWorldType);
        for(var child:create.children()) if(child instanceof Button b&&b.getMessage().getString().equals(Component.translatable("selectWorld.create").getString())) {
            press(b);stage=Stage.WORLD;ticks=0;return;
        }
    }
    private static List<Shot> buildShots() {
        List<Shot> list=new ArrayList<>();
        if(Boolean.getBoolean("quarkos_emotes.store")) {
            list.add(new Shot("store-wheel","quarkos_emotes:wave",CameraType.THIRD_PERSON_FRONT,"ui-wheel"));
            list.add(new Shot("store-wave-steve","quarkos_emotes:wave",CameraType.THIRD_PERSON_FRONT,""));
            list.add(new Shot("store-floss-alex","quarkos_emotes:floss",CameraType.THIRD_PERSON_FRONT,"34"));
            list.add(new Shot("store-backflip-steve","quarkos_emotes:backflip",CameraType.THIRD_PERSON_FRONT,"34"));
            list.add(new Shot("store-meditate-steve","quarkos_emotes:meditate",CameraType.THIRD_PERSON_FRONT,""));
            list.add(new Shot("store-sword-steve","quarkos_emotes:sword_salute",CameraType.THIRD_PERSON_FRONT,""));
            list.add(new Shot("store-sword-alex","quarkos_emotes:sword_salute",CameraType.THIRD_PERSON_FRONT,""));
            return list;
        }
        if(EmoteResources.REGISTRY.all().isEmpty()) {
            fail++;LoggerFactory.getLogger("quarkos_emotes").error("EMOTE_CHECK name=registry result=FAIL detail=no_emotes_loaded");
        }
        Set<String> only=new HashSet<>(Arrays.asList(System.getProperty("quarkos_emotes.only","").split("\\s*,\\s*")));
        for(var e:EmoteResources.REGISTRY.all()) {
            String name=e.id().split(":",2)[1].replace('/','_');
            boolean all=only.contains("")||only.contains(name)||only.contains(e.id())||only.contains("emote-"+name);
            if(all) list.add(new Shot("emote-"+name,e.id(),CameraType.THIRD_PERSON_FRONT,""));
            if(!QUICK&&name.equals("sword_salute"))for(String rig:List.of("steve","alex"))
                for(String view:List.of("front","side","34","hidden")) {
                    String variant="emote-"+name+"-"+view+"-"+rig;
                    if(all||only.contains(variant))list.add(new Shot(variant,e.id(),CameraType.THIRD_PERSON_FRONT,view));
                }
            if(!QUICK&&(all||only.contains("emote-"+name+"-34"))) list.add(new Shot("emote-"+name+"-34",e.id(),CameraType.THIRD_PERSON_FRONT,"34"));
            if(!QUICK&&Set.of("backflip","worm","floss").contains(name))for(String model:List.of("steve","alex")) {
                String variant="emote-"+name+"-"+model;
                if(all||only.contains(variant))list.add(new Shot(variant,e.id(),CameraType.THIRD_PERSON_FRONT,"34"));
            }
            if(!QUICK&&name.equals("spin")&&(all||only.contains("emote-spin-90")))
                list.add(new Shot("emote-spin-90",e.id(),CameraType.THIRD_PERSON_FRONT,"90"));
            if(!QUICK&&Set.of("tableflip","groove","bigdance","backflip","worm").contains(name)) {
                if(!Set.of("backflip","worm").contains(name)&&(all||only.contains("emote-"+name+"-back")))
                    list.add(new Shot("emote-"+name+"-back",e.id(),CameraType.THIRD_PERSON_BACK,""));
                if(all||only.contains("emote-"+name+"-side")) list.add(new Shot("emote-"+name+"-side",e.id(),CameraType.THIRD_PERSON_FRONT,"side"));
            }
        }
        String wave=EmoteResources.REGISTRY.all().stream().filter(e->e.id().endsWith(":wave")).map(Emote::id).findFirst()
                .orElse(EmoteResources.REGISTRY.all().isEmpty()?"":EmoteResources.REGISTRY.all().getFirst().id());
        for(String ui:List.of("ui-wheel-hover","ui-wheel-s"+GUI_SCALE,"ui-config","ui-config-meditate","ui-config-pushups","ui-config-lay_down","fp-wave","fp-wave-camera","fp-sleep","fp-sleep-camera")) if((!QUICK||ui.startsWith("ui-"))&&(only.contains("")||only.contains(ui)||(ui.startsWith("ui-wheel-s")&&only.contains("ui-wheel"))||(ui.startsWith("ui-config-")&&only.contains("ui-config"))))
            list.add(new Shot(ui,ui.startsWith("fp-sleep")?"quarkos_emotes:sleep":ui.startsWith("ui-config-")?"quarkos_emotes:"+ui.substring("ui-config-".length()):wave,
                    ui.equals("fp-wave")||ui.equals("fp-sleep")?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT,ui));
        if(!QUICK) for(int[] res:new int[][]{{854,480},{1280,720},{1920,1080},{2560,1440}})
            for(int scale:new int[]{1,2,3,4,0}) {
                String name="ui-wheel-"+res[0]+"x"+res[1]+"-s"+scale;
                if(only.contains("")||only.contains("ui-matrix")||only.contains(name))
                    list.add(new Shot(name,wave,CameraType.THIRD_PERSON_FRONT,"matrix:"+res[0]+":"+res[1]+":"+scale));
            }
        for (UiShot ui : UI_SHOTS) {
            if (only.contains("") || only.contains(ui.name())) {
                list.add(new Shot(ui.name(), wave, CameraType.THIRD_PERSON_FRONT, ui.name()));
            }
        }
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
        var shot=shots.get(index);
        boolean matrix=shot.ui().startsWith("matrix:");
        UiShot ui = UI_SHOTS.stream().filter(candidate -> candidate.name().equals(shot.name())).findFirst().orElse(null);
        int setupTick = matrix || ui != null ? 12 : 0;
        if (setupTick > 0 && wait == 0) {
            mc.setScreen(null);
            if (matrix) {
                String[] parts = shot.ui().split(":");
                resize(mc, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            } else {
                resize(mc, ui.width(), ui.height(), ui.scale());
            }
            wait++;
            return;
        }
        // Wait for the framebuffer callback before laying out the screen.
        if (wait < setupTick) { wait++; return; }
        anchor(mc);
        if(restoringCamera) {
            if(++restoreTicks<6)return;
            check(shot.name()+"-restore",mc.options.getCameraType()==CameraType.FIRST_PERSON&&EmotesClient.player(mc.player.getUUID())==null,
                    "camera_restored_after_0.2s_cancel_blend");
            restoringCamera=false;index++;wait=0;ticks=0;return;
        }
        if (wait == setupTick) {
            if (setupTick > 0) resize(mc, wantWidth, wantHeight, mc.options.guiScale().get());
            moved=false;handMoved=false;queued=false;bodyX=bodyY=0;renderedProps.clear();propPass=false;mc.setScreen(null);
            model=shot.name().endsWith("-alex")?"alex":"steve";
            boolean namedView=shot.ui().equals("side")||shot.ui().equals("34");
            float requested=shot.ui().equals("side")?90:shot.ui().equals("34")?35:0;
            var frame=ShotFraming.forId(shot.id(),requested);
            portraitOrbit=namedView?requested:frame.orbit();
            portraitDistance=frame.distance();portraitDrop=frame.drop();portraitForward=frame.forward();portraitPitch=frame.pitch();
            boolean cameraShot=shot.ui().endsWith("-camera");
            EmoteConfig.get().firstPerson=cameraShot?EmoteConfig.FirstPerson.THIRD_PERSON_CAMERA:EmoteConfig.FirstPerson.SHOW_ARMS;
            mc.options.setCameraType(shot.camera());
            if(cameraShot) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);EmotesClient.play(shot.id());
                if(mc.options.getCameraType()!=CameraType.THIRD_PERSON_BACK) throw new IllegalStateException("First-person camera mode did not switch");
            } else EmotesClient.preview(shot.id());
            var p=EmotesClient.player(mc.player.getUUID());chainSwitched=false;chainFrom=null;
            if(p!=null) {
                if(p.emote().enter()!=null||p.emote().followLoop()!=null) {
                    double boundary=p.emote().enter()!=null?p.emote().entryLength():p.emote().length();
                    p.seek(boundary-.1);chainFrom=p.segment(0).name();
                } else p.freeze(shot.ui().equals("hidden")?.249:shot.ui().equals("90")?.63:
                        p.emote().loop()==Emote.Loop.HOLD?p.emote().length()+1:poseTime(p.emote()));
            }
            boolean menu = shot.ui().startsWith("ui-") || matrix;
            //? if >=26.3 {
            /*setHudHidden(mc, !menu && !(shot.ui().equals("fp-wave") || shot.ui().equals("fp-sleep")));
            *///?} else {
            mc.options.hideGui = !menu && !(shot.ui().equals("fp-wave") || shot.ui().equals("fp-sleep"));
            //?}
            if (ui != null) seedUiFixture();
            if (shot.ui().startsWith("ui-wheel") || matrix) {
                var wheel = new EmoteWheelScreen(EmoteConfig.MenuView.WHEEL);
                mc.setScreen(wheel);
                if (ui != null || shot.ui().endsWith("hover")) {
                    wheel.harnessHover = 0;
                    var layout = WheelLayout.of(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                    cursor(mc, layout.cx(), layout.cy() - (layout.innerRadius() + layout.radius()) / 2.0);
                } else {
                    cursor(mc, 2, 2);
                }
            } else if (shot.ui().startsWith("ui-grid")) {
                EmotesClient.selected = shot.id();
                var grid = new EmoteWheelScreen(EmoteConfig.MenuView.GRID);
                mc.setScreen(grid);
                var layout = GridLayout.of(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                if (shot.ui().equals("ui-grid-search")) {
                    typeSearch(grid);
                    cursor(mc, 2, 2);
                } else {
                    var card = layout.cards().card(1, 0);
                    cursor(mc, card.x() + card.w() / 2.0, card.y() + card.h() / 2.0);
                }
            } else if (shot.ui().startsWith("ui-config") || shot.ui().startsWith("ui-settings")) {
                EmotesClient.selected = shot.id();
                var settings = new EmoteConfigScreen(null);
                mc.setScreen(settings);
                settings.harnessHoverSound = ui != null;
                if (ui == null) {
                    var emote = EmoteResources.REGISTRY.get(shot.id());
                    if (emote != null) settings.freezePreview(poseTime(emote));
                }
                var geo = ConfigLayout.geometry(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                var row = geo.row(ui == null ? geo.playback() : geo.sound(), 0);
                cursor(mc, row.x() + row.w() / 2.0, row.y() + row.h() / 2.0);
            }
            LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_SHOT name={} time={} camera={} distance={} orbit={} guiScale={} effectiveScale={}",
                    shot.name(),p==null?0:p.time(0),mc.options.getCameraType(),portraitDistance,portraitOrbit,mc.options.guiScale().get(),mc.getWindow().getGuiScale());
        }
        var chainPlayer=EmotesClient.player(mc.player.getUUID());
        if(chainFrom!=null&&!chainSwitched&&chainPlayer!=null&&!chainPlayer.segment(0).name().equals(chainFrom)) {
            chainSwitched=true;
            LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_CHECK chain={} from={} to={} result=PASS",shot.id(),chainFrom,chainPlayer.segment(0).name());
            chainPlayer.freeze(poseTime(chainPlayer.emote()));
        }
        wait++;
        // New UI shots sample the live previews after about 0.8 s in the screen.
        if (wait < (ui != null ? setupTick + 17 : matrix ? 48 : 35)) return;
        Path path=directory().resolve(shot.name()+".png");
        if (!queued) {
            written = null;
            screenshot(shot.name());
            queued = true;
            return;
        }
        if (!path.equals(written) || !Files.isRegularFile(path)) {
            if (wait > 300) throw new IllegalStateException("Screenshot not written: " + path);
            return;
        }
        if (shot.name().startsWith("ui-")) {
            LoggerFactory.getLogger("quarkos_emotes").info("UI_SHOT_DONE {}", shot.name());
        }
        if (ui != null) restoreUiFixture();
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
        if(shot.id().equals("quarkos_emotes:sword_salute")) {
            boolean expected=!shot.ui().equals("hidden");
            boolean visible=playback!=null&&playback.emote().props().stream().anyMatch(g->g.bones().stream()
                    .anyMatch(b->b.name().equals("sword")&&g.visible(b,playback,0)));
            check(shot.name()+"-prop",propPass&&visible==expected&&renderedProps.contains("sword")==expected,
                    "expected_visible="+expected+"_sampled="+visible+"_submitted="+renderedProps.contains("sword")+"_render_pass="+propPass);
        }
        if(chainFrom!=null&&shot.name().startsWith("emote-"))check(shot.name()+"-chain",chainSwitched&&playback!=null&&playback.weight(0)==1,
                "segment_switched_without_blend");
        if(!shot.name().startsWith("ui-")&&Set.of("quarkos_emotes:sit","quarkos_emotes:sleep","quarkos_emotes:kneel","quarkos_emotes:lay_down").contains(shot.id()))
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
        if(shot.name().startsWith("store-")&&!shot.name().equals("store-wheel"))
            check(shot.name()+"-rendered",moved,"store pose rendered");
        if(shot.ui().endsWith("-camera")) {
            boolean ok=mc.options.getCameraType()==CameraType.THIRD_PERSON_BACK&&moved;
            if(ok)pass++;else fail++;
            LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_CHECK name={} result={} detail=camera_switched_and_pose_rendered",shot.name(),ok?"PASS":"FAIL");
            EmotesClient.stop();
            restoringCamera=true;restoreTicks=0;return;
        }
        index++;wait=0;ticks=0;
    }
    private static void seedUiFixture() {
        var config = EmoteConfig.get();
        var slots = new ArrayList<String>(EmoteConfig.SLOT_COUNT);
        for (int slot = 0; slot < EmoteConfig.SLOT_COUNT; slot++) slots.add(config.slot(slot));
        uiFixture = new UiFixture(config.favorites, slots, config.wheelFavoritesOnly);
        config.favorites = new ArrayList<>(List.of("quarkos_emotes:wave", "quarkos_emotes:thinking", "quarkos_emotes:clap"));
        config.wheelFavoritesOnly = false;
        for (int slot = 0; slot < EmoteConfig.SLOT_COUNT; slot++) config.setSlot(slot, "");
        config.setSlot(0, "quarkos_emotes:wave");
        config.setSlot(2, "quarkos_emotes:bigdance");
    }

    private static void restoreUiFixture() {
        if (uiFixture == null) return;
        var config = EmoteConfig.get();
        config.favorites = uiFixture.favorites();
        config.wheelFavoritesOnly = uiFixture.favoritesOnly();
        for (int slot = 0; slot < EmoteConfig.SLOT_COUNT; slot++) config.setSlot(slot, uiFixture.slots().get(slot));
        uiFixture = null;
    }

    private static void typeSearch(EmoteWheelScreen screen) {
        EditBox search = (EditBox) screen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
        screen.setFocused(search);
        for (char letter : "da".toCharArray()) {
            //? if >=26.1 {
            /*screen.charTyped(new net.minecraft.client.input.CharacterEvent(letter));
            *///?} elif >=1.21.11 {
            screen.charTyped(new net.minecraft.client.input.CharacterEvent(letter, 0));
            //?} else {
            /*screen.charTyped(letter, 0);
            *///?}
        }
        if (!search.getValue().equals("da") || !"quarkos_emotes:bigdance".equals(EmotesClient.selected)) {
            throw new IllegalStateException("UI search did not select Big Dance for da");
        }
        screen.setFocused(null);
    }

    public static Path screenshot(String name) throws java.io.IOException {
        Path path=directory().resolve(name+".png");Files.createDirectories(path.getParent());Files.deleteIfExists(path);pending=path;return path;
    }
    private static Path directory() { return Path.of(System.getProperty("quarkos_emotes.shots","build/shots")); }
    private static int wantWidth,wantHeight;
    private static void resize(Minecraft mc,int width,int height,int guiScale) {
        wantWidth=width;wantHeight=height;
        mc.options.guiScale().set(guiScale);
        mc.getWindow().setWindowed(width,height);
        //? if >=26.3 {
        /*// The renderer resizes its target after the SDL framebuffer event.
        org.lwjgl.sdl.SDLVideo.SDL_SyncWindow(mc.getWindow().handle());
        *///?} else {
        // The framebuffer callback can lag a GLFW poll, and resizeDisplay then
        // rebuilds the color target at the previous size. Pin both dimensions
        // so the wheel is laid out for the requested window.
        org.lwjgl.glfw.GLFW.glfwPollEvents();
        mc.getWindow().setWidth(width);
        mc.getWindow().setHeight(height);
        //?}
        mc.resizeDisplay();
        var target=mc.getMainRenderTarget();
        LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_UI resize={}x{} requestedScale={} logical={}x{} effective={} target={}x{}",
                width,height,guiScale,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),mc.getWindow().getGuiScale(),target.width,target.height);
    }
    private static void cursor(Minecraft mc,double logicalX,double logicalY) {
        var window = mc.getWindow();
        double x = logicalX * window.getScreenWidth() / window.getGuiScaledWidth();
        double y = logicalY * window.getScreenHeight() / window.getGuiScaledHeight();
        //? if >=26.3 {
        /*org.lwjgl.sdl.SDLMouse.SDL_WarpMouseInWindow(window.handle(), (float) x, (float) y);
        *///?} elif >=1.21.11 {
        long handle=window.handle();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(handle,x,y);
        //?} else {
        /*long handle=window.getWindow();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(handle,x,y);
        *///?}
    }
    //? if >=26.3 {
    /*static void setHudHidden(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
    }
    *///?}
    /** At GameRenderer TAIL: includes deferred GUI and the fully rendered world. */
    /** Hide HUD, chat and toasts for a store or lifecycle frame. */
    public static void quiet(Minecraft mc) {
        //? if >=26.3 {
        /*setHudHidden(mc, true);
        mc.gui.hud.getChat().clearMessages(false);
        mc.gui.toastManager().clear();
        *///?} else {
        mc.options.hideGui=true;
        if(mc.gui!=null) mc.gui.getChat().clearMessages(false);
        //? if >=1.21.11 {
        mc.getToastManager().clear();
        //?} else {
        /*mc.getToasts().clear();
        *///?}
        //?}
    }
    public static void capture() {
        if(pending==null) return;Path path=pending;pending=null;Minecraft mc=Minecraft.getInstance();
        if(Boolean.getBoolean("quarkos_emotes.store")||MultiplayerHarness.cleanShot()) quiet(mc);
        //? if >=1.21.11 {
        // The presented default framebuffer repeats the previous color target after a
        // resize. Copy the main target, which resizeDisplay rebuilds at the new size.
        var target=mc.getMainRenderTarget();
        LoggerFactory.getLogger("quarkos_emotes").info("EMOTE_CAPTURE target={}x{} window={}x{}",target.width,target.height,mc.getWindow().getWidth(),mc.getWindow().getHeight());
        net.minecraft.client.Screenshot.takeScreenshot(target,image->write(path,image));
        //?} else {
        /*write(path,Screenshot.takeScreenshot(mc.getMainRenderTarget()));
        *///?}
    }
    private static void write(Path path,NativeImage img) {
        try {
            img.writeToFile(path);
            written = path;
        } catch (Exception e) {
            LoggerFactory.getLogger("quarkos_emotes").error("Screenshot failed", e);
        } finally {
            img.close();
        }
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
