package com.github.hackclient.antidetect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for anti-cheat bypass utilities.
 */
class AntiCheatBypassTest {

    @Test
    void testRotationNoiseIsSmall() {
        // Noise should be small enough to not cause visible jitter
        for (int i = 0; i < 1000; i++) {
            float noised = AntiCheatBypass.addRotationNoise(90.0f);
            assertTrue(Math.abs(noised - 90.0f) < 5.0f,
                    "Rotation noise too large: " + (noised - 90.0f));
        }
    }

    @Test
    void testSmoothRotationConverges() {
        float current = 0;
        float target = 90;

        // Should converge toward target over multiple steps
        for (int i = 0; i < 20; i++) {
            current = AntiCheatBypass.smoothRotation(current, target, 0.5f);
        }

        assertTrue(Math.abs(current - target) < 5.0f,
                "Smooth rotation didn't converge: " + current);
    }

    @Test
    void testSmoothRotationNeverOvershoots() {
        float current = 0;
        float target = 45;

        for (int i = 0; i < 50; i++) {
            float next = AntiCheatBypass.smoothRotation(current, target, 0.3f);
            // Should generally move toward target, not past it by a large amount
            assertTrue(Math.abs(next - target) <= Math.abs(current - target) + 5.0f,
                    "Overshot: current=" + current + " next=" + next + " target=" + target);
            current = next;
        }
    }

    @Test
    void testMousePathHasCorrectLength() {
        float[][] path = AntiCheatBypass.generateMousePath(0, 0, 100, 100, 10);
        assertEquals(10, path.length);

        // Last point should be near the target
        assertTrue(Math.abs(path[9][0] - 100) < 5,
                "End X too far: " + path[9][0]);
        assertTrue(Math.abs(path[9][1] - 100) < 5,
                "End Y too far: " + path[9][1]);
    }

    @Test
    void testMousePathIsNotStraightLine() {
        float[][] path = AntiCheatBypass.generateMousePath(0, 0, 100, 100, 20);

        // Check that the path deviates from a straight line
        boolean hasDeviation = false;
        for (float[] point : path) {
            // On a straight line from (0,0) to (100,100), x should equal y
            if (Math.abs(point[0] - point[1]) > 3.0f) {
                hasDeviation = true;
                break;
            }
        }

        // Path should deviate from straight line (bezier curve + jitter)
        assertTrue(hasDeviation, "Mouse path is too straight - looks bot-like");
    }

    @Test
    void testShouldActProbability() {
        int acted = 0;
        int samples = 10000;

        for (int i = 0; i < samples; i++) {
            if (AntiCheatBypass.shouldActThisTick(0.5)) {
                acted++;
            }
        }

        double ratio = (double) acted / samples;
        assertTrue(ratio > 0.45 && ratio < 0.55,
                "Probability ratio off: " + ratio);
    }

    @Test
    void testPacketDelayVariance() {
        long base = 100;
        Set<Long> uniqueDelays = new java.util.HashSet<>();

        for (int i = 0; i < 100; i++) {
            uniqueDelays.add(AntiCheatBypass.getPacketDelay(base));
        }

        // Should produce many different delays (not all the same)
        assertTrue(uniqueDelays.size() > 20,
                "Not enough packet delay variance: " + uniqueDelays.size() + " unique values");
    }
}
