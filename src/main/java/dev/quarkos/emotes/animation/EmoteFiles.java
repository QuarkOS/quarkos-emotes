package dev.quarkos.emotes.animation;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Shared filename contract for resource packs and config files. */
public final class EmoteFiles {
    private EmoteFiles() {}
    public static boolean animation(String path) {
        String name=path.replace('\\','/');
        if(name.startsWith("emotes/")) name=name.substring(7);
        return name.endsWith(".animation.json") || (!name.contains("/") && name.endsWith(".json")
                && !name.endsWith(".geo.json") && !name.endsWith(".quarkos.json"));
    }
    public static String stem(String path) {
        String name=path.replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1);
        return name.endsWith(".animation.json")?name.substring(0,name.length()-15):name.replaceFirst("\\.json$","");
    }
    public static String sidecar(String path) { return path.substring(0,path.lastIndexOf('/')+1)+stem(path)+".quarkos.json"; }
    public static String title(String id) {
        return Arrays.stream(id.split("[_/.-]+" )).filter(s->!s.isEmpty())
                .map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(java.util.stream.Collectors.joining(" "));
    }
    public static List<Path> discover(Path directory) throws IOException {
        try(var files=Files.walk(directory)) {
            return files.filter(Files::isRegularFile).filter(p->animation(directory.relativize(p).toString())).sorted().toList();
        }
    }
}
