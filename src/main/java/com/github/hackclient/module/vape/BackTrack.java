package com.github.hackclient.module.vape;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class BackTrack extends Module {
    public BackTrack() {
        super("BackTrack", "Delays enemy movement packets to extend your hit range", GameMode.VAPE, HumanizedTimer.SkillLevel.EXPERT);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.9)) return;
    }
}
