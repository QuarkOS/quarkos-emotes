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
        if(emote.loop()==Emote.Loop.ONCE&&elapsed>=emote.length()) { frozen=emote.length();stopTime=emote.length(); }
    }
    /** Resource reload replaces definitions without replaying already emitted sound events. */
    public EmotePlayer rebind(Emote definition) {
        var next=new EmotePlayer(definition);next.seek(elapsed);return next;
    }
    public Emote emote() { return emote; }
    public void freeze(double seconds) { frozen=seconds; elapsed=Math.max(BLEND_SECONDS,seconds); }
    public void stop() { if(Double.isNaN(stopTime)) { frozen=time(0); stopTime=elapsed; } }
    public boolean stopping() { return !Double.isNaN(stopTime); }
    public void tick() {
        elapsed+=.05;
        if(Double.isNaN(frozen)&&emote.loop()==Emote.Loop.ONCE&&elapsed>=emote.length()) {
            frozen=emote.length(); stopTime=emote.length();
        }
    }
    public State state() {
        if(finished()) return State.DONE;
        if(stopping()) return State.BLENDING_OUT;
        return emote.loop()==Emote.Loop.HOLD && time(0)>=emote.length() ? State.HOLDING : State.PLAYING;
    }
    public boolean finished() { return stopping() && elapsed-stopTime>=BLEND_OUT_SECONDS-1e-9; }
    /** Props disappear immediately on stop; only the player pose blends back to vanilla. */
    public boolean propsVisible(double partialTicks) { return elapsed+partialTicks/20<endTime() && weight(partialTicks)>0; }
    public double time(double partialTicks) { return emote.wrap(Double.isNaN(frozen)?elapsed+partialTicks/20:frozen); }
    public double weight(double partialTicks) {
        double now=elapsed+partialTicks/20,end=endTime();
        // Cancel during entry from the current weight; entry must not keep
        // increasing while the pose is already fading out.
        return Math.min(1,Math.min(now,end)/BLEND_SECONDS)*Math.max(0,Math.min(1,1-(now-end)/BLEND_OUT_SECONDS));
    }
    private double endTime() {
        if(stopping()) return stopTime;
        return Double.isNaN(frozen)&&emote.loop()==Emote.Loop.ONCE ? emote.length() : Double.POSITIVE_INFINITY;
    }
    public Map<String,Emote.Pose> pose(double partialTicks) {
        Map<String,Emote.Pose> pose=new LinkedHashMap<>(); double t=time(partialTicks);
        emote.bones().forEach((name,bone)->pose.put(name,bone.sample(t,emote.easing(),emote.loop()==Emote.Loop.LOOP))); return pose;
    }
    public List<Emote.Sound> sounds() {
        double now=Double.isNaN(frozen)?elapsed:frozen;
        List<Emote.Sound> due=new ArrayList<>();
        if(!stopping() && Double.isNaN(frozen)) {
            for(var sound:emote.sounds()) {
                if(emote.loop()==Emote.Loop.LOOP) {
                    long cycle=(long)Math.floor(now/emote.length());
                    for(long c=Math.max(0,cycle-1);c<=cycle;c++) {
                        double t=c*emote.length()+sound.time(); if(t>previous&&t<=now) due.add(sound);
                    }
                } else if(sound.time()>previous&&sound.time()<=now) due.add(sound);
            }
        }
        previous=now; return due;
    }
}
