package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Jesus extends Module {
    private int waterTicks = 0;

    public Jesus() {
        super("Jesus", "Walk on water by manipulating vertical velocity", GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED, "movement");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.9)) return;
        double[] vel = McReflect.getPlayerVelocity();
        boolean onGround = McReflect.isPlayerOnGround();

        if (vel[1] < -0.04 && !onGround && McReflect.getPlayerY() % 1.0 < 0.5) {
            waterTicks++;
            if (waterTicks > 2) {
                McReflect.setPlayerVelocity(vel[0], 0.05, vel[2]);
                McReflect.setOnGround(true);
                recordAction();
            }
        } else {
            waterTicks = 0;
        }
    }
}
