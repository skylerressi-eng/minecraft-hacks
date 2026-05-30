package com.github.hackclient.module.bedwars;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Auto Bridge — placeholder. A real impl needs block raycasting and the
 * interactionManager.interactBlock packet path (place + look-pitch coordination),
 * neither of which the current McReflect surface exposes. The toggle still
 * shows up in the GUI / chat and routes through the stealth engine.
 */
public class AutoBridge extends Module {
    public AutoBridge() {
        super("AutoBridge", "Placeholder — needs block-placement raycasting (not shipped)",
                GameMode.BEDWARS, HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        // No-op: see class javadoc.
    }
}
