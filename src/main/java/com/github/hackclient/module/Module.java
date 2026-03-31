package com.github.hackclient.module;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;

/**
 * Base class for all hack modules.
 * Each module belongs to a game mode and uses humanized timing for anti-detection.
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final GameMode gameMode;
    private boolean enabled;
    protected final HumanizedTimer timer;

    public Module(String name, String description, GameMode gameMode,
                  HumanizedTimer.SkillLevel skillLevel) {
        this.name = name;
        this.description = description;
        this.gameMode = gameMode;
        this.enabled = false;
        this.timer = new HumanizedTimer(skillLevel);
    }

    /** Called every game tick when the module is enabled */
    public abstract void onTick();

    /** Called when the module is toggled on */
    public void onEnable() {
        this.enabled = true;
        timer.resetSession();
    }

    /** Called when the module is toggled off */
    public void onDisable() {
        this.enabled = false;
    }

    public void toggle() {
        if (enabled) {
            onDisable();
        } else {
            onEnable();
        }
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public GameMode getGameMode() { return gameMode; }
    public boolean isEnabled() { return enabled; }
    public HumanizedTimer getTimer() { return timer; }
}
