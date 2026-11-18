package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.animation.Emote;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class EmoteIcon {
    private EmoteIcon() {}

    public static void draw(GuiGraphics g, Emote emote, float x, float y, float scale) {
        Identifier id = Identifier.tryParse(emote.icon());
        if (id == null) return;
        //? if >=1.21.11 {
        var item = BuiltInRegistries.ITEM.getValue(id);
        //?} else {
        /*var item = BuiltInRegistries.ITEM.get(id);
        *///?}
        if (item == null) return;

        Ui.push(g, x, y, scale);
        try {
            g.renderItem(new ItemStack(item), -8, -8);
        } finally {
            Ui.pop(g);
        }
    }
}
