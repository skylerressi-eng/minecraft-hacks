package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Triggers a right-click on the held item once the player's hunger drops below
 * the configured threshold, which causes Minecraft to consume the food if a
 * food item is currently held. We don't auto-swap the food into the hand —
 * that needs full inventory access we don't currently expose — so the user
 * should keep food in their main hand for this to do anything visible.
 */
public class AutoEat extends Module {

    private int hungerThreshold = 14;
    private long lastEatAttempt = 0;
    private static final long MIN_RETRY_MS = 1500;

    public AutoEat() {
        super("AutoEat", "Right-click held item to eat when hunger is low (keep food in hand)",
                GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "inventory");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.85)) return;
        int food = McReflect.getPlayerFoodLevel();
        if (food >= hungerThreshold) return;

        long now = System.currentTimeMillis();
        if (now - lastEatAttempt < MIN_RETRY_MS) return;

        if (McReflect.useHeldItem()) {
            lastEatAttempt = now;
            recordAction();
        }
    }

    public void setHungerThreshold(int threshold) {
        this.hungerThreshold = Math.max(1, Math.min(19, threshold));
    }

    public int getHungerThreshold() {
        return hungerThreshold;
    }
}
