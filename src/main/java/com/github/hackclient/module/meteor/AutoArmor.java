package com.github.hackclient.module.meteor;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * AutoArmor — placeholder. A working impl needs inventory-slot click packets
 * (CONTAINER_CLICK) and per-slot armor scoring, neither of which the current
 * McReflect surface exposes. Left here as a tracked feature flag so it shows
 * up in the GUI and the stealth engine accounts for it; it currently does no
 * inventory work. (Honest label rather than a silent no-op.)
 */
public class AutoArmor extends Module {

    public AutoArmor() {
        super("AutoArmor", "Placeholder — needs inventory-click packets (not shipped)",
                GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "inventory");
    }

    @Override
    public void onTick() {
        // No-op: see class javadoc.
    }
}
