package com.github.hackclient.module.combat;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Instant Pot Macro - Automatically throws splash healing potions when health is low.
 *
 * Works across all game modes. Detects low health and:
 * 1. Switches to splash healing potion in hotbar
 * 2. Looks straight down (splash pots need to land at your feet)
 * 3. Throws the potion
 * 4. Restores previous pitch/yaw and hotbar slot
 *
 * Anti-detection: Humanized delays between slot switch → look down → throw → restore.
 * Varies the look-down angle slightly and adds rotation noise.
 */
public class InstantPot extends Module {

    private long lastPotTime = 0;
    private int potStage = 0;
    // Stages: 0=monitoring, 1=switch slot, 2=look down, 3=throw, 4=restore

    private float savedPitch = 0;
    private float savedYaw = 0;
    private int savedSlot = 0;
    private float currentPitch = 0;

    // Configurable
    private double healthThreshold = 10.0;    // Pot when health drops below this (out of 20)
    private double criticalThreshold = 4.0;   // Emergency instant pot
    private long potCooldownMs = 800;         // Min time between pots

    public InstantPot() {
        super("InstantPot",
              "Auto-throws splash healing potions when health is low",
              GameMode.MACE, // Registered for MACE but works in any mode
              HumanizedTimer.SkillLevel.EXPERT);
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();

        switch (potStage) {
            case 0: // Monitor health
                if (now - lastPotTime < potCooldownMs) return;

                double health = getPlayerHealth();
                boolean critical = health <= criticalThreshold;
                boolean shouldPot = health <= healthThreshold && hasSplashHealingPot();

                if (shouldPot) {
                    // Critical health = faster reaction (reduced delay)
                    if (critical || AntiCheatBypass.shouldActThisTick(0.93)) {
                        // Save current state
                        savedPitch = getPlayerPitch();
                        savedYaw = getPlayerYaw();
                        savedSlot = getCurrentHotbarSlot();
                        currentPitch = savedPitch;
                        potStage = 1;
                    }
                }
                break;

            case 1: // Switch to potion slot
                if (AntiCheatBypass.shouldActThisTick(0.96)) {
                    int potSlot = findSplashHealingPotSlot();
                    if (potSlot >= 0) {
                        switchHotbarSlot(potSlot);
                        potStage = 2;
                        lastPotTime = now;
                    } else {
                        potStage = 0; // No pot found, abort
                    }
                }
                break;

            case 2: // Look down (splash pots land at feet)
                float targetPitch = 88.0f + AntiCheatBypass.addRotationNoise(0);
                currentPitch = AntiCheatBypass.smoothRotation(currentPitch, targetPitch, 0.65f);
                applyPitch(currentPitch);

                if (Math.abs(currentPitch - targetPitch) < 3.0f) {
                    potStage = 3;
                }
                break;

            case 3: // Throw potion
                if (AntiCheatBypass.shouldActThisTick(0.97)) {
                    useItem(); // Right click to throw
                    potStage = 4;
                    lastPotTime = now;
                }
                break;

            case 4: // Restore previous state
                if (AntiCheatBypass.shouldActThisTick(0.9)) {
                    // Smooth rotate back to original view
                    currentPitch = AntiCheatBypass.smoothRotation(currentPitch, savedPitch, 0.5f);
                    applyPitch(currentPitch);

                    if (Math.abs(currentPitch - savedPitch) < 5.0f) {
                        switchHotbarSlot(savedSlot);
                        potStage = 0;
                    }
                }
                break;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        potStage = 0;
    }

    // --- Stub methods ---
    private double getPlayerHealth() { return 20.0; }
    private float getPlayerPitch() { return 0; }
    private float getPlayerYaw() { return 0; }
    private int getCurrentHotbarSlot() { return 0; }
    private boolean hasSplashHealingPot() { return false; }
    private int findSplashHealingPotSlot() { return -1; }
    private void switchHotbarSlot(int slot) { /* TODO */ }
    private void applyPitch(float pitch) { /* TODO */ }
    private void useItem() { /* TODO: right click */ }

    // Configuration
    public void setHealthThreshold(double threshold) { this.healthThreshold = threshold; }
    public void setCriticalThreshold(double threshold) { this.criticalThreshold = threshold; }
}
