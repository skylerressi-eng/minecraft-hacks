package com.github.hackclient.antidetect;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class StealthEngine {

    private static StealthEngine instance;

    private final AntiCheatAnalyzer analyzer;
    private final Map<String, Long> actionTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Integer> actionBudgets = new ConcurrentHashMap<>();

    private long sessionStartTime;
    private long sessionSeed;
    private boolean staffNearby = false;
    private boolean panicMode = false;
    private int suspicionScore = 0;
    private long lastSuspicionDecay = 0;

    private static final int MAX_SUSPICION = 100;
    private static final int PANIC_THRESHOLD = 70;
    private static final int DECAY_INTERVAL_MS = 5000;
    private static final int DECAY_AMOUNT = 5;

    private int tickCount = 0;
    private double globalActionRate = 1.0;
    private boolean legitMode = false;

    public StealthEngine(AntiCheatAnalyzer analyzer) {
        this.analyzer = analyzer;
        this.sessionStartTime = System.currentTimeMillis();
        this.sessionSeed = ThreadLocalRandom.current().nextLong();
        instance = this;
        resetBudgets();
    }

    public static StealthEngine getInstance() {
        return instance;
    }

    public void onTick() {
        tickCount++;
        long now = System.currentTimeMillis();

        if (now - lastSuspicionDecay > DECAY_INTERVAL_MS) {
            suspicionScore = Math.max(0, suspicionScore - DECAY_AMOUNT);
            lastSuspicionDecay = now;
        }

        panicMode = suspicionScore >= PANIC_THRESHOLD;

        if (tickCount % 200 == 0) {
            resetBudgets();
        }

        updateGlobalActionRate();
    }

    private void updateGlobalActionRate() {
        if (panicMode) {
            globalActionRate = 0.3;
        } else if (legitMode) {
            globalActionRate = 0.6;
        } else if (staffNearby) {
            globalActionRate = 0.4;
        } else {
            double sessionMinutes = (System.currentTimeMillis() - sessionStartTime) / 60000.0;
            globalActionRate = Math.max(0.7, 1.0 - sessionMinutes * 0.003);
        }
    }

    private void resetBudgets() {
        actionBudgets.put("combat", 20);
        actionBudgets.put("movement", 30);
        actionBudgets.put("render", 100);
        actionBudgets.put("inventory", 15);
        actionBudgets.put("world", 10);
    }

    public boolean canAct(String category) {
        if (panicMode && !"render".equals(category)) return false;

        Integer budget = actionBudgets.get(category);
        if (budget == null) budget = 10;
        if (budget <= 0) return false;

        double prob = globalActionRate;
        AntiCheatAnalyzer.BypassProfile profile = analyzer.getActiveProfile();
        if (profile != null) {
            prob *= profile.actionProbability;
        }

        return ThreadLocalRandom.current().nextDouble() < prob;
    }

    public void recordAction(String category) {
        actionBudgets.computeIfPresent(category, (k, v) -> Math.max(0, v - 1));
        actionTimestamps.put(category, System.currentTimeMillis());
    }

    public void addSuspicion(int amount) {
        suspicionScore = Math.min(MAX_SUSPICION, suspicionScore + amount);
    }

    public long getSmartDelay(String category, long baseDelay) {
        double multiplier = 1.0;

        if (panicMode) multiplier = 2.5;
        else if (staffNearby) multiplier = 1.8;
        else if (legitMode) multiplier = 1.4;

        double sessionMinutes = (System.currentTimeMillis() - sessionStartTime) / 60000.0;
        if (sessionMinutes > 15) {
            multiplier *= 1.0 + Math.min(0.3, (sessionMinutes - 15) * 0.005);
        }

        long delay = (long) (baseDelay * multiplier);
        double variance = delay * 0.25;
        delay += (long) (ThreadLocalRandom.current().nextGaussian() * variance);
        return Math.max(baseDelay / 2, delay);
    }

    public boolean shouldSkipTick(double baseProbability) {
        double effective = baseProbability * globalActionRate;
        if (panicMode) effective *= 0.3;
        if (staffNearby) effective *= 0.5;
        return ThreadLocalRandom.current().nextDouble() >= effective;
    }

    public float getMaxRotationSpeed() {
        AntiCheatAnalyzer.BypassProfile profile = analyzer.getActiveProfile();
        float base = profile != null ? (float) profile.maxRotationSpeed : 10.0f;
        if (panicMode) base *= 0.4f;
        if (staffNearby) base *= 0.6f;
        return base;
    }

    public double getMaxReachExtra() {
        AntiCheatAnalyzer.BypassProfile profile = analyzer.getActiveProfile();
        double base = profile != null ? profile.maxReachExtra : 0.0;
        if (panicMode) return 0.0;
        if (staffNearby) return 0.0;
        return base;
    }

    public double getMaxCps() {
        AntiCheatAnalyzer.BypassProfile profile = analyzer.getActiveProfile();
        double base = profile != null ? profile.maxCps : 12.0;
        if (panicMode) base *= 0.6;
        if (staffNearby) base *= 0.7;
        return base;
    }

    public void setStaffNearby(boolean nearby) {
        this.staffNearby = nearby;
        if (nearby) addSuspicion(10);
    }

    public void setLegitMode(boolean legit) {
        this.legitMode = legit;
    }

    public void triggerPanic() {
        suspicionScore = MAX_SUSPICION;
        panicMode = true;
    }

    public boolean isPanicMode() { return panicMode; }
    public boolean isStaffNearby() { return staffNearby; }
    public boolean isLegitMode() { return legitMode; }
    public int getSuspicionScore() { return suspicionScore; }
    public double getGlobalActionRate() { return globalActionRate; }
    public long getSessionSeed() { return sessionSeed; }
    public int getTickCount() { return tickCount; }
}
