package com.github.hackclient.module.mace;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * Shield Rotation - Auto-rotate to block attacks from behind.
 *
 * Monitors nearby players' positions and detects when an attack is coming
 * from outside the player's current field of view. Automatically rotates
 * the player to face the incoming threat so the shield blocks the damage.
 *
 * Key behaviors:
 * - Scans all nearby players to find the closest approaching threat
 * - Detects threats from behind (outside +/-90 degrees of current view)
 * - Smooth rotation to face the attacker (not instant snap)
 * - After blocking, quick counter-attack swing
 * - Respects stealth engine rotation speed limits
 *
 * Uses McReflect for player scanning, position tracking, angle calculation,
 * and rotation application.
 */
public class ShieldRotation extends Module {

    private float smoothedYaw = 0;
    private float smoothedPitch = 0;
    private boolean aimInitialized = false;
    private boolean isTrackingThreat = false;
    private Object trackedThreat = null;
    private long lastBlockTime = 0;

    // Configurable
    private double detectionRange = 12.0;
    private double blockTriggerDistance = 6.0;
    private float rotationSpeed = 0.55f;

    // Threat approach tracking
    private double lastThreatDist = 999;

    public ShieldRotation() {
        super("ShieldRotation",
              "Auto-rotates to block incoming attacks from behind",
              GameMode.MACE,
              HumanizedTimer.SkillLevel.AVERAGE,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        aimInitialized = false;
        isTrackingThreat = false;
        trackedThreat = null;
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();
        if (now - lastBlockTime < delay / 3) return;

        // Initialize aim from current look direction
        if (!aimInitialized) {
            smoothedYaw = McReflect.getPlayerYaw();
            smoothedPitch = McReflect.getPlayerPitch();
            aimInitialized = true;
        }

        // Scan for threats - prioritize threats from behind
        Object threat = findBehindThreat();

        if (threat == null) {
            if (isTrackingThreat) {
                isTrackingThreat = false;
                trackedThreat = null;
            }
            return;
        }

        isTrackingThreat = true;
        trackedThreat = threat;

        // Calculate angles to the threat
        float[] threatAngles = McReflect.getAnglesTo(threat);
        if (threatAngles == null) return;

        // Determine rotation speed - constrained by stealth engine
        float speed = rotationSpeed + (float) (Math.random() * 0.1 - 0.05);
        StealthEngine stealth = StealthEngine.getInstance();
        if (stealth != null) {
            float maxRotSpeed = stealth.getMaxRotationSpeed();
            float yawDelta = wrapAngle(threatAngles[0] - smoothedYaw);
            if (Math.abs(yawDelta) > 0) {
                speed = Math.min(speed, maxRotSpeed / Math.abs(yawDelta));
            }
        }

        // Smooth rotation toward threat
        smoothedYaw = AntiCheatBypass.smoothRotation(
                smoothedYaw,
                AntiCheatBypass.addRotationNoise(threatAngles[0]),
                speed
        );
        smoothedPitch = AntiCheatBypass.smoothRotation(
                smoothedPitch,
                AntiCheatBypass.addRotationNoise(threatAngles[1]),
                speed
        );

        // Apply the rotation
        setPlayerRotation(smoothedYaw, smoothedPitch);

        // Track whether threat is getting closer
        double dist = McReflect.distanceTo(threat);
        boolean approaching = dist < lastThreatDist;
        lastThreatDist = dist;

        // If threat is close and approaching, record blocking action
        if (dist <= blockTriggerDistance && approaching) {
            if (shouldAct(0.90)) {
                // Face the threat and "block" (stop sprinting as shield proxy)
                McReflect.setSprinting(false);
                lastBlockTime = now;
                recordAction();
            }
        }

        // Counter-attack: if we've turned to face them and they're in melee range
        float facingDiff = Math.abs(wrapAngle(smoothedYaw - threatAngles[0]));
        if (facingDiff < 10.0f && dist <= 3.5) {
            if (shouldAct(0.85)) {
                McReflect.attackEntity(threat);
                McReflect.swingHand();
                McReflect.setSprinting(true);
                recordAction();
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isTrackingThreat = false;
        trackedThreat = null;
    }

    /**
     * Find the closest threat that is behind the player (outside forward FOV).
     * If no behind-threat exists, returns the closest threat overall.
     */
    private Object findBehindThreat() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        float playerYaw = McReflect.getPlayerYaw();
        Object bestBehind = null;
        double bestBehindDist = detectionRange + 1;
        Object bestAny = null;
        double bestAnyDist = detectionRange + 1;

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist > detectionRange) continue;

            float[] angles = McReflect.getAnglesTo(entity);
            if (angles == null) continue;

            // Check if entity is behind us (more than 90 degrees from look direction)
            float yawDiff = Math.abs(wrapAngle(angles[0] - playerYaw));
            boolean isBehind = yawDiff > 90.0f;

            if (isBehind && dist < bestBehindDist) {
                bestBehindDist = dist;
                bestBehind = entity;
            }
            if (dist < bestAnyDist) {
                bestAnyDist = dist;
                bestAny = entity;
            }
        }

        // Prioritize behind threats, fall back to any close threat
        return bestBehind != null ? bestBehind : (bestAnyDist <= blockTriggerDistance ? bestAny : null);
    }

    private void setPlayerRotation(float yaw, float pitch) {
        try {
            Object player = McReflect.getPlayer();
            if (player == null) return;
            java.lang.reflect.Field yawField = findField(player.getClass(), "field_6031", "yaw");
            java.lang.reflect.Field pitchField = findField(player.getClass(), "field_6036", "pitch");
            if (yawField != null) {
                yawField.setAccessible(true);
                yawField.setFloat(player, yaw);
            }
            if (pitchField != null) {
                pitchField.setAccessible(true);
                pitchField.setFloat(player, Math.max(-90f, Math.min(90f, pitch)));
            }
        } catch (Exception ignored) {}
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String... names) {
        Class<?> current = clazz;
        while (current != null) {
            for (String name : names) {
                try {
                    java.lang.reflect.Field f = current.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static float wrapAngle(float angle) {
        angle = angle % 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    // Configuration
    public void setDetectionRange(double range) { this.detectionRange = range; }
    public void setBlockTriggerDistance(double dist) { this.blockTriggerDistance = dist; }
    public void setRotationSpeed(float speed) { this.rotationSpeed = speed; }
}
