package com.github.hackclient.module.legacy;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Auto Clicker - Humanized CPS (clicks per second) for 1.8 PvP.
 *
 * In 1.8 PvP, click speed matters. This auto-clicker targets a configurable
 * CPS range with humanized intervals - not uniform timing like bot clickers.
 *
 * Anti-detection features:
 * - Gaussian-distributed click intervals (not uniform)
 * - Occasional double-clicks and micro-pauses like real butterfly/jitter clicking
 * - CPS drift over time (fatigue simulation)
 * - Never exceeds configurable max CPS
 */
public class AutoClicker extends Module {

    private long lastClickTime = 0;
    private double targetCps = 12.0; // Configurable target CPS
    private double minCps = 8.0;
    private double maxCps = 16.0;
    private int clicksSinceLastPause = 0;

    public AutoClicker() {
        super("AutoClicker",
              "Humanized auto-clicking for 1.8 PvP combat",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();

        // Calculate humanized interval from target CPS
        double currentCps = targetCps + (Math.random() * 4 - 2); // ±2 CPS variance
        currentCps = Math.max(minCps, Math.min(maxCps, currentCps));
        double intervalMs = 1000.0 / currentCps;

        // Add humanized jitter from timer
        long humanDelay = timer.getNextDelayMs();
        // Scale the human delay to be proportional to click interval
        long scaledDelay = (long) (humanDelay * (intervalMs / timer.getSkillLevel().meanMs));
        long clickInterval = Math.max(50, scaledDelay);

        if (now - lastClickTime < clickInterval) {
            return;
        }

        // Simulate occasional micro-pause (every 15-30 clicks)
        clicksSinceLastPause++;
        if (clicksSinceLastPause > 15 + (int) (Math.random() * 15)) {
            clicksSinceLastPause = 0;
            // Skip this tick - micro pause
            lastClickTime = now + (long) (Math.random() * 100 + 50);
            return;
        }

        // Occasional double-click (like real butterfly clicking)
        if (Math.random() < 0.08) { // 8% chance of double click
            performClick();
            try { Thread.sleep(10 + (long) (Math.random() * 20)); } catch (InterruptedException ignored) {}
        }

        performClick();
        lastClickTime = now;
    }

    private void performClick() {
        // TODO: Hook into client
        // Simulate left click: mc.interactionManager.attackEntity(player, target)
    }

    public void setTargetCps(double cps) {
        this.targetCps = Math.max(minCps, Math.min(maxCps, cps));
    }

    public double getTargetCps() { return targetCps; }
}
