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
        String[] parts=name.split("/");
        for(int i=0;i<parts.length-1;i++) if(parts[i].startsWith("_")) return false;
        if(parts.length==1) return name.endsWith(".animation.json") || (name.endsWith(".json")
                && !name.endsWith(".geo.json") && !name.endsWith(".quarkos.json"));
        return parts[parts.length-1].equals(parts[parts.length-2]+".animation.json");
    }
    public static String stem(String path) {
        String name=path.replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1);
        return name.endsWith(".animation.json")?name.substring(0,name.length()-15):name.replaceFirst("\\.json$","");
    }
    public static String sidecar(String path) { return path.substring(0,path.lastIndexOf('/')+1)+stem(path)+".quarkos.json"; }
    public static String chain(String main,String segment) {
        // Flat legacy files have no companion segments.
        String normalized=main.replace('\\','/');
        if(normalized.startsWith("emotes/")) normalized=normalized.substring(7);
        if(!normalized.contains("/")) return null;
        return main.substring(0,main.length()-15)+"_"+segment+".animation.json";
    }
    @FunctionalInterface public interface Readers { java.io.Reader open(String path) throws IOException; }
    /** Optional segments belong to the main ID and never become registry entries. */
    public static Emote chained(Emote main,String path,Readers readers) throws IOException {
        Emote[] segments=new Emote[2];String[] names={"enter","loop"};
        for(int i=0;i<2;i++) {
            if(i==1&&main.loop()!=Emote.Loop.HOLD) continue;
            String file=chain(path,names[i]);if(file==null) continue;
            try(var reader=readers.open(file)) {
                if(reader==null) continue;
                var parsed=new EmoteParser().parse(reader,"chain",file,null);
                if(!parsed.valid()||parsed.emotes().size()!=1) throw new IOException("Invalid chain file "+file+": "+parsed.errors());
                var segment=parsed.emotes().getFirst();
                if(segment.loop()!=(i==0?Emote.Loop.ONCE:Emote.Loop.LOOP)) throw new IOException("Invalid chain loop mode: "+file);
                segments[i]=segment;
            }
        }
        return main.withChain(segments[0],segments[1]);
    }
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
