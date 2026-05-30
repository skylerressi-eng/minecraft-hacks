package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Maxes out the gamma option to make dark areas fully visible. We save the
 * previous gamma on enable and restore it on disable so the user's normal
 * brightness comes back when the module turns off.
 */
public class FullBright extends Module {

    private static final double MAX_GAMMA = 10000.0;
    private double previousGamma = 1.0;
    private boolean savedGamma = false;

    public FullBright() {
        super("FullBright", "Sets gamma to maximum for full visibility in dark areas",
                GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "render");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!savedGamma) {
            previousGamma = McReflect.getGamma();
            savedGamma = true;
        }
        McReflect.setGamma(MAX_GAMMA);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (savedGamma) {
            McReflect.setGamma(previousGamma);
            savedGamma = false;
        }
    }

    @Override
    public void onTick() {
        // Re-assert max gamma every few seconds in case anything resets it.
        incrementTick();
        if (getTicksSinceEnabled() % 100 == 0) {
            McReflect.setGamma(MAX_GAMMA);
        }
    }
}
