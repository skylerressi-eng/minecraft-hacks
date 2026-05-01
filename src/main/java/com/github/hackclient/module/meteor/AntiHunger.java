package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class AntiHunger extends Module {
    public AntiHunger() {
        super("AntiHunger", "Reduces hunger drain while moving", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.85)) return;
    }
}
