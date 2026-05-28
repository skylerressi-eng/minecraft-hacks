package com.github.hackclient.module.mace;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mace Swap Combo - Quick swap to mace mid-combo for bonus falling damage.
 *
 * Tracks sword hit count and detects when the player is falling after a hit.
 * Swaps to mace at the right moment to land a falling hit for bonus smash
 * damage, then swaps back to sword for continued combat.
 *
 * Combo flow:
 * 1. Track sword hits on the target
 * 2. After 2-3 sword hits, detect if player is falling
 * 3. Swap to mace for the next hit (falling = bonus damage)
 * 4. Land the mace hit, then swap back to sword
 *
 * Uses McReflect for velocity checks, hotbar slot manipulation, and attacks.
 */
public class MaceSwapCombo extends Module {

    private long lastSwapTime = 0;
    private int comboStage = 0; // 0=sword-hitting, 1=swap-to-mace, 2=mace-hit, 3=swap-back
    private int swordHitCount = 0;
    private Object comboTarget = null;
    private float lastTargetHealth = 0;

    // Configurable hotbar slots
    private int swordSlot = 0;
    private int maceSlot = 1;

    // How many sword hits before swapping to mace
    private int hitsBeforeSwap = 2;

    public MaceSwapCombo() {
        super("MaceSwapCombo",
              "Quick swap to mace mid-combo for bonus falling damage",
              GameMode.MACE,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        comboStage = 0;
        swordHitCount = 0;
        comboTarget = null;
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();
        if (now - lastSwapTime < delay) return;

        // Find or validate combat target
        Object target = findNearestTarget();
        if (target == null) {
            resetCombo();
            return;
        }

        double dist = McReflect.distanceTo(target);
        if (dist > 4.0) {
            resetCombo();
            return;
        }

        // Detect hits by monitoring target health drops
        float targetHealth = McReflect.getEntityHealth(target);
        boolean justHit = false;
        if (comboTarget != null && target.equals(comboTarget) &&
            lastTargetHealth > 0 && targetHealth < lastTargetHealth) {
            justHit = true;
        }
        comboTarget = target;
        lastTargetHealth = targetHealth;

        double[] velocity = McReflect.getPlayerVelocity();
        boolean isFalling = velocity[1] < -0.05;

        switch (comboStage) {
            case 0: // Sword phase - counting hits
                if (justHit) {
                    swordHitCount++;
                }

                // After enough sword hits and if falling, prepare mace swap
                if (swordHitCount >= hitsBeforeSwap && isFalling) {
                    if (shouldAct(0.88)) {
                        comboStage = 1;
                    }
                }
                break;

            case 1: // Swap to mace
                if (shouldAct(0.92)) {
                    setHotbarSlot(maceSlot);
                    comboStage = 2;
                    lastSwapTime = now;
                    recordAction();
                }
                break;

            case 2: // Mace equipped - attack with falling bonus
                if (dist <= 3.5) {
                    if (shouldAct(0.93)) {
                        McReflect.attackEntity(target);
                        McReflect.swingHand();
                        comboStage = 3;
                        lastSwapTime = now;
                        recordAction();
                    }
                }
                break;

            case 3: // Swap back to sword
                if (shouldAct(0.90)) {
                    setHotbarSlot(swordSlot);
                    resetCombo();
                    lastSwapTime = now;
                    recordAction();
                }
                break;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetCombo();
    }

    private void resetCombo() {
        comboStage = 0;
        swordHitCount = 0;
        // Randomize hits before next mace swap (2-3)
        hitsBeforeSwap = 2 + ThreadLocalRandom.current().nextInt(2);
    }

    private Object findNearestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object nearest = null;
        double nearestDist = 5.0;

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double d = McReflect.distanceTo(entity);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = entity;
            }
        }

        return nearest;
    }

    private void setHotbarSlot(int slot) {
        try {
            Object player = McReflect.getPlayer();
            if (player == null) return;

            java.lang.reflect.Method getInventory = findMethod(player.getClass(),
                    "method_31548", "getInventory", "method_7355");
            if (getInventory == null) return;

            Object inventory = getInventory.invoke(player);
            if (inventory == null) return;

            java.lang.reflect.Field selectedSlotField = findFieldInHierarchy(
                    inventory.getClass(), "field_7545", "selectedSlot");
            if (selectedSlotField != null) {
                selectedSlotField.setAccessible(true);
                selectedSlotField.setInt(inventory, slot);
            }
        } catch (Exception ignored) {}
    }

    private java.lang.reflect.Method findMethod(Class<?> clazz, String... names) {
        Class<?> current = clazz;
        while (current != null) {
            for (String name : names) {
                try {
                    java.lang.reflect.Method m = current.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String... names) {
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

    public void setSwordSlot(int slot) { this.swordSlot = slot; }
    public void setMaceSlot(int slot) { this.maceSlot = slot; }
}
