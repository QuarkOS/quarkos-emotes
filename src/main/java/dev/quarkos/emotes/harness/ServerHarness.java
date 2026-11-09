package dev.quarkos.emotes.harness;

import net.minecraft.server.MinecraftServer;
import java.util.*;
/** Only enabled in dedicated proof runs. Places fresh connections without requiring operators. */
public final class ServerHarness implements dev.quarkos.emotes.network.ServerProofHooks.Support {
    private static final Set<Object> PLACED=Collections.newSetFromMap(new WeakHashMap<>());
    public void tick(MinecraftServer server) {
        if(!Boolean.getBoolean("quarkos_emotes.serverHarness"))return;
        for(var player:server.getPlayerList().getPlayers())if(PLACED.add(player)) {
            boolean viewer=player.getName().getString().equals("B");
            String name=player.getName().getString();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+name+" 0.5 -60 "+(viewer?"3.5 180":"0.5 0")+" 0");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode creative "+name);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set noon");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"weather clear");
        }
    }
}
