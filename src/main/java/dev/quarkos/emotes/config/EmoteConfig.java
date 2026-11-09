package dev.quarkos.emotes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.slf4j.LoggerFactory;
public final class EmoteConfig {
    public enum FirstPerson { THIRD_PERSON_CAMERA, SHOW_ARMS, NONE }
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static EmoteConfig instance=new EmoteConfig();
    private static Path file;
    public FirstPerson firstPerson=FirstPerson.THIRD_PERSON_CAMERA;
    public boolean cancelOnMove=true, playSounds=true, showOthers=true, holdWheel=true;
    public float soundVolume=1;
    public List<String> favorites=new ArrayList<>();
    public static EmoteConfig get() { return instance; }
    public static void load(Path game) {
        file=game.resolve("config/quarkos_emotes.json");
        if(Files.isRegularFile(file)) try(Reader r=Files.newBufferedReader(file)) {
            EmoteConfig c=GSON.fromJson(r,EmoteConfig.class); if(c!=null) instance=c;
        } catch(Exception e) { LoggerFactory.getLogger("quarkos_emotes").warn("Could not load emote settings",e); }
        if(instance.firstPerson==null) instance.firstPerson=FirstPerson.THIRD_PERSON_CAMERA;
        if(instance.favorites==null) instance.favorites=new ArrayList<>();
        instance.favorites=new ArrayList<>(instance.favorites.stream().filter(Objects::nonNull).distinct().limit(8).toList());
        instance.soundVolume=Float.isFinite(instance.soundVolume)?Math.clamp(instance.soundVolume,0,1):1;
        save();
    }
    public static void save() {
        if(file==null) return;
        try { Files.createDirectories(file.getParent()); Path tmp=file.resolveSibling(file.getFileName()+".tmp");
            Files.writeString(tmp,GSON.toJson(instance));
            try { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING); }
        } catch(IOException e) { LoggerFactory.getLogger("quarkos_emotes").warn("Could not save emote settings",e); }
    }
    public void favorite(String id) { if(!favorites.remove(id)&&favorites.size()<8) favorites.add(id); save(); }
}
