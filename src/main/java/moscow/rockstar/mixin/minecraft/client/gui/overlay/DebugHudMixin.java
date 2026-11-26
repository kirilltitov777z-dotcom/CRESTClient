package moscow.rockstar.mixin.minecraft.client.gui.overlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rockstar.modules.other.NameProtectModule;
import rockstar.client.CRESTClient;

@Mixin(value={DebugHud.class})
public class DebugHudMixin {
    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true)
    private void hideDebugHud(DrawContext drawContext, CallbackInfo callbackInfo) {
        NameProtectModule typedValue207 = CRESTClient.getInstance().getModuleManager().getModule(NameProtectModule.class);
        if (typedValue207 != null && typedValue207.internalMethod09833()) {
            callbackInfo.cancel();
        }
    }
}
