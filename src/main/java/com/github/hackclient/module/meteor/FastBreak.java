package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class FastBreak extends Module {
    private double speedMultiplier = 1.4;

    public FastBreak() {
        super("FastBreak", "Speeds up block breaking beyond vanilla rate", GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED, "world");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.9)) return;
        incrementTick();
    }

    public void setSpeedMultiplier(double mult) { this.speedMultiplier = Math.min(2.0, Math.max(1.0, mult)); }
    public double getSpeedMultiplier() { return speedMultiplier; }
}
