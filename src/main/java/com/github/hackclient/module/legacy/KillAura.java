package com.github.hackclient.module.legacy;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * Kill Aura - Auto-attacks nearby entities with humanized aiming for 1.8 PvP.
 *
 * Features:
 * - Smooth rotation to target using AntiCheatBypass.smoothRotation
 * - Target priority: closest alive player within range
 * - FOV check: only targets within a configurable field of view
 * - Humanized attack timing synced with stealth engine
 * - Rotation noise to mimic real mouse movements
 *
 * Uses McReflect for all MC access: player scanning, distance checks,
 * angle calculations, and attack execution.
 */
public class KillAura extends Module {

    private long lastAttackTime = 0;
    private double attackRange = 3.0;
    private float fov = 120.0f;

    // Aim smoothing state
    private float smoothedYaw = 0;
    private float smoothedPitch = 0;
    private boolean hasInitializedAim = false;

    public KillAura() {
        super("KillAura",
              "Auto-attacks nearby players with humanized aim",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        hasInitializedAim = false;
        lastAttackTime = 0;
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        // Initialize smoothed aim from current player look direction
        if (!hasInitializedAim) {
            smoothedYaw = McReflect.getPlayerYaw();
            smoothedPitch = McReflect.getPlayerPitch();
            hasInitializedAim = true;
        }

        // Timer-based action gating
        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();
        if (now - lastAttackTime < delay) return;

        // Stealth-aware probability check
        if (!shouldAct(0.90)) return;

        // Find best target (closest alive player in range)
        Object target = findBestTarget();
        if (target == null) return;

        // Calculate angles to target
        float[] targetAngles = McReflect.getAnglesTo(target);
        if (targetAngles == null) return;

        // FOV check: only attack targets within our field of view
        float currentYaw = McReflect.getPlayerYaw();
        float angleToTarget = angleDifference(currentYaw, targetAngles[0]);
        if (Math.abs(angleToTarget) > fov / 2) return;

        // Smooth aim toward target (not instant snap)
        // Speed varies per tick to simulate human inconsistency
        float aimSpeed = 0.35f + (float) (Math.random() * 0.2);

        // Query stealth engine for max rotation speed
        StealthEngine stealth = StealthEngine.getInstance();
        if (stealth != null) {
            float maxSpeed = stealth.getMaxRotationSpeed();
            // Clamp the per-tick rotation delta
            float yawDelta = angleDifference(smoothedYaw, targetAngles[0]);
            float pitchDelta = targetAngles[1] - smoothedPitch;
            if (Math.abs(yawDelta) > maxSpeed) {
                aimSpeed = Math.min(aimSpeed, maxSpeed / Math.abs(yawDelta));
            }
        }

        smoothedYaw = AntiCheatBypass.smoothRotation(smoothedYaw, targetAngles[0], aimSpeed);
        smoothedPitch = AntiCheatBypass.smoothRotation(smoothedPitch, targetAngles[1], aimSpeed);

        // Add rotation noise to simulate real mouse micro-movements
        float noisyYaw = AntiCheatBypass.addRotationNoise(smoothedYaw);
        float noisyPitch = AntiCheatBypass.addRotationNoise(smoothedPitch);

        // Apply rotation to player via reflection
        setPlayerRotation(noisyYaw, noisyPitch);

        // Check if aimed close enough to attack (within 4 degrees)
        boolean aimed = Math.abs(angleDifference(noisyYaw, targetAngles[0])) < 4.0f &&
                        Math.abs(noisyPitch - targetAngles[1]) < 4.0f;

        if (aimed) {
            McReflect.attackEntity(target);
            McReflect.swingHand();
            lastAttackTime = now;
            recordAction();
        }
    }

    /**
     * Find the best target: closest alive player within attack range.
     */
    private Object findBestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object best = null;
        double bestDist = attackRange + 1;

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist <= attackRange && dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }

        return best;
    }

    private void setPlayerRotation(float yaw, float pitch) {
        try {
            Object player = McReflect.getPlayer();
            if (player == null) return;
            java.lang.reflect.Field yawField = findField(player.getClass(), "field_6031", "yaw");
            java.lang.reflect.Field pitchField = findField(player.getClass(), "field_6036", "pitch");
            if (yawField != null) {
                yawField.setAccessible(true);
                yawField.setFloat(player, yaw);
            }
            if (pitchField != null) {
                pitchField.setAccessible(true);
                pitchField.setFloat(player, Math.max(-90f, Math.min(90f, pitch)));
            }
        } catch (Exception ignored) {}
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String... names) {
        Class<?> current = clazz;
        while (current != null) {
            for (String name : names) {
                try {
                    java.lang.reflect.Field f = current.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private float angleDifference(float a, float b) {
        float diff = ((b - a) % 360 + 540) % 360 - 180;
        return diff;
    }

    public void setRange(double range) { this.attackRange = range; }
    public void setFov(float fov) { this.fov = fov; }
}
