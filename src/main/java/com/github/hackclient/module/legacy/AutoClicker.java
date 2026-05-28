package com.github.hackclient.module.legacy;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Clicker - Humanized CPS (clicks per second) for 1.8 PvP.
 *
 * In 1.8 PvP, click speed matters. This auto-clicker targets a configurable
 * CPS range with humanized intervals - not uniform timing like bot clickers.
 *
 * Anti-detection features:
 * - Gaussian-distributed click intervals (not uniform)
 * - Occasional double-clicks simulating real butterfly clicking
 * - Micro-pauses every 15-30 clicks like human fatigue
 * - CPS drift over time (fatigue simulation)
 * - Stealth engine integration for adaptive gating
 */
public class AutoClicker extends Module {

    private long lastClickTime = 0;
    private double targetCps = 12.0;
    private double minCps = 8.0;
    private double maxCps = 16.0;
    private int clicksSinceLastPause = 0;
    private int pauseThreshold = 0;
    private boolean inMicroPause = false;
    private long microPauseEnd = 0;

    // Double-click tracking
    private boolean pendingDoubleClick = false;
    private long doubleClickTime = 0;

    public AutoClicker() {
        super("AutoClicker",
              "Humanized auto-clicking for 1.8 PvP combat",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
        resetPauseThreshold();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clicksSinceLastPause = 0;
        inMicroPause = false;
        pendingDoubleClick = false;
        resetPauseThreshold();
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();

        // Handle micro-pause state
        if (inMicroPause) {
            if (now < microPauseEnd) return;
            inMicroPause = false;
            clicksSinceLastPause = 0;
            resetPauseThreshold();
        }

        // Handle pending double-click (butterfly clicking simulation)
        if (pendingDoubleClick && now >= doubleClickTime) {
            pendingDoubleClick = false;
            performClick();
            return;
        }
        if (pendingDoubleClick) return;

        // Determine effective CPS from stealth engine
        double effectiveCps = targetCps;
        StealthEngine stealth = StealthEngine.getInstance();
        if (stealth != null) {
            effectiveCps = Math.min(effectiveCps, stealth.getMaxCps());
        }

        // Gaussian-distributed CPS variance (not uniform random)
        double currentCps = effectiveCps + ThreadLocalRandom.current().nextGaussian() * 1.5;
        currentCps = Math.max(minCps, Math.min(maxCps, currentCps));

        // Convert CPS to interval and scale with humanized timer
        double intervalMs = 1000.0 / currentCps;
        long humanDelay = timer.getNextDelayMs();
        long scaledDelay = (long) (humanDelay * (intervalMs / timer.getSkillLevel().meanMs));
        long clickInterval = Math.max(50, scaledDelay);

        if (now - lastClickTime < clickInterval) return;

        // Stealth-aware action gating
        if (!shouldAct(0.92)) return;

        // Check for micro-pause (every 15-30 clicks, like real human fatigue)
        clicksSinceLastPause++;
        if (clicksSinceLastPause >= pauseThreshold) {
            inMicroPause = true;
            // Pause duration: 50-150ms, Gaussian centered on 100ms
            long pauseDuration = 50 + (long) Math.abs(ThreadLocalRandom.current().nextGaussian() * 35);
            microPauseEnd = now + pauseDuration;
            return;
        }

        // Find nearest target to attack
        Object target = findNearestTarget();
        if (target == null) return;

        // Perform the click/attack
        performClickOnTarget(target);
        lastClickTime = now;
        recordAction();

        // Occasional double-click simulating butterfly clicking (8% chance)
        if (ThreadLocalRandom.current().nextDouble() < 0.08) {
            pendingDoubleClick = true;
            // Double-click delay: 10-30ms after first click
            doubleClickTime = now + 10 + ThreadLocalRandom.current().nextLong(20);
        }
    }

    private Object findNearestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object nearest = null;
        double nearestDist = 3.0; // vanilla 1.8 reach

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

    private void performClickOnTarget(Object target) {
        McReflect.attackEntity(target);
        McReflect.swingHand();
    }

    private void performClick() {
        Object target = findNearestTarget();
        if (target != null) {
            performClickOnTarget(target);
            recordAction();
        }
    }

    private void resetPauseThreshold() {
        // 15-30 clicks between micro-pauses
        pauseThreshold = 15 + ThreadLocalRandom.current().nextInt(16);
    }

    public void setTargetCps(double cps) {
        this.targetCps = Math.max(minCps, Math.min(maxCps, cps));
    }

    public double getTargetCps() { return targetCps; }
}
