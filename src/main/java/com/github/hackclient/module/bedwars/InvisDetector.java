package com.github.hackclient.module.bedwars;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Invis Detector — placeholder. Spotting invisible players via floating armor,
 * particles, footstep sounds, or block-interaction events needs sound-event
 * and particle hooks (mixins) we don't ship. Toggle flag only.
 */
public class InvisDetector extends Module {
    public InvisDetector() {
        super("InvisDetector", "Placeholder — needs sound/particle event hooks (not shipped)",
                GameMode.BEDWARS, HumanizedTimer.SkillLevel.AVERAGE);
    }

    @Override
    public void onTick() {
        // No-op: see class javadoc.
    }
}
