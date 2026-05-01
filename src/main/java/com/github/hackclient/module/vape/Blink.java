package com.github.hackclient.module.vape;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Blink extends Module {
    public Blink() {
        super("Blink", "Hold movement packets then release all at once (teleport)", GameMode.VAPE, HumanizedTimer.SkillLevel.EXPERT);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.95)) return;
    }
}
