package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * AutoBlock - Automatically raises the sword block between attacks.
 *
 * Tracks the timing of the last attack and engages block (use-item /
 * right-click with sword) during the cooldown window between hits.
 * Unblocks just before the next attack can fire, mimicking a skilled
 * player who blocks reflexively between swings.
 */
public class AutoBlock extends Module {

    private static final long BLOCK_DELAY_AFTER_ATTACK_MS = 60;
    private static final long UNBLOCK_BEFORE_ATTACK_MS = 80;
    private static final double MAX_TARGET_DISTANCE = 5.0;
    private static final long ATTACK_COOLDOWN_MS = 500; // ~10 CPS window

    private long lastAttackTime = 0;
    private boolean isBlocking = false;
    private boolean attackedThisCycle = false;

    public AutoBlock() {
        super("AutoBlock",
              "Auto-blocks with sword between attacks for damage reduction",
              GameMode.VAPE,
              HumanizedTimer.SkillLevel.AVERAGE,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastAttackTime = 0;
        isBlocking = false;
        attackedThisCycle = false;
    }

    @Override
    public void onDisable() {
        // Make sure we release block on disable
        if (isBlocking) {
            releaseBlock();
            isBlocking = false;
        }
        super.onDisable();
    }

    @Override
    public void onTick() {
        incrementTick();

        if (!McReflect.isCoreReady() || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();
        long timeSinceAttack = now - lastAttackTime;

        // Check if there's a target nearby worth blocking against
        boolean hasNearbyTarget = hasTargetInRange();

        if (!hasNearbyTarget) {
            if (isBlocking) {
                releaseBlock();
                isBlocking = false;
            }
            return;
        }

        if (!shouldAct(0.85)) return;

        // State machine: attack -> wait -> block -> wait -> unblock -> attack
        if (attackedThisCycle && !isBlocking) {
            // After attacking, wait a small delay then block
            long blockDelay = BLOCK_DELAY_AFTER_ATTACK_MS
                    + (long) (Math.random() * 40 - 20); // jitter +-20ms
            if (timeSinceAttack >= blockDelay) {
                engageBlock();
                isBlocking = true;
                recordAction();
            }
        } else if (isBlocking) {
            // While blocking, check if it's time to unblock for the next swing
            long unblockTime = ATTACK_COOLDOWN_MS - UNBLOCK_BEFORE_ATTACK_MS
                    + (long) (Math.random() * 30 - 15);
            if (timeSinceAttack >= unblockTime) {
                releaseBlock();
                isBlocking = false;
                attackedThisCycle = false;
                recordAction();
            }
        }
    }

    /**
     * Called externally when the player performs an attack, so we can
     * track timing for the block cycle.
     */
    public void onAttack() {
        lastAttackTime = System.currentTimeMillis();
        attackedThisCycle = true;
        if (isBlocking) {
            releaseBlock();
            isBlocking = false;
        }
    }

    private boolean hasTargetInRange() {
        if (!McReflect.canGetPlayers()) return false;
        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null) return false;

        for (Object player : players) {
            if (player == self || player.equals(self)) continue;
            if (!McReflect.isEntityAlive(player)) continue;
            if (McReflect.distanceTo(player) <= MAX_TARGET_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Engage sword block by simulating a right-click / use-item action
     * via the interaction manager.
     */
    private void engageBlock() {
        try {
            Object mc = McReflect.getMinecraftClient();
            if (mc == null) return;

            // Invoke interactionManager.interactItem or simulate use-key press
            // Approach: set the use-item key state via options field
            java.lang.reflect.Field optionsField = findField(mc.getClass(), "field_1690", "options");
            if (optionsField == null) return;
            Object options = optionsField.get(mc);
            if (options == null) return;

            java.lang.reflect.Field useKeyField = findField(options.getClass(), "field_1904", "useKey", "keyUse");
            if (useKeyField == null) return;
            Object keyBinding = useKeyField.get(options);
            if (keyBinding == null) return;

            // Set the key pressed state to true
            java.lang.reflect.Field pressedField = findField(keyBinding.getClass(), "field_1660", "pressed");
            if (pressedField != null) {
                pressedField.setBoolean(keyBinding, true);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Release sword block by clearing the use-item key state.
     */
    private void releaseBlock() {
        try {
            Object mc = McReflect.getMinecraftClient();
            if (mc == null) return;

            java.lang.reflect.Field optionsField = findField(mc.getClass(), "field_1690", "options");
            if (optionsField == null) return;
            Object options = optionsField.get(mc);
            if (options == null) return;

            java.lang.reflect.Field useKeyField = findField(options.getClass(), "field_1904", "useKey", "keyUse");
            if (useKeyField == null) return;
            Object keyBinding = useKeyField.get(options);
            if (keyBinding == null) return;

            java.lang.reflect.Field pressedField = findField(keyBinding.getClass(), "field_1660", "pressed");
            if (pressedField != null) {
                pressedField.setBoolean(keyBinding, false);
            }
        } catch (Exception ignored) {}
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String... names) {
        Class<?> current = clazz;
        while (current != null) {
            for (String name : names) {
                try {
                    java.lang.reflect.Field f = current.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public boolean isCurrentlyBlocking() {
        return isBlocking;
    }
}
