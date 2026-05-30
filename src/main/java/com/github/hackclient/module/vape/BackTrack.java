package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BackTrack - Extends effective hitbox by considering recent entity positions.
 *
 * Records each nearby player's position over the last 3-5 ticks and, when
 * the player attacks, checks whether any stored historical position was
 * closer than the entity's current position.  If so, the attack is fired
 * against the entity at that advantageous moment, effectively extending
 * the player's perceived reach without modifying packets.
 *
 * This exploits the fact that the server accepts hits validated against
 * positions within a small time window (lag compensation).
 */
public class BackTrack extends Module {

    private static final int MAX_HISTORY_TICKS = 5;
    private static final double MAX_TRACK_DISTANCE = 6.0;
    private static final double ATTACK_RANGE = 3.5;
    private static final double BACKTRACK_RANGE_BONUS = 1.0;

    /**
     * Stores a snapshot of an entity's position at a given tick.
     */
    private static class PositionSnapshot {
        final double x, y, z;
        final long timestamp;

        PositionSnapshot(double x, double y, double z, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
        }
    }

    // Entity name -> list of recent position snapshots
    private final Map<String, List<PositionSnapshot>> positionHistory = new HashMap<>();
    private long tickCounter = 0;

    public BackTrack() {
        super("BackTrack",
              "Delays enemy movement packets to extend your hit range",
              GameMode.VAPE,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        positionHistory.clear();
        tickCounter = 0;
    }

    @Override
    public void onDisable() {
        positionHistory.clear();
        super.onDisable();
    }

    @Override
    public void onTick() {
        incrementTick();
        tickCounter++;

        if (!McReflect.isCoreReady() || !McReflect.isPlayerAlive()) return;
        if (!McReflect.canGetPlayers()) return;

        long now = System.currentTimeMillis();
        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null) return;

        double myX = McReflect.getPlayerX();
        double myY = McReflect.getPlayerY();
        double myZ = McReflect.getPlayerZ();

        // Phase 1: Record positions for all nearby players
        for (Object player : players) {
            if (player == self || player.equals(self)) continue;
            if (!McReflect.isEntityAlive(player)) continue;

            double dist = McReflect.distanceTo(player);
            if (dist > MAX_TRACK_DISTANCE) continue;

            String name = McReflect.getEntityName(player);
            double ex = McReflect.getEntityX(player);
            double ey = McReflect.getEntityY(player);
            double ez = McReflect.getEntityZ(player);

            List<PositionSnapshot> history = positionHistory.computeIfAbsent(name, k -> new ArrayList<>());
            history.add(new PositionSnapshot(ex, ey, ez, now));

            // Trim old entries beyond our tracking window
            while (history.size() > MAX_HISTORY_TICKS) {
                history.remove(0);
            }
        }

        // Prune entities that haven't been seen recently
        positionHistory.entrySet().removeIf(entry -> {
            List<PositionSnapshot> hist = entry.getValue();
            if (hist.isEmpty()) return true;
            return now - hist.get(hist.size() - 1).timestamp > 500; // 500ms staleness
        });

        // Phase 2: Check for backtrack attack opportunity
        if (!isTimerReady()) return;
        if (!shouldAct(0.90)) return;

        for (Object player : players) {
            if (player == self || player.equals(self)) continue;
            if (!McReflect.isEntityAlive(player)) continue;

            double currentDist = McReflect.distanceTo(player);
            // Skip if already in normal range or way too far
            if (currentDist <= ATTACK_RANGE || currentDist > ATTACK_RANGE + BACKTRACK_RANGE_BONUS * 2) {
                continue;
            }

            String name = McReflect.getEntityName(player);
            List<PositionSnapshot> history = positionHistory.get(name);
            if (history == null || history.size() < 2) continue;

            // Check if any historical position is within attack range
            PositionSnapshot bestSnap = null;
            double bestDist = currentDist;

            for (PositionSnapshot snap : history) {
                double dx = snap.x - myX;
                double dy = snap.y - myY;
                double dz = snap.z - myZ;
                double snapDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (snapDist < bestDist && snapDist <= ATTACK_RANGE + BACKTRACK_RANGE_BONUS) {
                    bestDist = snapDist;
                    bestSnap = snap;
                }
            }

            if (bestSnap != null && bestDist <= ATTACK_RANGE + BACKTRACK_RANGE_BONUS) {
                // The entity was closer in a recent tick - fire the attack now.
                // Server-side lag compensation should accept the hit.
                McReflect.attackEntity(player);
                McReflect.swingHand();
                recordAction();
                break; // Only one backtrack attack per tick
            }
        }
    }

    /**
     * Query whether an entity has a backtracked position closer than its
     * current position.  Other modules (e.g. HitSelect) can use this.
     */
    public double getClosestHistoricalDistance(Object entity) {
        if (entity == null) return 999;
        String name = McReflect.getEntityName(entity);
        List<PositionSnapshot> history = positionHistory.get(name);
        if (history == null || history.isEmpty()) return McReflect.distanceTo(entity);

        double myX = McReflect.getPlayerX();
        double myY = McReflect.getPlayerY();
        double myZ = McReflect.getPlayerZ();
        double best = McReflect.distanceTo(entity);

        for (PositionSnapshot snap : history) {
            double dx = snap.x - myX;
            double dy = snap.y - myY;
            double dz = snap.z - myZ;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < best) best = d;
        }
        return best;
    }

    public int getTrackedEntityCount() {
        return positionHistory.size();
    }
}
