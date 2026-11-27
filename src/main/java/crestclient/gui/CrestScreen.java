package crestclient.gui;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import crestclient.CrestClient;
import crestclient.module.Module;
import crestclient.module.ModuleManager;

/** CRESTClient own GUI. Right Shift opens/closes. */
public class CrestScreen extends Screen {

    private static final int ROW_H = 22;
    private static final int PANEL_W = 220;
    private static final int ACCENT = 0xFFB060FF; // purple cross accent
    private static final int BG = 0xE0101014;
    private static final int ROW_BG = 0xFF1C1C22;
    private static final int ON = 0xFF6C4CD8;
    private static final int OFF = 0xFF3A3A44;

    public CrestScreen() {
        super(Text.literal(CrestClient.NAME));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        List<Module> mods = ModuleManager.getInstance().modules();
        int panelX = (this.width - PANEL_W) / 2;
        int panelY = 24;
        int panelH = ROW_H * mods.size() + 40;

        context.fill(panelX - 6, panelY - 6, panelX + PANEL_W + 6, panelY + panelH, BG);

        context.drawCenteredTextWithShadow(this.textRenderer, CrestClient.NAME, this.width / 2, panelY, ACCENT);
        // purple-cross accent bar
        context.fill(panelX, panelY + 14, panelX + PANEL_W, panelY + 16, ACCENT);

        int y = panelY + 24;
        for (Module m : mods) {
            boolean hover = mouseX >= panelX && mouseX <= panelX + PANEL_W && mouseY >= y && mouseY <= y + ROW_H - 2;
            context.fill(panelX, y, panelX + PANEL_W, y + ROW_H - 2, hover ? 0xFF26262E : ROW_BG);
            context.fill(panelX, y, panelX + 3, y + ROW_H - 2, m.isEnabled() ? ON : OFF);

            String label = m.name() + "  [" + (m.isEnabled() ? "ON" : "OFF") + "]";
            context.drawTextWithShadow(this.textRenderer, Text.literal(label), panelX + 10, y + 7, m.isEnabled() ? 0xFFFFFFFF : 0xFFB8B8C0);
            y += ROW_H;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        List<Module> mods = ModuleManager.getInstance().modules();
        int panelX = (this.width - PANEL_W) / 2;
        int y = 24 + 24;
        double mx = click.x();
        double my = click.y();
        for (Module m : mods) {
            if (mx >= panelX && mx <= panelX + PANEL_W && my >= y && my <= y + ROW_H - 2) {
                m.toggle();
                return true;
            }
            y += ROW_H;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }
}
