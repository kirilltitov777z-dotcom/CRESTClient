package rockstar.client.ui;

import net.minecraft.util.Identifier;
import pyrock.utility.render.ColorRGBA;
import pyrock.utility.render.CustomDrawContext;

/** Draws the CRESTClient purple-cross logo. */
public final class CrestBranding {
    /** crestclient:textures/logo.png */
    public static final Identifier LOGO = Identifier.of("crestclient", "textures/logo.png");

    private CrestBranding() {
    }

    public static void renderLogo(CustomDrawContext context, float x, float y, float size) {
        if (context == null) return;
        context.drawTexture(LOGO, x, y, size, size, ColorRGBA.WHITE);
    }
}
