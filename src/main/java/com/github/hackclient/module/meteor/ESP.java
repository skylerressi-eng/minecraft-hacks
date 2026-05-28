package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ESP (Extra Sensory Perception) tracks nearby players and maintains
 * a sorted list of player data for HUD rendering. The actual rendering
 * is handled by the HUD overlay system; this module provides the data.
 */
public class ESP extends Module {

    /** Data class holding tracked player information for the HUD */
    public static class TrackedPlayer {
        public final String name;
        public final double distance;
        public final float health;
        public final double x, y, z;
        public final boolean alive;

        public TrackedPlayer(String name, double distance, float health,
                             double x, double y, double z, boolean alive) {
            this.name = name;
            this.distance = distance;
            this.health = health;
            this.x = x;
            this.y = y;
            this.z = z;
            this.alive = alive;
        }
    }

    private static final double MAX_RANGE = 256.0;
    private volatile List<TrackedPlayer> trackedPlayers = Collections.emptyList();

    public ESP() {
        super("ESP", "See players and entities through walls with colored outlines",
              GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "render");
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null) {
            trackedPlayers = Collections.emptyList();
            return;
        }

        // Render category has generous budgets, but still gate
        if (!shouldAct(0.95)) return;

        List<Object> worldPlayers = McReflect.getPlayers();
        List<TrackedPlayer> newTracked = new ArrayList<>();

        double playerX = McReflect.getPlayerX();
        double playerY = McReflect.getPlayerY();
        double playerZ = McReflect.getPlayerZ();

        for (Object entity : worldPlayers) {
            if (entity == null || entity.equals(player)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist > MAX_RANGE) continue;

            String name = McReflect.getEntityName(entity);
            float health = McReflect.getEntityHealth(entity);
            boolean alive = McReflect.isEntityAlive(entity);
            double ex = McReflect.getEntityX(entity);
            double ey = McReflect.getEntityY(entity);
            double ez = McReflect.getEntityZ(entity);

            newTracked.add(new TrackedPlayer(name, dist, health, ex, ey, ez, alive));
        }

        // Sort by distance (nearest first) for HUD display priority
        newTracked.sort((a, b) -> Double.compare(a.distance, b.distance));

        trackedPlayers = Collections.unmodifiableList(newTracked);
        recordAction();
    }

    /**
     * Returns the current list of tracked players, sorted by distance.
     * Thread-safe for reading from the render thread.
     */
    public List<TrackedPlayer> getTrackedPlayers() {
        return trackedPlayers;
    }

    /**
     * Get the count of tracked players within a specific range.
     */
    public int getPlayerCountInRange(double range) {
        int count = 0;
        for (TrackedPlayer tp : trackedPlayers) {
            if (tp.distance <= range) count++;
        }
        return count;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        trackedPlayers = Collections.emptyList();
    }
}
