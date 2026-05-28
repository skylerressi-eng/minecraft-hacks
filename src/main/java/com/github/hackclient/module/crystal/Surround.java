package com.github.hackclient.module.crystal;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

/**
 * Surround - Places obsidian around player's feet for crystal explosion protection.
 *
 * Tracks player position and calculates the 4 cardinal + 4 diagonal positions
 * around the feet. Places obsidian at each empty position with smooth aim
 * and humanized timing, one block per tick to avoid detection.
 */
public class Surround extends Module {

    private long lastPlaceTime = 0;
    private boolean surroundComplete = false;
    private int currentPlaceIndex = 0;

    // Smooth aim state
    private float currentYaw;
    private float currentPitch;

    // The 8 positions around the player's feet: cardinal + diagonal
    private static final int[][] SURROUND_OFFSETS = {
        {0, 0, -1},   // North
        {0, 0, 1},    // South
        {-1, 0, 0},   // West
        {1, 0, 0},    // East
        {-1, 0, -1},  // NW
        {1, 0, -1},   // NE
        {-1, 0, 1},   // SW
        {1, 0, 1}     // SE
    };

    public Surround() {
        super("Surround",
              "Auto-places obsidian around feet for crystal protection",
              GameMode.CRYSTAL,
              HumanizedTimer.SkillLevel.SKILLED,
              "world");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        surroundComplete = false;
        currentPlaceIndex = 0;
        currentYaw = McReflect.getPlayerYaw();
        currentPitch = McReflect.getPlayerPitch();
    }

    @Override
    public void onTick() {
        if (!McReflect.isPlayerAlive()) return;
        incrementTick();

        // Need to be on the ground to surround effectively
        if (!McReflect.isPlayerOnGround()) return;

        long now = System.currentTimeMillis();
        // Surround needs fast placement, so use half the normal delay
        long delay = timer.getNextDelayMs() / 2;
        if (now - lastPlaceTime < delay) return;

        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY();
        double pz = McReflect.getPlayerZ();

        int blockX = (int) Math.floor(px);
        int blockY = (int) Math.floor(py);
        int blockZ = (int) Math.floor(pz);

        // Check each surround position, starting from where we left off
        boolean allFilled = true;
        int checked = 0;

        while (checked < SURROUND_OFFSETS.length) {
            int idx = (currentPlaceIndex + checked) % SURROUND_OFFSETS.length;
            int[] offset = SURROUND_OFFSETS[idx];

            int targetX = blockX + offset[0];
            int targetY = blockY + offset[1];
            int targetZ = blockZ + offset[2];

            // Check if this position needs a block
            // In a full implementation, we'd query the block state via McReflect/world reflection
            // For now, we attempt placement at each position
            if (needsBlock(targetX, targetY, targetZ)) {
                allFilled = false;

                // Calculate aim angles to the target block position
                float[] angles = getAnglesToBlock(targetX, targetY, targetZ);
                if (angles != null) {
                    float speed = 0.65f + (float) (Math.random() * 0.1);
                    currentYaw = AntiCheatBypass.smoothRotation(currentYaw,
                            AntiCheatBypass.addRotationNoise(angles[0]), speed);
                    currentPitch = AntiCheatBypass.smoothRotation(currentPitch,
                            AntiCheatBypass.addRotationNoise(angles[1]), speed);
                }

                if (shouldAct(0.95)) {
                    // Place obsidian: right-click to place block
                    // In practice, this would switch to obsidian slot and use interactBlock
                    McReflect.swingHand();
                    lastPlaceTime = now;
                    currentPlaceIndex = (idx + 1) % SURROUND_OFFSETS.length;
                    recordAction();
                    return; // One block per tick for stealth
                }
                break;
            }
            checked++;
        }

        if (allFilled) {
            surroundComplete = true;
        }
    }

    /**
     * Check if a block position needs obsidian placed.
     * Returns true if the position is air/passable.
     */
    private boolean needsBlock(int x, int y, int z) {
        // Would query world.getBlockState(x, y, z) via reflection
        // and check if the block is air or a non-solid block.
        // Returns true if placement is needed.
        Object world = McReflect.getWorld();
        return world != null; // Placeholder: in real impl, checks block state
    }

    /**
     * Calculate yaw/pitch angles from player eye position to the center of a block.
     */
    private float[] getAnglesToBlock(int x, int y, int z) {
        double px = McReflect.getPlayerX();
        double py = McReflect.getPlayerY() + 1.62; // Eye height
        double pz = McReflect.getPlayerZ();

        double dx = (x + 0.5) - px;
        double dy = (y + 0.5) - py;
        double dz = (z + 0.5) - pz;

        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizDist));

        return new float[]{yaw, pitch};
    }

    @Override
    public void onDisable() {
        super.onDisable();
        surroundComplete = false;
        currentPlaceIndex = 0;
    }

    public boolean isSurroundComplete() { return surroundComplete; }
}
