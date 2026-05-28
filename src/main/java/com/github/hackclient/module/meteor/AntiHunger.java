package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class AntiHunger extends Module {
    public AntiHunger() {
        super("AntiHunger", "Reduces hunger drain by managing sprint and ground state", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "movement");
    }

    @Override
    public void onTick() {
        int food = McReflect.getPlayerFoodLevel();
        if (food <= 8 && McReflect.isPlayerSprinting()) {
            McReflect.setSprinting(false);
        }
        if (food <= 6 && !McReflect.isPlayerOnGround()) {
            McReflect.setOnGround(true);
        }
    }
}
