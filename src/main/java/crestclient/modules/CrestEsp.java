package crestclient.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import crestclient.module.Module;
import crestclient.module.ModuleManager;

/** ESP: wireframe boxes around living entities. No KillAura. */
public class CrestEsp extends Module {

    public CrestEsp() {
        super("ESP", "Wireframe boxes around living entities");
    }

    public static void registerRendering() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            CrestEsp esp = ModuleManager.getInstance().get(CrestEsp.class);
            if (esp == null || !esp.isEnabled()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;

            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            Camera camera = context.camera();
            if (matrices == null || consumers == null || camera == null) return;

            Vec3d cam = camera.getPos();
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

            matrices.push();
            matrices.translate(-cam.x, -cam.y, -cam.z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player) continue;
                if (!(e instanceof LivingEntity living)) continue;
                if (!living.isAlive() || living.isSpectator()) continue;
                drawBox(lines, matrix, e.getBoundingBox());
            }

            matrices.pop();
        });
    }

    private static void drawBox(VertexConsumer vc, Matrix4f m, Box box) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        int r = 176, g = 96, b = 255, a = 200; // purple

        // 12 edges
        line(vc, m, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(vc, m, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(vc, m, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(vc, m, x1, y1, z2, x1, y1, z1, r, g, b, a);
        line(vc, m, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(vc, m, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(vc, m, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(vc, m, x1, y2, z2, x1, y2, z1, r, g, b, a);
        line(vc, m, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(vc, m, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(vc, m, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(vc, m, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        vc.vertex(m, x1, y1, z1).color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
        vc.vertex(m, x2, y2, z2).color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
    }
}
