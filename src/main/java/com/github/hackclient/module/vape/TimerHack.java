package com.github.hackclient.module.vape;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class TimerHack extends Module {
    public TimerHack() {
        super("TimerHack", "Speed up game tick rate for faster movement and actions", GameMode.VAPE, HumanizedTimer.SkillLevel.EXPERT);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.95)) return;
    }
}
