package dev.quarkos.emotes.config;

import dev.quarkos.emotes.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
public final class EmoteConfigScreen extends Screen {
    private final Screen parent;
    public EmoteConfigScreen(Screen parent) { super(Component.translatable("screen.quarkos_emotes.config"));this.parent=parent; }
    private Component text(String key,Object value) { return Component.translatable("option.quarkos_emotes."+key).append(": ").append(String.valueOf(value)); }
    private Component toggle(String key,boolean on) { return Component.translatable("option.quarkos_emotes."+key).append(": ").append(Component.translatable(on?"options.on":"options.off")); }
    @Override protected void init() {
        var c=EmoteConfig.get();int x=Math.max(8,width/2-145),w=Math.min(204,width-24),y=38;
        addRenderableWidget(Button.builder(text("first_person",Component.translatable("value.quarkos_emotes."+c.firstPerson.name().toLowerCase(java.util.Locale.ROOT)).getString()),b->{
            c.firstPerson=EmoteConfig.FirstPerson.values()[(c.firstPerson.ordinal()+1)%3];refresh();}).bounds(x,y,w,20).build()); y+=24;
        addRenderableWidget(Button.builder(toggle("cancel_on_move",c.cancelOnMove),b->{c.cancelOnMove=!c.cancelOnMove;refresh();}).bounds(x,y,w,20).build()); y+=24;
        addRenderableWidget(Button.builder(toggle("play_sounds",c.playSounds),b->{c.playSounds=!c.playSounds;refresh();}).bounds(x,y,w,20).build()); y+=24;
        addRenderableWidget(Button.builder(text("sound_volume",Math.round(c.soundVolume*100)+"%"),b->{c.soundVolume=c.soundVolume>=.99f?0:Math.min(1,c.soundVolume+.25f);refresh();}).bounds(x,y,w,20).build());y+=24;
        addRenderableWidget(Button.builder(toggle("show_others",c.showOthers),b->{c.showOthers=!c.showOthers;refresh();}).bounds(x,y,w,20).build());y+=24;
        addRenderableWidget(Button.builder(text("wheel_mode",Component.translatable(c.holdWheel?"value.quarkos_emotes.hold":"value.quarkos_emotes.click").getString()),b->{c.holdWheel=!c.holdWheel;refresh();}).bounds(x,y,w,20).build());y+=24;
        addRenderableWidget(Button.builder(Component.translatable("text.quarkos_emotes.reset_favorites"),b->{c.favorites.clear();EmoteConfig.save();}).bounds(x,y,w,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width/2-45,height-26,90,20).build());
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
        g.fill(0,0,width,height,0xee121b2c);g.drawCenteredString(font,title,width/2,14,0xfff0f5ff);
        int x=width/2+72;
        if(minecraft.player!=null&&width>=340) {
            g.drawCenteredString(font,Component.translatable("text.quarkos_emotes.preview"),x,38,0xffbfd8ed);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,x-42,53,x+42,height-34,Math.max(25,Math.min(65,(height-100)/3)),.0625f,mx,my,minecraft.player);
        }
        super.render(g,mx,my,partial);
    }
    @Override public void onClose() { EmoteConfig.save();EmotesClient.stop();minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
