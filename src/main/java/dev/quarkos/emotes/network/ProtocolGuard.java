package dev.quarkos.emotes.network;
import java.util.*;
/** Server accepts only bounded ids; no animation data or paths ever cross the wire. */
public final class ProtocolGuard {
    private static final class Timing { Long play, stop; }
    private final Map<Object,Timing> last=new WeakHashMap<>();
    public synchronized void remove(Object player) { last.remove(player); }
    public synchronized boolean accept(Object player,String id,long now) {
        if(id==null||id.length()>128||(!id.isEmpty()&&!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))) return false;
        Timing timing=last.computeIfAbsent(player,key->new Timing());
        Long previous=id.isEmpty()?timing.stop:timing.play;
        long interval=id.isEmpty()?50_000_000L:200_000_000L;
        if(previous!=null&&now-previous<interval) return false;
        // A stop immediately after play must be accepted; separate clocks still bound stop spam.
        if(id.isEmpty()) timing.stop=now;else timing.play=now;
        return true;
    }
}
