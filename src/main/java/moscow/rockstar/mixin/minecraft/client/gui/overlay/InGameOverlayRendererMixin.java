package moscow.rockstar.mixin.minecraft.client.gui.overlay;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rockstar.modules.visual.RemovalsModule;
import rockstar.client.CRESTClient;

@Mixin(value={InGameOverlayRenderer.class})
public class InGameOverlayRendererMixin {
    @Inject(method={"renderFireOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void renderFireOverlayHook(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Sprite sprite, CallbackInfo callbackInfo) {
        RemovalsModule typedValue322 = CRESTClient.getInstance().getModuleManager().getModule(RemovalsModule.class);
        if (typedValue322.isEnabled() && typedValue322.internalMethod08070().isSelected()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method={"renderInWallOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void renderInWallOverlayHook(Sprite sprite, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, CallbackInfo callbackInfo) {
        RemovalsModule typedValue322 = CRESTClient.getInstance().getModuleManager().getModule(RemovalsModule.class);
        if (typedValue322.isEnabled() && typedValue322.internalMethod08111().isSelected()) {
            callbackInfo.cancel();
        }
    }
}
