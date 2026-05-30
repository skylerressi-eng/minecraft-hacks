package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * TimerHack - Subtly speeds up the player's effective movement rate.
 *
 * Rather than directly modifying the game timer (which is trivially
 * detected), this module applies small velocity boosts in the player's
 * current movement direction on select ticks.  The boost magnitude is
 * kept low (~2-5 %) and is only applied intermittently so the resulting
 * speed distribution stays within normal statistical bounds.
 *
 * Additional stealth measures:
 * - Boosts are skipped when the player is standing still
 * - Magnitude varies with Gaussian noise
 * - Periodic "rest" ticks where no boost is applied
 * - Automatically disables when fall distance is high (anti-flag)
 */
public class TimerHack extends Module {

    private static final double BASE_SPEED_MULTIPLIER = 1.03; // 3 % boost
    private static final double MULTIPLIER_VARIANCE = 0.02;   // +-2 %
    private static final double MIN_VELOCITY_THRESHOLD = 0.01;
    private static final float MAX_SAFE_FALL_DISTANCE = 2.5f;
    private static final int REST_CYCLE_MIN = 15;
    private static final int REST_CYCLE_MAX = 30;

    private int ticksUntilRest = 0;
    private int restTicksRemaining = 0;
    private boolean inRestPhase = false;
    private long totalBoostTicks = 0;

    public TimerHack() {
        super("TimerHack",
              "Speed up game tick rate for faster movement and actions",
              GameMode.VAPE,
              HumanizedTimer.SkillLevel.EXPERT,
              "movement");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ticksUntilRest = REST_CYCLE_MIN + (int) (Math.random() * (REST_CYCLE_MAX - REST_CYCLE_MIN));
        restTicksRemaining = 0;
        inRestPhase = false;
        totalBoostTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onTick() {
        incrementTick();

        if (!McReflect.isCoreReady() || !McReflect.isPlayerAlive()) return;
        if (!McReflect.canMove()) return;

        // Safety: don't boost while falling (anti-cheat flag risk)
        float fallDist = McReflect.getPlayerFallDistance();
        if (fallDist > MAX_SAFE_FALL_DISTANCE) return;

        // Safety: don't boost while not on the ground (mid-air detection risk)
        if (!McReflect.isPlayerOnGround()) return;

        // Rest phase management: periodically stop boosting
        if (inRestPhase) {
            restTicksRemaining--;
            if (restTicksRemaining <= 0) {
                inRestPhase = false;
                ticksUntilRest = REST_CYCLE_MIN
                        + (int) (Math.random() * (REST_CYCLE_MAX - REST_CYCLE_MIN));
            }
            return;
        }

        ticksUntilRest--;
        if (ticksUntilRest <= 0) {
            inRestPhase = true;
            restTicksRemaining = 3 + (int) (Math.random() * 5); // Rest for 3-7 ticks
            return;
        }

        if (!isTimerReady()) return;
        if (!shouldAct(0.80)) return;

        // Get current velocity
        double[] vel = McReflect.getPlayerVelocity();
        double vx = vel[0];
        double vy = vel[1];
        double vz = vel[2];

        // Only boost when the player is actually moving horizontally
        double horizontalSpeed = Math.sqrt(vx * vx + vz * vz);
        if (horizontalSpeed < MIN_VELOCITY_THRESHOLD) return;

        // Calculate the boost multiplier with Gaussian variance
        double multiplier = BASE_SPEED_MULTIPLIER
                + (Math.random() * 2 - 1) * MULTIPLIER_VARIANCE;

        // Apply the boost to horizontal velocity only (vertical unchanged)
        double boostedVx = vx * multiplier;
        double boostedVz = vz * multiplier;

        McReflect.setPlayerVelocity(boostedVx, vy, boostedVz);

        totalBoostTicks++;
        recordAction();
    }

    public long getTotalBoostTicks() {
        return totalBoostTicks;
    }

    public boolean isInRestPhase() {
        return inRestPhase;
    }
}
