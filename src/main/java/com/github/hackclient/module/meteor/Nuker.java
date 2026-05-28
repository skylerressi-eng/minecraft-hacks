package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Nuker extends Module {
    private double range = 4.5;
    private long lastBreak = 0;

    public Nuker() {
        super("Nuker", "Auto-break blocks around player in radius", GameMode.METEOR, HumanizedTimer.SkillLevel.EXPERT, "world");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.8)) return;
        long now = System.currentTimeMillis();
        if (now - lastBreak < getSmartDelay(200)) return;
        McReflect.swingHand();
        lastBreak = now;
        recordAction();
    }

    public void setRange(double range) { this.range = Math.min(6.0, Math.max(1.0, range)); }
}
