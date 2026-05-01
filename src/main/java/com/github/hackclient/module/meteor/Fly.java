package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Fly extends Module {
    public Fly() {
        super("Fly", "Creative-like flight in survival with multiple modes", GameMode.METEOR, HumanizedTimer.SkillLevel.EXPERT);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.85)) return;
    }
}
