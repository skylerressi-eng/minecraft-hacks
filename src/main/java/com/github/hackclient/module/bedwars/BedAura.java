package com.github.hackclient.module.bedwars;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Bed Aura — placeholder. Detecting enemy beds and auto-breaking them needs
 * world-block scanning plus interactionManager.attackBlock packets, neither of
 * which McReflect currently exposes. Toggle flag only.
 */
public class BedAura extends Module {
    public BedAura() {
        super("BedAura", "Placeholder — needs world-block scanning (not shipped)",
                GameMode.BEDWARS, HumanizedTimer.SkillLevel.SKILLED);
    }

    @Override
    public void onTick() {
        // No-op: see class javadoc.
    }
}
