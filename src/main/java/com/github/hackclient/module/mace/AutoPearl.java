package com.github.hackclient.module.mace;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Pearl - Automatically throws ender pearls for gap closing in mace PvP.
 *
 * In mace PvP, getting above your opponent is critical. This module
 * calculates distance to the target and throws pearls at optimal range
 * for height advantage or gap closing.
 *
 * Features:
 * - Calculates optimal throw distance (10-25 blocks from target)
 * - Smooth rotation to look upward for height gain
 * - Only throws when target is alive and within optimal range
 * - Stealth-aware cooldown between pearl throws
 *
 * Uses McReflect for player scanning, distance calculation, and rotation.
 */
public class AutoPearl extends Module {

    private long lastPearlTime = 0;
    private static final long PEARL_COOLDOWN_BASE_MS = 500;
    private static final double OPTIMAL_PEARL_MIN = 10.0;
    private static final double OPTIMAL_PEARL_MAX = 25.0;

    // Aim smoothing for look-up rotation
    private float smoothedPitch = 0;
    private float smoothedYaw = 0;
    private boolean aimInitialized = false;
    private int aimTicks = 0;

    public AutoPearl() {
        super("AutoPearl",
              "Auto-throws pearls for height advantage in mace fights",
              GameMode.MACE,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        aimInitialized = false;
        aimTicks = 0;
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();
        long cooldown = PEARL_COOLDOWN_BASE_MS + timer.getNextDelayMs();
        if (now - lastPearlTime < cooldown) return;

        // Initialize aim from current look direction
        if (!aimInitialized) {
            smoothedYaw = McReflect.getPlayerYaw();
            smoothedPitch = McReflect.getPlayerPitch();
            aimInitialized = true;
        }

        // Find target and check distance
        Object target = findNearestTarget();
        if (target == null) {
            aimTicks = 0;
            return;
        }

        double distToTarget = McReflect.distanceTo(target);

        // Only throw pearl when target is at optimal gap-close distance
        if (distToTarget < OPTIMAL_PEARL_MIN || distToTarget > OPTIMAL_PEARL_MAX) {
            aimTicks = 0;
            return;
        }

        // Check if we don't already have height advantage
        double playerY = McReflect.getPlayerY();
        double targetY = McReflect.getEntityY(target);
        if (playerY > targetY + 5.0) {
            // Already above target, no need to pearl
            return;
        }

        if (!shouldAct(0.88)) return;

        // Smoothly rotate to look upward for height gain
        // Pitch target: between -75 and -85 degrees (nearly straight up)
        float targetPitch = -80.0f + (float) (ThreadLocalRandom.current().nextGaussian() * 3.0);
        // Yaw: aim toward the target so the pearl arcs in their direction
        float[] targetAngles = McReflect.getAnglesTo(target);
        float targetYaw = targetAngles != null ? targetAngles[0] : smoothedYaw;

        float rotSpeed = 0.4f + (float) (Math.random() * 0.1);
        smoothedPitch = AntiCheatBypass.smoothRotation(smoothedPitch, targetPitch, rotSpeed);
        smoothedYaw = AntiCheatBypass.smoothRotation(smoothedYaw, targetYaw, rotSpeed);

        // Apply the rotation with noise
        setPlayerRotation(
            AntiCheatBypass.addRotationNoise(smoothedYaw),
            AntiCheatBypass.addRotationNoise(smoothedPitch)
        );

        aimTicks++;

        // Throw pearl once aimed close enough (within 5 degrees)
        if (Math.abs(smoothedPitch - targetPitch) < 5.0f && aimTicks >= 3) {
            if (shouldAct(0.90)) {
                // Simulate pearl throw (right-click/use item)
                McReflect.swingHand();
                lastPearlTime = now;
                aimTicks = 0;
                recordAction();
            }
        }
    }

    private Object findNearestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object nearest = null;
        double nearestDist = OPTIMAL_PEARL_MAX + 1;

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }

        return nearest;
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
}
