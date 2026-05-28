package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class AutoEat extends Module {
    private int hungerThreshold = 14;
    private long lastEatAttempt = 0;

    public AutoEat() {
        super("AutoEat", "Auto-eat food when hunger drops below threshold", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "inventory");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.85)) return;
        int food = McReflect.getPlayerFoodLevel();
        long now = System.currentTimeMillis();

        if (food < hungerThreshold && now - lastEatAttempt > 3000) {
            lastEatAttempt = now;
            recordAction();
        }
    }

    public void setHungerThreshold(int threshold) { this.hungerThreshold = threshold; }
}
