package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Scaffold extends Module {
    private long lastPlace = 0;
    private double lastX = 0, lastZ = 0;

    public Scaffold() {
        super("Scaffold", "Auto-place blocks under feet while walking", GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED, "world");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.9)) return;
        double px = McReflect.getPlayerX();
        double pz = McReflect.getPlayerZ();
        long now = System.currentTimeMillis();

        double moved = Math.sqrt((px - lastX) * (px - lastX) + (pz - lastZ) * (pz - lastZ));
        if (moved > 0.3 && now - lastPlace >= getSmartDelay(100)) {
            McReflect.swingHand();
            lastPlace = now;
            lastX = px;
            lastZ = pz;
            recordAction();
        }
    }
}
