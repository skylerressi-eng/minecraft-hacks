package com.github.hackclient.module.mace;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Windburst Pearl Macro - Chains wind burst + pearl + mace slam into a macro sequence.
 *
 * Automated macro that executes the optimal mace PvP sequence:
 * 1. Throw pearl upward for height advantage
 * 2. Wait for pearl teleport (player gains height)
 * 3. Switch to mace during ascent/early fall
 * 4. Track target below with smooth aim
 * 5. Trigger mace slam at optimal fall distance for max damage
 *
 * All transitions between stages use humanized delays to avoid detection.
 * Uses McReflect for all MC access: position, velocity, fall distance,
 * rotation, and attack execution.
 */
public class WindburstPearlMacro extends Module {

    private long lastActionTime = 0;
    private int macroStage = 0;
    // Stages: 0=ready, 1=aim-up, 2=throw-pearl, 3=wait-teleport, 4=falling-aim, 5=slam-attack

    private float smoothedPitch = 0;
    private float smoothedYaw = 0;
    private boolean aimInitialized = false;

    private long pearlThrownTime = 0;
    private double prePearlY = 0;

    private static final long MAX_PEARL_WAIT_MS = 3000;
    private static final double MIN_HEIGHT_GAIN = 5.0;
    private static final double OPTIMAL_BURST_DISTANCE = 6.0;
    private static final double BURST_VARIANCE = 0.75;

    // Mace hotbar slot
    private int maceSlot = 0;

    public WindburstPearlMacro() {
        super("WindburstPearlMacro",
              "Pearl up + windburst slam combo macro with humanized timing",
              GameMode.MACE,
              HumanizedTimer.SkillLevel.EXPERT,
              "combat");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        macroStage = 0;
        aimInitialized = false;
    }

    @Override
    public void onTick() {
        incrementTick();
        if (!isEnabled()) return;

        Object player = McReflect.getPlayer();
        if (player == null || !McReflect.isPlayerAlive()) return;

        long now = System.currentTimeMillis();
        long delay = timer.getNextDelayMs();
        if (now - lastActionTime < delay / 2) return;

        // Initialize aim
        if (!aimInitialized) {
            smoothedYaw = McReflect.getPlayerYaw();
            smoothedPitch = McReflect.getPlayerPitch();
            aimInitialized = true;
        }

        // Need a target to execute the combo
        Object target = findNearestTarget();
        if (target == null) {
            macroStage = 0;
            return;
        }

        switch (macroStage) {
            case 0: // Ready - check if we should start the combo
                // Start if target is within range and we don't already have height advantage
                double dist = McReflect.distanceTo(target);
                double heightDiff = McReflect.getPlayerY() - McReflect.getEntityY(target);

                if (dist <= 20.0 && dist >= 5.0 && heightDiff < 3.0) {
                    if (shouldAct(0.85)) {
                        prePearlY = McReflect.getPlayerY();
                        macroStage = 1;
                        lastActionTime = now;
                    }
                }
                break;

            case 1: // Aim upward for pearl throw
                float upPitch = -80.0f + (float) (ThreadLocalRandom.current().nextGaussian() * 3.0);
                float rotSpeed = 0.5f + (float) (Math.random() * 0.1);

                smoothedPitch = AntiCheatBypass.smoothRotation(smoothedPitch, upPitch, rotSpeed);

                // Aim yaw toward target so pearl arcs that way
                float[] targetAngles = McReflect.getAnglesTo(target);
                if (targetAngles != null) {
                    smoothedYaw = AntiCheatBypass.smoothRotation(smoothedYaw, targetAngles[0], rotSpeed);
                }

                setPlayerRotation(
                    AntiCheatBypass.addRotationNoise(smoothedYaw),
                    AntiCheatBypass.addRotationNoise(smoothedPitch)
                );

                // Once aimed up enough, proceed to throw
                if (Math.abs(smoothedPitch - upPitch) < 5.0f) {
                    macroStage = 2;
                    lastActionTime = now;
                }
                break;

            case 2: // Throw pearl
                if (shouldAct(0.92)) {
                    // Simulate pearl throw (right-click with pearl equipped)
                    McReflect.swingHand();
                    pearlThrownTime = now;
                    macroStage = 3;
                    lastActionTime = now;
                    recordAction();
                }
                break;

            case 3: // Wait for pearl teleport (height gain)
                if (now - pearlThrownTime > MAX_PEARL_WAIT_MS) {
                    // Timeout - pearl didn't land or no height gained
                    macroStage = 0;
                    return;
                }

                double currentY = McReflect.getPlayerY();
                boolean gainedHeight = currentY - prePearlY >= MIN_HEIGHT_GAIN;

                if (gainedHeight) {
                    // Pearl landed and we teleported up - switch to mace
                    if (shouldAct(0.95)) {
                        setHotbarSlot(maceSlot);
                        macroStage = 4;
                        lastActionTime = now;
                        recordAction();
                    }
                }
                break;

            case 4: // Falling - aim at target below
                double[] velocity = McReflect.getPlayerVelocity();
                boolean isFalling = velocity[1] < -0.1;

                if (!isFalling && McReflect.getPlayerFallDistance() < 1.0) {
                    // Not falling yet, wait
                    return;
                }

                // Smooth aim toward target below
                float[] aimAngles = McReflect.getAnglesTo(target);
                if (aimAngles != null) {
                    float aimSpeed = 0.45f + (float) (Math.random() * 0.15);
                    smoothedYaw = AntiCheatBypass.smoothRotation(smoothedYaw,
                            AntiCheatBypass.addRotationNoise(aimAngles[0]), aimSpeed);
                    smoothedPitch = AntiCheatBypass.smoothRotation(smoothedPitch,
                            AntiCheatBypass.addRotationNoise(aimAngles[1]), aimSpeed);
                    setPlayerRotation(smoothedYaw, smoothedPitch);
                }

                // Check fall distance for optimal wind burst slam
                float fallDist = McReflect.getPlayerFallDistance();
                double triggerDist = OPTIMAL_BURST_DISTANCE
                        + ThreadLocalRandom.current().nextGaussian() * BURST_VARIANCE;

                if (fallDist >= triggerDist) {
                    macroStage = 5;
                    lastActionTime = now;
                }
                break;

            case 5: // Execute slam attack
                if (shouldAct(0.93)) {
                    Object slamTarget = findNearestTarget();
                    if (slamTarget != null) {
                        McReflect.attackEntity(slamTarget);
                    }
                    McReflect.swingHand();
                    macroStage = 0; // Reset for next combo
                    lastActionTime = now;
                    recordAction();
                }
                break;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        macroStage = 0;
    }

    private Object findNearestTarget() {
        if (!McReflect.canGetPlayers()) return null;

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null || players.isEmpty()) return null;

        Object nearest = null;
        double nearestDist = 30.0;

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

    private void setPlayerRotation(float yaw, float pitch) {
        try {
            Object player = McReflect.getPlayer();
            if (player == null) return;
            java.lang.reflect.Field yawField = findField(player.getClass(), "field_6031", "yaw");
            java.lang.reflect.Field pitchField = findField(player.getClass(), "field_6036", "pitch");
            if (yawField != null) {
                yawField.setAccessible(true);
                yawField.setFloat(player, yaw);
            }
            if (pitchField != null) {
                pitchField.setAccessible(true);
                pitchField.setFloat(player, Math.max(-90f, Math.min(90f, pitch)));
            }
        } catch (Exception ignored) {}
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

    public void setMaceSlot(int slot) { this.maceSlot = slot; }
}
