package com.github.hackclient.module.legacy;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Reach - Extends hit reach distance for 1.8 PvP.
 *
 * Vanilla reach is 3.0 blocks. This extends it slightly with randomization
 * so each hit has a different effective reach, making it very hard
 * for server-side anti-cheat to flag a consistent pattern.
 */
public class Reach extends Module {

    private double baseReach = 3.0;
    private double extraReach = 0.3; // Conservative extra - less detectable
    private double maxExtraReach = 0.5;

    public Reach() {
        super("Reach",
              "Slightly extends attack reach with per-hit randomization",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.AVERAGE);
    }

    @Override
    public void onTick() {
        // Reach is applied per-hit via getEffectiveReach(), not per-tick
    }

    /**
     * Get the effective reach for this hit.
     * Randomized each call to prevent pattern detection.
     */
    public double getEffectiveReach() {
        // Gaussian random around the extra reach value
        double variance = extraReach * 0.4;
        double thisHitExtra = extraReach + (Math.random() * variance * 2 - variance);
        thisHitExtra = Math.max(0, Math.min(maxExtraReach, thisHitExtra));

        return baseReach + thisHitExtra;
    }

    public void setExtraReach(double extra) {
        this.extraReach = Math.min(extra, maxExtraReach);
    }
}
