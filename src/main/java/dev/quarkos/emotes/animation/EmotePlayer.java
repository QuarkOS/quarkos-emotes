package dev.quarkos.emotes.animation;

import java.util.*;
/** One instance per entity; time advances at 20 Hz and render sampling adds partial ticks. */
public final class EmotePlayer {
    public static final double BLEND_SECONDS=.15;
    public static final double BLEND_OUT_SECONDS=.2;
    public enum State { PLAYING, HOLDING, BLENDING_OUT, DONE }
    private final Emote emote;
    private double elapsed, stopTime=Double.NaN, frozen=Double.NaN, previous=-1e-9;
    public EmotePlayer(Emote emote) { this.emote=emote; }
    /** Resume tracking without replaying old sounds or freezing the animation. */
    public void seek(double seconds) {
        if(!Double.isFinite(seconds)||seconds<0)throw new IllegalArgumentException("Invalid elapsed time");
        elapsed=seconds;previous=seconds;frozen=Double.NaN;stopTime=Double.NaN;
        if(elapsed>=emote.totalLength()) { frozen=emote.totalLength();stopTime=emote.totalLength(); }
    }
    /** Resource reload replaces definitions without replaying already emitted sound events. */
    public EmotePlayer rebind(Emote definition) {
        var next=new EmotePlayer(definition);next.seek(elapsed);return next;
    }
    public Emote emote() { return emote; }
    public void freeze(double seconds) { frozen=seconds; elapsed=Math.max(BLEND_SECONDS,seconds); }
    public void stop() { if(Double.isNaN(stopTime)) { frozen=timeline(0); stopTime=elapsed; } }
    public boolean stopping() { return !Double.isNaN(stopTime); }
    public void tick() {
        elapsed+=.05;
        if(Double.isNaN(frozen)&&elapsed>=emote.totalLength()) {
            frozen=emote.totalLength(); stopTime=emote.totalLength();
        }
    }
    public State state() {
        if(finished()) return State.DONE;
        if(stopping()) return State.BLENDING_OUT;
        return segment(0).animation().loop()==Emote.Loop.HOLD && time(0)>=emote.length() ? State.HOLDING : State.PLAYING;
    }
    public boolean finished() { return stopping() && elapsed-stopTime>=BLEND_OUT_SECONDS-1e-9; }
    /** Props disappear immediately on stop; only the player pose blends back to vanilla. */
    public boolean propsVisible(double partialTicks) { return elapsed+partialTicks/20<endTime() && weight(partialTicks)>0; }
    private double timeline(double partialTicks) { return Double.isNaN(frozen)?elapsed+partialTicks/20:frozen; }
    public Emote.Segment segment(double partialTicks) { return emote.segment(timeline(partialTicks)); }
    public double time(double partialTicks) { return segment(partialTicks).time(); }
    public double weight(double partialTicks) {
        double now=elapsed+partialTicks/20,end=endTime();
        // Cancel during entry from the current weight; entry must not keep
        // increasing while the pose is already fading out.
        return Math.min(1,Math.min(now,end)/BLEND_SECONDS)*Math.max(0,Math.min(1,1-(now-end)/BLEND_OUT_SECONDS));
    }
    private double endTime() {
        if(stopping()) return stopTime;
        return Double.isNaN(frozen) ? emote.totalLength() : Double.POSITIVE_INFINITY;
    }
    public Map<String,Emote.Pose> pose(double partialTicks) {
        Map<String,Emote.Pose> pose=new LinkedHashMap<>(); var segment=segment(partialTicks);var clip=segment.animation();
        clip.bones().forEach((name,bone)->pose.put(name,bone.sample(segment.time(),clip.easing(),clip.loop()==Emote.Loop.LOOP))); return pose;
    }
    public List<Emote.Sound> sounds() {
        double now=Double.isNaN(frozen)?elapsed:frozen;
        List<Emote.Sound> due=new ArrayList<>();
        if(!stopping() && Double.isNaN(frozen)) {
            if(emote.enter()!=null) collectSounds(emote.enter(),0,false,now,due);
            collectSounds(emote,emote.entryLength(),emote.loop()==Emote.Loop.LOOP,now,due);
            if(emote.followLoop()!=null) collectSounds(emote.followLoop(),emote.entryLength()+emote.length(),true,now,due);
        }
        previous=now; return due;
    }
    private void collectSounds(Emote clip,double start,boolean repeat,double now,List<Emote.Sound> due) {
        // Tick sums may land a few ULPs below an authored timestamp. Use the
        // same tolerance for both bounds so a boundary cue fires once on time.
        double limit=now+1e-9,after=previous<0?previous:previous+1e-9;
        if(limit<start)return;
        for(var sound:clip.sounds()) {
            long first=repeat?Math.max(0,(long)Math.floor((after-start-sound.time())/clip.length())+1):0;
            long last=repeat?(long)Math.floor((limit-start-sound.time())/clip.length()):0;
            for(long c=first;c<=last;c++) {
                double at=start+c*clip.length()+sound.time();
                if(at>after && at<=limit)due.add(sound);
            }
        }
    }

}
