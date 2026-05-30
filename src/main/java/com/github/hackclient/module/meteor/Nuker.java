package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Rapidly retriggers "use item / attack" on whatever the player is looking at,
 * letting you break or place blocks faster than vanilla cadence. We don't
 * directly drive block-break packets (that needs world/raycast access we don't
 * currently expose), so the visible effect is faster swing cadence — point at
 * a block and hold the break key to clear it quickly.
 */
public class Nuker extends Module {

    private long lastSwing = 0;

    public Nuker() {
        super("Nuker", "Faster swing cadence — point at a block and hold break to clear quickly",
                GameMode.METEOR, HumanizedTimer.SkillLevel.EXPERT, "world");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.8)) return;
        long now = System.currentTimeMillis();
        if (now - lastSwing < getSmartDelay(150)) return;
        McReflect.swingHand();
        lastSwing = now;
        recordAction();
    }
}
