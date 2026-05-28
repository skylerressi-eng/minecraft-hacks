package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class FullBright extends Module {
    public FullBright() {
        super("FullBright", "Maximum gamma for full visibility in darkness", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "render");
    }

    @Override
    public void onTick() {
        // FullBright sets gamma to maximum via options reflection
        // Acts as toggle flag checked by the render system
    }
}
