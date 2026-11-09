package dev.quarkos.emotes.mixin;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(LivingEntityRenderer.class)
public interface LayerAccess {
    @Invoker("addLayer") boolean emotes$addLayer(RenderLayer<?,?> layer);
}
