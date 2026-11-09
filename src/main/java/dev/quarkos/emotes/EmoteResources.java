package dev.quarkos.emotes;

import com.google.gson.*;
import dev.quarkos.emotes.animation.*;
import dev.quarkos.emotes.render.GeometryProps;
import net.minecraft.server.packs.resources.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.slf4j.LoggerFactory;

public final class EmoteResources implements ResourceManagerReloadListener {
    public static final EmoteRegistry REGISTRY=new EmoteRegistry();
    @Override public void onResourceManagerReload(ResourceManager manager) {
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()) {mc.submit(()->onResourceManagerReload(manager)).join();return;}
        EmotesClient.reload();
        GeometryProps.invalidate();
        List<Emote> emotes=new ArrayList<>();
        List<String> packOrder=manager.listPacks().map(p->p.packId()).toList();
        record Source(Identifier id,Resource resource) {}
        List<Source> sources=new ArrayList<>();
        manager.listResourceStacks("emotes",id->EmoteFiles.animation(id.getPath())).forEach((id,stack)->
                stack.forEach(resource->sources.add(new Source(id,resource))));
        var geometries=manager.listResources("emotes",id->id.getPath().endsWith(".geo.json"));
        sources.sort(Comparator.<Source>comparingInt(s->packOrder.indexOf(s.resource().sourcePackId())).thenComparing(s->s.id().toString()));
        for(var s:sources) try(Reader reader=s.resource().openAsReader()) {
            String path=s.id().getPath(),folder=path.substring(0,path.lastIndexOf('/')+1);
            JsonObject metadata=null;
            var side=manager.getResource(Identifier.fromNamespaceAndPath(s.id().getNamespace(),EmoteFiles.sidecar(path)));
            if(side.isPresent())try(Reader r=side.get().openAsReader()) { metadata=JsonParser.parseReader(r).getAsJsonObject(); }
            catch(Exception e) {warn(EmoteFiles.sidecar(path),e);}
            List<PropGeometry> props=new ArrayList<>();
            for(var geo:geometries.entrySet()) {
                var id=geo.getKey();String gp=id.getPath();
                if(!id.getNamespace().equals(s.id().getNamespace())||!gp.substring(0,gp.lastIndexOf('/')+1).equals(folder))continue;
                String texture=id.toString().replaceFirst("\\.geo\\.json$",".png");
                if(manager.getResource(Identifier.parse(texture)).isEmpty()) { warn(gp,new IOException("Missing matching PNG"));continue; }
                try(Reader r=geo.getValue().openAsReader()) { props.addAll(PropGeometry.parse(r,texture)); }
                catch(Exception e) {warn(gp,e);}
            }
            read(reader,s.id().getNamespace(),path,metadata,props,emotes);
        } catch(Exception e) {warn(s.id().toString(),e);}
        Path custom=Minecraft.getInstance().gameDirectory.toPath().resolve("config/quarkos_emotes/emotes");
        try {
            Files.createDirectories(custom);
            for(Path p:EmoteFiles.discover(custom)) try(Reader reader=Files.newBufferedReader(p)) {
                JsonObject metadata=null;Path side=p.resolveSibling(EmoteFiles.stem(p.toString())+".quarkos.json");
                if(Files.exists(side))try(Reader r=Files.newBufferedReader(side)) {metadata=JsonParser.parseReader(r).getAsJsonObject();}
                catch(Exception e) {warn(side.toString(),e);}
                List<PropGeometry> props=new ArrayList<>();
                try(var files=Files.list(p.getParent())) {
                    for(Path geo:files.filter(f->f.getFileName().toString().endsWith(".geo.json")).sorted().toList()) {
                        Path png=geo.resolveSibling(geo.getFileName().toString().replaceFirst("\\.geo\\.json$",".png"));
                        try(Reader r=Files.newBufferedReader(geo)) {
                            String texture=GeometryProps.localTexture(custom.relativize(png).toString(),png);
                            props.addAll(PropGeometry.parse(r,texture));
                        } catch(Exception e) {warn(geo.toString(),e);}
                    }
                }
                read(reader,"custom",p.getFileName().toString(),metadata,props,emotes);
            } catch(Exception e) {warn(p.toString(),e);}
        } catch(IOException e) {warn(custom.toString(),e);}
        REGISTRY.replace(emotes);
        EmotesClient.resourcesReloaded();
        dev.quarkos.emotes.render.PropAttachment.invalidate();
        LoggerFactory.getLogger("quarkos_emotes").info("Loaded {} emotes",REGISTRY.all().size());
    }
    private static void read(Reader r,String namespace,String source,JsonObject metadata,List<PropGeometry> props,List<Emote> target) {
        var result=new EmoteParser().parse(r,namespace,source,metadata);
        result.emotes().forEach(e->target.add(e.withProps(props)));
        result.warnings().forEach(w->LoggerFactory.getLogger("quarkos_emotes").warn("{}: {}",source,w));
        result.errors().forEach(e->LoggerFactory.getLogger("quarkos_emotes").error("{}: {}",source,e));
    }
    private static void warn(String source,Exception e) {LoggerFactory.getLogger("quarkos_emotes").warn("Could not load {}",source,e);}
}
