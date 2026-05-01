package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class FreeCam extends Module {
    public FreeCam() {
        super("FreeCam", "Detach camera and fly around freely while body stays still", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE);
    }
    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.95)) return;
    }
}
