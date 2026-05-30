package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Toggle-only flag for Xray. True client-side Xray (hiding non-ore blocks)
 * requires a world-renderer mixin which this build does not ship — that's the
 * one feature that fundamentally needs a deep render hook. Leaving the module
 * in the list as a state flag so other features (the GUI, the active-module
 * indicator) can still react to it, but it does not visually change blocks.
 */
public class Xray extends Module {

    public Xray() {
        super("Xray", "State flag only — true Xray needs a world-renderer mixin (not shipped)",
                GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "render");
    }

    @Override
    public void onTick() {
        // Pure state module — toggle feedback is sent by the base class.
    }
}
