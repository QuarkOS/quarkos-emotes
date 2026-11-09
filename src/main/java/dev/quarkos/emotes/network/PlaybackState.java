package dev.quarkos.emotes.network;

import java.util.*;
/** Server-only protocol state. Stores identifiers and monotonic start times, never assets. */
public final class PlaybackState {
    public static final int MAX_ELAPSED_MS=7*24*60*60*1000;
    public record Playing(String id,long started) {
        public int elapsed(long now) { return (int)Math.min(MAX_ELAPSED_MS,Math.max(0,(now-started)/1_000_000)); }
    }
    private final Map<UUID,Playing> playing=new HashMap<>();
    public void update(UUID player,String id,long now) {
        if(id.isEmpty()) playing.remove(player);else playing.put(player,new Playing(id,now));
    }
    public Playing get(UUID player) { return playing.get(player); }
    public void remove(UUID player) { playing.remove(player); }
    public static boolean validElapsed(int ms) { return ms>=0&&ms<=MAX_ELAPSED_MS; }
}
