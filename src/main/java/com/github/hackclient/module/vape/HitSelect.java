package com.github.hackclient.module.vape;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class HitSelect extends Module {
    public HitSelect() {
        super("HitSelect", "Only swings when hit will connect - no wasted attacks", GameMode.VAPE, HumanizedTimer.SkillLevel.EXPERT);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.9)) return;
    }
}
