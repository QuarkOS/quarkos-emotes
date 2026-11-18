package dev.quarkos.emotes.mixin;

import org.spongepowered.asm.mixin.Mixin;

//? if >=1.21.11 {
import dev.quarkos.emotes.config.ui.EmotePreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
//? if >=26.3 {
/*import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
*///?} else {
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiRenderer.class)
//?} else {
/*@Mixin(net.minecraft.client.gui.GuiGraphics.class)
*///?}
public abstract class PreviewGuiMixin {
    //? if >=1.21.11 {
    //? if >=26.3 {
    /*@Shadow @Final private GuiRenderState renderState;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    *///?} else {
    @Shadow @Final GuiRenderState renderState;
    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    //?}

    @Unique private final List<GuiEntityRenderer> emotes$previews = new ArrayList<>();
    @Unique private int emotes$usedPreviews;

    @Inject(method = "preparePictureInPicture", at = @At("HEAD"))
    private void emotes$beginPreviews(CallbackInfo ci) {
        emotes$usedPreviews = 0;
    }

    @Inject(method = "preparePictureInPictureState", at = @At("HEAD"), cancellable = true)
    private void emotes$preparePreview(PictureInPictureRenderState state, int scale, CallbackInfo ci) {
        if (!(state instanceof GuiEntityRenderState entity)) return;
        EmotePreview.Sample sample = EmotePreview.take(entity.renderState());
        if (sample == null) return;

        // Vanilla's entity PiP renderer reuses one texture. Each visible
        // emote needs its own until the GUI has blitted all of them.
        int index = emotes$usedPreviews++;
        if (index == emotes$previews.size()) {
            //? if >=26.3 {
            /*emotes$previews.add(new GuiEntityRenderer(Minecraft.getInstance().getEntityRenderDispatcher()));
            *///?} else {
            emotes$previews.add(new GuiEntityRenderer(bufferSource, Minecraft.getInstance().getEntityRenderDispatcher()));
            //?}
        }
        GuiEntityRenderer renderer = emotes$previews.get(index);
        //? if >=26.3 {
        /*EmotePreview.withSample(sample, () -> renderer.prepare(entity, renderState, featureRenderDispatcher, scale));
        *///?} else {
        EmotePreview.withSample(sample, () -> renderer.prepare(entity, renderState, scale));
        //?}
        ci.cancel();
    }

    @Inject(method = "preparePictureInPicture", at = @At("TAIL"))
    private void emotes$releaseIdlePreviews(CallbackInfo ci) {
        if (emotes$usedPreviews == 0) emotes$clearPreviews();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void emotes$closePreviews(CallbackInfo ci) {
        emotes$clearPreviews();
    }

    @Unique private void emotes$clearPreviews() {
        emotes$previews.forEach(GuiEntityRenderer::close);
        emotes$previews.clear();
    }
    //?}
}
