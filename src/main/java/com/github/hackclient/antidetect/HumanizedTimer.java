package com.github.hackclient.antidetect;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Anti-detection timing system that mimics human reaction times.
 *
 * Uses a combination of Gaussian distribution, micro-jitter, fatigue simulation,
 * and burst/rest patterns to produce timing that is statistically indistinguishable
 * from real human input.
 *
 * Key anti-detection features:
 * - Gaussian-distributed base reaction times (not uniform random)
 * - Micro-jitter simulating muscle twitch variance (±5-15ms)
 * - Fatigue simulation: reaction times slowly drift over play sessions
 * - Burst/rest pattern: occasional faster/slower sequences like real players
 * - Configurable skill level that shifts the entire distribution
 * - No two consecutive actions have the same delay
 */
public class HumanizedTimer {

    /** Skill levels shift the base reaction time distribution */
    public enum SkillLevel {
        BEGINNER(280, 60),    // Mean 280ms, stddev 60ms
        AVERAGE(210, 45),     // Mean 210ms, stddev 45ms
        SKILLED(160, 35),     // Mean 160ms, stddev 35ms
        EXPERT(120, 25),      // Mean 120ms, stddev 25ms
        LEGIT_RAGE(90, 15);   // Mean 90ms, stddev 15ms - fast but still human-like

        public final double meanMs;
        public final double stdDevMs;

        SkillLevel(double meanMs, double stdDevMs) {
            this.meanMs = meanMs;
            this.stdDevMs = stdDevMs;
        }
    }

    private final Random random;
    private final SkillLevel skillLevel;
    private final double minDelayMs;
    private final double maxDelayMs;

    // Fatigue simulation state
    private long sessionStartTime;
    private int actionCount;
    private double fatigueFactor;

    // Burst/rest pattern state
    private int burstCounter;
    private boolean inBurstMode;
    private int burstLength;
    private int restLength;
    private int restCounter;

    // Anti-pattern detection: track last few delays to avoid repetition
    private final double[] recentDelays = new double[5];
    private int recentIndex = 0;

    public HumanizedTimer(SkillLevel skillLevel) {
        this(skillLevel, 50, 500);
    }

    public HumanizedTimer(SkillLevel skillLevel, double minDelayMs, double maxDelayMs) {
        this.random = ThreadLocalRandom.current();
        this.skillLevel = skillLevel;
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.sessionStartTime = System.currentTimeMillis();
        this.actionCount = 0;
        this.fatigueFactor = 1.0;
        this.burstCounter = 0;
        this.inBurstMode = false;
        this.burstLength = 3 + random.nextInt(5); // 3-7 actions in burst
        this.restLength = 1 + random.nextInt(3);  // 1-3 slower actions
        this.restCounter = 0;
    }

    /**
     * Get the next humanized delay in milliseconds.
     * Each call produces a unique, human-like delay.
     */
    public long getNextDelayMs() {
        double delay = calculateBaseDelay();
        delay = applyMicroJitter(delay);
        delay = applyFatigueEffect(delay);
        delay = applyBurstRestPattern(delay);
        delay = applyAntiPatternVariance(delay);
        delay = clamp(delay, minDelayMs, maxDelayMs);

        // Track for anti-pattern
        recentDelays[recentIndex % recentDelays.length] = delay;
        recentIndex++;
        actionCount++;

        return Math.round(delay);
    }

    /**
     * Gaussian-distributed base delay centered on skill level mean.
     */
    private double calculateBaseDelay() {
        return skillLevel.meanMs + (random.nextGaussian() * skillLevel.stdDevMs);
    }

    /**
     * Micro-jitter: small random fluctuations simulating motor control variance.
     * Real humans have ±5-15ms variance from muscle micro-movements.
     */
    private double applyMicroJitter(double delay) {
        double jitter = (random.nextGaussian() * 8.0); // ±~8ms standard
        return delay + jitter;
    }

    /**
     * Fatigue: reaction times gradually increase over long sessions.
     * Real humans slow down ~5-15% over 30+ minutes of intense play.
     */
    private double applyFatigueEffect(double delay) {
        long sessionDurationMs = System.currentTimeMillis() - sessionStartTime;
        double minutesPlayed = sessionDurationMs / 60000.0;

        // Fatigue kicks in after 10 minutes, maxes at ~15% slower after 60 min
        if (minutesPlayed > 10) {
            fatigueFactor = 1.0 + Math.min(0.15, (minutesPlayed - 10) * 0.003);
        }

        return delay * fatigueFactor;
    }

    /**
     * Burst/rest pattern: humans naturally have periods of faster reactions
     * (focused/engaged) followed by slightly slower periods (micro-rest).
     */
    private double applyBurstRestPattern(double delay) {
        if (inBurstMode) {
            burstCounter++;
            delay *= 0.85 + (random.nextDouble() * 0.1); // 85-95% speed during burst
            if (burstCounter >= burstLength) {
                inBurstMode = false;
                restCounter = 0;
                restLength = 1 + random.nextInt(3);
            }
        } else {
            restCounter++;
            delay *= 1.05 + (random.nextDouble() * 0.15); // 105-120% during rest
            if (restCounter >= restLength) {
                inBurstMode = true;
                burstCounter = 0;
                burstLength = 3 + random.nextInt(5);
            }
        }
        return delay;
    }

    /**
     * Anti-pattern: ensure no two consecutive delays are too similar.
     * Anti-cheat systems flag suspiciously uniform timing.
     */
    private double applyAntiPatternVariance(double delay) {
        if (recentIndex > 0) {
            double lastDelay = recentDelays[(recentIndex - 1) % recentDelays.length];
            double diff = Math.abs(delay - lastDelay);

            // If too close to last delay, nudge it
            if (diff < 10) {
                delay += (random.nextBoolean() ? 1 : -1) * (15 + random.nextDouble() * 20);
            }
        }
        return delay;
    }

    /** Reset session state (call when game mode changes or player rejoins) */
    public void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        actionCount = 0;
        fatigueFactor = 1.0;
        burstCounter = 0;
        inBurstMode = false;
        recentIndex = 0;
    }

    public int getActionCount() {
        return actionCount;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
