package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * AimAssist - Subtle crosshair pull toward the nearest enemy player.
 *
 * Applies a small, smooth angular correction each tick (2-4 degrees max)
 * only when the target is already within the player's field of view.
 * Uses Gaussian noise and eased interpolation so the resulting mouse
 * movement is statistically indistinguishable from a skilled human.
 */
public class AimAssist extends Module {

    private static final double MAX_CORRECTION_DEGREES = 3.5;
    private static final double MIN_CORRECTION_DEGREES = 1.5;
    private static final double FOV_THRESHOLD = 90.0;
    private static final double MAX_TARGET_DISTANCE = 6.0;
    private static final double SMOOTHING_FACTOR = 0.35;

    private Object lastTarget = null;
    private float residualYaw = 0f;
    private float residualPitch = 0f;

    public AimAssist() {
        super("AimAssist",
              "Subtle crosshair pull toward nearest player (closet safe)",
              GameMode.VAPE,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onTick() {
        incrementTick();

        if (!McReflect.isCoreReady() || !McReflect.isPlayerAlive()) return;
        if (!isTimerReady()) return;
        if (!shouldAct(0.88)) return;

        Object target = findBestTarget();
        if (target == null) {
            lastTarget = null;
            residualYaw = 0f;
            residualPitch = 0f;
            return;
        }

        float[] angles = McReflect.getAnglesTo(target);
        if (angles == null) return;

        float targetYaw = angles[0];
        float targetPitch = angles[1];
        float currentYaw = McReflect.getPlayerYaw();
        float currentPitch = McReflect.getPlayerPitch();

        float yawDiff = wrapAngle(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        // Only assist if target is within FOV
        double angularDistance = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        if (angularDistance > FOV_THRESHOLD) return;

        // Calculate correction magnitude - scales with distance from crosshair
        // Closer to crosshair = smaller correction (more natural)
        double correctionScale = Math.min(angularDistance / FOV_THRESHOLD, 1.0);
        double maxCorrection = MIN_CORRECTION_DEGREES
                + (MAX_CORRECTION_DEGREES - MIN_CORRECTION_DEGREES) * correctionScale;

        // Smooth the correction using an eased interpolation
        float smoothing = (float) (SMOOTHING_FACTOR + (Math.random() * 0.1 - 0.05));

        float yawCorrection = yawDiff * smoothing;
        float pitchCorrection = pitchDiff * smoothing * 0.7f; // Pitch is less aggressive

        // Clamp to max correction per tick
        yawCorrection = clamp(yawCorrection, (float) -maxCorrection, (float) maxCorrection);
        pitchCorrection = clamp(pitchCorrection, (float) -maxCorrection * 0.6f, (float) maxCorrection * 0.6f);

        // Add Gaussian noise for humanization
        yawCorrection = AntiCheatBypass.addRotationNoise(yawCorrection);
        pitchCorrection = AntiCheatBypass.addRotationNoise(pitchCorrection);

        // Accumulate sub-pixel residuals for smoother micro-corrections
        residualYaw += yawCorrection;
        residualPitch += pitchCorrection;

        // Only apply when residual exceeds a small threshold (prevents jitter)
        if (Math.abs(residualYaw) >= 0.5f || Math.abs(residualPitch) >= 0.5f) {
            float applyYaw = currentYaw + residualYaw;
            float applyPitch = clamp(currentPitch + residualPitch, -90f, 90f);

            // Use reflection to set yaw/pitch through McReflect's player object
            setPlayerRotation(applyYaw, applyPitch);

            residualYaw = 0f;
            residualPitch = 0f;
            lastTarget = target;
            recordAction();
        }
    }

    private Object findBestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object best = null;
        double bestScore = Double.MAX_VALUE;
        float myYaw = McReflect.getPlayerYaw();
        float myPitch = McReflect.getPlayerPitch();

        for (Object player : players) {
            if (player == self || player.equals(self)) continue;
            if (!McReflect.isEntityAlive(player)) continue;

            double dist = McReflect.distanceTo(player);
            if (dist > MAX_TARGET_DISTANCE || dist < 0.5) continue;

            float[] angles = McReflect.getAnglesTo(player);
            if (angles == null) continue;

            float yawDiff = Math.abs(wrapAngle(angles[0] - myYaw));
            float pitchDiff = Math.abs(angles[1] - myPitch);
            double angularDist = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

            // Skip targets outside FOV
            if (angularDist > FOV_THRESHOLD) continue;

            // Score: prefer closer targets that are already near crosshair
            double score = dist * 0.4 + angularDist * 0.6;
            if (score < bestScore) {
                bestScore = score;
                best = player;
            }
        }

        return best;
    }

    private void setPlayerRotation(float yaw, float pitch) {
        try {
            Object player = McReflect.getPlayer();
            if (player == null) return;
            // Set yaw and pitch via reflection on the entity fields
            java.lang.reflect.Field yawField = findField(player.getClass(), "field_6031", "yaw");
            java.lang.reflect.Field pitchField = findField(player.getClass(), "field_6036", "pitch");
            if (yawField != null) {
                yawField.setAccessible(true);
                yawField.setFloat(player, yaw);
            }
            if (pitchField != null) {
                pitchField.setAccessible(true);
                pitchField.setFloat(player, pitch);
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
