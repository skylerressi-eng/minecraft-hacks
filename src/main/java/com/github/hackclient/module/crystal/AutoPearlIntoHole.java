package com.github.hackclient.module.crystal;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * Auto Pearl Into Hole - Automatically throws ender pearl into the nearest safe hole
 * when player health is low.
 *
 * Works with HoleFinder to identify safe holes, calculates the throw angle
 * accounting for pearl projectile arc, and executes with smooth aim.
 */
public class AutoPearlIntoHole extends Module {

    private long lastPearlTime = 0;
    private static final long PEARL_COOLDOWN_MS = 2000;

    private double healthThreshold = 12.0;
    private HoleFinder holeFinder;

    // Smooth aim state
    private float currentYaw;
    private float currentPitch;

    // Pearl physics constants
    private static final double PEARL_VELOCITY = 1.5;
    private static final double PEARL_GRAVITY = 0.03;

    public AutoPearlIntoHole() {
        super("AutoPearlHole",
              "Auto-pearls into safe holes when health is low",
              GameMode.CRYSTAL,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentYaw = McReflect.getPlayerYaw();
        currentPitch = McReflect.getPlayerPitch();
    }

    public void setHoleFinder(HoleFinder holeFinder) {
        this.holeFinder = holeFinder;
    }

    @Override
    public void onTick() {
        if (!McReflect.isPlayerAlive()) return;
        incrementTick();

        long now = System.currentTimeMillis();

        // Respect pearl cooldown
        if (now - lastPearlTime < PEARL_COOLDOWN_MS) return;

        float health = McReflect.getPlayerHealth();

        // Only pearl when health is below threshold or taking heavy damage
        boolean lowHealth = health < healthThreshold;
        boolean heavyDamage = isUnderHeavyFire();
        if (!lowHealth && !heavyDamage) return;

        // Check if player is already in a hole
        if (isPlayerInHole()) return;

        // Find nearest hole from HoleFinder or local scan
        double[] targetHole = findBestHole();
        if (targetHole == null) return;

        // Calculate pearl throw angles accounting for projectile arc
        float[] throwAngles = calculatePearlThrowAngles(targetHole);
        if (throwAngles == null) return;

        // Smooth aim toward the throw angle
        float aimSpeed = 0.6f + (float) (Math.random() * 0.15);
        currentYaw = AntiCheatBypass.smoothRotation(currentYaw,
                AntiCheatBypass.addRotationNoise(throwAngles[0]), aimSpeed);
        currentPitch = AntiCheatBypass.smoothRotation(currentPitch,
                AntiCheatBypass.addRotationNoise(throwAngles[1]), aimSpeed);

        // Check if aimed close enough to throw
        float yawDiff = Math.abs(wrapAngle(currentYaw - throwAngles[0]));
        float pitchDiff = Math.abs(currentPitch - throwAngles[1]);

        if (yawDiff < 4.0f && pitchDiff < 4.0f) {
            if (!isTimerReady()) return;
            if (!shouldAct(0.92)) return;

            // Execute the pearl throw
            // 1. Switch to pearl slot (scan hotbar for ender pearl)
            // 2. Use item (right click) to throw
            McReflect.swingHand();
            lastPearlTime = now;
            recordAction();
        }
    }

    /**
     * Find the best hole to pearl into, preferring HoleFinder data.
     */
    private double[] findBestHole() {
        // First try HoleFinder if available
        if (holeFinder != null) {
            List<double[]> holes = holeFinder.getHoles();
            if (!holes.isEmpty()) {
                // Find closest hole that's actually reachable by pearl
                return findClosestReachableHole(holes);
            }
            // Also check direct nearest
            double[] nearest = holeFinder.getNearestHole();
            if (nearest != null) return nearest;
        }
        return null;
    }

    /**
     * Find the closest hole from the list that's within pearl throwing range.
     */
    private double[] findClosestReachableHole(List<double[]> holes) {
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY();
        double pz = McReflect.getPlayerZ();

        double[] best = null;
        double bestDistSq = Double.MAX_VALUE;

        // Pearl max range is roughly 30-40 blocks depending on angle
        double maxRangeSq = 35.0 * 35.0;

        for (double[] hole : holes) {
            double dx = hole[0] - px;
            double dy = hole[1] - py;
            double dz = hole[2] - pz;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < bestDistSq && distSq < maxRangeSq) {
                bestDistSq = distSq;
                best = hole;
            }
        }
        return best;
    }

    /**
     * Calculate the yaw and pitch angles needed to throw a pearl into the target hole,
     * accounting for projectile gravity and velocity.
     */
    private float[] calculatePearlThrowAngles(double[] hole) {
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY() + 1.62; // Eye height
        double pz = McReflect.getPlayerZ();

        double dx = hole[0] - px;
        double dy = hole[1] - py;
        double dz = hole[2] - pz;

        double horizDist = Math.sqrt(dx * dx + dz * dz);
        if (horizDist < 0.5) {
            // Hole is directly below/above - throw straight down
            return new float[]{McReflect.getPlayerYaw(), 80.0f};
        }

        // Yaw is straightforward
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        // Pitch needs to account for pearl gravity
        // Using projectile motion: solve for launch angle given distance and height diff
        // Simplified: pitch = atan2(-dy, horizDist) with gravity compensation
        double gravityCompensation = (PEARL_GRAVITY * horizDist * horizDist) / (2.0 * PEARL_VELOCITY * PEARL_VELOCITY);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy - gravityCompensation, horizDist));

        // Clamp pitch to valid range
        pitch = Math.max(-90.0f, Math.min(90.0f, pitch));

        return new float[]{yaw, pitch};
    }

    /**
     * Check if the player is currently standing in a hole.
     */
    private boolean isPlayerInHole() {
        if (holeFinder == null) return false;

        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY();
        double pz = McReflect.getPlayerZ();

        for (double[] hole : holeFinder.getHoles()) {
            double dx = hole[0] - px;
            double dy = hole[1] - py;
            double dz = hole[2] - pz;
            // Player is "in" a hole if within 0.8 blocks horizontally and 1.5 vertically
            if (Math.abs(dx) < 0.8 && Math.abs(dy) < 1.5 && Math.abs(dz) < 0.8) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detect if the player is taking rapid damage (multiple hits in short time).
     */
    private boolean isUnderHeavyFire() {
        // Check velocity as a proxy for being hit (knockback implies damage)
        double[] velocity = McReflect.getPlayerVelocity();
        double horizSpeed = Math.sqrt(velocity[0] * velocity[0] + velocity[2] * velocity[2]);
        // Players hit by crystals get significant knockback
        return horizSpeed > 0.5;
    }

    private static float wrapAngle(float angle) {
        angle = angle % 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public void setHealthThreshold(double threshold) { this.healthThreshold = threshold; }
    public double getHealthThreshold() { return healthThreshold; }
}
