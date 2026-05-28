package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class AutoArmor extends Module {
    private long lastSwapTime = 0;

    public AutoArmor() {
        super("AutoArmor", "Auto-equip best armor pieces from inventory", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "inventory");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.8)) return;
        long now = System.currentTimeMillis();
        if (now - lastSwapTime < 2000) return;
        if (isTimerReady()) {
            lastSwapTime = now;
            recordAction();
        }
    }
}
