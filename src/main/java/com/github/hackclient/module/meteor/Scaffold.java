package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Scaffold extends Module {
    public Scaffold() {
        super("Scaffold", "Auto-places blocks under you while bridging", GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.85)) return;
    }
}
