package com.github.hackclient.module.legacy;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reach - Extends hit reach distance for 1.8 PvP.
 *
 * Vanilla reach is 3.0 blocks. This extends it slightly using the stealth
 * engine's maxReachExtra value, with per-hit randomization so each attack
 * has a different effective reach. Only targets entities within the extended
 * range, making it very hard for server-side anti-cheat to flag.
 *
 * Uses McReflect to scan for players slightly beyond vanilla range and
 * attack them when stealth conditions permit.
 */
public class Reach extends Module {

    private static final double BASE_REACH = 3.0;
    private long lastAttackTime = 0;

    public Reach() {
        super("Reach",
              "Slightly extends attack reach with per-hit randomization",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        if (!isTimerReady()) return;
        if (!shouldAct(0.88)) return;

        // Get the stealth-aware effective reach for this tick
        double effectiveReach = getEffectiveReach();

        // Only operate when we have extra reach beyond vanilla
        if (effectiveReach <= BASE_REACH) return;

        // Find a target in the extended range zone (beyond vanilla but within our reach)
        Object target = findTargetInExtendedRange(effectiveReach);
        if (target == null) return;

        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();
        if (now - lastAttackTime < delay) return;

        // Attack the target that is beyond vanilla reach
        McReflect.attackEntity(target);
        McReflect.swingHand();
        lastAttackTime = now;
        recordAction();
    }

    /**
     * Get the effective reach for this hit.
     * Queries the stealth engine for the safe max extra reach,
     * then applies Gaussian randomization per-hit to prevent pattern detection.
     */
    public double getEffectiveReach() {
        double maxExtra = 0.3; // default conservative extra

        StealthEngine stealth = StealthEngine.getInstance();
        if (stealth != null) {
            maxExtra = stealth.getMaxReachExtra();
        }

        if (maxExtra <= 0) return BASE_REACH;

        // Gaussian random around the extra reach value
        double variance = maxExtra * 0.4;
        double thisHitExtra = maxExtra + ThreadLocalRandom.current().nextGaussian() * variance;
        thisHitExtra = Math.max(0, Math.min(maxExtra, thisHitExtra));

        return BASE_REACH + thisHitExtra;
    }

    /**
     * Find a target that is beyond vanilla reach but within our extended reach.
     * This is the sweet spot where Reach provides value without being obvious.
     */
    private Object findTargetInExtendedRange(double effectiveReach) {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object bestTarget = null;
        double bestDist = effectiveReach + 1;

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);

            // Target entities within our extended reach
            if (dist <= effectiveReach && dist < bestDist) {
                bestDist = dist;
                bestTarget = entity;
            }
        }

        return bestTarget;
    }
}
