package dev.quarkos.emotes.network;

import net.minecraft.server.MinecraftServer;
/** Keeps development proof code and all client classes out of dedicated release server loading. */
public final class ServerProofHooks {
    @FunctionalInterface public interface Support {void tick(MinecraftServer server);}
    private static Support support;
    public static void tick(MinecraftServer server) {
        if(!Boolean.getBoolean("quarkos_emotes.serverHarness"))return;
        if(support==null)try {
            support=(Support)Class.forName("dev.quarkos.emotes.harness.ServerHarness").getConstructor().newInstance();
        } catch(ReflectiveOperationException e) {throw new IllegalStateException("Proof support requires a development run classpath",e);}
        support.tick(server);
    }
}
