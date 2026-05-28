package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * ClickAssist - Probabilistically inserts extra attack clicks.
 *
 * Tracks the player's natural click rhythm and occasionally fires a
 * bonus attack on ticks where the player would not normally click.
 * The bonus probability is 15-25 %, randomised per-session, with
 * Gaussian jitter so the resulting CPS distribution still looks human.
 * Only fires when a valid target is within attack range.
 */
public class ClickAssist extends Module {

    private static final double MAX_ATTACK_RANGE = 4.0;
    private static final long MIN_CLICK_GAP_MS = 45;
    private static final long MAX_CLICK_GAP_MS = 120;

    private double bonusProbability;
    private long lastClickTime = 0;
    private long lastBonusClickTime = 0;
    private int naturalClickCount = 0;
    private int bonusClickCount = 0;
    private long sessionStartTime = 0;

    public ClickAssist() {
        super("ClickAssist",
              "Adds extra clicks to boost CPS while looking legit",
              GameMode.VAPE,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
        // Randomize bonus probability in the 15-25 % range per session
        bonusProbability = 0.15 + Math.random() * 0.10;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastClickTime = System.currentTimeMillis();
        lastBonusClickTime = 0;
        naturalClickCount = 0;
        bonusClickCount = 0;
        sessionStartTime = System.currentTimeMillis();
        // Re-roll probability each enable for variance across sessions
        bonusProbability = 0.15 + Math.random() * 0.10;
    }

    @Override
    public void onTick() {
        incrementTick();

        if (!McReflect.isCoreReady() || !McReflect.isPlayerAlive()) return;
        if (!isTimerReady()) return;

        long now = System.currentTimeMillis();

        // Safety: don't add bonus clicks too frequently
        long timeSinceLastBonus = now - lastBonusClickTime;
        if (timeSinceLastBonus < MIN_CLICK_GAP_MS) return;

        // Probabilistic gate with stealth engine awareness
        if (!shouldAct(bonusProbability)) return;

        // Find a valid target within attack range
        Object target = findNearestTarget();
        if (target == null) return;

        double dist = McReflect.distanceTo(target);
        if (dist > MAX_ATTACK_RANGE || dist < 0.5) return;

        // Add a humanized gap between the natural click and our bonus click
        long clickGap = MIN_CLICK_GAP_MS
                + (long) (Math.random() * (MAX_CLICK_GAP_MS - MIN_CLICK_GAP_MS));

        if (timeSinceLastBonus < clickGap) return;

        // Fire the bonus attack
        McReflect.attackEntity(target);
        McReflect.swingHand();

        lastBonusClickTime = now;
        bonusClickCount++;
        recordAction();

        // Adaptive: if bonus-to-natural ratio is getting too high, throttle back
        if (naturalClickCount > 10) {
            double ratio = (double) bonusClickCount / naturalClickCount;
            if (ratio > 0.30) {
                bonusProbability = Math.max(0.10, bonusProbability - 0.02);
            }
        }
    }

    /**
     * Called externally (or by hook) when the player performs a natural click,
     * so we can track the ratio. Also tracks timing internally.
     */
    public void registerNaturalClick() {
        naturalClickCount++;
        lastClickTime = System.currentTimeMillis();
    }

    private Object findNearestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Object player : players) {
            if (player == self || player.equals(self)) continue;
            if (!McReflect.isEntityAlive(player)) continue;

            double dist = McReflect.distanceTo(player);
            if (dist < nearestDist && dist <= MAX_ATTACK_RANGE) {
                nearestDist = dist;
                nearest = player;
            }
        }

        return nearest;
    }

    public double getBonusProbability() {
        return bonusProbability;
    }

    public int getBonusClickCount() {
        return bonusClickCount;
    }
}
