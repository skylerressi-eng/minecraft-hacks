package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class ESP extends Module {
    public ESP() {
        super("ESP", "See players and entities through walls with colored outlines", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.85)) return;
    }
}
