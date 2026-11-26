package moscow.rockstar.mixin.minecraft.client.util;

import java.io.InputStream;
import java.util.List;

import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import rockstar.client.CRESTClient;

@Mixin(value = { Window.class })
public class WindowMixin {

    @Redirect(method = { "setIcon" }, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/util/Icons;getIcons(Lnet/minecraft/resource/ResourcePack;)Ljava/util/List;"))
    public List<InputSupplier<InputStream>> setCustomIcon(Icons icons, ResourcePack resourcePack) {
        if (CRESTClient.getInstance().internalMethod06896()) {
            try {
                return icons.getIcons(resourcePack);
            } catch (Exception ignored) {
            }
        }

        String namespace = CRESTClient.internalField1077;
        // CRESTClient purple-cross logo if present, else bundled Rockstar window icons
        InputStream cross16 = CRESTClient.class.getResourceAsStream("/assets/crestclient/icons/window/icon16x16.png");
        InputStream cross32 = CRESTClient.class.getResourceAsStream("/assets/crestclient/icons/window/icon32x32.png");
        InputStream icon16 = CRESTClient.class.getResourceAsStream("/assets/" + namespace + "/icons/window/icon16x16.png");
        InputStream icon32 = CRESTClient.class.getResourceAsStream("/assets/" + namespace + "/icons/window/icon32x32.png");

        InputStream chosen16 = firstNonNull(cross16, icon16);
        InputStream chosen32 = firstNonNull(cross32, icon32);

        // Never hand Minecraft a null stream (was NPE in TextureUtil.readResource).
        if (chosen16 == null && chosen32 == null) {
            try {
                return icons.getIcons(resourcePack);
            } catch (Exception ignored) {
                return List.of();
            }
        }

        java.util.List<InputSupplier<InputStream>> result = new java.util.ArrayList<>(2);
        if (chosen16 != null) {
            result.add(() -> chosen16);
        }
        if (chosen32 != null) {
            result.add(() -> chosen32);
        }
        if (result.isEmpty()) {
            try {
                return icons.getIcons(resourcePack);
            } catch (Exception ignored) {
                return List.of();
            }
        }
        return result;
    }

    private static InputStream firstNonNull(InputStream a, InputStream b) {
        return a != null ? a : b;
    }
}
