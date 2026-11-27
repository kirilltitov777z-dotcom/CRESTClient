package crestclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

import crestclient.module.Module;

/**
 * Trigger bot. When the crosshair rests on a living target and you're in range,
 * attack it with a tiny human-like delay. Does not aim for you (see AimAssist)
 * and is not a KillAura — it only clicks when you're already looking at them.
 */
public class TriggerBot extends Module {

    private static final double RANGE = 3.5;
    private int cooldown = 0;

    public TriggerBot() {
        super("TriggerBot", "Attack what your crosshair already rests on");
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.getAttackCooldownProgress(0.0f) < 1.0f) return;

        if (cooldown > 0) { cooldown--; return; }

        Entity target = mc.targetedEntity;
        if (!(target instanceof LivingEntity living)) return;
        if (target == mc.player || !living.isAlive() || living.isSpectator() || !target.isAttackable()) return;
        if (mc.player.getEyePos().distanceTo(target.getPos().add(0, target.getHeight() * 0.5, 0)) > RANGE) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        cooldown = 1; // tiny delay, no perfect robotic clicks
    }
}
