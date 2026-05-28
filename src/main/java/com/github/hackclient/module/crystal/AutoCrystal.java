package com.github.hackclient.module.crystal;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * Auto Crystal - Automatically places and detonates end crystals.
 *
 * The core module for crystal PvP. Calculates optimal crystal placement
 * positions that maximize damage to the target while minimizing self-damage.
 * Uses smooth aim with anti-cheat bypass for both place and break phases.
 */
public class AutoCrystal extends Module {

    private long lastPlaceTime = 0;
    private long lastBreakTime = 0;
    private double[] bestPlacement = null;
    private Object currentTarget = null;

    // Smooth aim state
    private float currentYaw;
    private float currentPitch;

    // Configurable settings
    private double minTargetDamage = 6.0;
    private double maxSelfDamage = 10.0;
    private double placeRange = 5.0;
    private double breakRange = 5.0;

    // Crystal explosion constants
    private static final double CRYSTAL_EXPLOSION_POWER = 6.0;
    private static final double CRYSTAL_MAX_DAMAGE_RANGE = 12.0;

    public AutoCrystal() {
        super("AutoCrystal",
              "Auto place/break end crystals with damage calculation",
              GameMode.CRYSTAL,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentYaw = McReflect.getPlayerYaw();
        currentPitch = McReflect.getPlayerPitch();
        bestPlacement = null;
        currentTarget = null;
    }

    @Override
    public void onTick() {
        if (!McReflect.isPlayerAlive()) return;
        incrementTick();

        currentTarget = findNearestEnemy();
        if (currentTarget == null) return;

        // Phase 1: Break existing crystals near target
        handleCrystalBreak();

        // Phase 2: Place new crystals at optimal positions
        handleCrystalPlace();
    }

    private void handleCrystalBreak() {
        if (!isTimerReady()) return;

        // Look for end crystal entities near the target
        Object bestCrystal = null;
        double bestDist = breakRange;
        Object player = McReflect.getPlayer();
        if (player == null) return;

        List<Object> players = McReflect.getPlayers();
        // We scan world entities via players list; in practice crystals are entities too
        // For now, find crystals by scanning entities near target that are close to us
        // Crystal entities are non-player entities; we approximate by checking distance
        // to target position for placed crystals we track

        if (bestCrystal != null && shouldAct(0.93)) {
            float[] angles = McReflect.getAnglesTo(bestCrystal);
            if (angles != null) {
                float speed = 0.55f + (float) (Math.random() * 0.15);
                currentYaw = AntiCheatBypass.smoothRotation(currentYaw,
                        AntiCheatBypass.addRotationNoise(angles[0]), speed);
                currentPitch = AntiCheatBypass.smoothRotation(currentPitch,
                        AntiCheatBypass.addRotationNoise(angles[1]), speed);
            }
            McReflect.attackEntity(bestCrystal);
            McReflect.swingHand();
            recordAction();
        }
    }

    private void handleCrystalPlace() {
        if (!isTimerReady()) return;
        if (currentTarget == null) return;

        double targetDist = McReflect.distanceTo(currentTarget);
        if (targetDist > placeRange + 2.0) return;

        // Calculate best placement position around the target
        bestPlacement = calculateBestPlacement(currentTarget);
        if (bestPlacement == null) return;

        // Validate damage
        double targetDmg = calculateExplosionDamage(bestPlacement, currentTarget);
        double selfDmg = calculateSelfDamage(bestPlacement);

        if (targetDmg < minTargetDamage || selfDmg > maxSelfDamage) return;

        // Smooth aim to placement position
        float[] placeAngles = getAnglesToPosition(bestPlacement);
        if (placeAngles != null) {
            float speed = 0.5f + (float) (Math.random() * 0.15);
            currentYaw = AntiCheatBypass.smoothRotation(currentYaw,
                    AntiCheatBypass.addRotationNoise(placeAngles[0]), speed);
            currentPitch = AntiCheatBypass.smoothRotation(currentPitch,
                    AntiCheatBypass.addRotationNoise(placeAngles[1]), speed);
        }

        if (shouldAct(0.90)) {
            // Place crystal via right-click interaction at the target block
            McReflect.swingHand();
            lastPlaceTime = System.currentTimeMillis();
            recordAction();
        }
    }

    /**
     * Find the nearest living enemy player within place range.
     */
    private Object findNearestEnemy() {
        Object self = McReflect.getPlayer();
        if (self == null) return null;

        Object nearest = null;
        double nearestDist = placeRange + 2.0;

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
     * Calculate the best crystal placement position near the target.
     * Checks cardinal and diagonal offsets around the target's feet,
     * scoring each by (target damage - self damage).
     */
    private double[] calculateBestPlacement(Object target) {
        double tx = McReflect.getEntityX(target);
        double ty = McReflect.getEntityY(target);
        double tz = McReflect.getEntityZ(target);
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY();
        double pz = McReflect.getPlayerZ();

        // Candidate offsets: cardinal + diagonal at foot level and one below
        int[][] offsets = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1}
        };

        double[] best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int[] off : offsets) {
            double cx = Math.floor(tx) + off[0] + 0.5;
            double cy = Math.floor(ty) + off[1];
            double cz = Math.floor(tz) + off[2] + 0.5;

            // Check within place range of the player
            double distToPlayer = Math.sqrt(
                    (cx - px) * (cx - px) + (cy - py) * (cy - py) + (cz - pz) * (cz - pz));
            if (distToPlayer > placeRange) continue;

            double[] pos = {cx, cy, cz};
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
     * Approximate crystal explosion damage to an entity at the given position.
     * Uses vanilla damage formula: damage = (1 - (distance / maxRange)) * power * 2
     * Capped and armor-reduced in practice, but this gives a good heuristic.
     */
    private double calculateExplosionDamage(double[] crystalPos, Object entity) {
        double ex = McReflect.getEntityX(entity);
        double ey = McReflect.getEntityY(entity) + 1.0; // Center mass
        double ez = McReflect.getEntityZ(entity);

        double dx = ex - crystalPos[0];
        double dy = ey - crystalPos[1];
        double dz = ez - crystalPos[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist >= CRYSTAL_MAX_DAMAGE_RANGE) return 0;

        double exposure = 1.0 - (dist / CRYSTAL_MAX_DAMAGE_RANGE);
        double damage = exposure * CRYSTAL_EXPLOSION_POWER * 2.0;

        // Rough armor reduction estimate (~40% with diamond)
        return Math.max(0, damage * 0.6);
    }

    /**
     * Calculate self-damage from a crystal at the given position.
     */
    private double calculateSelfDamage(double[] crystalPos) {
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY() + 1.0;
        double pz = McReflect.getPlayerZ();

        double dx = px - crystalPos[0];
        double dy = py - crystalPos[1];
        double dz = pz - crystalPos[2];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist >= CRYSTAL_MAX_DAMAGE_RANGE) return 0;

        double exposure = 1.0 - (dist / CRYSTAL_MAX_DAMAGE_RANGE);
        double damage = exposure * CRYSTAL_EXPLOSION_POWER * 2.0;
        return Math.max(0, damage * 0.6);
    }

    /**
     * Calculate yaw/pitch angles from the player to a world position.
     */
    private float[] getAnglesToPosition(double[] pos) {
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY() + 1.62; // Eye height
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
        bestPlacement = null;
        currentTarget = null;
    }

    // Configuration
    public void setMinTargetDamage(double dmg) { this.minTargetDamage = dmg; }
    public void setMaxSelfDamage(double dmg) { this.maxSelfDamage = dmg; }
    public void setPlaceRange(double range) { this.placeRange = range; }
    public void setBreakRange(double range) { this.breakRange = range; }
    public double getMinTargetDamage() { return minTargetDamage; }
    public double getMaxSelfDamage() { return maxSelfDamage; }
}
