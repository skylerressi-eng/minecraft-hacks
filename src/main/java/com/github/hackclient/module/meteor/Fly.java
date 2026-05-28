package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Enables creative-like flight in survival mode.
 * Controls vertical movement through velocity manipulation and
 * sets the flying ability flag via McReflect.
 */
public class Fly extends Module {

    private static final double FLY_SPEED = 0.5;
    private static final double ASCEND_SPEED = 0.42;
    private static final double DESCEND_SPEED = -0.35;
    private static final double GLIDE_SPEED = -0.04; // slow descent to look less blatant

    private boolean wasFlying = false;
    private int antiKickCounter = 0;

    public Fly() {
        super("Fly", "Creative-like flight in survival with multiple modes",
              GameMode.METEOR, HumanizedTimer.SkillLevel.EXPERT, "movement");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        wasFlying = false;
        antiKickCounter = 0;
        McReflect.setFlying(true);
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        if (!shouldAct(0.90)) return;

        // Keep flying enabled
        McReflect.setFlying(true);

        double[] velocity = McReflect.getPlayerVelocity();
        double vx = velocity[0];
        double vy = velocity[1];
        double vz = velocity[2];

        // Determine vertical intent from current velocity direction
        // Positive vy = player is pressing jump, negative = pressing sneak
        double targetVy;

        if (vy > 0.1) {
            // Player wants to go up (jump key)
            targetVy = ASCEND_SPEED;
        } else if (vy < -0.1) {
            // Player wants to go down (sneak key)
            targetVy = DESCEND_SPEED;
        } else {
            // Neutral: hold altitude with slight drift for anti-detection
            targetVy = GLIDE_SPEED;
        }

        // Anti-kick: periodically dip down slightly to reset server-side fly checks
        antiKickCounter++;
        if (antiKickCounter >= 40) {
            targetVy = -0.08;
            antiKickCounter = 0;
        }

        // Apply horizontal speed boost based on player facing direction
        float yawRad = (float) Math.toRadians(McReflect.getPlayerYaw());
        double horizontalSpeed = Math.sqrt(vx * vx + vz * vz);

        if (horizontalSpeed > 0.01) {
            // Cap horizontal fly speed
            double cappedSpeed = Math.min(horizontalSpeed, FLY_SPEED);
            double motionAngle = Math.atan2(-vx, vz);
            vx = -Math.sin(motionAngle) * cappedSpeed;
            vz = Math.cos(motionAngle) * cappedSpeed;
        }

        // Prevent fall damage while flying
        McReflect.setFallDistance(0.0f);

        McReflect.setPlayerVelocity(vx, targetVy, vz);
        recordAction();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Restore normal state
        McReflect.setFlying(false);
        // Gentle landing: reduce velocity to prevent fall damage on disable
        double[] velocity = McReflect.getPlayerVelocity();
        if (velocity != null) {
            McReflect.setPlayerVelocity(velocity[0], Math.max(velocity[1], -0.1), velocity[2]);
        }
        McReflect.setFallDistance(0.0f);
        wasFlying = false;
    }
}
