package com.github.hackclient.module.vape;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class AntiBot extends Module {
    public AntiBot() {
        super("AntiBot", "Detect and ignore anti-cheat bot players", GameMode.VAPE, HumanizedTimer.SkillLevel.AVERAGE);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.8)) return;
    }
}
