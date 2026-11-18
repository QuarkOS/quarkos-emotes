package dev.quarkos.emotes.config.ui;

import dev.quarkos.emotes.animation.Emote;
import dev.quarkos.emotes.animation.EmotePlayer;
import dev.quarkos.emotes.config.ConfigLayout;
import dev.quarkos.emotes.config.ConfigLayout.Box;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;

//? if >=1.21.11 {
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import java.util.Map;
import java.util.WeakHashMap;
//?}

/** Preview-only playback for the inventory renderer, including the regular player pose and prop layers. */
public final class EmotePreview {
    public record Sample(int entityId, EmotePlayer playback, float yaw) {}

    // On 1.21.1 the inventory draws immediately. On 1.21.11 its extracted state
    // carries the sample to the later picture-in-picture render.
    private static Sample active;
    //? if >=1.21.11 {
    private static final Map<EntityRenderState, Sample> submitted = new WeakHashMap<>();

    public static void capture(int entityId, EntityRenderState state) {
        if (active != null && entityId == active.entityId()) submitted.put(state, active);
    }

    public static Sample take(EntityRenderState state) {
        return submitted.remove(state);
    }
    //?}

    public static EmotePlayer playback(int entityId) {
        return active != null && entityId == active.entityId() ? active.playback() : null;
    }

    public static float orbit() {
        return active == null ? 0f : active.yaw();
    }

    public static void withSample(Sample sample, Runnable render) {
        Sample previous = active;
        active = sample;
        try {
            render.run();
        } finally {
            active = previous;
        }
    }

    public static void draw(GuiGraphics g, Box box, LocalPlayer player, Emote emote, double seconds, float yaw) {
        if (player == null || emote == null || box.h() < 8 || box.w() < 8) return;
        ConfigLayout.Fit fit = ConfigLayout.preview(emote.id(), box.w(), box.h());
        EmotePlayer playback = new EmotePlayer(emote);
        playback.freeze(seconds);
        Sample sample = new Sample(player.getId(), playback, fit.yaw() + yaw);
        float cx = (box.x() + box.right()) / 2f;
        float cy = (box.y() + box.bottom()) / 2f;
        withSample(sample, () -> InventoryScreen.renderEntityInInventoryFollowsMouse(g, box.x(), box.y(), box.right(),
                box.bottom(), fit.scale(), fit.yOffset(), cx, cy, player));
    }

    public static double stillTime(Emote emote) {
        String name = emote.id().substring(emote.id().indexOf(':') + 1);
        return switch (name) {
            case "wave", "floss" -> .5;
            case "tableflip" -> .75;
            case "thinking" -> 1.2;
            case "default_dance" -> 3.25;
            case "sword_salute", "backflip" -> .8;
            default -> emote.entryLength() + (emote.loop() == Emote.Loop.HOLD ? emote.length() : emote.length() * .38);
        };
    }

    /** A clock and optional manual yaw for one animated preview surface. */
    public static final class Motion {
        private Emote emote;
        private int ticks;
        private float manualYaw;
        private boolean dragging, hasManualYaw;
        private double pinned = Double.NaN;

        public void show(Emote next) {
            if (emote == next) return;
            emote = next;
            ticks = 0;
            pinned = Double.NaN;
            hasManualYaw = dragging = false;
        }

        public void tick() {
            if (emote != null) ticks++;
        }

        public void draw(GuiGraphics g, Box box, LocalPlayer player, float partial, boolean orbit) {
            if (emote == null) return;
            double elapsed = (ticks + Math.clamp(partial, 0, 1)) / 20.0;
            double seconds = elapsed;
            if (emote.loop() == Emote.Loop.ONCE) {
                double duration = emote.totalLength();
                seconds = Math.min(duration, elapsed % (duration + .40));
            }
            float yaw = orbit ? hasManualYaw ? manualYaw : (float) Math.sin(elapsed * .90) * 26f : 0;
            if (!Double.isNaN(pinned)) {
                seconds = pinned;
                if (!hasManualYaw) yaw = 0;
            }
            EmotePreview.draw(g, box, player, emote, seconds, yaw);
        }

        public void pin(double seconds) {
            pinned = seconds;
        }

        public void startDrag() {
            if (!hasManualYaw) manualYaw = (float) Math.sin(ticks / 20.0 * .90) * 26f;
            hasManualYaw = dragging = true;
        }

        public boolean drag(double dx) {
            if (!dragging) return false;
            manualYaw = (manualYaw + (float) dx * 2f) % 360f;
            return true;
        }

        public boolean endDrag() {
            boolean wasDragging = dragging;
            dragging = false;
            return wasDragging;
        }
    }
}
