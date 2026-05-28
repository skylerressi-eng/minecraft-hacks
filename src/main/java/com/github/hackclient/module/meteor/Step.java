package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Step extends Module {
    private double lastY = 0;
    private int stuckTicks = 0;

    public Step() {
        super("Step", "Step up full blocks instantly", GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED, "movement");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.85)) return;
        double py = McReflect.getPlayerY();
        boolean onGround = McReflect.isPlayerOnGround();

        if (onGround && Math.abs(py - lastY) < 0.01) {
            stuckTicks++;
            if (stuckTicks > 4) {
                double[] vel = McReflect.getPlayerVelocity();
                double hSpeed = Math.sqrt(vel[0] * vel[0] + vel[2] * vel[2]);
                if (hSpeed > 0.01) {
                    McReflect.setPlayerVelocity(vel[0], 0.42, vel[2]);
                    recordAction();
                    stuckTicks = 0;
                }
            }
        } else {
            stuckTicks = 0;
        }
        lastY = py;
    }
}
