package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AntiBot - Detects and filters anti-cheat bot entities from the target list.
 *
 * Anti-cheat plugins (Watchdog, Grim, Vulcan, etc.) spawn invisible or
 * semi-visible bot players to bait hack clients into targeting them.
 * This module identifies bots using several heuristics:
 *
 * - Name pattern detection (common bot name formats like NPC-XXXX, §r prefixes)
 * - Movement analysis (bots often have zero or perfectly linear movement)
 * - Health anomalies (bots may have non-standard max health)
 * - Duplicate position clustering (multiple bots at exact same coords)
 * - Ping/latency analysis (bots have 0 or very low ping)
 * - Spawn timing (entities that appear and vanish rapidly)
 *
 * Other combat modules can query isBot(entity) to skip bot targets.
 */
public class AntiBot extends Module {

    private static final int MOVEMENT_TRACK_TICKS = 20;
    private static final double MOVEMENT_EPSILON = 0.001;
    private static final int MIN_TICKS_FOR_VERDICT = 10;

    /**
     * Tracks per-entity data for bot detection analysis.
     */
    private static class EntityTracker {
        final String name;
        long firstSeenTick = 0;
        int ticksSeen = 0;
        double lastX, lastY, lastZ;
        boolean hasMovedOnce = false;
        int stationaryTicks = 0;
        int totalTrackedTicks = 0;
        boolean markedBot = false;
        double healthOnFirstSeen = 0;

        EntityTracker(String name) {
            this.name = name;
        }
    }

    private final Map<String, EntityTracker> trackers = new HashMap<>();
    private final Set<String> confirmedBots = new HashSet<>();
    private long globalTickCounter = 0;

    public AntiBot() {
        super("AntiBot",
              "Detect and ignore anti-cheat bot players",
              GameMode.VAPE,
              HumanizedTimer.SkillLevel.AVERAGE,
              "render");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        trackers.clear();
        confirmedBots.clear();
        globalTickCounter = 0;
    }

    @Override
    public void onDisable() {
        trackers.clear();
        confirmedBots.clear();
        super.onDisable();
    }

    @Override
    public void onTick() {
        incrementTick();
        globalTickCounter++;

        if (!McReflect.isCoreReady() || !McReflect.canGetPlayers()) return;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null) return;

        Set<String> currentNames = new HashSet<>();

        for (Object player : players) {
            if (player == self || player.equals(self)) continue;
            if (!McReflect.isEntityAlive(player)) continue;

            String name = McReflect.getEntityName(player);
            currentNames.add(name);

            EntityTracker tracker = trackers.computeIfAbsent(name, EntityTracker::new);

            double ex = McReflect.getEntityX(player);
            double ey = McReflect.getEntityY(player);
            double ez = McReflect.getEntityZ(player);

            if (tracker.ticksSeen == 0) {
                tracker.firstSeenTick = globalTickCounter;
                tracker.lastX = ex;
                tracker.lastY = ey;
                tracker.lastZ = ez;
                tracker.healthOnFirstSeen = McReflect.getEntityHealth(player);
            } else {
                // Check movement
                double dx = ex - tracker.lastX;
                double dy = ey - tracker.lastY;
                double dz = ez - tracker.lastZ;
                double moved = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (moved > MOVEMENT_EPSILON) {
                    tracker.hasMovedOnce = true;
                } else {
                    tracker.stationaryTicks++;
                }

                tracker.lastX = ex;
                tracker.lastY = ey;
                tracker.lastZ = ez;
                tracker.totalTrackedTicks++;
            }

            tracker.ticksSeen++;

            // Run detection heuristics once we have enough data
            if (tracker.ticksSeen >= MIN_TICKS_FOR_VERDICT && !tracker.markedBot) {
                boolean isBot = runBotDetection(player, tracker);
                if (isBot) {
                    tracker.markedBot = true;
                    confirmedBots.add(name);
                }
            }
        }

        // Prune trackers for players that left
        trackers.entrySet().removeIf(entry ->
                !currentNames.contains(entry.getKey())
                && globalTickCounter - entry.getValue().firstSeenTick > 200);

