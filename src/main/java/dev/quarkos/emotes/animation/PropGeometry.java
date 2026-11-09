package dev.quarkos.emotes.animation;

import com.google.gson.*;
import java.io.Reader;
import java.util.*;

/** Bedrock box-UV geometry; names are preserved exactly for animation binding. */
public record PropGeometry(String texture,int textureWidth,int textureHeight,List<Bone> bones) {
    public record Cube(Vec3 origin,Vec3 size,int u,int v,Vec3 pivot,Vec3 rotation,boolean mirror,double inflate) {}
    public record Bone(String name,String parent,Vec3 pivot,Vec3 rotation,List<Cube> cubes) {
        public Bone { cubes=List.copyOf(cubes); }
        public Emote.Pose sample(EmotePlayer player,double partial) {
            var track=player.emote().bones().get(name);
            return track==null?new Emote.Pose(Vec3.ZERO,Vec3.ZERO,Vec3.ONE):track.sample(player.time(partial),player.emote().easing(),player.emote().loop()==Emote.Loop.LOOP);
        }
    }
    public PropGeometry { bones=List.copyOf(bones); }
    public static List<PropGeometry> parse(Reader reader,String texture) {
        JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();
        if(!"1.12.0".equals(root.get("format_version").getAsString())) throw new IllegalArgumentException("Expected geometry format_version 1.12.0");
        List<PropGeometry> result=new ArrayList<>();
        for(var entry:root.getAsJsonArray("minecraft:geometry")) {
            var geo=entry.getAsJsonObject(); var description=geo.getAsJsonObject("description");
            int width=description.get("texture_width").getAsInt(),height=description.get("texture_height").getAsInt();
            if(width<=0||height<=0)throw new IllegalArgumentException("Invalid texture dimensions");
            List<Bone> bones=new ArrayList<>();Set<String> names=new HashSet<>();
            for(var value:geo.getAsJsonArray("bones")) {
                var b=value.getAsJsonObject();String name=b.get("name").getAsString();
                if(!names.add(name))throw new IllegalArgumentException("Duplicate geometry bone "+name);
                List<Cube> cubes=new ArrayList<>();
                if(b.has("cubes"))for(var item:b.getAsJsonArray("cubes")) {
                    var c=item.getAsJsonObject();var uv=c.getAsJsonArray("uv");
                    if(uv.size()!=2)throw new IllegalArgumentException("Expected box UV [u,v]");
                    Vec3 size=vec(c,"size",Vec3.ZERO);
                    if(size.x()<0||size.y()<0||size.z()<0)throw new IllegalArgumentException("Negative cube size");
                    cubes.add(new Cube(vec(c,"origin",Vec3.ZERO),size,uv.get(0).getAsInt(),uv.get(1).getAsInt(),
                            vec(c,"pivot",Vec3.ZERO),vec(c,"rotation",Vec3.ZERO),bool(c,"mirror",bool(b,"mirror",false)),
                            c.has("inflate")?c.get("inflate").getAsDouble():0));
                }
                bones.add(new Bone(name,b.has("parent")?b.get("parent").getAsString():null,vec(b,"pivot",Vec3.ZERO),vec(b,"rotation",Vec3.ZERO),cubes));
            }
            for(var bone:bones) {
                Set<String> visited=new HashSet<>();Bone cursor=bone;
                while(cursor!=null) {
                    if(!visited.add(cursor.name()))throw new IllegalArgumentException("Geometry parent cycle");
                    String parent=cursor.parent();
                    cursor=parent==null?null:bones.stream().filter(b->b.name().equals(parent)).findFirst()
                            .orElseThrow(()->new IllegalArgumentException("Missing geometry parent "+parent));
                }
            }
            result.add(new PropGeometry(texture,width,height,bones));
        }
        return List.copyOf(result);
    }
    private static boolean bool(JsonObject o,String key,boolean fallback) { return o.has(key)?o.get(key).getAsBoolean():fallback; }
    private static Vec3 vec(JsonObject o,String key,Vec3 fallback) {
        if(!o.has(key))return fallback;
        var a=o.getAsJsonArray(key);if(a.size()!=3)throw new IllegalArgumentException("Expected vec3 "+key);
        var v=new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());
        if(!Double.isFinite(v.x()+v.y()+v.z()))throw new IllegalArgumentException("Non-finite "+key);
        return v;
    }
}
