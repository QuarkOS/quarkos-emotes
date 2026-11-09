package dev.quarkos.emotes;
//? if forge {
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.PacketDistributor;
import dev.quarkos.emotes.network.ForgeNetwork;
import dev.quarkos.emotes.config.EmoteConfigScreen;
public final class ForgeEmotesClient {
    public static void init() {
        //? if >=1.21.11 {
        RegisterKeyMappingsEvent.BUS.addListener(ForgeEmotesClient::keys);
        RegisterClientReloadListenersEvent.BUS.addListener(ForgeEmotesClient::reload);
        //?} else {
        /*var bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(ForgeEmotesClient::keys); bus.addListener(ForgeEmotesClient::reload);
        *///?}
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,()->new ConfigScreenHandler.ConfigScreenFactory(EmoteConfigScreen::new));
        ForgeNetwork.receiver=EmotesClient::receive;
        EmotesClient.capability(()-> {
            var connection=Minecraft.getInstance().getConnection();
            return connection!=null&&ForgeNetwork.CHANNEL!=null&&ForgeNetwork.CHANNEL.isRemotePresent(connection.getConnection());
        });
        EmotesClient.sender(id->{
            var connection=Minecraft.getInstance().getConnection();
            if(connection==null||ForgeNetwork.CHANNEL==null||!ForgeNetwork.CHANNEL.isRemotePresent(connection.getConnection())) return false;
            ForgeNetwork.CHANNEL.send(new ForgeNetwork.Request(id),PacketDistributor.SERVER.noArg()); return true;
        });
    }
    private static void keys(RegisterKeyMappingsEvent event) { event.register(EmotesClient.OPEN); for(var key:EmotesClient.FAVORITES) event.register(key); }
    private static void reload(RegisterClientReloadListenersEvent event) { event.registerReloadListener(new EmoteResources()); }
}
//?}
