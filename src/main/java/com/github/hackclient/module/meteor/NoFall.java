package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", "Prevents fall damage by spoofing ground state", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "movement");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.95)) return;
        float fallDist = McReflect.getPlayerFallDistance();
        if (fallDist > 2.5f) {
            McReflect.setOnGround(true);
            McReflect.setFallDistance(0f);
            recordAction();
        }
    }
}
