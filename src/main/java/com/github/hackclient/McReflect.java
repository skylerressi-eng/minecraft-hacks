package com.github.hackclient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection helper that uses INTERMEDIARY names to access Minecraft classes.
 * This is needed because we compile without Fabric Loom (no remapping).
 * At runtime, Minecraft uses intermediary names like class_310, method_1551, etc.
 */
public class McReflect {

    // Cached classes
    private static Class<?> mcClientClass;
    private static Class<?> textClass;
    private static Class<?> screenClass;
    private static Class<?> drawContextClass;

    // Cached methods
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

    // Cached fields
    private static Field playerField;
    private static Field currentScreenField;
    private static Field textRendererField;
    private static Field optionsField;
    private static Field hudHiddenField;

    private static boolean initialized = false;
    private static boolean initFailed = false;

    public static boolean init() {
        if (initialized) return !initFailed;
        initialized = true;
        try {
            // Classes (intermediary names)
            mcClientClass = Class.forName("net.minecraft.class_310");
            textClass = Class.forName("net.minecraft.class_2561");
            screenClass = Class.forName("net.minecraft.class_437");
            drawContextClass = Class.forName("net.minecraft.class_332");
            Class<?> textRendererClass = Class.forName("net.minecraft.class_327");
            Class<?> windowClass = Class.forName("net.minecraft.class_1041");
            Class<?> gameOptionsClass = Class.forName("net.minecraft.class_315");

            // MinecraftClient methods/fields
            getInstanceMethod = mcClientClass.getMethod("method_1551");
            playerField = mcClientClass.getDeclaredField("field_1724");
            playerField.setAccessible(true);
            currentScreenField = mcClientClass.getDeclaredField("field_1755");
            currentScreenField.setAccessible(true);
            textRendererField = mcClientClass.getDeclaredField("field_1772");
            textRendererField.setAccessible(true);
            optionsField = mcClientClass.getDeclaredField("field_1690");
            optionsField.setAccessible(true);
            setScreenMethod = findMethod(mcClientClass, "method_1507", screenClass);
            getWindowMethod = mcClientClass.getMethod("method_22683");

            // Window methods
            getScaledWidthMethod = windowClass.getMethod("method_4486");
            getScaledHeightMethod = windowClass.getMethod("method_4502");

            // GameOptions fields
            hudHiddenField = gameOptionsClass.getDeclaredField("field_1842");
            hudHiddenField.setAccessible(true);

            // Text.literal(String) - static method
            textLiteralMethod = textClass.getMethod("method_43470", String.class);

            // DrawContext methods
            drawContextFillMethod = findMethod(drawContextClass, "method_25294",
                    int.class, int.class, int.class, int.class, int.class);
            drawContextDrawTextMethod = findMethod(drawContextClass, "method_51433",
                    textRendererClass, String.class, int.class, int.class, int.class, boolean.class);

            // Entity.getX/Y/Z - on the player object's parent class
            Class<?> entityClass = Class.forName("net.minecraft.class_1297");
            getXMethod = entityClass.getMethod("method_23317");
            getYMethod = entityClass.getMethod("method_23318");
            getZMethod = entityClass.getMethod("method_23321");

            // sendMessage - try to find it on player's class hierarchy
            // PlayerEntity.sendMessage(Text, boolean) = method_9203
            sendMessageMethod = findSendMessage();

            log("MC reflection initialized successfully");
            return true;
        } catch (Exception e) {
            log("MC reflection init FAILED: " + e.getMessage());
            e.printStackTrace();
            initFailed = true;
            return false;
        }
    }

