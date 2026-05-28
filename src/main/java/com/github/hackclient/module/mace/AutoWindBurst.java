package com.github.hackclient.module.mace;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Wind Burst - Triggers wind burst at optimal fall distance.
 *
 * Monitors the player's fall distance via McReflect.getPlayerFallDistance()
 * and triggers the wind burst attack when falling 6+ blocks with a
 * randomized variance of +/-0.75 blocks. The trigger point varies each
 * time to avoid pattern detection.
 *
 * Uses McReflect for fall distance monitoring, velocity checks, player
 * scanning for targets, and attack execution.
 */
public class AutoWindBurst extends Module {

    private long lastBurstTime = 0;
    private boolean burstArmed = false;
    private double triggerDistance = 6.0;

    private static final double MIN_FALL_DISTANCE = 3.0;
    private static final double OPTIMAL_FALL_DISTANCE = 6.0;
    private static final double TRIGGER_VARIANCE = 0.75;

    public AutoWindBurst() {
        super("AutoWindBurst",
              "Triggers wind burst at optimal fall distance for max damage",
              GameMode.MACE,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        burstArmed = false;
        randomizeTriggerDistance();
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();
        if (now - lastBurstTime < delay) return;

        // Check if player is falling (negative Y velocity)
        double[] velocity = McReflect.getPlayerVelocity();
        boolean isFalling = velocity[1] < -0.1;

        if (!isFalling) {
            // Reset arm when player lands or stops falling
            if (burstArmed) {
                burstArmed = false;
                randomizeTriggerDistance();
            }
            return;
        }

        float fallDistance = McReflect.getPlayerFallDistance();

        // Arm the burst when we pass minimum fall distance
        if (fallDistance >= MIN_FALL_DISTANCE && !burstArmed) {
            burstArmed = true;
        }

        // Trigger wind burst at the randomized optimal distance
        if (burstArmed && fallDistance >= triggerDistance) {
            if (!shouldAct(0.85)) return;

            // Find closest target below us to attack
            Object target = findTargetBelow();
            if (target != null) {
                McReflect.attackEntity(target);
                McReflect.swingHand();
            } else {
                // Even without a target, swing (wind burst is area effect)
                McReflect.swingHand();
            }

            lastBurstTime = now;
            burstArmed = false;
            recordAction();

            // Re-randomize trigger distance for next fall
            randomizeTriggerDistance();
        }
    }

    /**
     * Find the closest player target that is below or at our level.
     */
    private Object findTargetBelow() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        double playerY = McReflect.getPlayerY();
        Object best = null;
        double bestDist = 8.0; // wind burst effective range

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double entityY = McReflect.getEntityY(entity);
            // Target must be below or at our level
            if (entityY > playerY + 1.0) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }

        return best;
    }

    /**
     * Randomize the trigger distance around the optimal value with Gaussian variance.
     */
    private void randomizeTriggerDistance() {
        triggerDistance = OPTIMAL_FALL_DISTANCE
                + ThreadLocalRandom.current().nextGaussian() * TRIGGER_VARIANCE;
        triggerDistance = Math.max(MIN_FALL_DISTANCE + 1.0, triggerDistance);
    }
}
