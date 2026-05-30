package com.github.hackclient.module.combat;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * InstantPot — placeholder. The classic splash-heal combo needs hotbar-swap +
 * look-pitch-down + use-item + swap-back in tight succession (typical Bedwars
 * potion clutch). The packet-level chain isn't wired through McReflect.
 * Toggle flag only.
 */
public class InstantPot extends Module {
    public InstantPot() {
        super("InstantPot", "Placeholder — needs hotbar + use-item packet chain (not shipped)",
                GameMode.BEDWARS, HumanizedTimer.SkillLevel.EXPERT);
    }

    @Override
    public void onTick() {
        // No-op: see class javadoc.
    }
}
