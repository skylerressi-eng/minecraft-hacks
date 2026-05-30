package com.github.hackclient.module.crystal;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;


/**
 * Anchor Aura - Uses respawn anchors as explosive weapons in the overworld/end.
 *
 * Respawn anchors explode when activated outside the Nether. This module:
 * 1. Finds nearest enemy target
 * 2. Calculates optimal anchor placement position
 * 3. Places the anchor
 * 4. Charges it with glowstone
 * 5. Right-clicks to detonate
 *
 * Each stage is separated by humanized timing to avoid detection.
 */
public class AnchorAura extends Module {

    private long lastActionTime = 0;
    private int stage = 0; // 0=find/place, 1=charge, 2=detonate
    private double[] anchorPosition = null;
    private Object currentTarget = null;

    // Smooth aim state
    private float currentYaw;
    private float currentPitch;

    // Settings
    private double placeRange = 5.0;
    private double minTargetDamage = 6.0;
    private double maxSelfDamage = 10.0;

    // Anchor explosion constants (similar to crystal but slightly different)
    private static final double ANCHOR_EXPLOSION_POWER = 5.0;
    private static final double ANCHOR_MAX_DAMAGE_RANGE = 10.0;

    public AnchorAura() {
        super("AnchorAura",
              "Auto place/charge/detonate respawn anchors in combat",
              GameMode.CRYSTAL,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        stage = 0;
        anchorPosition = null;
        currentTarget = null;
        currentYaw = McReflect.getPlayerYaw();
        currentPitch = McReflect.getPlayerPitch();
    }

    @Override
    public void onTick() {
        if (!McReflect.isPlayerAlive()) return;
        incrementTick();

        if (!isTimerReady()) return;

        // Find target
        currentTarget = findNearestEnemy();
        if (currentTarget == null) {
            stage = 0;
            anchorPosition = null;
            return;
        }

        switch (stage) {
            case 0:
                handlePlaceStage();
                break;
            case 1:
                handleChargeStage();
                break;
            case 2:
                handleDetonateStage();
                break;
        }
    }

    /**
     * Stage 0: Find best placement and place the anchor.
     */
    private void handlePlaceStage() {
        // Calculate best anchor position near the target
        anchorPosition = calculateBestAnchorPlacement(currentTarget);
        if (anchorPosition == null) return;

        // Validate damage
        double targetDmg = calculateExplosionDamage(anchorPosition, currentTarget);
        double selfDmg = calculateSelfDamage(anchorPosition);
        if (targetDmg < minTargetDamage || selfDmg > maxSelfDamage) return;

        // Smooth aim to placement position
        float[] angles = getAnglesToPosition(anchorPosition);
        if (angles != null) {
            float speed = 0.5f + (float) (Math.random() * 0.12);
            currentYaw = AntiCheatBypass.smoothRotation(currentYaw,
                    AntiCheatBypass.addRotationNoise(angles[0]), speed);
            currentPitch = AntiCheatBypass.smoothRotation(currentPitch,
                    AntiCheatBypass.addRotationNoise(angles[1]), speed);
        }

        if (shouldAct(0.90)) {
            // Switch to anchor slot and place via right-click
            McReflect.swingHand();
            stage = 1;
            lastActionTime = System.currentTimeMillis();
            recordAction();
        }
    }

    /**
     * Stage 1: Charge the placed anchor with glowstone.
     */
    private void handleChargeStage() {
        if (anchorPosition == null) {
            stage = 0;
            return;
        }

        // Aim at the placed anchor
        float[] angles = getAnglesToPosition(anchorPosition);
        if (angles != null) {
            float speed = 0.55f + (float) (Math.random() * 0.1);
            currentYaw = AntiCheatBypass.smoothRotation(currentYaw,
                    AntiCheatBypass.addRotationNoise(angles[0]), speed);
            currentPitch = AntiCheatBypass.smoothRotation(currentPitch,
                    AntiCheatBypass.addRotationNoise(angles[1]), speed);
        }

        if (shouldAct(0.93)) {
            // Switch to glowstone and right-click anchor to charge
            McReflect.swingHand();
            stage = 2;
            lastActionTime = System.currentTimeMillis();
            recordAction();
        }
    }

    /**
     * Stage 2: Detonate the charged anchor when target is in blast radius.
     */
    private void handleDetonateStage() {
        if (anchorPosition == null || currentTarget == null) {
            stage = 0;
            return;
        }

        // Check if target is close enough to the anchor for effective damage
        double tx = McReflect.getEntityX(currentTarget);
        double ty = McReflect.getEntityY(currentTarget);
        double tz = McReflect.getEntityZ(currentTarget);
        double dx = tx - anchorPosition[0];
        double dy = ty - anchorPosition[1];
        double dz = tz - anchorPosition[2];
        double distToAnchor = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distToAnchor > 4.0) return; // Wait for target to be closer

        // Aim at the anchor to detonate
        float[] angles = getAnglesToPosition(anchorPosition);
        if (angles != null) {
            float speed = 0.6f + (float) (Math.random() * 0.1);
            currentYaw = AntiCheatBypass.smoothRotation(currentYaw,
                    AntiCheatBypass.addRotationNoise(angles[0]), speed);
            currentPitch = AntiCheatBypass.smoothRotation(currentPitch,
                    AntiCheatBypass.addRotationNoise(angles[1]), speed);
        }

        if (shouldAct(0.95)) {
            // Right-click anchor without glowstone to detonate (causes explosion outside Nether)
            McReflect.swingHand();
            stage = 0;
            anchorPosition = null;
            lastActionTime = System.currentTimeMillis();
            recordAction();
        }
    }

