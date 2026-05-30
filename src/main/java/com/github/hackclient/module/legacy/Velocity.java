package com.github.hackclient.module.legacy;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Velocity - Reduces knockback taken from hits in 1.8 PvP.
 *
 * Instead of fully canceling knockback (obvious to anti-cheat), this
 * reduces it by a randomized percentage each hit. The reduction varies
 * per hit to avoid statistical detection. Occasionally takes full KB
 * (1 in 12 hits) to appear legit.
 *
 * Uses McReflect to read current player velocity after taking a hit
 * and scale it down by the knockback multiplier.
 */
public class Velocity extends Module {

    private double horizontalReduction = 0.7;
    private double verticalReduction = 0.8;
    private static final double MAX_REDUCTION = 0.9;

    // Track velocity spikes to detect incoming knockback
    private double[] lastVelocity = {0, 0, 0};
    private int hitCounter = 0;
    private float lastHealth = 20.0f;

    public Velocity() {
        super("Velocity",
              "Reduces knockback with per-hit randomization",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.AVERAGE,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastVelocity = new double[]{0, 0, 0};
        hitCounter = 0;
        lastHealth = McReflect.getPlayerHealth();
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        // Detect if we just took a hit by checking health decrease or velocity spike
        float currentHealth = McReflect.getPlayerHealth();
        double[] currentVelocity = McReflect.getPlayerVelocity();

        boolean tookHit = currentHealth < lastHealth;

        // Also detect KB from velocity spike (sudden horizontal acceleration)
        double horizontalVelSq = currentVelocity[0] * currentVelocity[0]
                               + currentVelocity[2] * currentVelocity[2];
        double lastHorizontalVelSq = lastVelocity[0] * lastVelocity[0]
                                   + lastVelocity[2] * lastVelocity[2];
        boolean velocitySpike = horizontalVelSq > lastHorizontalVelSq * 4 && horizontalVelSq > 0.04;

        if (tookHit || velocitySpike) {
            hitCounter++;

            // Stealth-aware gating
            if (!shouldAct(0.90)) {
                lastHealth = currentHealth;
                lastVelocity = currentVelocity;
                return;
            }

            // Get the knockback multiplier for this hit
            double[] kbMult = getKnockbackMultiplier();

            // Apply the reduced knockback
            double newVx = currentVelocity[0] * kbMult[0];
            double newVy = currentVelocity[1] * kbMult[1];
            double newVz = currentVelocity[2] * kbMult[0]; // horizontal uses same multiplier

            McReflect.setPlayerVelocity(newVx, newVy, newVz);
            recordAction();
        }

        lastHealth = currentHealth;
        lastVelocity = currentVelocity;
    }

    /**
     * Get the knockback multiplier for this hit.
     * Returns values between 0 (full cancel) and 1 (no reduction).
     * Randomized per-hit to avoid detection.
     *
     * @return double[2] where [0] = horizontal multiplier, [1] = vertical multiplier
     */
    public double[] getKnockbackMultiplier() {
        // Occasionally take full knockback to appear legit (1 in 12 hits)
        if (ThreadLocalRandom.current().nextInt(12) == 0) {
            return new double[]{1.0, 1.0};
        }

        // Add Gaussian randomization to each hit's reduction
        double hVariance = ThreadLocalRandom.current().nextGaussian() * 0.1;
        double vVariance = ThreadLocalRandom.current().nextGaussian() * 0.075;

        double hMult = 1.0 - Math.min(MAX_REDUCTION, horizontalReduction + hVariance);
        double vMult = 1.0 - Math.min(MAX_REDUCTION, verticalReduction + vVariance);

        return new double[]{Math.max(0.05, hMult), Math.max(0.05, vMult)};
    }

    public void setReduction(double horizontal, double vertical) {
        this.horizontalReduction = Math.min(horizontal, MAX_REDUCTION);
        this.verticalReduction = Math.min(vertical, MAX_REDUCTION);
    }
}
