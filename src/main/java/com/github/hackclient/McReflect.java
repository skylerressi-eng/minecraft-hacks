package com.github.hackclient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class McReflect {

    private static Class<?> mcClientClass;
    private static Class<?> textClass;
    private static Class<?> screenClass;
    private static Class<?> drawContextClass;
    private static Class<?> textRendererClass;
    private static Class<?> entityClass;
    private static Class<?> livingEntityClass;
    private static Class<?> playerEntityClass;
    private static Class<?> clientPlayerClass;
    private static Class<?> worldClass;
    private static Class<?> vec3dClass;
    private static Class<?> itemStackClass;
    private static Class<?> playerInventoryClass;

    private static Method getInstanceMethod;
    private static Method textLiteralMethod;
    private static Method setScreenMethod;
    private static Method getWindowMethod;
    private static Method getScaledWidthMethod;
    private static Method getScaledHeightMethod;
    private static Method drawContextFillMethod;
    private static Method drawContextDrawTextMethod;
    private static Method sendMessageMethod;
    private static Method getXMethod;
    private static Method getYMethod;
    private static Method getZMethod;

    private static Method getHealthMethod;
    private static Method isAliveMethod;
    private static Method getVelocityMethod;
    private static Method setVelocityMethod;
    private static Method setVelocity3Method;
    private static Method isOnGroundMethod;
    private static Method setSprintingMethod;
    private static Method isSprintingMethod;
    private static Method jumpMethod;
    private static Method swingHandMethod;
    private static Method getYawMethod;
    private static Method getPitchMethod;
    private static Method setYawMethod;
    private static Method setPitchMethod;
    private static Method getMainHandStackMethod;
    private static Method getInventoryMethod;
    private static Method getPlayersMethod;
    private static Method distanceToMethod;
    private static Method squaredDistToMethod;
    private static Method getNameMethod;
    private static Method getFoodLevelMethod;
    private static Method getAbilitiesMethod;
    private static Method attackEntityMethod;

    private static Field playerField;
    private static Field currentScreenField;
    private static Field textRendererField;
    private static Field optionsField;
    private static Field hudHiddenField;
    private static Field worldField;
    private static Field fallDistanceField;
    private static Field onGroundField;
    private static Field interactionManagerField;
    private static Field flyingField;

    private static boolean initialized = false;
    private static boolean coreReady = false;
    private static int successCount = 0;
    private static int failCount = 0;

    public static boolean init() {
        if (initialized) return coreReady;
        initialized = true;
        successCount = 0;
        failCount = 0;

        mcClientClass = tryLoadClass("net.minecraft.class_310");
        if (mcClientClass == null) {
            log("FATAL: Could not find MinecraftClient class");
            return false;
        }
        log("Found MinecraftClient: " + mcClientClass.getName());

        getInstanceMethod = tryFindMethod(mcClientClass, new String[]{"method_1551", "getInstance"});
        if (getInstanceMethod == null) {
            log("FATAL: Could not find getInstance()");
            return false;
        }
        success("MinecraftClient.getInstance()");

        try {
            Object mc = getInstanceMethod.invoke(null);
            if (mc == null) {
                log("WARNING: getInstance() returned null");
                initialized = false;
                return false;
            }
        } catch (Exception e) {
            log("WARNING: getInstance() threw: " + e.getMessage());
            initialized = false;
            return false;
        }

        coreReady = true;

        initFields();
        initClasses();
        initCoreMethods();
        initCombatMethods();
        initMovementMethods();
        initEntityMethods();
        initWorldMethods();
        initSendMessage();

        log("McReflect init complete: " + successCount + " succeeded, " + failCount + " failed");
        return coreReady;
    }

    private static void initFields() {
        playerField = tryFindFieldAccessible(mcClientClass, new String[]{"field_1724", "player"});
        if (playerField != null) success("player field"); else fail("player field");

        currentScreenField = tryFindFieldAccessible(mcClientClass, new String[]{"field_1755", "currentScreen"});
        if (currentScreenField != null) success("currentScreen field"); else fail("currentScreen field");

        textRendererField = tryFindFieldAccessible(mcClientClass, new String[]{"field_1772", "textRenderer"});
        if (textRendererField != null) success("textRenderer field"); else fail("textRenderer field");

        optionsField = tryFindFieldAccessible(mcClientClass, new String[]{"field_1690", "options"});
        if (optionsField != null) success("options field"); else fail("options field");

        worldField = tryFindFieldAccessible(mcClientClass, new String[]{"field_1687", "world"});
        if (worldField != null) success("world field"); else fail("world field");

        interactionManagerField = tryFindFieldAccessible(mcClientClass, new String[]{"field_1761", "interactionManager"});
        if (interactionManagerField != null) success("interactionManager field"); else fail("interactionManager field");
    }

    private static void initClasses() {
        textClass = tryLoadClass("net.minecraft.class_2561");
        if (textClass != null) success("Text class"); else fail("Text class");

        screenClass = tryLoadClass("net.minecraft.class_437");
        if (screenClass != null) success("Screen class"); else fail("Screen class");

        drawContextClass = tryLoadClass("net.minecraft.class_332");
        if (drawContextClass != null) success("DrawContext class"); else fail("DrawContext class");

        textRendererClass = tryLoadClass("net.minecraft.class_327");
        if (textRendererClass != null) success("TextRenderer class"); else fail("TextRenderer class");

        entityClass = tryLoadClass("net.minecraft.class_1297");
        if (entityClass != null) success("Entity class"); else fail("Entity class");

        livingEntityClass = tryLoadClass("net.minecraft.class_1309");
        if (livingEntityClass != null) success("LivingEntity class"); else fail("LivingEntity class");

        playerEntityClass = tryLoadClass("net.minecraft.class_1657");
        if (playerEntityClass != null) success("PlayerEntity class"); else fail("PlayerEntity class");

        clientPlayerClass = tryLoadClass("net.minecraft.class_746");
        if (clientPlayerClass != null) success("ClientPlayerEntity class"); else fail("ClientPlayerEntity class");

        worldClass = tryLoadClass("net.minecraft.class_1937", "net.minecraft.class_1511");
        if (worldClass != null) success("World class"); else fail("World class");

        vec3dClass = tryLoadClass("net.minecraft.class_243");
        if (vec3dClass != null) success("Vec3d class"); else fail("Vec3d class");

        itemStackClass = tryLoadClass("net.minecraft.class_1799");
        if (itemStackClass != null) success("ItemStack class"); else fail("ItemStack class");

        playerInventoryClass = tryLoadClass("net.minecraft.class_1661");
        if (playerInventoryClass != null) success("PlayerInventory class"); else fail("PlayerInventory class");
    }

    private static void initCoreMethods() {
        getWindowMethod = tryFindMethod(mcClientClass, new String[]{"method_22683", "getWindow"});
        if (getWindowMethod != null) {
            success("getWindow()");
            Class<?> windowClass = getWindowMethod.getReturnType();
            getScaledWidthMethod = tryFindMethod(windowClass, new String[]{"method_4486", "getScaledWidth"});
            if (getScaledWidthMethod != null) success("getScaledWidth()"); else fail("getScaledWidth()");
            getScaledHeightMethod = tryFindMethod(windowClass, new String[]{"method_4502", "getScaledHeight"});
            if (getScaledHeightMethod != null) success("getScaledHeight()"); else fail("getScaledHeight()");
        } else {
            fail("getWindow()");
        }

        if (optionsField != null) {
            try {
                Object mc = getInstanceMethod.invoke(null);
                Object options = optionsField.get(mc);
                if (options != null) {
                    hudHiddenField = tryFindFieldAccessible(options.getClass(), new String[]{"field_1842", "hudHidden"});
                    if (hudHiddenField != null) success("hudHidden field"); else fail("hudHidden field");
                }
            } catch (Exception e) { fail("hudHidden lookup"); }
        }

        if (screenClass != null) {
            setScreenMethod = tryFindMethod(mcClientClass, new String[]{"method_1507", "setScreen"}, screenClass);
            if (setScreenMethod != null) success("setScreen()"); else fail("setScreen()");
        }

        if (textClass != null) {
            textLiteralMethod = tryFindStaticMethod(textClass, new String[]{"method_43470", "literal"}, String.class);
            if (textLiteralMethod == null) {
                textLiteralMethod = findStaticMethodBySignature(textClass, String.class);
            }
            if (textLiteralMethod != null) success("Text.literal()"); else fail("Text.literal()");
        }

        if (drawContextClass != null) {
            drawContextFillMethod = tryFindMethod(drawContextClass, new String[]{"method_25294", "fill"},
                int.class, int.class, int.class, int.class, int.class);
            if (drawContextFillMethod == null) {
                drawContextFillMethod = findMethodByParamTypes(drawContextClass,
                    int.class, int.class, int.class, int.class, int.class);
            }
            if (drawContextFillMethod != null) success("DrawContext.fill()"); else fail("DrawContext.fill()");

            if (textRendererClass != null) {
                drawContextDrawTextMethod = tryFindMethod(drawContextClass, new String[]{"method_25303", "method_51433", "drawText"},
                    textRendererClass, String.class, int.class, int.class, int.class, boolean.class);
                if (drawContextDrawTextMethod == null) {
                    drawContextDrawTextMethod = findMethodByParamTypes(drawContextClass,
                        textRendererClass, String.class, int.class, int.class, int.class, boolean.class);
                }
                if (drawContextDrawTextMethod != null) success("DrawContext.drawText()"); else fail("DrawContext.drawText()");
            }
        }

        if (entityClass != null) {
            getXMethod = tryFindMethod(entityClass, new String[]{"method_23317", "getX"});
            getYMethod = tryFindMethod(entityClass, new String[]{"method_23318", "getY"});
            getZMethod = tryFindMethod(entityClass, new String[]{"method_23321", "getZ"});
            if (getXMethod != null) success("getX/Y/Z()"); else fail("getX/Y/Z()");
        }
    }

    private static void initCombatMethods() {
        if (livingEntityClass != null) {
            getHealthMethod = tryFindMethod(livingEntityClass, new String[]{"method_6032", "getHealth"});
            if (getHealthMethod != null) success("getHealth()"); else fail("getHealth()");

            isAliveMethod = tryFindMethod(livingEntityClass, new String[]{"method_6020", "isAlive"});
            if (isAliveMethod != null) success("isAlive()"); else fail("isAlive()");

            getMainHandStackMethod = tryFindMethod(livingEntityClass, new String[]{"method_6079", "getMainHandStack"});
            if (getMainHandStackMethod != null) success("getMainHandStack()"); else fail("getMainHandStack()");

            jumpMethod = tryFindMethod(livingEntityClass, new String[]{"method_6043", "jump"});
            if (jumpMethod != null) success("jump()"); else fail("jump()");

            swingHandMethod = null;
            if (swingHandMethod == null) {
                try {
                    Class<?> handClass = tryLoadClass("net.minecraft.class_1268");
                    if (handClass != null) {
                        swingHandMethod = tryFindMethod(livingEntityClass, new String[]{"method_6104", "swingHand"}, handClass);
                    }
                } catch (Exception ignored) {}
            }
            if (swingHandMethod != null) success("swingHand()"); else fail("swingHand()");
        }

        if (interactionManagerField != null) {
            try {
                Object mc = getInstanceMethod.invoke(null);
                Object im = interactionManagerField.get(mc);
                if (im != null && entityClass != null && playerEntityClass != null) {
                    attackEntityMethod = tryFindMethod(im.getClass(),
                        new String[]{"method_2918", "attackEntity"}, playerEntityClass, entityClass);
                    if (attackEntityMethod == null) {
                        for (Method m : im.getClass().getMethods()) {
                            if (m.getParameterCount() == 2) {
                                Class<?>[] params = m.getParameterTypes();
                                if (params[0].isAssignableFrom(playerEntityClass) && params[1].isAssignableFrom(entityClass)) {
                                    attackEntityMethod = m;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (attackEntityMethod != null) success("attackEntity()"); else fail("attackEntity()");
        }
    }

    private static void initMovementMethods() {
        if (entityClass != null) {
            getVelocityMethod = tryFindMethod(entityClass, new String[]{"method_18798", "getVelocity"});
            if (getVelocityMethod != null) success("getVelocity()"); else fail("getVelocity()");

            if (vec3dClass != null) {
                setVelocityMethod = tryFindMethod(entityClass, new String[]{"method_18799", "setVelocity"}, vec3dClass);
                if (setVelocityMethod != null) success("setVelocity(Vec3d)"); else fail("setVelocity(Vec3d)");
            }

            setVelocity3Method = tryFindMethod(entityClass, new String[]{"method_18800", "setVelocity"},
                double.class, double.class, double.class);
            if (setVelocity3Method != null) success("setVelocity(d,d,d)"); else fail("setVelocity(d,d,d)");

            setSprintingMethod = tryFindMethod(entityClass, new String[]{"method_6115", "setSprinting"}, boolean.class);
            if (setSprintingMethod != null) success("setSprinting()"); else fail("setSprinting()");

            isSprintingMethod = tryFindMethod(entityClass, new String[]{"method_5624", "isSprinting"});
            if (isSprintingMethod != null) success("isSprinting()"); else fail("isSprinting()");

            isOnGroundMethod = tryFindMethod(entityClass, new String[]{"method_24828", "isOnGround"});
            if (isOnGroundMethod != null) success("isOnGround()"); else fail("isOnGround()");

            getYawMethod = tryFindMethod(entityClass, new String[]{"method_36454", "getYaw"});
            if (getYawMethod == null) {
                getYawMethod = tryFindMethod(entityClass, new String[]{"method_36454"}, float.class);
            }
            if (getYawMethod != null) success("getYaw()"); else fail("getYaw()");

            getPitchMethod = tryFindMethod(entityClass, new String[]{"method_36455", "getPitch"});
            if (getPitchMethod == null) {
                getPitchMethod = tryFindMethod(entityClass, new String[]{"method_36455"}, float.class);
            }
            if (getPitchMethod != null) success("getPitch()"); else fail("getPitch()");

            fallDistanceField = tryFindFieldAccessible(entityClass, new String[]{"field_6017", "fallDistance"});
            if (fallDistanceField != null) success("fallDistance field"); else fail("fallDistance field");

            onGroundField = tryFindFieldAccessible(entityClass, new String[]{"field_6002", "onGround"});
            if (onGroundField != null) success("onGround field"); else fail("onGround field");

            distanceToMethod = tryFindMethod(entityClass, new String[]{"method_5739", "distanceTo"}, entityClass);
            if (distanceToMethod != null) success("distanceTo()"); else fail("distanceTo()");

            squaredDistToMethod = tryFindMethod(entityClass, new String[]{"method_5858", "squaredDistanceTo"},
                double.class, double.class, double.class);
            if (squaredDistToMethod != null) success("squaredDistanceTo()"); else fail("squaredDistanceTo()");

            getNameMethod = tryFindMethod(entityClass, new String[]{"method_5477", "getName"});
            if (getNameMethod == null) {
                getNameMethod = tryFindMethod(entityClass, new String[]{"method_5476", "getDisplayName"});
            }
            if (getNameMethod != null) success("getName()"); else fail("getName()");
        }

        if (clientPlayerClass != null) {
            setYawMethod = null;
            setPitchMethod = null;
            try {
                for (Field f : entityClass.getDeclaredFields()) {
                    if (f.getType() == float.class) {
                        f.setAccessible(true);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private static void initEntityMethods() {
        if (playerEntityClass != null) {
            getInventoryMethod = tryFindMethod(playerEntityClass, new String[]{"method_31548", "getInventory"});
            if (getInventoryMethod == null) {
                getInventoryMethod = tryFindMethod(playerEntityClass, new String[]{"method_7355", "getInventory"});
            }
            if (getInventoryMethod != null) success("getInventory()"); else fail("getInventory()");

            getFoodLevelMethod = null;
            try {
                Method getHungerManager = tryFindMethod(playerEntityClass, new String[]{"method_7344", "getHungerManager"});
                if (getHungerManager != null) {
                    Class<?> hungerClass = getHungerManager.getReturnType();
                    getFoodLevelMethod = tryFindMethod(hungerClass, new String[]{"method_7336", "getFoodLevel"});
                }
            } catch (Exception ignored) {}
            if (getFoodLevelMethod != null) success("getFoodLevel()"); else fail("getFoodLevel()");

            getAbilitiesMethod = tryFindMethod(playerEntityClass, new String[]{"method_7337", "getAbilities"});
            if (getAbilitiesMethod != null) {
                try {
                    Class<?> abilitiesClass = getAbilitiesMethod.getReturnType();
                    flyingField = tryFindFieldAccessible(abilitiesClass, new String[]{"field_7438", "flying"});
                    if (flyingField != null) success("abilities.flying"); else fail("abilities.flying");
                } catch (Exception ignored) {}
                success("getAbilities()");
            } else {
                fail("getAbilities()");
            }
        }
    }

    private static void initWorldMethods() {
        if (worldClass != null || worldField != null) {
            try {
                Object mc = getInstanceMethod.invoke(null);
                Object world = worldField != null ? worldField.get(mc) : null;
                if (world != null) {
                    Class<?> clientWorldClass = world.getClass();
                    getPlayersMethod = tryFindMethod(clientWorldClass, new String[]{"method_18456", "getPlayers"});
                    if (getPlayersMethod == null) {
                        for (Method m : clientWorldClass.getMethods()) {
                            if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                                String name = m.getName();
                                if (name.contains("layer") || name.contains("method_184")) {
                                    getPlayersMethod = m;
                                    break;
                                }
                            }
                        }
                    }
                    if (getPlayersMethod != null) success("getPlayers()"); else fail("getPlayers()");
                }
            } catch (Exception e) { fail("world methods"); }
        }
    }

    private static void initSendMessage() {
        String[] classes = {"net.minecraft.class_746", "net.minecraft.class_1657", "net.minecraft.class_1297"};
        String[] methodNames = {"method_9203", "method_44096", "sendMessage"};

        for (String className : classes) {
            Class<?> clazz = tryLoadClass(className);
            if (clazz == null) continue;

            if (textClass != null) {
                for (String name : methodNames) {
                    sendMessageMethod = tryFindMethod(clazz, new String[]{name}, textClass, boolean.class);
                    if (sendMessageMethod != null) { success("sendMessage(Text,bool) on " + className); return; }
                }
                for (String name : methodNames) {
                    sendMessageMethod = tryFindMethod(clazz, new String[]{name}, textClass);
                    if (sendMessageMethod != null) { success("sendMessage(Text) on " + className); return; }
                }
                for (Method m : clazz.getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(textClass)) {
                        sendMessageMethod = m; success("sendMessage (by sig) on " + className); return;
                    }
                    if (m.getParameterCount() == 2 && m.getParameterTypes()[0].equals(textClass) && m.getParameterTypes()[1] == boolean.class) {
                        sendMessageMethod = m; success("sendMessage (by sig 2p) on " + className); return;
                    }
                }
            }
        }
        fail("sendMessage");
    }

    // === Reflection helpers ===

    private static Class<?> tryLoadClass(String... names) {
        for (String name : names) {
            try { return Class.forName(name); } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private static Method tryFindMethod(Class<?> clazz, String[] names, Class<?>... params) {
        for (String name : names) {
            try { return clazz.getMethod(name, params); } catch (NoSuchMethodException ignored) {}
            try { Method m = clazz.getDeclaredMethod(name, params); m.setAccessible(true); return m; } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private static Method tryFindStaticMethod(Class<?> clazz, String[] names, Class<?>... params) {
        Method m = tryFindMethod(clazz, names, params);
        if (m != null && java.lang.reflect.Modifier.isStatic(m.getModifiers())) return m;
        return null;
    }

    private static Method findStaticMethodBySignature(Class<?> clazz, Class<?>... params) {
        for (Method m : clazz.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != params.length) continue;
            boolean match = true;
            for (int i = 0; i < p.length; i++) { if (!p[i].equals(params[i])) { match = false; break; } }
            if (match) return m;
        }
        return null;
    }

    private static Method findMethodByParamTypes(Class<?> clazz, Class<?>... params) {
        for (Method m : clazz.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length != params.length) continue;
            boolean match = true;
            for (int i = 0; i < p.length; i++) { if (!p[i].equals(params[i])) { match = false; break; } }
            if (match) return m;
        }
        return null;
    }

    private static Field tryFindFieldAccessible(Class<?> clazz, String[] names) {
        for (String name : names) {
            try { Field f = clazz.getDeclaredField(name); f.setAccessible(true); return f; } catch (Exception ignored) {}
            try { Field f = clazz.getField(name); f.setAccessible(true); return f; } catch (Exception ignored) {}
        }
        return null;
    }

    private static void success(String what) { successCount++; log("  OK: " + what); }
    private static void fail(String what) { failCount++; log("  FAIL: " + what); }

    // === Public API: Core ===

    public static Object getMinecraftClient() {
        try { return getInstanceMethod.invoke(null); } catch (Exception e) { return null; }
    }

    public static Object getPlayer() {
        try {
            if (playerField == null) return null;
            Object mc = getMinecraftClient();
            return mc != null ? playerField.get(mc) : null;
        } catch (Exception e) { return null; }
    }

    public static Object getWorld() {
        try {
            if (worldField == null) return null;
            Object mc = getMinecraftClient();
            return mc != null ? worldField.get(mc) : null;
        } catch (Exception e) { return null; }
    }

    public static Object getCurrentScreen() {
        try {
            if (currentScreenField == null) return null;
            Object mc = getMinecraftClient();
            return mc != null ? currentScreenField.get(mc) : null;
        } catch (Exception e) { return null; }
    }

    public static void setScreen(Object screen) {
        try {
            if (setScreenMethod == null) return;
            Object mc = getMinecraftClient();
            if (mc != null) setScreenMethod.invoke(mc, screen);
        } catch (Exception ignored) {}
    }

    public static Object getTextRenderer() {
        try {
            if (textRendererField == null) return null;
            Object mc = getMinecraftClient();
            return mc != null ? textRendererField.get(mc) : null;
        } catch (Exception e) { return null; }
    }

    public static int getScaledWidth() {
        try {
            if (getWindowMethod == null || getScaledWidthMethod == null) return 800;
            Object mc = getMinecraftClient();
            Object window = getWindowMethod.invoke(mc);
            return (int) getScaledWidthMethod.invoke(window);
        } catch (Exception e) { return 800; }
    }

    public static int getScaledHeight() {
        try {
            if (getWindowMethod == null || getScaledHeightMethod == null) return 600;
            Object mc = getMinecraftClient();
            Object window = getWindowMethod.invoke(mc);
            return (int) getScaledHeightMethod.invoke(window);
        } catch (Exception e) { return 600; }
    }

    public static boolean isHudHidden() {
        try {
            if (hudHiddenField == null || optionsField == null) return false;
            Object mc = getMinecraftClient();
            Object options = optionsField.get(mc);
            Object value = hudHiddenField.get(options);
            if (value instanceof Boolean) return (boolean) value;
            return false;
        } catch (Exception e) { return false; }
    }

    // === Public API: Player State ===

    public static double getPlayerX() {
        try { Object p = getPlayer(); return p != null && getXMethod != null ? (double) getXMethod.invoke(p) : 0; } catch (Exception e) { return 0; }
    }

    public static double getPlayerY() {
        try { Object p = getPlayer(); return p != null && getYMethod != null ? (double) getYMethod.invoke(p) : 0; } catch (Exception e) { return 0; }
    }

    public static double getPlayerZ() {
        try { Object p = getPlayer(); return p != null && getZMethod != null ? (double) getZMethod.invoke(p) : 0; } catch (Exception e) { return 0; }
    }

    public static float getPlayerYaw() {
        try {
            Object p = getPlayer();
            if (p == null || getYawMethod == null) return 0;
            if (getYawMethod.getParameterCount() == 0) return (float) getYawMethod.invoke(p);
            return (float) getYawMethod.invoke(p, 1.0f);
        } catch (Exception e) { return 0; }
    }

    public static float getPlayerPitch() {
        try {
            Object p = getPlayer();
            if (p == null || getPitchMethod == null) return 0;
            if (getPitchMethod.getParameterCount() == 0) return (float) getPitchMethod.invoke(p);
            return (float) getPitchMethod.invoke(p, 1.0f);
        } catch (Exception e) { return 0; }
    }

    public static float getPlayerHealth() {
        try {
            Object p = getPlayer();
            return p != null && getHealthMethod != null ? (float) getHealthMethod.invoke(p) : 20.0f;
        } catch (Exception e) { return 20.0f; }
    }

    public static boolean isPlayerAlive() {
        try {
            Object p = getPlayer();
            return p != null && isAliveMethod != null && (boolean) isAliveMethod.invoke(p);
        } catch (Exception e) { return false; }
    }

    public static boolean isPlayerOnGround() {
        try {
            Object p = getPlayer();
            if (p == null) return true;
            if (isOnGroundMethod != null) return (boolean) isOnGroundMethod.invoke(p);
            if (onGroundField != null) {
                Object val = onGroundField.get(p);
                if (val instanceof Boolean) return (boolean) val;
            }
            return true;
        } catch (Exception e) { return true; }
    }

    public static boolean isPlayerSprinting() {
        try {
            Object p = getPlayer();
            return p != null && isSprintingMethod != null && (boolean) isSprintingMethod.invoke(p);
        } catch (Exception e) { return false; }
    }

    public static float getPlayerFallDistance() {
        try {
            Object p = getPlayer();
            if (p == null || fallDistanceField == null) return 0;
            Object val = fallDistanceField.get(p);
            if (val instanceof Float) return (float) val;
            if (val instanceof Number) return ((Number) val).floatValue();
            return 0;
        } catch (Exception e) { return 0; }
    }

    public static int getPlayerFoodLevel() {
        try {
            if (getFoodLevelMethod == null) return 20;
            Object p = getPlayer();
            if (p == null) return 20;
            Method hungerMgr = tryFindMethod(p.getClass(), new String[]{"method_7344", "getHungerManager"});
            if (hungerMgr == null) return 20;
            Object hm = hungerMgr.invoke(p);
            if (hm == null) return 20;
            return (int) getFoodLevelMethod.invoke(hm);
        } catch (Exception e) { return 20; }
    }

    // === Public API: Movement ===

    public static void setPlayerVelocity(double x, double y, double z) {
        try {
            Object p = getPlayer();
            if (p == null) return;
            if (setVelocity3Method != null) {
                setVelocity3Method.invoke(p, x, y, z);
            } else if (setVelocityMethod != null && vec3dClass != null) {
                Object vec = vec3dClass.getConstructor(double.class, double.class, double.class).newInstance(x, y, z);
                setVelocityMethod.invoke(p, vec);
            }
        } catch (Exception ignored) {}
    }

    public static double[] getPlayerVelocity() {
        try {
            Object p = getPlayer();
            if (p == null || getVelocityMethod == null) return new double[]{0, 0, 0};
            Object vec = getVelocityMethod.invoke(p);
            if (vec == null) return new double[]{0, 0, 0};
            Field xf = tryFindFieldAccessible(vec.getClass(), new String[]{"field_1352", "x"});
            Field yf = tryFindFieldAccessible(vec.getClass(), new String[]{"field_1351", "y"});
            Field zf = tryFindFieldAccessible(vec.getClass(), new String[]{"field_1350", "z"});
            double vx = xf != null ? xf.getDouble(vec) : 0;
            double vy = yf != null ? yf.getDouble(vec) : 0;
            double vz = zf != null ? zf.getDouble(vec) : 0;
            return new double[]{vx, vy, vz};
        } catch (Exception e) { return new double[]{0, 0, 0}; }
    }

    public static void setSprinting(boolean sprint) {
        try {
            Object p = getPlayer();
            if (p != null && setSprintingMethod != null) setSprintingMethod.invoke(p, sprint);
        } catch (Exception ignored) {}
    }

    public static void playerJump() {
        try {
            Object p = getPlayer();
            if (p != null && jumpMethod != null) jumpMethod.invoke(p);
        } catch (Exception ignored) {}
    }

    public static void setFlying(boolean flying) {
        try {
            Object p = getPlayer();
            if (p == null || getAbilitiesMethod == null || flyingField == null) return;
            Object abilities = getAbilitiesMethod.invoke(p);
            if (abilities != null) flyingField.set(abilities, flying);
        } catch (Exception ignored) {}
    }

    public static void setOnGround(boolean onGround) {
        try {
            Object p = getPlayer();
            if (p != null && onGroundField != null) onGroundField.set(p, onGround);
        } catch (Exception ignored) {}
    }

    public static void setFallDistance(float distance) {
        try {
            Object p = getPlayer();
            if (p != null && fallDistanceField != null) fallDistanceField.set(p, distance);
        } catch (Exception ignored) {}
    }

    // === Public API: Combat ===

    public static void attackEntity(Object target) {
        try {
            Object mc = getMinecraftClient();
            Object player = getPlayer();
            if (mc == null || player == null || target == null) return;
            if (attackEntityMethod != null) {
                Object im = interactionManagerField.get(mc);
                if (im != null) attackEntityMethod.invoke(im, player, target);
            }
        } catch (Exception ignored) {}
    }

    public static void swingHand() {
        try {
            Object p = getPlayer();
            if (p == null || swingHandMethod == null) return;
            Class<?> handClass = tryLoadClass("net.minecraft.class_1268");
            if (handClass != null) {
                Object mainHand = handClass.getEnumConstants()[0];
                swingHandMethod.invoke(p, mainHand);
            }
        } catch (Exception ignored) {}
    }

    // === Public API: World & Entities ===

    @SuppressWarnings("unchecked")
    public static List<Object> getPlayers() {
        try {
            Object world = getWorld();
            if (world == null || getPlayersMethod == null) return new ArrayList<>();
            Object result = getPlayersMethod.invoke(world);
            if (result instanceof List) return (List<Object>) result;
            return new ArrayList<>();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public static float getEntityHealth(Object entity) {
        try {
            if (entity == null || getHealthMethod == null) return 0;
            return (float) getHealthMethod.invoke(entity);
        } catch (Exception e) { return 0; }
    }

    public static boolean isEntityAlive(Object entity) {
        try {
            return entity != null && isAliveMethod != null && (boolean) isAliveMethod.invoke(entity);
        } catch (Exception e) { return false; }
    }

    public static double getEntityX(Object entity) {
        try { return entity != null && getXMethod != null ? (double) getXMethod.invoke(entity) : 0; } catch (Exception e) { return 0; }
    }

    public static double getEntityY(Object entity) {
        try { return entity != null && getYMethod != null ? (double) getYMethod.invoke(entity) : 0; } catch (Exception e) { return 0; }
    }

    public static double getEntityZ(Object entity) {
        try { return entity != null && getZMethod != null ? (double) getZMethod.invoke(entity) : 0; } catch (Exception e) { return 0; }
    }

    public static double distanceTo(Object entity) {
        try {
            Object player = getPlayer();
            if (player == null || entity == null) return 999;
            if (distanceToMethod != null) return (float) distanceToMethod.invoke(player, entity);
            double dx = getEntityX(entity) - getPlayerX();
            double dy = getEntityY(entity) - getPlayerY();
            double dz = getEntityZ(entity) - getPlayerZ();
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        } catch (Exception e) { return 999; }
    }

    public static String getEntityName(Object entity) {
        try {
            if (entity == null || getNameMethod == null) return "Unknown";
            Object nameObj = getNameMethod.invoke(entity);
            return nameObj != null ? nameObj.toString() : "Unknown";
        } catch (Exception e) { return "Unknown"; }
    }

    public static float[] getAnglesTo(Object entity) {
        try {
            if (entity == null) return null;
            double dx = getEntityX(entity) - getPlayerX();
            double dy = (getEntityY(entity) + 1.6) - (getPlayerY() + 1.62);
            double dz = getEntityZ(entity) - getPlayerZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
            return new float[]{yaw, pitch};
        } catch (Exception e) { return null; }
    }

    // === Public API: Rendering ===

    public static Object createText(String message) {
        try {
            if (textLiteralMethod == null) return null;
            return textLiteralMethod.invoke(null, message);
        } catch (Exception e) { return null; }
    }

    public static void sendChatMessage(String message) {
        try {
            Object player = getPlayer();
            if (player == null || sendMessageMethod == null) return;
            Object text = createText(message);
            if (text == null) return;
            if (sendMessageMethod.getParameterCount() == 2) {
                sendMessageMethod.invoke(player, text, false);
            } else {
                sendMessageMethod.invoke(player, text);
            }
        } catch (Exception ignored) {}
    }

    public static void drawFill(Object drawContext, int x1, int y1, int x2, int y2, int color) {
        try {
            if (drawContextFillMethod != null) drawContextFillMethod.invoke(drawContext, x1, y1, x2, y2, color);
        } catch (Exception ignored) {}
    }

    public static void drawText(Object drawContext, String text, int x, int y, int color, boolean shadow) {
        try {
            Object textRenderer = getTextRenderer();
            if (drawContextDrawTextMethod != null && textRenderer != null)
                drawContextDrawTextMethod.invoke(drawContext, textRenderer, text, x, y, color, shadow);
        } catch (Exception ignored) {}
    }

    public static int getTextWidth(String text) {
        return text.length() * 6;
    }

    // === State checks ===

    public static boolean isCoreReady() { return coreReady; }
    public static boolean canRender() { return drawContextFillMethod != null; }
    public static boolean canSendMessage() { return sendMessageMethod != null && textLiteralMethod != null; }
    public static boolean canCombat() { return attackEntityMethod != null; }
    public static boolean canMove() { return setVelocity3Method != null || setVelocityMethod != null; }
    public static boolean canGetPlayers() { return getPlayersMethod != null; }
    public static Class<?> getScreenClass() { return screenClass; }
    public static Class<?> getDrawContextClass() { return drawContextClass; }

    private static void log(String msg) {
        System.out.println("[Phantom McReflect] " + msg);
    }
}
