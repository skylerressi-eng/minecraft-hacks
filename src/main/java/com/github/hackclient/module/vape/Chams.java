package com.github.hackclient.module.vape;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Chams extends Module {
    public Chams() {
        super("Chams", "Colored player models visible through walls", GameMode.VAPE, HumanizedTimer.SkillLevel.AVERAGE);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.95)) return;
    }
}
