package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Increases player movement speed by amplifying horizontal velocity.
 * Uses strafe-aware calculations and stealth-gated action timing
 * to stay within anti-cheat tolerances.
 */
public class Speed extends Module {

    private static final double MIN_MULTIPLIER = 1.2;
    private static final double MAX_MULTIPLIER = 1.4;
    private static final double SNEAK_MULTIPLIER = 1.0; // no boost while sneaking

    private double currentMultiplier = MIN_MULTIPLIER;

    public Speed() {
        super("Speed", "Increases movement speed with anti-cheat safe values",
              GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED, "movement");
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        if (!shouldAct(0.85)) return;
        if (!isTimerReady()) return;

        // Only boost when on the ground to avoid fly-like detection
        if (!McReflect.isPlayerOnGround()) return;

        double[] velocity = McReflect.getPlayerVelocity();
        double vx = velocity[0];
        double vy = velocity[1];
        double vz = velocity[2];

        // Only boost if player is actually moving horizontally
        double horizontalSpeed = Math.sqrt(vx * vx + vz * vz);
        if (horizontalSpeed < 0.01) return;

        // Vary the multiplier slightly each tick for anti-detection
        currentMultiplier = MIN_MULTIPLIER + Math.random() * (MAX_MULTIPLIER - MIN_MULTIPLIER);

        // Apply strafe-aware speed boost using yaw
        float yawRad = (float) Math.toRadians(McReflect.getPlayerYaw());
        double motionAngle = Math.atan2(-vx, vz);

        // Calculate the forward/strafe components relative to player facing
        double forward = Math.cos(motionAngle - (-yawRad));
        double strafe = Math.sin(motionAngle - (-yawRad));

        // Reconstruct velocity with boosted magnitude
        double boostedSpeed = horizontalSpeed * currentMultiplier;

        // Cap boosted speed to avoid blatant detection
        double maxAllowed = 0.35; // vanilla sprint is ~0.26
        boostedSpeed = Math.min(boostedSpeed, maxAllowed);

        // Reconstruct the motion direction with boosted speed
        double newVx = -Math.sin(motionAngle) * boostedSpeed;
        double newVz = Math.cos(motionAngle) * boostedSpeed;

        McReflect.setPlayerVelocity(newVx, vy, newVz);
        recordAction();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentMultiplier = MIN_MULTIPLIER;
    }
}
