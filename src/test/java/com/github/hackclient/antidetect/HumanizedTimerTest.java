package com.github.hackclient.antidetect;

import com.github.hackclient.antidetect.HumanizedTimer.SkillLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the HumanizedTimer anti-detection system.
 *
 * These tests verify that the timing distribution looks human-like
 * and would not be flagged by common anti-cheat statistical analysis.
 */
class HumanizedTimerTest {

    /**
     * Test that delays are never negative and stay within bounds.
     */
    @Test
    void testDelaysWithinBounds() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED, 50, 500);

        for (int i = 0; i < 1000; i++) {
            long delay = timer.getNextDelayMs();
            assertTrue(delay >= 50, "Delay should be >= min: got " + delay);
            assertTrue(delay <= 500, "Delay should be <= max: got " + delay);
        }
    }

    /**
     * Test that the mean delay is close to the skill level's configured mean.
     * Anti-cheat systems check if average reaction time matches human norms.
     */
    @Test
    void testMeanDelayMatchesSkillLevel() {
        for (SkillLevel level : SkillLevel.values()) {
            HumanizedTimer timer = new HumanizedTimer(level, 30, 800);
            double sum = 0;
            int samples = 5000;

            for (int i = 0; i < samples; i++) {
                sum += timer.getNextDelayMs();
            }

            double mean = sum / samples;
            // Mean should be within 40% of configured mean (burst/rest shifts it)
            double tolerance = level.meanMs * 0.4;
            assertTrue(Math.abs(mean - level.meanMs) < tolerance,
                    String.format("Mean %.1f too far from expected %.1f for %s",
                            mean, level.meanMs, level.name()));
        }
    }

    /**
     * Test that delays have sufficient variance (not too uniform).
     * Anti-cheat flags low-variance timing as bot-like.
     */
    @Test
    void testSufficientVariance() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED, 50, 500);
        double[] delays = new double[1000];
        double sum = 0;

        for (int i = 0; i < delays.length; i++) {
            delays[i] = timer.getNextDelayMs();
            sum += delays[i];
        }

        double mean = sum / delays.length;
        double varianceSum = 0;
        for (double d : delays) {
            varianceSum += (d - mean) * (d - mean);
        }
        double stdDev = Math.sqrt(varianceSum / delays.length);

        // Standard deviation should be at least 10ms (humans have high variance)
        assertTrue(stdDev > 10, "Standard deviation too low: " + stdDev +
                " - timing looks bot-like");
    }

    /**
     * Test that no two consecutive delays are identical.
     * Repeated identical intervals are the #1 bot detection signal.
     */
    @Test
    void testNoConsecutiveDuplicates() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED);
        long prev = -1;
        int duplicates = 0;

        for (int i = 0; i < 2000; i++) {
            long delay = timer.getNextDelayMs();
            if (delay == prev) {
                duplicates++;
            }
            prev = delay;
        }

        // Allow at most 1% duplicates (rounding can cause rare collisions)
        assertTrue(duplicates < 20,
                "Too many consecutive duplicates: " + duplicates + " - easily detectable");
    }

    /**
     * Test that the delay distribution is approximately Gaussian (normal).
     * Human reaction times follow a normal distribution, not uniform.
     * Anti-cheat systems run normality tests on timing data.
     */
    @Test
    void testDistributionIsApproximatelyGaussian() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.AVERAGE, 50, 600);
        int samples = 3000;
        double[] delays = new double[samples];
        double sum = 0;

        for (int i = 0; i < samples; i++) {
            delays[i] = timer.getNextDelayMs();
            sum += delays[i];
        }

        double mean = sum / samples;

        // Count samples within 1 and 2 standard deviations
        double varianceSum = 0;
        for (double d : delays) {
            varianceSum += (d - mean) * (d - mean);
        }
        double stdDev = Math.sqrt(varianceSum / samples);

        int within1Sd = 0, within2Sd = 0;
        for (double d : delays) {
            if (Math.abs(d - mean) <= stdDev) within1Sd++;
            if (Math.abs(d - mean) <= 2 * stdDev) within2Sd++;
        }

        double pct1Sd = (double) within1Sd / samples;
        double pct2Sd = (double) within2Sd / samples;

        // For normal distribution: ~68% within 1 SD, ~95% within 2 SD
        // We allow wider bounds because burst/rest modifies the distribution
        assertTrue(pct1Sd > 0.45 && pct1Sd < 0.85,
                "Distribution not Gaussian-like. Within 1 SD: " + (pct1Sd * 100) + "%");
        assertTrue(pct2Sd > 0.80,
                "Distribution not Gaussian-like. Within 2 SD: " + (pct2Sd * 100) + "%");
    }

    /**
     * Test that the session reset actually resets fatigue and counters.
     */
    @Test
    void testSessionReset() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED);

        // Generate some delays to build up state
        for (int i = 0; i < 100; i++) {
            timer.getNextDelayMs();
        }

        assertEquals(100, timer.getActionCount());

        timer.resetSession();
        assertEquals(0, timer.getActionCount());
    }

    /**
     * Test that different skill levels produce distinctly different timing.
     * BEGINNER should be significantly slower than EXPERT.
     */
    @Test
    void testSkillLevelsAreDifferent() {
        Map<SkillLevel, Double> means = new EnumMap<>(SkillLevel.class);
        int samples = 2000;

        for (SkillLevel level : SkillLevel.values()) {
            HumanizedTimer timer = new HumanizedTimer(level, 30, 800);
            double sum = 0;
            for (int i = 0; i < samples; i++) {
                sum += timer.getNextDelayMs();
            }
            means.put(level, sum / samples);
        }

        // Each skill level should be faster than the previous
        assertTrue(means.get(SkillLevel.BEGINNER) > means.get(SkillLevel.AVERAGE),
                "Beginner should be slower than average");
        assertTrue(means.get(SkillLevel.AVERAGE) > means.get(SkillLevel.SKILLED),
                "Average should be slower than skilled");
        assertTrue(means.get(SkillLevel.SKILLED) > means.get(SkillLevel.EXPERT),
                "Skilled should be slower than expert");
    }

    /**
     * Test anti-pattern detection: consecutive delays shouldn't be too similar.
     * Checks that the minimum gap between consecutive delays is reasonable.
     */
    @Test
    void testAntiPatternVariance() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED);
        long prev = timer.getNextDelayMs();
        int tooCloseCount = 0;

        for (int i = 0; i < 500; i++) {
            long current = timer.getNextDelayMs();
            if (Math.abs(current - prev) < 3) {
                tooCloseCount++;
            }
            prev = current;
        }

        // Less than 5% of consecutive pairs should be within 3ms of each other
        assertTrue(tooCloseCount < 25,
                "Too many similar consecutive delays: " + tooCloseCount);
    }
}
