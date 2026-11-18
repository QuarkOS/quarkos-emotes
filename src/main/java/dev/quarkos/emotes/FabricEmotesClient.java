package dev.quarkos.emotes;
//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
*///?} else {
import net.fabricmc.fabric.api.resource.*;
//?}
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
//? if <26.1 {
import net.minecraft.server.packs.resources.ResourceManager;
//?}
import dev.quarkos.emotes.network.FabricNetwork;
public final class FabricEmotesClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(EmotesClient.OPEN);
        for(var key:EmotesClient.FAVORITES) KeyBindingHelper.registerKeyBinding(key);
        for(var key:EmotesClient.QUICK_SLOTS) KeyBindingHelper.registerKeyBinding(key);
        //? if >=26.1 {
        /*ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(EmotesClient.MOD_ID, "emotes"), new EmoteResources());
        *///?} else {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId() { return Identifier.fromNamespaceAndPath("quarkos_emotes","emotes"); }
            @Override public void onResourceManagerReload(ResourceManager manager) { new EmoteResources().onResourceManagerReload(manager); }
        });
        //?}
        ClientPlayNetworking.registerGlobalReceiver(FabricNetwork.Broadcast.TYPE,(packet,context)->context.client().execute(()->EmotesClient.receive(packet.player(),packet.id(),packet.elapsed())));
        EmotesClient.capability(()->ClientPlayNetworking.canSend(FabricNetwork.Request.TYPE));
        EmotesClient.sender(id->{
            if(!ClientPlayNetworking.canSend(FabricNetwork.Request.TYPE)) return false;
            ClientPlayNetworking.send(new FabricNetwork.Request(id)); return true;
        });
    }
}
//?}
