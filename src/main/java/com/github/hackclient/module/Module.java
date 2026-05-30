package com.github.hackclient.module;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;

public abstract class Module {
    private final String name;
    private final String description;
    private final GameMode gameMode;
    private final String category;
    private boolean enabled;
    protected final HumanizedTimer timer;

    private long lastTickTime = 0;
    private int ticksSinceEnabled = 0;
    private boolean wasActive = false;

    public Module(String name, String description, GameMode gameMode,
                  HumanizedTimer.SkillLevel skillLevel) {
        this(name, description, gameMode, skillLevel, "general");
    }

    public Module(String name, String description, GameMode gameMode,
                  HumanizedTimer.SkillLevel skillLevel, String category) {
        this.name = name;
        this.description = description;
        this.gameMode = gameMode;
        this.category = category;
        this.enabled = false;
        this.timer = new HumanizedTimer(skillLevel);
    }

    public abstract void onTick();

    public void onEnable() {
        this.enabled = true;
        this.ticksSinceEnabled = 0;
        timer.resetSession();
        notifyToggle(true);
    }

    public void onDisable() {
        this.enabled = false;
        this.ticksSinceEnabled = 0;
        notifyToggle(false);
    }

    /** Send a chat message confirming the toggle state. Silent if chat isn't available. */
    private void notifyToggle(boolean on) {
        if (!McReflect.canSendMessage()) return;
        McReflect.sendChatMessage(on
                ? "§b[Phantom] §f" + name + " §aENABLED"
                : "§b[Phantom] §f" + name + " §cDISABLED");
    }

    public void toggle() {
        if (enabled) onDisable(); else onEnable();
    }

    protected boolean shouldAct(double baseProbability) {
        StealthEngine stealth = StealthEngine.getInstance();
        if (stealth != null) {
            if (stealth.shouldSkipTick(baseProbability)) return false;
            return stealth.canAct(category);
        }
        return Math.random() < baseProbability;
    }

    protected void recordAction() {
        StealthEngine stealth = StealthEngine.getInstance();
        if (stealth != null) stealth.recordAction(category);
        lastTickTime = System.currentTimeMillis();
    }

    protected long getSmartDelay(long baseDelay) {
        StealthEngine stealth = StealthEngine.getInstance();
        if (stealth != null) return stealth.getSmartDelay(category, baseDelay);
        return baseDelay;
    }

    protected boolean isTimerReady() {
        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();
        StealthEngine stealth = StealthEngine.getInstance();
        if (stealth != null) delay = stealth.getSmartDelay(category, delay);
        return now - lastTickTime >= delay;
    }

    protected void incrementTick() {
        ticksSinceEnabled++;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public GameMode getGameMode() { return gameMode; }
    public String getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public HumanizedTimer getTimer() { return timer; }
    public int getTicksSinceEnabled() { return ticksSinceEnabled; }
}
