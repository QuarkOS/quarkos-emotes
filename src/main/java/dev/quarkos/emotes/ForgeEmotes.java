package dev.quarkos.emotes;
//? if forge {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import dev.quarkos.emotes.network.ForgeNetwork;
@Mod("quarkos_emotes")
public final class ForgeEmotes {
    public ForgeEmotes() {
        ForgeNetwork.init();
        // Client classes are loaded only after the physical-side check.
        if(FMLEnvironment.dist==Dist.CLIENT) ForgeEmotesClient.init();
    }
}
//?}
