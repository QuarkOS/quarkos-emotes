package dev.quarkos.emotes.animation;

import java.util.*;
/** One instance per entity; time advances at 20 Hz and render sampling adds partial ticks. */
public final class EmotePlayer {
    public static final double BLEND_SECONDS=.15;
    private final Emote emote;
    private double elapsed, stopTime=Double.NaN, frozen=Double.NaN, previous=-1e-9;
    public EmotePlayer(Emote emote) { this.emote=emote; }
    public Emote emote() { return emote; }
    public void freeze(double seconds) { frozen=seconds; elapsed=Math.max(BLEND_SECONDS,seconds); }
    public void stop() { if(Double.isNaN(stopTime)) stopTime=elapsed; }
    public boolean stopping() { return !Double.isNaN(stopTime); }
    public void tick() {
        elapsed+=.05;
        if(Double.isNaN(frozen)&&emote.loop()==Emote.Loop.ONCE&&elapsed>=emote.length()) stop();
    }
    public boolean finished() { return stopping() && elapsed-stopTime>=BLEND_SECONDS; }
    public double time(double partialTicks) { return emote.wrap(Double.isNaN(frozen)?elapsed+partialTicks/20:frozen); }
    public double weight(double partialTicks) {
        double now=elapsed+partialTicks/20;
        return Math.min(1,now/BLEND_SECONDS)*(stopping()?Math.max(0,1-(now-stopTime)/BLEND_SECONDS):1);
    }
    public Map<String,Emote.Pose> pose(double partialTicks) {
        Map<String,Emote.Pose> pose=new LinkedHashMap<>(); double t=time(partialTicks);
        emote.bones().forEach((name,bone)->pose.put(name,bone.sample(t,emote.easing()))); return pose;
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
