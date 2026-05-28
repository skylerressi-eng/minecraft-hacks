package com.github.hackclient.module.legacy;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spear Combo - Trident throw combos with melee follow-up for 1.8 PvP.
 *
 * Tracks trident throw timing and follows up with melee attacks:
 * 1. Detects when holding a trident and target is at optimal throw range
 * 2. Smoothly aims at the target with lead prediction
 * 3. Throws the trident at optimal range (8-15 blocks)
 * 4. Sprints toward target for melee follow-up
 * 5. Attacks with melee when within range
 *
 * Uses McReflect for player scanning, distance tracking, aim smoothing,
 * and sprint state control.
 */
public class SpearCombo extends Module {

    private long lastThrowTime = 0;
    private boolean tridentThrown = false;
    private int comboPhase = 0; // 0=idle, 1=aim-for-throw, 2=thrown-rushing, 3=melee-follow-up
    private Object comboTarget = null;

    private static final double OPTIMAL_THROW_MIN = 8.0;
    private static final double OPTIMAL_THROW_MAX = 15.0;
    private static final double MELEE_RANGE = 3.0;

    // Aim smoothing
    private float smoothedYaw = 0;
    private float smoothedPitch = 0;
    private boolean aimInitialized = false;

    // Throw charge tracking
    private long chargeStartTime = 0;
    private static final long MIN_CHARGE_MS = 600; // min trident charge time

    public SpearCombo() {
        super("SpearCombo",
              "Auto-throws trident at optimal range then rushes for melee",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        comboPhase = 0;
        tridentThrown = false;
        comboTarget = null;
        aimInitialized = false;
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();

        // Initialize aim from current look direction
        if (!aimInitialized) {
            smoothedYaw = McReflect.getPlayerYaw();
            smoothedPitch = McReflect.getPlayerPitch();
            aimInitialized = true;
        }

        // Find or validate target
        Object target = findBestTarget();
        if (target == null) {
            comboPhase = 0;
            tridentThrown = false;
            comboTarget = null;
            return;
        }

        double distToTarget = McReflect.distanceTo(target);
        comboTarget = target;

        switch (comboPhase) {
            case 0: // Idle - check if target is at throw range
                if (distToTarget >= OPTIMAL_THROW_MIN && distToTarget <= OPTIMAL_THROW_MAX) {
                    if (shouldAct(0.88)) {
                        comboPhase = 1;
                        chargeStartTime = now;
                    }
                }
                break;

            case 1: // Aiming and charging for throw
                // Smooth aim at target with lead prediction
                float[] targetAngles = McReflect.getAnglesTo(target);
                if (targetAngles == null) { comboPhase = 0; break; }

                // Lead prediction: offset aim slightly in direction target is moving
                float leadYaw = predictLeadAngle(target, targetAngles[0]);

                float aimSpeed = 0.5f + (float) (Math.random() * 0.15);
                smoothedYaw = AntiCheatBypass.smoothRotation(smoothedYaw, leadYaw, aimSpeed);
                smoothedPitch = AntiCheatBypass.smoothRotation(smoothedPitch, targetAngles[1], aimSpeed);

                setPlayerRotation(
                    AntiCheatBypass.addRotationNoise(smoothedYaw),
                    AntiCheatBypass.addRotationNoise(smoothedPitch)
                );

                // Check if charge is ready and aim is close enough
                boolean chargeReady = now - chargeStartTime >= MIN_CHARGE_MS;
                boolean aimed = Math.abs(smoothedYaw - leadYaw) < 5.0f &&
                               Math.abs(smoothedPitch - targetAngles[1]) < 5.0f;

                if (chargeReady && aimed) {
                    long delay = timer.getNextDelayMs();
                    if (now - lastThrowTime >= delay && shouldAct(0.93)) {
                        // Simulate trident release (swing hand as throw proxy)
                        McReflect.swingHand();
                        tridentThrown = true;
                        lastThrowTime = now;
                        comboPhase = 2;
                        recordAction();
                    }
                }
                break;

            case 2: // Thrown - sprint toward target for melee follow-up
                if (distToTarget > MELEE_RANGE) {
                    McReflect.setSprinting(true);

                    // Keep aiming at target while rushing
                    float[] rushAngles = McReflect.getAnglesTo(target);
                    if (rushAngles != null) {
                        float rushSpeed = 0.4f + (float) (Math.random() * 0.15);
                        smoothedYaw = AntiCheatBypass.smoothRotation(smoothedYaw, rushAngles[0], rushSpeed);
                        smoothedPitch = AntiCheatBypass.smoothRotation(smoothedPitch, rushAngles[1], rushSpeed);
                        setPlayerRotation(smoothedYaw, smoothedPitch);
                    }
                } else {
                    // Close enough for melee
                    comboPhase = 3;
                }

                // Timeout: if we haven't closed distance in 5 seconds, reset
                if (now - lastThrowTime > 5000) {
                    comboPhase = 0;
                    tridentThrown = false;
                }
                break;

            case 3: // Melee follow-up attack
                if (distToTarget <= MELEE_RANGE) {
                    if (shouldAct(0.92)) {
                        McReflect.attackEntity(target);
                        McReflect.swingHand();
                        recordAction();
                    }
                }
                // Reset combo after melee
                comboPhase = 0;
                tridentThrown = false;
                break;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        comboPhase = 0;
        tridentThrown = false;
        comboTarget = null;
    }

    /**
     * Predict a lead angle offset based on target's apparent lateral movement.
     */
    private float predictLeadAngle(Object target, float baseYaw) {
        // Simple lead: offset yaw slightly based on distance (farther = more lead)
        double dist = McReflect.distanceTo(target);
        float leadOffset = (float) (ThreadLocalRandom.current().nextGaussian() * (dist * 0.15));
        return baseYaw + leadOffset;
    }

    private Object findBestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object best = null;
        double bestDist = OPTIMAL_THROW_MAX + 1;

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist <= OPTIMAL_THROW_MAX && dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }

        return best;
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
}
