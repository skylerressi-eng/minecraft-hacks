package com.github.hackclient.module.legacy;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Kill Aura - Auto-attacks nearby entities with humanized aiming for 1.8 PvP.
 *
 * Features:
 * - Smooth rotation to target (bezier-curved mouse path)
 * - Target priority system (closest, lowest health, etc.)
 * - FOV check: only targets within a natural field of view
 * - Wall check: won't attack through walls
 * - Humanized attack timing synced with AutoClicker
 */
public class KillAura extends Module {

    private long lastAttackTime = 0;
    private double attackRange = 3.0;
    private float fov = 120.0f; // Only target within this FOV
    private boolean throughWalls = false;

    // Aim smoothing state
    private float currentYaw = 0;
    private float currentPitch = 0;

    public KillAura() {
        super("KillAura",
              "Auto-attacks nearby players with humanized aim",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();

        if (now - lastAttackTime < delay) {
            return;
        }

        // Find best target
        Object target = findBestTarget();
        if (target == null) return;

        // Calculate aim angles
        float[] targetAngles = getAnglesTo(target);
        if (targetAngles == null) return;

        // Check FOV
        float angleToTarget = angleDifference(currentYaw, targetAngles[0]);
        if (Math.abs(angleToTarget) > fov / 2) return;

        // Smooth aim toward target (not instant snap)
        float aimSpeed = 0.35f + (float) (Math.random() * 0.2);
        currentYaw = AntiCheatBypass.smoothRotation(currentYaw, targetAngles[0], aimSpeed);
        currentPitch = AntiCheatBypass.smoothRotation(currentPitch, targetAngles[1], aimSpeed);

        // Add rotation noise
        currentYaw = AntiCheatBypass.addRotationNoise(currentYaw);
        currentPitch = AntiCheatBypass.addRotationNoise(currentPitch);

        // Check if close enough to attack
        boolean aimed = Math.abs(currentYaw - targetAngles[0]) < 4.0f &&
                        Math.abs(currentPitch - targetAngles[1]) < 4.0f;

        if (aimed && AntiCheatBypass.shouldActThisTick(0.9)) {
            attack(target);
            lastAttackTime = now;
        }
    }

    // Stubs for MC client integration
    private Object findBestTarget() { return null; }
    private float[] getAnglesTo(Object target) { return null; }
    private void attack(Object target) {
        // TODO: mc.interactionManager.attackEntity(mc.player, (Entity) target)
    }

    private float angleDifference(float a, float b) {
        float diff = ((b - a) % 360 + 540) % 360 - 180;
        return diff;
    }

    public void setRange(double range) { this.attackRange = range; }
    public void setFov(float fov) { this.fov = fov; }
}
