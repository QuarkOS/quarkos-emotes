package dev.quarkos.emotes.animation;

import com.google.gson.*;
import java.io.Reader;
import java.util.*;

/** Bedrock 1.8 animation importer, with no game dependencies or executable Molang. */
public final class EmoteParser {
    public record Result(List<Emote> emotes, List<String> warnings, List<String> errors) {
        public boolean valid() { return errors.isEmpty(); }
    }
    private final List<String> warnings=new ArrayList<>();
    public Result parse(Reader reader, String namespace) {
        return parse(reader,namespace,null,null);
    }
    public Result parse(Reader reader, String namespace, String filename, JsonObject sidecar) {
        warnings.clear(); List<Emote> emotes=new ArrayList<>(); List<String> errors=new ArrayList<>();
        try {
            JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();
            if (!"1.8.0".equals(text(root,"format_version","1.8.0"))) warnings.add("Expected format_version 1.8.0");
            JsonObject animations=root.getAsJsonObject("animations");
            if (animations==null || animations.isEmpty()) throw new IllegalArgumentException("Missing animations object");
            for (var entry:animations.entrySet()) {
                try { emotes.add(animation(entry.getKey(),entry.getValue().getAsJsonObject(),namespace,filename,root.has("quarkos")?root.getAsJsonObject("quarkos"):sidecar)); }
                catch (RuntimeException e) { errors.add(entry.getKey()+": "+e.getMessage()); }
            }
        } catch (RuntimeException e) { errors.add("Invalid animation file: "+e.getMessage()); }
        return new Result(List.copyOf(emotes),List.copyOf(warnings),List.copyOf(errors));
    }
    private Emote animation(String key,JsonObject a,String ns,String filename,JsonObject metadata) {
        JsonObject q=a.has("quarkos")?object(a,"quarkos"):metadata==null?new JsonObject():metadata;
        String stem=filename==null?key.replaceFirst("^animation\\.",""):EmoteFiles.stem(filename);
        String id=text(q,"id",ns+":"+stem);
        if (!id.contains(":")) id=ns+":"+id;
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || id.length()>128) throw new IllegalArgumentException("Invalid id "+id);
        Map<String,Emote.Bone> bones=new LinkedHashMap<>();
        for (var e:object(a,"bones").entrySet()) {
            JsonObject b=e.getValue().getAsJsonObject();
            bones.put(BedrockAxes.bone(e.getKey()), new Emote.Bone(track(b.get("rotation"),Vec3.ZERO),track(b.get("position"),Vec3.ZERO),track(b.get("scale"),Vec3.ONE)));
        }
        double length=number(a.get("animation_length"));
        if (!a.has("animation_length")) length=Math.max(1,bones.values().stream().flatMap(b->java.util.stream.Stream.of(b.rotation(),b.position(),b.scale()))
                .flatMap(t->t.keys().stream()).mapToDouble(Track.Key::time).max().orElse(1));
        if (length<=0 || length>3600) throw new IllegalArgumentException("animation_length must be in (0, 3600]");
        String loop=text(a,"loop","false");
        Emote.Loop looping=switch(loop) { case "true" -> Emote.Loop.LOOP; case "hold_on_last_frame" -> Emote.Loop.HOLD; default -> Emote.Loop.ONCE; };
        List<Emote.Sound> sounds=new ArrayList<>();
        for(var e:object(a,"sound_effects").entrySet()) {
            double time=time(e.getKey());
            var list=e.getValue().isJsonArray() ? e.getValue().getAsJsonArray() : new JsonArray();
            if(!e.getValue().isJsonArray()) list.add(e.getValue());
            for(var s:list) sounds.add(new Emote.Sound(time,text(s.getAsJsonObject(),"effect",""),1,1));
        }
        JsonObject sound=object(q,"sound");
        if(sound.has("id")) sounds.add(new Emote.Sound(number(sound.get("at")),text(sound,"id",""),
                (float)(sound.has("volume")?number(sound.get("volume")):1),(float)(sound.has("pitch")?number(sound.get("pitch")):1)));
        sounds.sort(Comparator.comparingDouble(Emote.Sound::time));
        Emote.ArmPivot armPivot=switch(text(q,"arm_pivot","model")) {
            case "model" -> Emote.ArmPivot.MODEL;
            case "steve" -> Emote.ArmPivot.STEVE;
            default -> throw new IllegalArgumentException("arm_pivot must be model or steve");
        };
        return new Emote(id,text(q,"name",EmoteFiles.title(id.substring(id.indexOf(':')+1))),text(q,"icon","minecraft:note_block"),
                text(q,"category","general"),length,looping,!q.has("cancel_on_move")||q.get("cancel_on_move").getAsBoolean(),
                text(q,"easing","linear"),bones,sounds,q.has("prop")?q.getAsJsonObject("prop"):null,List.of(),armPivot);
    }
    private Track track(JsonElement e,Vec3 fallback) {
        if(e==null) return new Track(List.of());
        if(!e.isJsonObject()) return new Track(List.of(new Track.Key(0,vec(e,fallback),vec(e,fallback),Track.Mode.LINEAR)));
        List<Track.Key> keys=new ArrayList<>();
        for(var entry:e.getAsJsonObject().entrySet()) {
            double t=time(entry.getKey()); JsonElement v=entry.getValue();
            JsonObject obj=v.isJsonObject()?v.getAsJsonObject():new JsonObject();
            Vec3 post=vec(obj.has("post")?obj.get("post"):obj.has("pre")?obj.get("pre"):v,fallback);
            Vec3 pre=vec(obj.has("pre")?obj.get("pre"):obj.has("post")?obj.get("post"):v,fallback);
            Track.Mode mode=switch(text(obj,"lerp_mode","linear").toLowerCase(Locale.ROOT)) {
                case "step" -> Track.Mode.STEP; case "catmullrom" -> Track.Mode.CATMULLROM; default -> Track.Mode.LINEAR; };
            keys.add(new Track.Key(t,pre,post,mode));
        }
        return new Track(keys);
    }
    private double time(String s) { double n=Double.parseDouble(s); if(!Double.isFinite(n)||n<0||n>3600) throw new IllegalArgumentException("Invalid key time "+s); return n; }
    private Vec3 vec(JsonElement e,Vec3 fallback) {
        if(e==null) return fallback;
        if(e.isJsonPrimitive()) { double n=number(e); return new Vec3(n,n,n); }
        if(!e.isJsonArray()||e.getAsJsonArray().size()!=3) throw new IllegalArgumentException("Expected a 3 component vector");
        var a=e.getAsJsonArray(); return new Vec3(number(a.get(0)),number(a.get(1)),number(a.get(2)));
    }
    private double number(JsonElement e) {
        if(e==null) return 0;
        String s=e.getAsString().trim().toLowerCase(Locale.ROOT);
        try { double n=switch(s) { case "math.pi", "pi" -> Math.PI; case "math.e" -> Math.E; default -> Double.parseDouble(s); };
            if(Double.isFinite(n)) return n;
        } catch(NumberFormatException ignored) {}
        warnings.add("Unsupported Molang/value '"+s+"'; using 0"); return 0;
    }
    private static JsonObject object(JsonObject a,String k) { return a.has(k)&&a.get(k).isJsonObject()?a.getAsJsonObject(k):new JsonObject(); }
    private static String text(JsonObject a,String k,String fallback) { return a.has(k)?a.get(k).getAsString():fallback; }
}
