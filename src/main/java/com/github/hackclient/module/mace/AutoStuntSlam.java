package com.github.hackclient.module.mace;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Stunt Slam - Wind charge + slam combo for mace PvP.
 *
 * Detects when the player has height advantage over a target, then
 * triggers a slam attack at the optimal moment for maximum smash damage.
 * Includes smooth auto-aim to the target's position during descent.
 *
 * Combo flow:
 * 1. Detect height advantage (3+ blocks above target)
 * 2. If player is falling, smooth-aim toward target below
 * 3. Trigger attack at optimal fall distance for max damage
 *
 * Uses McReflect for all MC access: position queries, fall distance,
 * velocity checks, aim rotation, and attack execution.
 */
public class AutoStuntSlam extends Module {

    private long lastSlamTime = 0;
    private boolean slamArmed = false;
    private Object slamTarget = null;

    // Aim smoothing
    private float smoothedYaw = 0;
    private float smoothedPitch = 0;
    private boolean aimInitialized = false;

    private static final double MIN_HEIGHT_ADVANTAGE = 3.0;
    private static final double OPTIMAL_SLAM_FALL = 5.0;
    private static final double MAX_SLAM_RANGE = 6.0;

    public AutoStuntSlam() {
        super("AutoStuntSlam",
              "Auto-slams with mace when above target for max damage",
              GameMode.MACE,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        slamArmed = false;
        slamTarget = null;
        aimInitialized = false;
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();
        if (now - lastSlamTime < delay) return;

        // Initialize aim
        if (!aimInitialized) {
            smoothedYaw = McReflect.getPlayerYaw();
            smoothedPitch = McReflect.getPlayerPitch();
            aimInitialized = true;
        }

        double playerY = McReflect.getPlayerY();
        double[] velocity = McReflect.getPlayerVelocity();
        boolean isFalling = velocity[1] < -0.1;
        float fallDistance = McReflect.getPlayerFallDistance();

        // Find a target below us
        Object target = findTargetBelow(playerY);

        if (target == null) {
            slamArmed = false;
            slamTarget = null;
            return;
        }

        double targetY = McReflect.getEntityY(target);
        double heightAdvantage = playerY - targetY;
        double horizontalDist = getHorizontalDistance(target);

        // Phase 1: Detect height advantage
        if (heightAdvantage >= MIN_HEIGHT_ADVANTAGE && isFalling) {
            slamArmed = true;
            slamTarget = target;
        }

        if (!slamArmed || slamTarget == null) return;

        // Phase 2: Smooth aim toward target during descent
        float[] targetAngles = McReflect.getAnglesTo(slamTarget);
        if (targetAngles == null) { slamArmed = false; return; }

        float smoothSpeed = 0.5f + (float) (Math.random() * 0.2);
        smoothedYaw = AntiCheatBypass.smoothRotation(smoothedYaw, targetAngles[0], smoothSpeed);
        smoothedPitch = AntiCheatBypass.smoothRotation(smoothedPitch, targetAngles[1], smoothSpeed);

        // Apply rotation with noise
        setPlayerRotation(
            AntiCheatBypass.addRotationNoise(smoothedYaw),
            AntiCheatBypass.addRotationNoise(smoothedPitch)
        );

        // Phase 3: Check if aimed and at optimal slam distance
        boolean aimed = Math.abs(smoothedYaw - targetAngles[0]) < 3.0f &&
                        Math.abs(smoothedPitch - targetAngles[1]) < 3.0f;

        // Randomize the optimal slam trigger point
        double slamTrigger = OPTIMAL_SLAM_FALL
                + ThreadLocalRandom.current().nextGaussian() * 0.5;

        if (aimed && isFalling && fallDistance >= slamTrigger && horizontalDist <= MAX_SLAM_RANGE) {
            if (shouldAct(0.92)) {
                McReflect.attackEntity(slamTarget);
                McReflect.swingHand();
                lastSlamTime = now;
                slamArmed = false;
                slamTarget = null;
                recordAction();
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        slamArmed = false;
        slamTarget = null;
    }

    /**
     * Find the closest alive player below the given Y coordinate.
     */
    private Object findTargetBelow(double playerY) {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object best = null;
        double bestDist = MAX_SLAM_RANGE + 5.0;

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double entityY = McReflect.getEntityY(entity);
            if (entityY >= playerY) continue; // Must be below us

            double dist = McReflect.distanceTo(entity);
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }

        return best;
    }

    /**
     * Calculate horizontal distance to entity (ignoring Y component).
     */
    private double getHorizontalDistance(Object entity) {
        double dx = McReflect.getEntityX(entity) - McReflect.getPlayerX();
        double dz = McReflect.getEntityZ(entity) - McReflect.getPlayerZ();
        return Math.sqrt(dx * dx + dz * dz);
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
