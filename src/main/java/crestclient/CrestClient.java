package crestclient;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import crestclient.autobuy.AutoBuy;
import crestclient.autobuy.AutoSetup;
import crestclient.gui.CrestScreen;
import crestclient.module.Module;
import crestclient.module.ModuleManager;
import crestclient.modules.AimAssist;
import crestclient.modules.CrestEsp;
import crestclient.modules.TriggerBot;

/** CRESTClient — own clean Fabric client. No Rockstar, no KillAura. */
public final class CrestClient implements ClientModInitializer {
    public static final String MOD_ID = "crestclient";
    public static final String NAME = "CRESTClient";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.getInstance().register(new CrestEsp());
        ModuleManager.getInstance().register(new AimAssist());
        ModuleManager.getInstance().register(new TriggerBot());
        ModuleManager.getInstance().register(new AutoBuy());
        ModuleManager.getInstance().register(new AutoSetup());

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crestclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.crestclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen instanceof CrestScreen) {
                    mc.setScreen(null);
                } else {
                    mc.setScreen(new CrestScreen());
                }
            }
            for (Module m : ModuleManager.getInstance().modules()) {
                if (m.isEnabled()) {
                    m.tick(client);
                }
            }
        });

        CrestEsp.registerRendering();
        LOG.info("CRESTClient initialized");
    }
}
