package com.github.hackclient.module.mace;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Shield Swap - Automatically toggles shield on/off during combat.
 *
 * Detects incoming attacks by monitoring nearby players' proximity and
 * velocity to determine threat direction. Raises shield to block,
 * then quickly lowers it for a counter-attack window.
 *
 * Flow:
 * 1. Monitor nearby players for incoming threats
 * 2. When enemy is within attack range and approaching, raise shield (block)
 * 3. Hold block for a brief humanized duration
 * 4. Drop shield and counter-attack during recovery window
 *
 * Uses McReflect for player scanning, distance/velocity tracking, and sprint
 * state to simulate shield blocking (holding right-click equivalent).
 */
public class AutoShieldSwap extends Module {

    private long lastSwapTime = 0;
    private boolean isBlocking = false;
    private long blockStartTime = 0;

    // How long to hold the shield block (ticks)
    private long blockDurationMs = 300;
    private long counterWindowMs = 200;

    // Threat tracking
    private Object lastThreat = null;
    private double lastThreatDist = 999;
    private float lastHealth = 20.0f;

    // States: 0=monitoring, 1=blocking, 2=counter-attacking
    private int state = 0;

    public AutoShieldSwap() {
        super("AutoShieldSwap",
              "Auto swap shield on/off during combat for defense and counter",
              GameMode.MACE,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        isBlocking = false;
        state = 0;
        lastThreat = null;
        lastHealth = McReflect.getPlayerHealth();
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();
        float currentHealth = McReflect.getPlayerHealth();

        switch (state) {
            case 0: // Monitoring - detect incoming threats
                Object threat = findIncomingThreat();
                if (threat == null) {
                    lastThreat = null;
                    break;
                }

                double dist = McReflect.distanceTo(threat);

                // Detect if threat is closing in (getting closer)
                boolean approaching = lastThreat != null && threat.equals(lastThreat) &&
                                     dist < lastThreatDist;

                // Also detect if we just took damage (reactive blocking)
                boolean tookDamage = currentHealth < lastHealth;

                lastThreat = threat;
                lastThreatDist = dist;

                // Trigger shield if: enemy approaching within 4 blocks, or we just took damage
                if ((approaching && dist <= 4.0) || (tookDamage && dist <= 5.0)) {
                    if (shouldAct(0.90)) {
                        // Simulate shield raise (stop sprinting as blocking proxy)
                        McReflect.setSprinting(false);
                        isBlocking = true;
                        blockStartTime = now;
                        state = 1;
                        // Randomize block hold duration: 200-400ms
                        blockDurationMs = 200 + ThreadLocalRandom.current().nextLong(200);
                        recordAction();
                    }
                }
                break;

            case 1: // Blocking - hold shield for duration
                if (now - blockStartTime >= blockDurationMs) {
                    if (shouldAct(0.88)) {
                        // Drop shield for counter-attack
                        isBlocking = false;
                        state = 2;
                        lastSwapTime = now;
                    }
                }
                break;

            case 2: // Counter-attack window
                if (lastThreat != null && McReflect.isEntityAlive(lastThreat)) {
                    double counterDist = McReflect.distanceTo(lastThreat);
                    if (counterDist <= 3.5) {
                        if (shouldAct(0.90)) {
                            McReflect.attackEntity(lastThreat);
                            McReflect.swingHand();
                            McReflect.setSprinting(true);
                            recordAction();
                        }
                    }
                }

                // Return to monitoring after counter window
                if (now - lastSwapTime >= counterWindowMs) {
                    state = 0;
                }
                break;
        }

        lastHealth = currentHealth;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (isBlocking) {
            McReflect.setSprinting(true);
            isBlocking = false;
        }
        state = 0;
    }

    /**
     * Find the closest enemy player that is approaching (within threat range).
     */
    private Object findIncomingThreat() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object closest = null;
        double closestDist = 6.0; // threat detection range

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }

        return closest;
    }
}
