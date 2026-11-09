package dev.quarkos.emotes.config;

import dev.quarkos.emotes.*;
import dev.quarkos.emotes.animation.Emote;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import org.lwjgl.glfw.GLFW;
public final class EmoteWheelScreen extends Screen {
    private EditBox search;
    private int page,hover=-1,cx,cy,radius;
    private boolean favorites;
    public int harnessHover=-1;
    public EmoteWheelScreen() { super(Component.translatable("screen.quarkos_emotes.wheel")); }
    @Override protected void init() {
        cx=width/2; cy=height/2+3; radius=Math.min(100,Math.max(46,(height-120)/2));
        String query=search==null?"":search.getValue();
        search=new EditBox(font,cx-100,27,200,18,Component.translatable("text.quarkos_emotes.search"));
        search.setHint(Component.translatable("text.quarkos_emotes.search")); search.setMaxLength(80); search.setValue(query);
        search.setResponder(s->page=0); addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("‹"),b->page(-1)).bounds(cx-87,height-27,24,20).build());
        addRenderableWidget(Button.builder(Component.translatable("text.quarkos_emotes.favorites"),b->{favorites=!favorites;page=0;})
                .bounds(cx-59,height-27,118,20).build());
        addRenderableWidget(Button.builder(Component.literal("›"),b->page(1)).bounds(cx+63,height-27,24,20).build());
        addRenderableWidget(Button.builder(Component.translatable("text.quarkos_emotes.settings"),b->minecraft.setScreen(new EmoteConfigScreen(this)))
                .bounds(width-76,5,70,20).build());
    }
    private List<Emote> filtered() {
        String q=search==null?"":search.getValue().toLowerCase(Locale.ROOT);
        var list=EmoteResources.REGISTRY.all();
        if(favorites) list=EmoteConfig.get().favorites.stream().map(EmoteResources.REGISTRY::get).filter(Objects::nonNull).toList();
        return list.stream().filter(e->e.id().contains(q)||label(e).getString().toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    private List<Emote> slots() {
        var list=filtered(); page=Math.clamp(page,0,Math.max(0,(list.size()-1)/8));
        return list.subList(Math.min(list.size(),page*8),Math.min(list.size(),page*8+8));
    }
    private void page(int delta) { int pages=Math.max(1,(filtered().size()+7)/8); page=Math.floorMod(page+delta,pages); }
    public static Component label(Emote e) { return Component.translatable(e.name()); }
    private int sector(double x,double y) {
        double dx=x-cx,dy=y-cy,d=Math.hypot(dx,dy);
        if(d<radius*.38||d>radius) return -1;
        return Math.floorMod((int)Math.floor((Math.atan2(dy,dx)+Math.PI/2+Math.PI/8)/(Math.PI/4)),8);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        g.fill(0,0,width,height,0xb3101524);
        var slots=slots(); hover=harnessHover>=0?harnessHover:sector(mx,my);
        // Horizontal runs form a smooth annulus with eight independently highlighted sectors.
        for(int y=cy-radius;y<=cy+radius;y++) {
            int run=-1,last=-1;
            for(int x=cx-radius;x<=cx+radius+1;x++) {
                int s=x<=cx+radius?sector(x+.5,y+.5):-1;
                if(s!=last) {
                    if(last>=0) g.fill(run,y,x,y+1,last==hover?0xe75ca7cb:last<slots.size()?(last%2==0?0xd62a3853:0xd631415e):0x90202a3d);
                    run=x;last=s;
                }
            }
        }
        for(int i=0;i<slots.size();i++) {
            double angle=-Math.PI/2+i*Math.PI/4;
            int x=cx+(int)(Math.cos(angle)*radius*.70), y=cy+(int)(Math.sin(angle)*radius*.70);
            var e=slots.get(i); Identifier id=Identifier.tryParse(e.icon());
            if(id!=null) {
                //? if >=1.21.11 {
                var item=BuiltInRegistries.ITEM.getValue(id);
                //?} else {
                /*var item=BuiltInRegistries.ITEM.get(id);
                *///?}
                if(item!=null) g.renderItem(new ItemStack(item),x-8,y-10);
            }
            boolean starred=EmoteConfig.get().favorites.contains(e.id());
            g.drawCenteredString(font,Component.literal((i+1)+(starred?" ★":"")),x,y+8,0xffeff5ff);
        }
        g.drawCenteredString(font,title,cx,10,0xffeff5ff);
        g.drawCenteredString(font,Component.literal((page+1)+" / "+Math.max(1,(filtered().size()+7)/8)),cx,cy-4,0xffc7d5e9);
        if(hover>=0&&hover<slots.size()) {
            Emote e=slots.get(hover); g.drawCenteredString(font,Component.literal(font.plainSubstrByWidth(label(e).getString(),width-16)),cx,cy+radius+5,0xffffffff);
            EmotesClient.selected=e.id();
        } else if(slots.isEmpty()) g.drawCenteredString(font,Component.translatable("text.quarkos_emotes.empty"),cx,cy+radius+5,0xffced5df);
        g.drawString(font,Component.translatable("text.quarkos_emotes.wheel_help"),7,height-40,0xffb5c4dc,false);
        super.render(g,mx,my,partial);
    }
    private void select(int slot) { var list=slots(); if(slot>=0&&slot<list.size()) { minecraft.setScreen(null);EmotesClient.play(list.get(slot).id()); } else onClose(); }
    private void favorite() { var list=slots(); if(hover>=0&&hover<list.size()) EmoteConfig.get().favorite(list.get(hover).id()); }
    private boolean click(double x,double y,int button) {
        int s=sector(x,y); if(s<0) return false;
        hover=s; if(button==1) favorite();else if(button==0) select(s); return true;
    }
    private boolean key(int key) {
        if(search.isFocused()) return false;
        if(key>=GLFW.GLFW_KEY_1&&key<=GLFW.GLFW_KEY_8) { select(key-GLFW.GLFW_KEY_1);return true; }
        if(key==GLFW.GLFW_KEY_F) { favorite(); return true; }
        if(key==GLFW.GLFW_KEY_RIGHT||key==GLFW.GLFW_KEY_PAGE_DOWN) { page(1);return true; }
        if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_PAGE_UP) { page(-1);return true; }
        return false;
    }
    //? if >=1.21.11 {
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e,boolean twice) {
        return super.mouseClicked(e,twice)||click(e.x(),e.y(),e.button());
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
        if(!EmoteConfig.get().holdWheel&&EmotesClient.OPEN.matches(e)) { onClose();return true; }
        return key(e.key())||super.keyPressed(e);
    }
    @Override public boolean keyReleased(net.minecraft.client.input.KeyEvent e) {
        if(EmoteConfig.get().holdWheel&&EmotesClient.OPEN.matches(e)) { if(!search.isFocused())select(hover);return true; }
        return super.keyReleased(e);
    }
    //?} else {
    /*@Override public boolean mouseClicked(double x,double y,int button) { return super.mouseClicked(x,y,button)||click(x,y,button); }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if(!EmoteConfig.get().holdWheel&&EmotesClient.OPEN.matches(key,scan)) { onClose();return true; }
        return key(key)||super.keyPressed(key,scan,mods);
    }
    @Override public boolean keyReleased(int key,int scan,int mods) {
        if(EmoteConfig.get().holdWheel&&EmotesClient.OPEN.matches(key,scan)) { if(!search.isFocused())select(hover);return true; }
        return super.keyReleased(key,scan,mods);
    }
    *///?}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy) { page(sy>0?-1:1);return true; }
    @Override public boolean isPauseScreen() { return false; }
}
