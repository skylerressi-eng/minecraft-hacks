package com.github.hackclient.module.crystal;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * Smart Crystal Anchor - Intelligent combo of crystal + anchor placement.
 *
 * Alternates between end crystals and respawn anchors based on which weapon
 * deals more damage to the current target, considering:
 * - Target position relative to player (anchors better for targets above)
 * - Available materials (crystals vs anchors + glowstone)
 * - Damage calculations for both weapon types
 * - Self-damage minimization
 */
public class SmartCrystalAnchor extends Module {

    private long lastActionTime = 0;
    private int anchorStage = 0; // 0=place, 1=charge, 2=detonate
    private boolean usingAnchors = false;
    private Object currentTarget = null;
    private double[] activeAnchorPos = null;

    // Smooth aim state
    private float currentYaw;
    private float currentPitch;

    // Configurable
    private double anchorRange = 5.0;
    private double crystalRange = 5.0;
    private double heightThreshold = 2.0;
    private double minTargetDamage = 6.0;
    private double maxSelfDamage = 10.0;

    // Explosion constants
    private static final double CRYSTAL_EXPLOSION_POWER = 6.0;
    private static final double CRYSTAL_MAX_RANGE = 12.0;
    private static final double ANCHOR_EXPLOSION_POWER = 5.0;
    private static final double ANCHOR_MAX_RANGE = 10.0;

