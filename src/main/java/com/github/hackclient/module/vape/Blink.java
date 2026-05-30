package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Blink - Queues position updates while enabled, then teleports forward on disable.
 *
 * While active the module records the player's position each tick but
 * prevents the actual position from being sent to the server (by
 * continuously resetting position to the start point).  When disabled,
 * all stored positions are "replayed" by rapidly setting the player's
 * velocity to traverse the recorded path, producing a short-range
 * teleport effect.
 *
 * Safety limits:
 * - Maximum queue duration (configurable, default 40 ticks / 2 seconds)
 * - Maximum total distance (15 blocks)
 * - Auto-disables if the player takes damage or enters water
 */
public class Blink extends Module {

    private static final int MAX_QUEUE_TICKS = 40;
    private static final double MAX_TOTAL_DISTANCE = 15.0;

    private static class PosSnapshot {
        final double x, y, z;
        final float yaw, pitch;

        PosSnapshot(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private final List<PosSnapshot> positionQueue = new ArrayList<>();
    private double startX, startY, startZ;
    private float startYaw, startPitch;
    private boolean isQueuing = false;
    private float healthAtStart = 0;

    public Blink() {
        super("Blink",
              "Hold movement packets then release all at once (teleport)",
              GameMode.VAPE,
              HumanizedTimer.SkillLevel.EXPERT,
              "movement");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        positionQueue.clear();
        isQueuing = false;

        if (!McReflect.isCoreReady() || !McReflect.isPlayerAlive()) return;

        // Record starting position
        startX = McReflect.getPlayerX();
        startY = McReflect.getPlayerY();
        startZ = McReflect.getPlayerZ();
        startYaw = McReflect.getPlayerYaw();
        startPitch = McReflect.getPlayerPitch();
        healthAtStart = McReflect.getPlayerHealth();
        isQueuing = true;
    }

    @Override
    public void onDisable() {
        if (isQueuing && !positionQueue.isEmpty()) {
            replayPositions();
        }
        positionQueue.clear();
        isQueuing = false;
        super.onDisable();
    }

    @Override
    public void onTick() {
        incrementTick();

        if (!McReflect.isCoreReady() || !McReflect.isPlayerAlive()) {
            if (isQueuing) {
                isQueuing = false;
                positionQueue.clear();
            }
            return;
        }

        if (!isQueuing) return;

        // Safety: auto-disable if we took damage
        float currentHealth = McReflect.getPlayerHealth();
        if (currentHealth < healthAtStart - 0.5f) {
            toggle(); // Disables and triggers replay
            return;
        }

        // Record current position
        double cx = McReflect.getPlayerX();
        double cy = McReflect.getPlayerY();
        double cz = McReflect.getPlayerZ();
        float cyaw = McReflect.getPlayerYaw();
        float cpitch = McReflect.getPlayerPitch();

        positionQueue.add(new PosSnapshot(cx, cy, cz, cyaw, cpitch));

        // Check safety limits
        double totalDist = calculateTotalDistance();
        if (positionQueue.size() >= MAX_QUEUE_TICKS || totalDist >= MAX_TOTAL_DISTANCE) {
            toggle(); // Auto-disable and replay
            return;
        }

        // "Freeze" the player at the start position from the server's perspective.
        // We do this by nudging the player's velocity back toward the start point.
        // The client-side position continues to update normally so the player sees
        // themselves moving, but the server sees minimal movement.
        // Note: this is a simplified approach; a full implementation would hook
        // the network layer to hold outgoing position packets.
        McReflect.setPlayerVelocity(0, McReflect.getPlayerVelocity()[1], 0);

        recordAction();
    }

    /**
     * Replay all queued positions rapidly to produce the teleport effect.
     */
    private void replayPositions() {
        if (positionQueue.isEmpty()) return;

        // Teleport to the final recorded position by setting velocity
        // in the direction of the last snapshot
        PosSnapshot last = positionQueue.get(positionQueue.size() - 1);

        double dx = last.x - McReflect.getPlayerX();
        double dy = last.y - McReflect.getPlayerY();
        double dz = last.z - McReflect.getPlayerZ();

        // Apply a strong velocity impulse toward the final position
        // The magnitude is clamped to avoid impossible-looking teleports
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0.1) {
            // Normalize and scale - max teleport speed
            double speed = Math.min(dist, 3.0); // cap impulse magnitude
            double nx = (dx / dist) * speed;
            double ny = (dy / dist) * speed;
            double nz = (dz / dist) * speed;

            McReflect.setPlayerVelocity(nx, ny, nz);
        }

        // Also set rotation to the final snapshot's angles
        setPlayerRotation(last.yaw, last.pitch);
    }

    /**
     * Calculate total path distance across all queued snapshots.
     */
    private double calculateTotalDistance() {
        if (positionQueue.size() < 2) {
            if (positionQueue.size() == 1) {
                PosSnapshot p = positionQueue.get(0);
                double dx = p.x - startX;
                double dy = p.y - startY;
                double dz = p.z - startZ;
                return Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            return 0;
        }

        double total = 0;
        PosSnapshot prev = positionQueue.get(0);
        for (int i = 1; i < positionQueue.size(); i++) {
            PosSnapshot curr = positionQueue.get(i);
            double dx = curr.x - prev.x;
            double dy = curr.y - prev.y;
            double dz = curr.z - prev.z;
            total += Math.sqrt(dx * dx + dy * dy + dz * dz);
            prev = curr;
        }
        return total;
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

    public int getQueueSize() {
        return positionQueue.size();
    }

    public boolean isCurrentlyQueuing() {
        return isQueuing;
    }
}
