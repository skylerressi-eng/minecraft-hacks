package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * HitSelect - Only allows attacks when the target is in an optimal position.
 *
 * Evaluates the angle precision (how centered the target is on the crosshair),
 * the distance to the target, and the target's movement direction relative
 * to the player.  An attack is only permitted when all criteria indicate a
 * high-probability hit.  This eliminates wasted swings and maximizes damage
 * output while keeping the player's hit/miss ratio looking natural.
 */
public class HitSelect extends Module {

    private static final double MAX_ATTACK_RANGE = 3.5;
    private static final double OPTIMAL_DISTANCE_MIN = 2.0;
    private static final double OPTIMAL_DISTANCE_MAX = 3.3;
    private static final float MAX_YAW_DEVIATION = 12.0f;
    private static final float MAX_PITCH_DEVIATION = 15.0f;
    private static final double MIN_HIT_SCORE = 0.65;

    private long lastAttackTime = 0;
    private int blockedSwings = 0;
    private int allowedSwings = 0;

    // Track target movement for prediction
    private double lastTargetX = 0;
    private double lastTargetZ = 0;
    private boolean hasLastPosition = false;

    public HitSelect() {
        super("HitSelect",
              "Only swings when hit will connect - no wasted attacks",
              GameMode.VAPE,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastAttackTime = 0;
        blockedSwings = 0;
        allowedSwings = 0;
        hasLastPosition = false;
    }

    @Override
    public void onTick() {
        incrementTick();

        if (!McReflect.isCoreReady() || !McReflect.isPlayerAlive()) return;
        if (!isTimerReady()) return;
        if (!shouldAct(0.92)) return;

        Object target = findBestTarget();
        if (target == null) {
            hasLastPosition = false;
            return;
        }

        double hitScore = evaluateHitQuality(target);

        // Track target movement for next tick's prediction
        double tx = McReflect.getEntityX(target);
        double tz = McReflect.getEntityZ(target);

        if (hitScore >= MIN_HIT_SCORE) {
            // Good opportunity - allow the attack
            long now = System.currentTimeMillis();

            // Enforce a minimum cooldown to avoid superhuman attack speed
            long cooldown = 450 + (long) (Math.random() * 100); // 450-550ms
            if (now - lastAttackTime >= cooldown) {
                McReflect.attackEntity(target);
                McReflect.swingHand();
                lastAttackTime = now;
                allowedSwings++;
                recordAction();
            }
        } else {
            blockedSwings++;
        }

        lastTargetX = tx;
        lastTargetZ = tz;
        hasLastPosition = true;
    }

    /**
     * Evaluate how good the current moment is for landing a hit.
     * Returns a score from 0.0 (terrible) to 1.0 (perfect).
     */
    private double evaluateHitQuality(Object target) {
        double score = 0.0;

        // Factor 1: Distance (0.0-0.35)
        double dist = McReflect.distanceTo(target);
        if (dist > MAX_ATTACK_RANGE || dist < 0.5) return 0.0;

        if (dist >= OPTIMAL_DISTANCE_MIN && dist <= OPTIMAL_DISTANCE_MAX) {
            score += 0.35; // Perfect range
        } else if (dist < OPTIMAL_DISTANCE_MIN) {
            // Too close - still OK but slightly penalized
            score += 0.25 * (dist / OPTIMAL_DISTANCE_MIN);
        } else {
            // Approaching max range - penalize proportionally
            double rangeRatio = 1.0 - (dist - OPTIMAL_DISTANCE_MAX)
                    / (MAX_ATTACK_RANGE - OPTIMAL_DISTANCE_MAX);
            score += 0.35 * Math.max(0, rangeRatio);
        }

        // Factor 2: Angle precision (0.0-0.40)
        float[] angles = McReflect.getAnglesTo(target);
        if (angles == null) return 0.0;

        float currentYaw = McReflect.getPlayerYaw();
        float currentPitch = McReflect.getPlayerPitch();

        float yawDiff = Math.abs(wrapAngle(angles[0] - currentYaw));
        float pitchDiff = Math.abs(angles[1] - currentPitch);

        if (yawDiff > MAX_YAW_DEVIATION || pitchDiff > MAX_PITCH_DEVIATION) {
            return 0.0; // Way off target
        }

        double yawScore = 1.0 - (yawDiff / MAX_YAW_DEVIATION);
        double pitchScore = 1.0 - (pitchDiff / MAX_PITCH_DEVIATION);
        score += 0.40 * (yawScore * 0.6 + pitchScore * 0.4);

        // Factor 3: Target movement direction (0.0-0.25)
        if (hasLastPosition) {
            double tx = McReflect.getEntityX(target);
            double tz = McReflect.getEntityZ(target);
            double dx = tx - lastTargetX;
            double dz = tz - lastTargetZ;
            double moveSpeed = Math.sqrt(dx * dx + dz * dz);

            if (moveSpeed < 0.01) {
                // Stationary target - easy hit
                score += 0.25;
            } else {
                // Check if target is moving toward or away from us
                double myX = McReflect.getPlayerX();
                double myZ = McReflect.getPlayerZ();
                double toTargetX = tx - myX;
                double toTargetZ = tz - myZ;
                double toTargetLen = Math.sqrt(toTargetX * toTargetX + toTargetZ * toTargetZ);

                if (toTargetLen > 0.01) {
                    // Dot product to determine movement direction relative to us
                    double dot = (dx * toTargetX + dz * toTargetZ) / (moveSpeed * toTargetLen);
                    // dot < 0 means moving toward us (easier to hit)
                    // dot > 0 means moving away (harder)
                    if (dot < -0.3) {
                        score += 0.25; // Approaching - great
                    } else if (dot < 0.3) {
                        score += 0.15; // Lateral movement - OK
                    } else {
                        score += 0.05; // Running away - harder
                    }
                }
            }
        } else {
            score += 0.12; // No history yet, neutral
        }

        return score;
    }

    private Object findBestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object best = null;
        double bestDist = Double.MAX_VALUE;

        for (Object player : players) {
            if (player == self || player.equals(self)) continue;
            if (!McReflect.isEntityAlive(player)) continue;

            double dist = McReflect.distanceTo(player);
            if (dist <= MAX_ATTACK_RANGE && dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }

        return best;
    }

    private static float wrapAngle(float angle) {
        angle = angle % 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Check if an attack should be allowed right now.
     * Can be queried by other modules that want to defer to HitSelect.
     */
    public boolean shouldAllowAttack(Object target) {
        if (target == null) return true;
        return evaluateHitQuality(target) >= MIN_HIT_SCORE;
    }

    public int getBlockedSwings() {
        return blockedSwings;
    }

    public int getAllowedSwings() {
        return allowedSwings;
    }
}
