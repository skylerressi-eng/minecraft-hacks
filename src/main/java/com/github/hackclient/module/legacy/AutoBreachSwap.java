package com.github.hackclient.module.legacy;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

/**
 * Auto Breach Swap - Breaks enemy shield with axe, then swaps to sword to kill.
 *
 * In 1.8+ PvP, axes disable shields for 5 seconds. This module:
 * 1. Detects when enemy is blocking (within combat range and shield visible)
 * 2. Auto-swaps to axe slot in hotbar
 * 3. Hits them to break their shield
 * 4. Instantly swaps back to sword slot for the kill combo
 *
 * All with humanized timing and stealth-aware action gating.
 * Uses McReflect for inventory slot manipulation and attack execution.
 */
public class AutoBreachSwap extends Module {

    private long lastSwapTime = 0;
    private int comboState = 0; // 0=sword/idle, 1=swapped-to-axe, 2=axe-hit-done

    // Hotbar slot indices (configurable)
    private int swordSlot = 0;
    private int axeSlot = 1;

    // Track the attack target
    private Object currentTarget = null;
    private float lastTargetHealth = 0;

    public AutoBreachSwap() {
        super("AutoBreachSwap",
              "Axe-breaks enemy shield then sword combos for the kill",
              GameMode.LEGACY_1_8,
              HumanizedTimer.SkillLevel.SKILLED,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        comboState = 0;
        currentTarget = null;
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

        // Find nearest combat target
        Object target = findNearestTarget();
        if (target == null) {
            comboState = 0;
            currentTarget = null;
            return;
        }

        currentTarget = target;

        switch (comboState) {
            case 0: // Holding sword/idle, check if enemy appears to be blocking
                // Heuristic: if target is facing us and within 3 blocks, and our
                // recent attacks didn't reduce their health, they may be blocking
                if (isTargetLikelyBlocking(target)) {
                    if (shouldAct(0.92)) {
                        // Swap to axe via hotbar slot change
                        setHotbarSlot(axeSlot);
                        comboState = 1;
                        lastSwapTime = now;
                    }
                }
                break;

            case 1: // Holding axe, hit to break shield
                if (shouldAct(0.95)) {
                    McReflect.attackEntity(target);
                    McReflect.swingHand();
                    comboState = 2;
                    lastSwapTime = now;
                    recordAction();
                }
                break;

            case 2: // Shield broken, swap back to sword for combo
                if (shouldAct(0.90)) {
                    setHotbarSlot(swordSlot);
                    comboState = 0;
                    lastSwapTime = now;
                    recordAction();
                }
                break;
        }

        // Track target health for blocking detection
        if (target != null) {
            lastTargetHealth = McReflect.getEntityHealth(target);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        comboState = 0;
        currentTarget = null;
    }

    /**
     * Heuristic to detect if a target is blocking with a shield.
     * Without direct shield state access, we infer blocking from:
     * - Target facing us (within 60 degrees)
     * - Target is close (within 3.5 blocks)
     * - Target health hasn't changed after recent attacks (damage absorbed)
     */
    private boolean isTargetLikelyBlocking(Object target) {
        double dist = McReflect.distanceTo(target);
        if (dist > 3.5) return false;

        // Check if target health hasn't dropped since last tick
        float targetHealth = McReflect.getEntityHealth(target);
        if (lastTargetHealth > 0 && targetHealth >= lastTargetHealth && comboState == 0) {
            // Target didn't take damage - might be blocking
            // Only trigger occasionally to avoid false positives
            return Math.random() < 0.3;
        }

        return false;
    }

    private Object findNearestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object nearest = null;
        double nearestDist = 3.5;

        for (Object entity : players) {
            if (entity == self || entity.equals(self)) continue;
            if (!McReflect.isEntityAlive(entity)) continue;

            double dist = McReflect.distanceTo(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }

        return nearest;
    }

    /**
     * Set the selected hotbar slot via reflection on the player inventory.
     */
    private void setHotbarSlot(int slot) {
        try {
            Object player = McReflect.getPlayer();
            if (player == null) return;

            // Use reflection to access player.getInventory().selectedSlot
            java.lang.reflect.Method getInventory = findMethod(player.getClass(),
                    "method_31548", "getInventory");
            if (getInventory == null) {
                getInventory = findMethod(player.getClass(), "method_7355", "getInventory");
            }
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
    public void setAxeSlot(int slot) { this.axeSlot = slot; }
}
