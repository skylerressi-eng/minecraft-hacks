package com.github.hackclient.module.legacy;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * W-Tap - Automatically performs sprint-reset combos in 1.8 PvP.
 *
 * Rapidly toggles sprint between hits to get the sprint-hit knockback bonus
 * on every attack. Humanized timing prevents anti-cheat from detecting
 * the perfectly timed sprint resets.
 */
public class WTap extends Module {

    private long lastTapTime = 0;
    private boolean sprintReset = false;
    private int ticksSinceHit = 0;

    public WTap() {
        super("WTap",
              "Auto sprint-reset for combo knockback in 1.8 PvP",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();

        if (!isInCombat()) {
            sprintReset = false;
            return;
        }

        ticksSinceHit++;

        // After landing a hit, briefly stop sprinting then re-sprint
        if (justLandedHit()) {
            ticksSinceHit = 0;
            sprintReset = true;
            setSprinting(false);
            lastTapTime = now;
        }

        // Re-enable sprint after humanized delay (typically 1-3 ticks)
        if (sprintReset && now - lastTapTime > timer.getNextDelayMs() / 4) {
            if (AntiCheatBypass.shouldActThisTick(0.95)) {
                setSprinting(true);
                sprintReset = false;
            }
        }
    }

    // Stubs
    private boolean isInCombat() { return false; }
    private boolean justLandedHit() { return false; }
    private void setSprinting(boolean sprint) {
        // TODO: mc.player.setSprinting(sprint)
    }
}
