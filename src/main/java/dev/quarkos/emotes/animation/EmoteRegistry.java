package dev.quarkos.emotes.animation;

import java.util.*;
/** Input order is pack order: the last definition of an id wins. */
public final class EmoteRegistry {
    private volatile Map<String,Emote> entries=Map.of();
    public void replace(Iterable<Emote> definitions) {
        Map<String,Emote> next=new LinkedHashMap<>();
        definitions.forEach(e -> next.put(e.id(),e));
        entries=Collections.unmodifiableMap(next);
    }
    public Emote get(String id) { return entries.get(id); }
    public List<Emote> all() { return entries.values().stream().sorted(Comparator.comparing(Emote::id)).toList(); }
}
