package dev.quarkos.emotes.config;

import dev.quarkos.emotes.*;
import dev.quarkos.emotes.animation.Emote;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import org.lwjgl.glfw.GLFW;
public final class EmoteWheelScreen extends Screen {
    private EditBox search;
    private int page,hover=-1,cx,cy,radius;
    private boolean favorites;
    private int category;
    private static final List<String> CATEGORIES=List.of("all","greeting","reaction","dance","pose","fun");
    public int harnessHover=-1;
    public EmoteWheelScreen() { super(Component.translatable("screen.quarkos_emotes.wheel")); }
    @Override protected void init() {
        cx=width/2; radius=Math.min(104,Math.max(48,(height-118)/2)); cy=54+(height-118)/2;
        String query=search==null?"":search.getValue();
        int searchWidth=Math.min(160,width-132),left=cx-(searchWidth+100)/2;
        search=new EditBox(font,left,29,searchWidth,18,Component.translatable("text.quarkos_emotes.search"));
        search.setHint(Component.translatable("text.quarkos_emotes.search")); search.setMaxLength(80); search.setValue(query);
        search.setResponder(s->page=0); addRenderableWidget(search);
        addRenderableWidget(Button.builder(categoryText(),b->{category=(category+1)%CATEGORIES.size();page=0;b.setMessage(categoryText());})
                .bounds(left+searchWidth+4,28,96,20).tooltip(Tooltip.create(Component.translatable("tooltip.quarkos_emotes.category"))).build());
        addRenderableWidget(Button.builder(Component.literal("‹"),b->page(-1)).bounds(cx-104,height-27,24,20)
                .tooltip(Tooltip.create(Component.translatable("tooltip.quarkos_emotes.previous_page"))).build());
        addRenderableWidget(Button.builder(favoritesText(),b->{favorites=!favorites;page=0;b.setMessage(favoritesText());})
                .bounds(cx-76,height-27,152,20).tooltip(Tooltip.create(Component.translatable("tooltip.quarkos_emotes.favorites"))).build());
        addRenderableWidget(Button.builder(Component.literal("›"),b->page(1)).bounds(cx+80,height-27,24,20)
                .tooltip(Tooltip.create(Component.translatable("tooltip.quarkos_emotes.next_page"))).build());
        addRenderableWidget(Button.builder(Component.translatable("text.quarkos_emotes.settings"),b->minecraft.setScreen(new EmoteConfigScreen(this)))
                .bounds(width-76,5,70,20).build());
        clearFocus();
    }
    // Start with wheel shortcuts available; clicking or tabbing to search enables typing.
    @Override protected void setInitialFocus() {}
    private Component favoritesText() {
        return favorites?Component.translatable("text.quarkos_emotes.all_emotes"):
                Component.translatable("text.quarkos_emotes.favorites").append(" ("+EmoteConfig.get().favorites.size()+")");
    }
    private Component categoryText() {return Component.translatable("category.quarkos_emotes."+CATEGORIES.get(category));}

