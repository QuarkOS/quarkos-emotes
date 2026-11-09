package dev.quarkos.emotes;
//? if fabric {
import net.fabricmc.api.ModInitializer;
import dev.quarkos.emotes.network.FabricNetwork;
public final class FabricEmotes implements ModInitializer {
    @Override public void onInitialize() { FabricNetwork.init(); }
}
//?}
