package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Continuously sends swing animations while the module is enabled. This makes
 * block breaking feel snappier on laggy servers (the client visually swings
 * faster), but doesn't change the underlying break speed — that would need a
 * mining-progress mixin we don't currently have.
 */
public class FastBreak extends Module {

    private long lastSwing = 0;

    public FastBreak() {
        super("FastBreak", "Snappier swing animation while breaking blocks",
                GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED, "world");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.9)) return;
        long now = System.currentTimeMillis();
        if (now - lastSwing < getSmartDelay(120)) return;
        McReflect.swingHand();
        lastSwing = now;
        incrementTick();
    }
}