    public SmartCrystalAnchor() {
        super("SmartCrystalAnchor",
              "Intelligent crystal + anchor combo, picks highest damage option",
              GameMode.CRYSTAL,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        anchorStage = 0;
        usingAnchors = false;
        currentTarget = null;
        activeAnchorPos = null;
        currentYaw = McReflect.getPlayerYaw();
        currentPitch = McReflect.getPlayerPitch();
    }

    @Override
    public void onTick() {
        if (!McReflect.isPlayerAlive()) return;
        incrementTick();

        // Use half delay for faster combat response
        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs() / 2;
        if (now - lastActionTime < delay) return;

        currentTarget = findNearestEnemy();
        if (currentTarget == null) {
            anchorStage = 0;
            activeAnchorPos = null;
            return;
        }

        double targetY = McReflect.getEntityY(currentTarget);
        double selfY = McReflect.getPlayerY();
        double yDiff = targetY - selfY;
        double distance = McReflect.distanceTo(currentTarget);

        // Smart weapon selection: compare potential damage from both weapons
        double[] bestCrystalPos = findBestCrystalPlacement(currentTarget);
        double[] bestAnchorPos = findBestAnchorPlacement(currentTarget);

        double crystalDmg = bestCrystalPos != null ?
                calculateDamage(bestCrystalPos, currentTarget, CRYSTAL_EXPLOSION_POWER, CRYSTAL_MAX_RANGE) : 0;
        double anchorDmg = bestAnchorPos != null ?
                calculateDamage(bestAnchorPos, currentTarget, ANCHOR_EXPLOSION_POWER, ANCHOR_MAX_RANGE) : 0;

        // Factor in height advantage: anchors are better when target is above
        if (yDiff >= heightThreshold) {
            anchorDmg *= 1.3; // Boost anchor score when target is above
        }

        // If actively in an anchor sequence, finish it
        if (usingAnchors && anchorStage > 0) {
            handleAnchorMode(now);
            return;
        }

        // Choose weapon with higher damage
        if (anchorDmg > crystalDmg && anchorDmg >= minTargetDamage && distance <= anchorRange) {
            usingAnchors = true;
            activeAnchorPos = bestAnchorPos;
            handleAnchorMode(now);
        } else if (crystalDmg >= minTargetDamage && distance <= crystalRange) {
            usingAnchors = false;
            handleCrystalMode(now, bestCrystalPos);
        }
    }

    /**
     * Handle anchor combat: place, charge, detonate sequence.
     */
    private void handleAnchorMode(long now) {
        if (currentTarget == null) return;

        switch (anchorStage) {
            case 0: // Place anchor
                if (activeAnchorPos == null) {
                    activeAnchorPos = findBestAnchorPlacement(currentTarget);
                }
                if (activeAnchorPos == null) {
                    // Fallback to crystal
                    usingAnchors = false;
                    return;
                }

                aimAtPosition(activeAnchorPos);

                if (shouldAct(0.91)) {
                    McReflect.swingHand();
                    anchorStage = 1;
                    lastActionTime = now;
                    recordAction();
                }
                break;

            case 1: // Charge with glowstone
                if (activeAnchorPos == null) { anchorStage = 0; return; }
                aimAtPosition(activeAnchorPos);

                if (shouldAct(0.94)) {
                    McReflect.swingHand();
                    anchorStage = 2;
                    lastActionTime = now;
                    recordAction();
                }
                break;

            case 2: // Detonate when target is in blast radius
                if (activeAnchorPos == null || currentTarget == null) {
                    anchorStage = 0;
                    return;
                }

                double distToAnchor = distanceToPosition(currentTarget, activeAnchorPos);
                if (distToAnchor > 3.5) return; // Wait for target to be closer

                aimAtPosition(activeAnchorPos);

                if (shouldAct(0.96)) {
                    McReflect.swingHand();
                    anchorStage = 0;
                    activeAnchorPos = null;
                    lastActionTime = now;
                    recordAction();
                }
                break;
        }
    }

    /**
     * Handle crystal combat: find placement, validate damage, place crystal.
     */
    private void handleCrystalMode(long now, double[] crystalPos) {
        anchorStage = 0;
        activeAnchorPos = null;

        if (crystalPos == null) return;

        // Validate self-damage
        double selfDmg = calculateSelfDamage(crystalPos, CRYSTAL_EXPLOSION_POWER, CRYSTAL_MAX_RANGE);
        if (selfDmg > maxSelfDamage) return;

        aimAtPosition(crystalPos);

        if (shouldAct(0.90)) {
            McReflect.swingHand();
            lastActionTime = now;
            recordAction();
        }
    }

    /**
     * Smoothly aim at a world position.
     */
    private void aimAtPosition(double[] pos) {
        float[] angles = getAnglesToPosition(pos);
        if (angles != null) {
            float speed = 0.5f + (float) (Math.random() * 0.12);
            currentYaw = AntiCheatBypass.smoothRotation(currentYaw,
                    AntiCheatBypass.addRotationNoise(angles[0]), speed);
            currentPitch = AntiCheatBypass.smoothRotation(currentPitch,
                    AntiCheatBypass.addRotationNoise(angles[1]), speed);
        }
    }

    /**
     * Find nearest living enemy player.
     */
    private Object findNearestEnemy() {
        Object self = McReflect.getPlayer();
        if (self == null) return null;

        Object nearest = null;
        double maxRange = Math.max(anchorRange, crystalRange) + 3.0;
        double nearestDist = maxRange;

        for (Object entity : McReflect.getPlayers()) {
            if (entity == self) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }
        return nearest;
    }

    /**
     * Find best crystal placement position near a target.
     */
    private double[] findBestCrystalPlacement(Object target) {
        return findBestPlacement(target, crystalRange, CRYSTAL_EXPLOSION_POWER, CRYSTAL_MAX_RANGE);
    }

    /**
     * Find best anchor placement position near a target.
     */
    private double[] findBestAnchorPlacement(Object target) {
        return findBestPlacement(target, anchorRange, ANCHOR_EXPLOSION_POWER, ANCHOR_MAX_RANGE);
    }

    /**
     * Generic best placement finder for explosive weapons.
     */
    private double[] findBestPlacement(Object target, double range, double power, double maxDmgRange) {
        double tx = McReflect.getEntityX(target);
        double ty = McReflect.getEntityY(target);
        double tz = McReflect.getEntityZ(target);
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY();
        double pz = McReflect.getPlayerZ();

        int[][] offsets = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {0, -1, 0}, {1, -1, 0}, {-1, -1, 0}
        };

        double[] best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int[] off : offsets) {
            double wx = Math.floor(tx) + off[0] + 0.5;
            double wy = Math.floor(ty) + off[1];
            double wz = Math.floor(tz) + off[2] + 0.5;

            double distToPlayer = Math.sqrt(
                    (wx - px) * (wx - px) + (wy - py) * (wy - py) + (wz - pz) * (wz - pz));
            if (distToPlayer > range) continue;

            double[] pos = {wx, wy, wz};
            double targetDmg = calculateDamage(pos, target, power, maxDmgRange);
            double selfDmg = calculateSelfDamage(pos, power, maxDmgRange);

            if (targetDmg < minTargetDamage || selfDmg > maxSelfDamage) continue;

            double score = targetDmg - selfDmg;
            if (score > bestScore) {
                bestScore = score;
                best = pos;
            }
        }
        return best;
    }

    /**
     * Calculate explosion damage to a target entity.
     */
    private double calculateDamage(double[] explosionPos, Object entity, double power, double maxRange) {
        double ex = McReflect.getEntityX(entity);
        double ey = McReflect.getEntityY(entity) + 1.0;
        double ez = McReflect.getEntityZ(entity);

        double dx = ex - explosionPos[0];
        double dy = ey - explosionPos[1];
        double dz = ez - explosionPos[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist >= maxRange) return 0;

        double exposure = 1.0 - (dist / maxRange);
        return Math.max(0, exposure * power * 2.0 * 0.6);
    }

    /**
     * Calculate self-damage from an explosion.
     */
    private double calculateSelfDamage(double[] explosionPos, double power, double maxRange) {
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY() + 1.0;
        double pz = McReflect.getPlayerZ();

        double dx = px - explosionPos[0];
        double dy = py - explosionPos[1];
        double dz = pz - explosionPos[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist >= maxRange) return 0;

        double exposure = 1.0 - (dist / maxRange);
        return Math.max(0, exposure * power * 2.0 * 0.6);
    }

    /**
     * Distance from an entity to a world position.
     */
    private double distanceToPosition(Object entity, double[] pos) {
        double ex = McReflect.getEntityX(entity);
        double ey = McReflect.getEntityY(entity);
        double ez = McReflect.getEntityZ(entity);
        double dx = ex - pos[0];
        double dy = ey - pos[1];
        double dz = ez - pos[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculate yaw/pitch angles from player to a world position.
     */
    private float[] getAnglesToPosition(double[] pos) {
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY() + 1.62;
        double pz = McReflect.getPlayerZ();

        double dx = pos[0] - px;
        double dy = pos[1] - py;
        double dz = pos[2] - pz;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizDist));
        return new float[]{yaw, pitch};
    }

    @Override
    public void onDisable() {
        super.onDisable();
        anchorStage = 0;
        usingAnchors = false;
        currentTarget = null;
        activeAnchorPos = null;
    }

    public boolean isUsingAnchors() { return usingAnchors; }
    public void setAnchorRange(double range) { this.anchorRange = range; }
    public void setCrystalRange(double range) { this.crystalRange = range; }
    public void setHeightThreshold(double threshold) { this.heightThreshold = threshold; }
    public void setMinTargetDamage(double dmg) { this.minTargetDamage = dmg; }
    public void setMaxSelfDamage(double dmg) { this.maxSelfDamage = dmg; }
}
