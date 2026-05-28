package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Forces constant sprinting even while hitting", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "movement");
    }

    @Override
    public void onTick() {
        if (!McReflect.isPlayerSprinting() && McReflect.getPlayerFoodLevel() > 6) {
            McReflect.setSprinting(true);
        }
    }
}
