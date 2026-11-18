package dev.quarkos.emotes.harness;

import net.minecraft.server.MinecraftServer;
import java.util.*;
/** Only enabled in dedicated proof runs. Places fresh connections without requiring operators. */
public final class ServerHarness implements dev.quarkos.emotes.network.ServerProofHooks.Support {
    /** Names, not player objects. Respawn and dimension change replace the object and must not yank the player home. */
    private static final Set<String> PLACED=new HashSet<>();
    private static boolean rules;
    public void tick(MinecraftServer server) {
        if(!Boolean.getBoolean("quarkos_emotes.serverHarness"))return;
        var source=server.createCommandSourceStack();
        if(!rules) {
            rules=true;
            // Death during the lifecycle proof must come back without a confirm screen.
            // 1.21.11 renamed these rules to snake_case.
            //? if >=1.21.11 {
            server.getCommands().performPrefixedCommand(source,"gamerule immediate_respawn true");
            server.getCommands().performPrefixedCommand(source,"gamerule keep_inventory true");
            //?} else {
            /*server.getCommands().performPrefixedCommand(source,"gamerule doImmediateRespawn true");
            server.getCommands().performPrefixedCommand(source,"gamerule keepInventory true");
            *///?}
            if(Boolean.getBoolean("quarkos_emotes.cleanNames")) {
                server.getCommands().performPrefixedCommand(source,"team add qe");
                server.getCommands().performPrefixedCommand(source,"team modify qe nametagVisibility never");
            }
        }
        for(var player:server.getPlayerList().getPlayers())if(PLACED.add(player.getName().getString())) {
            boolean viewer=player.getName().getString().equals("B");
            String name=player.getName().getString();
            server.getCommands().performPrefixedCommand(source,"tp "+name+" 0.5 -60 "+(viewer?"3.5 180":"0.5 0")+" 0");
            server.getCommands().performPrefixedCommand(source,"gamemode creative "+name);
            server.getCommands().performPrefixedCommand(source,"op "+name);
            if(Boolean.getBoolean("quarkos_emotes.cleanNames")) server.getCommands().performPrefixedCommand(source,"team join qe "+name);
            server.getCommands().performPrefixedCommand(source,"time set noon");
            server.getCommands().performPrefixedCommand(source,"weather clear");
        }
    }
}
