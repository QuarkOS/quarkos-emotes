package dev.quarkos.emotes;

import dev.quarkos.emotes.animation.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.slf4j.LoggerFactory;
public final class EmoteResources implements ResourceManagerReloadListener {
    public static final EmoteRegistry REGISTRY=new EmoteRegistry();
    @Override public void onResourceManagerReload(ResourceManager manager) {
        List<Emote> emotes=new ArrayList<>();
        List<String> packOrder=manager.listPacks().map(p->p.packId()).toList();
        record Source(String name,String namespace,Resource resource) {}
        List<Source> sources=new ArrayList<>();
        manager.listResourceStacks("emotes",id->id.getPath().endsWith(".json")).forEach((id,stack)-> {
            stack.forEach(resource->sources.add(new Source(id.toString(),id.getNamespace(),resource)));
        });
        sources.sort(Comparator.<Source>comparingInt(s->packOrder.indexOf(s.resource().sourcePackId())).thenComparing(Source::name));
        for(var s:sources) try(Reader reader=s.resource().openAsReader()) { read(reader,s.namespace(),s.name(),emotes); }
        catch(Exception e) { LoggerFactory.getLogger("quarkos_emotes").warn("Could not load {}",s.name(),e); }
        Path custom=Minecraft.getInstance().gameDirectory.toPath().resolve("config/quarkos_emotes/emotes");
        try { Files.createDirectories(custom); try(var files=Files.list(custom)) {
            for(Path p:files.filter(p->p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                try(Reader r=Files.newBufferedReader(p)) { read(r,"custom",p.toString(),emotes); }
                catch(Exception e) { LoggerFactory.getLogger("quarkos_emotes").warn("Could not load {}",p,e); }
            }
        }} catch(IOException e) { LoggerFactory.getLogger("quarkos_emotes").warn("Could not scan custom emotes",e); }
        REGISTRY.replace(emotes);
        dev.quarkos.emotes.render.PropAttachment.invalidate();
        LoggerFactory.getLogger("quarkos_emotes").info("Loaded {} emotes",REGISTRY.all().size());
    }
    private static void read(Reader r,String namespace,String source,List<Emote> target) {
        var result=new EmoteParser().parse(r,namespace); target.addAll(result.emotes());
        result.warnings().forEach(w->LoggerFactory.getLogger("quarkos_emotes").warn("{}: {}",source,w));
        result.errors().forEach(e->LoggerFactory.getLogger("quarkos_emotes").error("{}: {}",source,e));
    }
}
