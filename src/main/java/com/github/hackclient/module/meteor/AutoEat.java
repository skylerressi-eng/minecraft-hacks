package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class AutoEat extends Module {
    public AutoEat() {
        super("AutoEat", "Auto-eats best food when hunger drops low", GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.85)) return;
    }
}
