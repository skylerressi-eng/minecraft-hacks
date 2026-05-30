package com.github.hackclient.module.bedwars;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Auto Shop — placeholder. Bedwars shop interaction needs container-slot click
 * packets which McReflect doesn't expose. Toggle flag only.
 */
public class AutoShop extends Module {
    public AutoShop() {
        super("AutoShop", "Placeholder — needs container-click packets (not shipped)",
                GameMode.BEDWARS, HumanizedTimer.SkillLevel.AVERAGE);
    }

    @Override
    public void onTick() {
        // No-op: see class javadoc.
    }
}
