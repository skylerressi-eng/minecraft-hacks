package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Saves the player's position/rotation on enable for inspection from other
 * modules (the HUD prints "saved at X/Y/Z" if active). True FreeCam — detaching
 * the camera entity from the player — needs a camera mixin we don't ship.
 */
public class FreeCam extends Module {

    private double savedX, savedY, savedZ;
    private float savedYaw, savedPitch;
    private boolean positionSaved = false;

    public FreeCam() {
        super("FreeCam", "Bookmarks current position (camera detach needs a mixin, not shipped)",
                GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "render");
    }

    @Override
    public void onEnable() {
        savedX = McReflect.getPlayerX();
        savedY = McReflect.getPlayerY();
        savedZ = McReflect.getPlayerZ();
        savedYaw = McReflect.getPlayerYaw();
        savedPitch = McReflect.getPlayerPitch();
        positionSaved = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        positionSaved = false;
        super.onDisable();
    }

    @Override
    public void onTick() {
        // No-op: this is a passive bookmark.
    }

    public boolean isPositionSaved() { return positionSaved; }
    public double getSavedX() { return savedX; }
    public double getSavedY() { return savedY; }
    public double getSavedZ() { return savedZ; }
    public float getSavedYaw() { return savedYaw; }
    public float getSavedPitch() { return savedPitch; }
}
