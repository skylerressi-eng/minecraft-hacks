package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class StorageESP extends Module {
    public StorageESP() {
        super("StorageESP", "Highlight chests, shulkers, and containers through walls", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE);
    }
    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.95)) return;
    }
}
