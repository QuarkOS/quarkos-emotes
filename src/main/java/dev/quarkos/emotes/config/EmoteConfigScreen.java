package dev.quarkos.emotes.config;

import dev.quarkos.emotes.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class EmoteConfigScreen extends Screen {
    private final Screen parent;
    private final Map<Button,String> tips=new HashMap<>();
    private ConfigLayout.Geometry geo=ConfigLayout.geometry(320,240);
    public EmoteConfigScreen(Screen parent) { super(Component.translatable("screen.quarkos_emotes.config"));this.parent=parent; }
    private Component text(String key,Object value) { return Component.translatable("option.quarkos_emotes."+key).append(": ").append(String.valueOf(value)); }
    private Component toggle(String key,boolean on) { return text(key,Component.translatable(on?"options.on":"options.off").getString()); }
    private void option(String key,Component message,int row,Runnable action) {
        var button=Button.builder(message,b->{action.run();refresh();})
                .bounds(geo.left(),geo.top()+row*geo.rowHeight(),geo.optionWidth(),20).build();
        tips.put(button,key);
        addRenderableWidget(button);
    }
    @Override protected void init() {
        tips.clear();
        geo=ConfigLayout.geometry(width,height);
        var c=EmoteConfig.get();
        option("first_person",text("first_person",Component.translatable("value.quarkos_emotes."+c.firstPerson.name().toLowerCase(java.util.Locale.ROOT)).getString()),0,
                ()->c.firstPerson=EmoteConfig.FirstPerson.values()[(c.firstPerson.ordinal()+1)%3]);
        option("cancel_on_move",toggle("cancel_on_move",c.cancelOnMove),1,()->c.cancelOnMove=!c.cancelOnMove);
        option("play_sounds",toggle("play_sounds",c.playSounds),2,()->c.playSounds=!c.playSounds);
        option("sound_volume",text("sound_volume",Math.round(c.soundVolume*100)+"%"),3,()->c.soundVolume=c.soundVolume>=.99f?0:Math.min(1,c.soundVolume+.25f));
        option("show_others",toggle("show_others",c.showOthers),4,()->c.showOthers=!c.showOthers);
        option("wheel_mode",text("wheel_mode",Component.translatable(c.holdWheel?"value.quarkos_emotes.hold":"value.quarkos_emotes.click").getString()),5,()->c.holdWheel=!c.holdWheel);
        option("reset_favorites",Component.translatable("text.quarkos_emotes.reset_favorites"),6,()->c.favorites.clear());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width/2-60,height-27,120,20).build());
        if(minecraft.player!=null) {
            if(EmoteResources.REGISTRY.get(EmotesClient.selected)==null&&!EmoteResources.REGISTRY.all().isEmpty()) EmotesClient.selected=EmoteResources.REGISTRY.all().getFirst().id();
            EmotesClient.preview(EmotesClient.selected);
        }
    }
    private void refresh() { EmoteConfig.save();rebuildWidgets(); }
    @Override public void tick() {
        if(minecraft.player!=null&&EmotesClient.player(minecraft.player.getUUID())==null) EmotesClient.preview(EmotesClient.selected);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        geo=ConfigLayout.geometry(width,height);
        g.fill(0,0,width,height,0xf0151920);
        g.drawCenteredString(font,title,width/2,13,0xffffffff);
        var preview=geo.preview();
        g.fill(preview.x()-1,preview.y()-1,preview.right()+1,preview.bottom()+1,0xff626975);
        g.fill(preview.x(),preview.y(),preview.right(),preview.bottom(),0xff222831);
        int center=preview.x()+preview.w()/2;
        g.drawCenteredString(font,Component.translatable("text.quarkos_emotes.preview"),center,preview.y()+8,0xffd4dbe5);
        if(minecraft.player!=null) {
            var fit=ConfigLayout.preview(EmotesClient.selected,preview.w(),preview.h());
            EmotesClient.previewOrbit=fit.yaw();
            // Confine both the entity and its mouse-follow rotation to the preview panel.
            g.enableScissor(preview.x()+2,preview.y()+22,preview.right()-2,preview.bottom()-18);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,preview.x()+4,preview.y()+24,preview.right()-4,preview.bottom()-20,fit.scale(),fit.yOffset(),
                    Math.clamp(mx,preview.x(),preview.right()),Math.clamp(my,preview.y()+24,preview.bottom()-20),minecraft.player);
            g.disableScissor();
            var e=EmoteResources.REGISTRY.get(EmotesClient.selected);
            if(e!=null) g.drawCenteredString(font,Component.literal(font.plainSubstrByWidth(EmoteWheelScreen.label(e).getString(),preview.w()-8)),center,preview.bottom()-12,0xffcbd5e1);
        }
        super.render(g,mx,my,partial);
        drawTooltip(g,mx,my);
    }
    private void drawTooltip(GuiGraphics g,int mx,int my) {
        Button hovered=null;
        for(var child:children()) if(child instanceof Button button&&tips.containsKey(button)&&button.isMouseOver(mx,my)) hovered=button;
        if(hovered==null) return;
        int column=Math.max(80,geo.optionWidth()-8);
        var lines=font.split(Component.translatable("tooltip.quarkos_emotes."+tips.get(hovered)),column);
        int textW=0; for(var line:lines) textW=Math.max(textW,font.width(line));
        int tipW=textW+8, tipH=lines.size()*font.lineHeight+8;
        var box=ConfigLayout.placeTooltip(width,height,new ConfigLayout.Box(hovered.getX(),hovered.getY(),hovered.getWidth(),hovered.getHeight()),geo.preview(),tipW,tipH);
        g.fill(box.x(),box.y(),box.right(),box.bottom(),0xf010141c);
        g.fill(box.x(),box.y(),box.right(),box.y()+1,0xff8ea2b5);
        int y=box.y()+4;
        for(var line:lines) { g.drawString(font,line,box.x()+4,y,0xfff2f6fb); y+=font.lineHeight; }
    }
    @Override public void onClose() { EmotesClient.previewOrbit=0;EmoteConfig.save();EmotesClient.stop();minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
