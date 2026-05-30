package com.github.hackclient.module.legacy;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * W-Tap - Automatically performs sprint-reset combos in 1.8 PvP.
 *
 * Rapidly toggles sprint off and back on between hits to get the sprint-hit
 * knockback bonus on every attack. The brief unsprint window (2-3 ticks) is
 * followed by a re-sprint, causing extra knockback on the next hit.
 *
 * Uses McReflect for sprint state manipulation and hit detection via
 * health monitoring of nearby targets.
 */
public class WTap extends Module {

    private boolean sprintResetActive = false;
    private long sprintOffTime = 0;
    private int ticksSinceHit = Integer.MAX_VALUE;
    private float lastTargetHealth = 0;
    private Object lastTarget = null;

    // Unsprint duration: 2-3 ticks (100-150ms), randomized per hit
    private long unsprintDurationMs = 100;

    public WTap() {
        super("WTap",
              "Auto sprint-reset for combo knockback in 1.8 PvP",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        sprintResetActive = false;
        ticksSinceHit = Integer.MAX_VALUE;
        lastTarget = null;
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();

        // Phase 1: Detect if we just landed a hit by monitoring closest target's health
        Object nearestTarget = findNearestTarget();
        boolean justLandedHit = false;

        if (nearestTarget != null) {
            float targetHealth = McReflect.getEntityHealth(nearestTarget);

            if (lastTarget != null && nearestTarget.equals(lastTarget)) {
                // Same target - check if health dropped (we hit them)
                if (targetHealth < lastTargetHealth && lastTargetHealth > 0) {
                    justLandedHit = true;
                }
            }

            lastTargetHealth = targetHealth;
            lastTarget = nearestTarget;
        } else {
            // No targets nearby, reset state
            sprintResetActive = false;
            lastTarget = null;
            return;
        }

        ticksSinceHit++;

        // Phase 2: On hit, initiate sprint reset (unsprint)
        if (justLandedHit) {
            ticksSinceHit = 0;

            if (!shouldAct(0.93)) return;

            sprintResetActive = true;
            McReflect.setSprinting(false);
            sprintOffTime = now;

            // Randomize unsprint duration: 2-3 ticks (100-150ms)
            unsprintDurationMs = 100 + ThreadLocalRandom.current().nextLong(50);
        }

        // Phase 3: Re-enable sprint after humanized delay
        if (sprintResetActive && now - sprintOffTime >= unsprintDurationMs) {
            if (shouldAct(0.95)) {
                McReflect.setSprinting(true);
                sprintResetActive = false;
                recordAction();
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Restore sprinting on disable
        if (sprintResetActive) {
            McReflect.setSprinting(true);
            sprintResetActive = false;
        }
    }

    private Object findNearestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object nearest = null;
        double nearestDist = 4.0; // slightly beyond attack range

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }

        return nearest;
    }
}
