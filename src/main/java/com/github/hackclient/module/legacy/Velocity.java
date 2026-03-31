package com.github.hackclient.module.legacy;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Velocity - Reduces knockback taken from hits in 1.8 PvP.
 *
 * Instead of fully canceling knockback (obvious to anti-cheat), this
 * reduces it by a randomized percentage each hit. The reduction varies
 * per hit to avoid statistical detection.
 */
public class Velocity extends Module {

    private double horizontalReduction = 0.7; // 70% reduction base
    private double verticalReduction = 0.8;   // 80% reduction base
    private static final double MAX_REDUCTION = 0.9;

    public Velocity() {
        super("Velocity",
              "Reduces knockback with per-hit randomization",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.AVERAGE);
    }

    @Override
    public void onTick() {
        // Velocity modification is applied per-hit via packet handling
    }

    /**
     * Get the knockback multiplier for this hit.
     * Returns values between 0 (full cancel) and 1 (no reduction).
     * Randomized per-hit to avoid detection.
     */
    public double[] getKnockbackMultiplier() {
        // Add randomization to each hit's reduction
        double hVariance = (Math.random() * 0.2 - 0.1); // ±10%
        double vVariance = (Math.random() * 0.15 - 0.075); // ±7.5%

        double hMult = 1.0 - Math.min(MAX_REDUCTION, horizontalReduction + hVariance);
        double vMult = 1.0 - Math.min(MAX_REDUCTION, verticalReduction + vVariance);

        // Occasionally take full knockback to appear legit (1 in 12 hits)
        if (Math.random() < 0.083) {
            return new double[]{1.0, 1.0};
        }

        return new double[]{Math.max(0.05, hMult), Math.max(0.05, vMult)};
    }

    public void setReduction(double horizontal, double vertical) {
        this.horizontalReduction = Math.min(horizontal, MAX_REDUCTION);
        this.verticalReduction = Math.min(vertical, MAX_REDUCTION);
    }
}
