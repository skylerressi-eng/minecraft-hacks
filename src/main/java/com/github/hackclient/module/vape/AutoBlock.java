package com.github.hackclient.module.vape;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class AutoBlock extends Module {
    public AutoBlock() {
        super("AutoBlock", "Auto-blocks with sword between attacks for damage reduction", GameMode.VAPE, HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.85)) return;
    }
}