    /**
     * Find the nearest living enemy player within range.
     */
    private Object findNearestEnemy() {
        Object self = McReflect.getPlayer();
        if (self == null) return null;

        Object nearest = null;
        double nearestDist = placeRange + 3.0;

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
     * Calculate the best anchor placement position near the target.
     * Prefers positions between the player and target for fast detonation.
     */
    private double[] calculateBestAnchorPlacement(Object target) {
        double tx = McReflect.getEntityX(target);
        double ty = McReflect.getEntityY(target);
        double tz = McReflect.getEntityZ(target);
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY();
        double pz = McReflect.getPlayerZ();

        // Try positions at target's feet level, offset in each direction
        int[][] offsets = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {0, -1, 0}, {1, 0, 1}, {-1, 0, -1}
        };

        double[] best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int[] off : offsets) {
            double ax = Math.floor(tx) + off[0] + 0.5;
            double ay = Math.floor(ty) + off[1];
            double az = Math.floor(tz) + off[2] + 0.5;

            double distToPlayer = Math.sqrt(
                    (ax - px) * (ax - px) + (ay - py) * (ay - py) + (az - pz) * (az - pz));
            if (distToPlayer > placeRange) continue;

            double[] pos = {ax, ay, az};
            double targetDmg = calculateExplosionDamage(pos, target);
            double selfDmg = calculateSelfDamage(pos);

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
     * Calculate anchor explosion damage to an entity.
     */
    private double calculateExplosionDamage(double[] anchorPos, Object entity) {
        double ex = McReflect.getEntityX(entity);
        double ey = McReflect.getEntityY(entity) + 1.0;
        double ez = McReflect.getEntityZ(entity);

        double dx = ex - anchorPos[0];
        double dy = ey - anchorPos[1];
        double dz = ez - anchorPos[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist >= ANCHOR_MAX_DAMAGE_RANGE) return 0;

        double exposure = 1.0 - (dist / ANCHOR_MAX_DAMAGE_RANGE);
        double damage = exposure * ANCHOR_EXPLOSION_POWER * 2.0;
        return Math.max(0, damage * 0.6);
    }

    /**
     * Calculate self-damage from an anchor explosion.
     */
    private double calculateSelfDamage(double[] anchorPos) {
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY() + 1.0;
        double pz = McReflect.getPlayerZ();

        double dx = px - anchorPos[0];
        double dy = py - anchorPos[1];
        double dz = pz - anchorPos[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist >= ANCHOR_MAX_DAMAGE_RANGE) return 0;

        double exposure = 1.0 - (dist / ANCHOR_MAX_DAMAGE_RANGE);
        double damage = exposure * ANCHOR_EXPLOSION_POWER * 2.0;
        return Math.max(0, damage * 0.6);
    }

    /**
     * Calculate yaw/pitch angles to a world position.
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
        stage = 0;
        anchorPosition = null;
        currentTarget = null;
    }

    public void setPlaceRange(double range) { this.placeRange = range; }
    public void setMinTargetDamage(double dmg) { this.minTargetDamage = dmg; }
    public void setMaxSelfDamage(double dmg) { this.maxSelfDamage = dmg; }
    public int getStage() { return stage; }
}