    private static Method findSendMessage() {
        try {
            // Try PlayerEntity (class_1657) method_9203(Text, boolean)
            Class<?> playerEntityClass = Class.forName("net.minecraft.class_1657");
            for (Method m : playerEntityClass.getMethods()) {
                if (m.getName().equals("method_9203") && m.getParameterCount() == 2) {
                    return m;
                }
            }
            // Fallback: try sendMessage with just Text parameter
            for (Method m : playerEntityClass.getMethods()) {
                if (m.getName().equals("method_9203") && m.getParameterCount() == 1) {
                    return m;
                }
            }
        } catch (Exception ignored) {}
        // Last resort: search all methods for one that takes a Text-like param
        try {
            Class<?> clientPlayerClass = Class.forName("net.minecraft.class_746");
            for (Method m : clientPlayerClass.getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(textClass)) {
                    return m;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            // Try declared methods
            try {
                Method m = clazz.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e2) {
                log("Could not find method " + name + " on " + clazz.getSimpleName());
                return null;
            }
        }
    }

    // --- Public API ---

    public static Object getMinecraftClient() {
        try { return getInstanceMethod.invoke(null); } catch (Exception e) { return null; }
    }

    public static Object getPlayer() {
        try {
            Object mc = getMinecraftClient();
            return mc != null ? playerField.get(mc) : null;
        } catch (Exception e) { return null; }
    }

    public static Object getCurrentScreen() {
        try {
            Object mc = getMinecraftClient();
            return mc != null ? currentScreenField.get(mc) : null;
        } catch (Exception e) { return null; }
    }

    public static void setScreen(Object screen) {
        try {
            Object mc = getMinecraftClient();
            if (mc != null && setScreenMethod != null) {
                setScreenMethod.invoke(mc, screen);
            }
        } catch (Exception ignored) {}
    }

    public static Object getTextRenderer() {
        try {
            Object mc = getMinecraftClient();
            return mc != null ? textRendererField.get(mc) : null;
        } catch (Exception e) { return null; }
    }

    public static int getScaledWidth() {
        try {
            Object mc = getMinecraftClient();
            Object window = getWindowMethod.invoke(mc);
            return (int) getScaledWidthMethod.invoke(window);
        } catch (Exception e) { return 800; }
    }

    public static int getScaledHeight() {
        try {
            Object mc = getMinecraftClient();
            Object window = getWindowMethod.invoke(mc);
            return (int) getScaledHeightMethod.invoke(window);
        } catch (Exception e) { return 600; }
    }

    public static boolean isHudHidden() {
        try {
            Object mc = getMinecraftClient();
            Object options = optionsField.get(mc);
            return (boolean) hudHiddenField.get(options);
        } catch (Exception e) { return false; }
    }

    public static Object createText(String message) {
        try { return textLiteralMethod.invoke(null, message); } catch (Exception e) { return null; }
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

    public static double getPlayerX() {
        try { Object p = getPlayer(); return p != null ? (double) getXMethod.invoke(p) : 0; }
        catch (Exception e) { return 0; }
    }

    public static double getPlayerY() {
        try { Object p = getPlayer(); return p != null ? (double) getYMethod.invoke(p) : 0; }
        catch (Exception e) { return 0; }
    }

    public static double getPlayerZ() {
        try { Object p = getPlayer(); return p != null ? (double) getZMethod.invoke(p) : 0; }
        catch (Exception e) { return 0; }
    }

    // --- DrawContext rendering helpers ---

    public static void drawFill(Object drawContext, int x1, int y1, int x2, int y2, int color) {
        try {
            if (drawContextFillMethod != null) {
                drawContextFillMethod.invoke(drawContext, x1, y1, x2, y2, color);
            }
        } catch (Exception ignored) {}
    }

    public static void drawText(Object drawContext, String text, int x, int y, int color, boolean shadow) {
        try {
            Object textRenderer = getTextRenderer();
            if (drawContextDrawTextMethod != null && textRenderer != null) {
                drawContextDrawTextMethod.invoke(drawContext, textRenderer, text, x, y, color, shadow);
            }
        } catch (Exception ignored) {}
    }

    public static int getTextWidth(String text) {
        return text.length() * 6; // Approximate
    }

    public static Class<?> getScreenClass() { return screenClass; }
    public static Class<?> getDrawContextClass() { return drawContextClass; }

    private static void log(String msg) {
        System.out.println("[Phantom Client] " + msg);
    }
}
