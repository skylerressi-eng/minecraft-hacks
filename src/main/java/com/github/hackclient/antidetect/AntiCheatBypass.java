package com.github.hackclient.antidetect;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Collection of anti-cheat bypass strategies.
 *
 * These methods make automated actions look natural by:
 * - Randomizing packet timing
 * - Adding noise to rotation angles
 * - Simulating natural mouse movement curves
 * - Throttling actions to stay within server rate limits
 */
public class AntiCheatBypass {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    // Rate limiting: track actions per second
    private int actionsThisTick = 0;
    private long lastTickTime = 0;
    private static final int MAX_ACTIONS_PER_TICK = 1; // conservative: 1 action per tick

    /**
     * Add natural-looking noise to a rotation angle (yaw or pitch).
     * Real mouse movements are never pixel-perfect.
     *
     * @param angle The target angle
     * @return The angle with human-like imprecision added
     */
    public static float addRotationNoise(float angle) {
        // Gaussian noise: most of the time very small, occasionally larger
        float noise = (float) (RANDOM.nextGaussian() * 0.8);
        return angle + noise;
    }

    /**
     * Smooth a rotation change to look like natural mouse movement.
     * Snapping instantly to a target angle is a dead giveaway.
     *
     * @param current Current angle
     * @param target Target angle
     * @param speed Smoothing speed (0.0-1.0, lower = smoother)
     * @return The next intermediate angle
     */
    public static float smoothRotation(float current, float target, float speed) {
        float diff = wrapAngle(target - current);
        float step = diff * clamp(speed + (float) (RANDOM.nextGaussian() * 0.05f), 0.1f, 1.0f);
        return current + step;
    }

    /**
     * Check if we can perform an action this tick without triggering rate limits.
     */
    public boolean canActThisTick() {
        long now = System.currentTimeMillis();
        if (now - lastTickTime >= 50) { // New tick (50ms = 1 game tick at 20 TPS)
            actionsThisTick = 0;
            lastTickTime = now;
        }
        return actionsThisTick < MAX_ACTIONS_PER_TICK;
    }

    /**
     * Record that an action was performed this tick.
     */
    public void recordAction() {
        actionsThisTick++;
    }

    /**
     * Simulate natural mouse movement path using bezier-like interpolation.
     * Returns intermediate points between start and end positions.
     *
     * @param startX Start X position
     * @param startY Start Y position
     * @param endX End X position
     * @param endY End Y position
     * @param steps Number of intermediate steps
     * @return Array of [x, y] positions along the path
     */
    public static float[][] generateMousePath(float startX, float startY,
                                               float endX, float endY, int steps) {
        float[][] path = new float[steps][2];

        // Random control point for bezier curve (simulates natural hand movement arc)
        float cpX = (startX + endX) / 2 + (float) (RANDOM.nextGaussian() * 20);
        float cpY = (startY + endY) / 2 + (float) (RANDOM.nextGaussian() * 20);

        for (int i = 0; i < steps; i++) {
            float t = (float) (i + 1) / steps;
            // Add slight speed variation (fast start, slow end like real mouse)
            float easedT = easeOutCubic(t);

            // Quadratic bezier interpolation
            float invT = 1 - easedT;
            path[i][0] = invT * invT * startX + 2 * invT * easedT * cpX + easedT * easedT * endX;
            path[i][1] = invT * invT * startY + 2 * invT * easedT * cpY + easedT * easedT * endY;

            // Add micro-jitter to each point
            path[i][0] += (float) (RANDOM.nextGaussian() * 0.3);
            path[i][1] += (float) (RANDOM.nextGaussian() * 0.3);
        }

        return path;
    }

    /**
     * Determine if we should skip this tick to appear more human.
     * Humans don't act on every single game tick.
     *
     * @param probability Probability of acting (0.0-1.0)
     * @return true if we should act this tick
     */
    public static boolean shouldActThisTick(double probability) {
        return RANDOM.nextDouble() < probability;
    }

    /**
     * Get a randomized packet delay to avoid sending packets at exact intervals.
     *
     * @param baseDelayMs The base delay between packets
     * @return A randomized delay
     */
    public static long getPacketDelay(long baseDelayMs) {
        double variance = baseDelayMs * 0.2; // ±20% variance
        return Math.max(1, Math.round(baseDelayMs + RANDOM.nextGaussian() * variance));
    }

    private static float easeOutCubic(float t) {
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    private static float wrapAngle(float angle) {
        angle = angle % 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
