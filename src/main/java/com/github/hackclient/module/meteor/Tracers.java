package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Tracers extends Module {
    public Tracers() {
        super("Tracers", "Draw lines pointing to nearby players through walls", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE);
    }
    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.95)) return;
    }
}
