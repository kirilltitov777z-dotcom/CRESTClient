package crestclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import crestclient.module.Module;

/**
 * Legit, unnoticeable aim assist. Only nudges aim a tiny bit toward the closest
 * visible target while you attack; smooth, small-step, no snapping, no aimbot,
 * no KillAura (nothing is clicked/attacked for you).
 */
public class AimAssist extends Module {

    private static final double RANGE = 4.5;
    private static final float MAX_FOV = 35.0f;   // only assist near where you already look
    private static final float SPEED = 4.0f;       // degrees per tick, smooth

    public AimAssist() {
        super("AimAssist", "Legit unnoticeable aim assist (no KillAura)");
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.attackKey.isPressed()) return;      // only while you attack
        if (mc.currentScreen != null) return;

        Entity target = nearestVisible(mc);
        if (target == null) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d aimPoint = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        Vec3d delta = aimPoint.subtract(eye);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        // only assist if already close to target (legit)
        if (Math.abs(angleDelta(currentYaw, targetYaw)) > MAX_FOV) return;
        if (Math.abs(currentPitch - targetPitch) > MAX_FOV) return;

        mc.player.setYaw(approach(currentYaw, targetYaw, SPEED));
        mc.player.setPitch(clamp(approach(currentPitch, targetPitch, SPEED), -90f, 90f));
    }

    private Entity nearestVisible(MinecraftClient mc) {
        Vec3d eye = mc.player.getEyePos();
        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            if (!(e instanceof LivingEntity living)) continue;
            if (!living.isAlive() || living.isSpectator() || !e.isAttackable()) continue;

            double dist = eye.distanceTo(e.getPos().add(0, e.getHeight() * 0.5, 0));
            if (dist > RANGE || dist >= bestDist) continue;
            if (!isVisible(mc, eye, e)) continue;

            best = e;
            bestDist = dist;
        }
        return best;
    }

    private boolean isVisible(MinecraftClient mc, Vec3d eye, Entity e) {
        Vec3d aimPoint = e.getPos().add(0.0, e.getHeight() * 0.5, 0.0);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                eye, aimPoint, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS || hit.getPos().distanceTo(aimPoint) < 0.5;
    }

    private static float approach(float current, float target, float maxStep) {
        float d = angleDelta(current, target);
        if (Math.abs(d) <= maxStep) return current + d;
        return current + Math.signum(d) * maxStep;
    }

    private static float angleDelta(float from, float to) {
        float d = (to - from) % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }
}
