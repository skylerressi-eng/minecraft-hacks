package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class NoSlow extends Module {
    public NoSlow() {
        super("NoSlow", "Prevents slowdown from using items (eating, blocking)", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "movement");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.92)) return;
        double[] vel = McReflect.getPlayerVelocity();
        double hSpeed = Math.sqrt(vel[0] * vel[0] + vel[2] * vel[2]);
        if (hSpeed < 0.05 && McReflect.isPlayerSprinting()) {
            double boost = 1.15;
            McReflect.setPlayerVelocity(vel[0] * boost, vel[1], vel[2] * boost);
            recordAction();
        }
    }
}
