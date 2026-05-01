package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class AutoArmor extends Module {
    public AutoArmor() {
        super("AutoArmor", "Auto-equips best armor from your inventory", GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        if (!AntiCheatBypass.shouldActThisTick(0.85)) return;
    }
}