    private List<Emote> filtered() {
        String q=search==null?"":search.getValue().toLowerCase(Locale.ROOT);
        var order=List.of("wave","thinking","clap","tableflip","facepalm","groove","bigdance","cheer",
                "yes","no","cry","rage","victory","heart_hands","wave_big","slow_clap",
                "bow","salute","laugh","shrug","point","dab","sit","sit_full",
                "backflip","spin","worm","floss","zombie_walk","sleep","tpose","stretch",
                "jump_hype","fishing_cast","sword_salute","guitar","drums","default_dance","kneel","lay_down","pushups","meditate");
        var list=EmoteResources.REGISTRY.all().stream().sorted(Comparator.<Emote>comparingInt(e->{int i=order.indexOf(e.id().replace("quarkos_emotes:",""));return i<0?Integer.MAX_VALUE:i;}).thenComparing(Emote::id)).toList();
        if(favorites) list=EmoteConfig.get().favorites.stream().map(EmoteResources.REGISTRY::get).filter(Objects::nonNull).toList();
        return list.stream().filter(e->category==0||e.category().equals(CATEGORIES.get(category)))
                .filter(e->e.id().contains(q)||label(e).getString().toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    private List<Emote> slots() {
        var list=filtered(); page=Math.clamp(page,0,Math.max(0,(list.size()-1)/8));
        return list.subList(Math.min(list.size(),page*8),Math.min(list.size(),page*8+8));
    }
    private void page(int delta) { int pages=Math.max(1,(filtered().size()+7)/8); page=Math.floorMod(page+delta,pages); }
    public static Component label(Emote e) {
        if(I18n.exists(e.name())) return Component.translatable(e.name());
        // Unknown resource-pack translation keys still get a readable display name.
        String name=e.name();
        if(name.startsWith("emote.")||name.startsWith("quarkos_emotes.")||name.contains(":")) name=dev.quarkos.emotes.animation.EmoteFiles.title(e.id().substring(e.id().indexOf(':')+1));
        return Component.literal(name);
    }
    private int sector(double x,double y) {
        double dx=x-cx,dy=y-cy,d=Math.hypot(dx,dy);
        if(d<radius*.38||d>radius) return -1;
        return Math.floorMod((int)Math.floor((Math.atan2(dy,dx)+Math.PI/2+Math.PI/8)/(Math.PI/4)),8);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        g.fill(0,0,width,height,0xda151920);
        g.fill(0,height-47,width,height,0xee101318);
        var slots=slots(); hover=harnessHover>=0?harnessHover:sector(mx,my);
        // Horizontal runs form a smooth annulus with eight independently highlighted sectors.
        for(int y=cy-radius;y<=cy+radius;y++) {
            int run=-1,last=-1;
            for(int x=cx-radius;x<=cx+radius+1;x++) {
                int s=x<=cx+radius?sector(x+.5,y+.5):-1;
                double angle=Math.atan2(y+.5-cy,x+.5-cx)+Math.PI/2+Math.PI/8;
                // A slim gap separates targets without reducing their clickable area.
                if(Math.abs(Math.sin(angle*4))<.055) s=-1;
                if(s!=last) {
                    if(last>=0) {
                        int color=last>=slots.size()?0xff242830:last==hover?0xff476a83:last%2==0?0xff343e4b:0xff303946;
                        g.fill(run,y,x,y+1,color);
                    }
                    run=x;last=s;
                }
            }
        }
        int inner=(int)(radius*.36);
        for(int dy=-inner;dy<=inner;dy++) {
            int half=(int)Math.sqrt(inner*inner-dy*dy);
            g.fill(cx-half,cy+dy,cx+half+1,cy+dy+1,0xff1c222b);
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
        g.drawString(font,title,10,11,0xffeff5ff,false);
        g.drawCenteredString(font,Component.translatable("text.quarkos_emotes.page"),cx,cy-13,0xffaab8c8);
        g.drawCenteredString(font,Component.literal((page+1)+" / "+Math.max(1,(filtered().size()+7)/8)),cx,cy+2,0xffeff5ff);
        if(hover>=0&&hover<slots.size()) {
            Emote e=slots.get(hover); g.drawCenteredString(font,Component.literal(font.plainSubstrByWidth(label(e).getString(),width-16)),cx,cy+radius+6,0xffffffff);
            EmotesClient.selected=e.id();
        } else if(slots.isEmpty()) g.drawCenteredString(font,Component.translatable("text.quarkos_emotes.empty"),cx,cy+radius+6,0xffced5df);
        else g.drawCenteredString(font,Component.translatable("text.quarkos_emotes.choose"),cx,cy+radius+6,0xffcbd5e1);
        Component help=Component.translatable("text.quarkos_emotes.wheel_help");
        g.drawCenteredString(font,help,cx,height-41,0xffc9d3df);
        super.render(g,mx,my,partial);
    }
    private void select(int slot) { var list=slots(); if(slot>=0&&slot<list.size()) { minecraft.setScreen(null);EmotesClient.play(list.get(slot).id()); } else onClose(); }
    private void favorite() {
        var list=slots();
        if(hover>=0&&hover<list.size()) { EmoteConfig.get().favorite(list.get(hover).id()); rebuildWidgets(); }
    }
    private boolean click(double x,double y,int button) {
        int s=sector(x,y); if(s<0) { search.setFocused(false);setFocused(null);return false; }
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
