package dev.quarkos.emotes;
//? if fabric {
import com.terraformersmc.modmenu.api.*;
import dev.quarkos.emotes.config.EmoteConfigScreen;
public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return EmoteConfigScreen::new; }
}
//?}