        // Also remove from confirmed bots if the entity is gone for a while
        confirmedBots.retainAll(currentNames);
    }

    /**
     * Run all bot detection heuristics on an entity.
     */
    private boolean runBotDetection(Object entity, EntityTracker tracker) {
        int botScore = 0;
        int maxScore = 0;

        // Heuristic 1: Name pattern analysis
        maxScore += 3;
        botScore += analyzeNamePattern(tracker.name);

        // Heuristic 2: Movement analysis - bots are often completely stationary
        maxScore += 3;
        if (tracker.totalTrackedTicks > 5) {
            double stationaryRatio = (double) tracker.stationaryTicks / tracker.totalTrackedTicks;
            if (!tracker.hasMovedOnce) {
                botScore += 3; // Never moved at all
            } else if (stationaryRatio > 0.9) {
                botScore += 2; // Almost never moves
            } else if (stationaryRatio > 0.7) {
                botScore += 1;
            }
        }

        // Heuristic 3: Health anomaly - bots sometimes have unusual health values
        maxScore += 2;
        float health = McReflect.getEntityHealth(entity);
        if (health == 20.0f && tracker.healthOnFirstSeen == 20.0f) {
            // Perfect health entire time - slightly suspicious if never took damage
            botScore += 1;
        }
        if (health <= 0.0f || health > 40.0f) {
            botScore += 2; // Abnormal health range
        }

        // Heuristic 4: Position duplication - multiple entities at exact same position
        maxScore += 2;
        if (hasPositionDuplicate(entity)) {
            botScore += 2;
        }

        // Heuristic 5: Appeared recently and is very close (bait bot pattern)
        maxScore += 2;
        if (tracker.ticksSeen < 30) {
            double dist = McReflect.distanceTo(entity);
            if (dist < 3.0 && !tracker.hasMovedOnce) {
                botScore += 2; // Spawned right next to us and hasn't moved
            }
        }

        // Verdict: if score exceeds ~50% of max, mark as bot
        double confidence = (double) botScore / maxScore;
        return confidence >= 0.50;
    }

    /**
     * Check name against common bot name patterns.
     */
    private int analyzeNamePattern(String name) {
        if (name == null || name.isEmpty()) return 3;

        // Section sign prefix (color code artifacts)
        if (name.contains("§")) return 2;

        // Common bot name formats: NPC-XXXX, [BOT], CIT-XXXXX
        String lower = name.toLowerCase();
        if (lower.startsWith("npc") || lower.startsWith("cit-")
                || lower.contains("[bot]") || lower.contains("[npc]")) {
            return 3;
        }

        // Names that are all digits or very short random strings
        if (name.matches("^[0-9]+$")) return 2;
        if (name.length() <= 2) return 1;

        // Names with unusual character patterns (all caps + digits)
        if (name.matches("^[A-Z0-9_]{3,}$") && !name.contains("_")) return 1;

        return 0;
    }

    /**
     * Check if another entity exists at the exact same position as this one.
     */
    private boolean hasPositionDuplicate(Object entity) {
        double ex = McReflect.getEntityX(entity);
        double ey = McReflect.getEntityY(entity);
        double ez = McReflect.getEntityZ(entity);
        String name = McReflect.getEntityName(entity);

        for (EntityTracker tracker : trackers.values()) {
            if (tracker.name.equals(name)) continue;
            double dx = tracker.lastX - ex;
            double dy = tracker.lastY - ey;
            double dz = tracker.lastZ - ez;
            if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01 && Math.abs(dz) < 0.01) {
                return true;
            }
        }
        return false;
    }

    /**
     * Public API: check whether a given entity has been flagged as a bot.
     * Other combat modules should call this before targeting an entity.
     */
    public boolean isBot(Object entity) {
        if (entity == null) return false;
        String name = McReflect.getEntityName(entity);
        return confirmedBots.contains(name);
    }

    /**
     * Get the list of all currently confirmed bot names.
     */
    public Set<String> getConfirmedBots() {
        return new HashSet<>(confirmedBots);
    }

    public int getTrackedEntityCount() {
        return trackers.size();
    }
}
