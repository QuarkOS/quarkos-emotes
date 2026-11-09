package dev.quarkos.emotes.config;

import dev.quarkos.emotes.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class EmoteConfigScreen extends Screen {
    private final Screen parent;
    private int left, top, optionWidth, previewLeft, right, bottom, rowHeight;
    public EmoteConfigScreen(Screen parent) { super(Component.translatable("screen.quarkos_emotes.config"));this.parent=parent; }
    private Component text(String key,Object value) { return Component.translatable("option.quarkos_emotes."+key).append(": ").append(String.valueOf(value)); }
    private Component toggle(String key,boolean on) { return text(key,Component.translatable(on?"options.on":"options.off").getString()); }
    private void option(String key,Component message,int row,Runnable action) {
        addRenderableWidget(Button.builder(message,b->{action.run();refresh();})
                .bounds(left,top+row*rowHeight,optionWidth,20)
                .tooltip(Tooltip.create(Component.translatable("tooltip.quarkos_emotes."+key))).build());
    }
    @Override protected void init() {
        var c=EmoteConfig.get();
        int contentWidth=Math.min(580,width-20);
        left=(width-contentWidth)/2; right=left+contentWidth; top=36; bottom=height-36;
        optionWidth=Math.min(260,(contentWidth-12)*2/3);previewLeft=left+optionWidth+12;
        rowHeight=Math.min(26,(bottom-top)/7);
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
        g.fill(0,0,width,height,0xf0151920);
        g.drawCenteredString(font,title,width/2,13,0xffffffff);
        g.fill(previewLeft-1,top-1,right+1,bottom+1,0xff626975);
        g.fill(previewLeft,top,right,bottom,0xff222831);
        int center=(previewLeft+right)/2;
        g.drawCenteredString(font,Component.translatable("text.quarkos_emotes.preview"),center,top+8,0xffd4dbe5);
        if(minecraft.player!=null) {
            int scale=Math.max(12,Math.min(70,Math.min((bottom-top-40)/3,(right-previewLeft-12)/2)));
            // Confine both the entity and its mouse-follow rotation to the preview panel.
            g.enableScissor(previewLeft+2,top+22,right-2,bottom-18);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,previewLeft+4,top+24,right-4,bottom-20,scale,.0625f,
                    Math.clamp(mx,previewLeft,right),Math.clamp(my,top+24,bottom-20),minecraft.player);
            g.disableScissor();
            var e=EmoteResources.REGISTRY.get(EmotesClient.selected);
            if(e!=null) g.drawCenteredString(font,Component.literal(font.plainSubstrByWidth(EmoteWheelScreen.label(e).getString(),right-previewLeft-8)),center,bottom-12,0xffcbd5e1);
        }
        super.render(g,mx,my,partial);
    }
    @Override public void onClose() { EmoteConfig.save();EmotesClient.stop();minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
