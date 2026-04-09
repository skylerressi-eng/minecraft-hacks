package com.github.hackclient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection helper that uses INTERMEDIARY names to access Minecraft classes.
 * This is needed because we compile without Fabric Loom (no remapping).
 * At runtime, Minecraft uses intermediary names like class_310, method_1551, etc.
 *
 * Each reflection lookup is independent — one failure won't break everything.
 * Multiple possible intermediary names are tried for each method/field.
 */
public class McReflect {

    // Cached classes
    private static Class<?> mcClientClass;
    private static Class<?> textClass;
    private static Class<?> screenClass;
    private static Class<?> drawContextClass;
    private static Class<?> textRendererClass;

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
    private static boolean coreReady = false; // MC client + player access works
    private static int successCount = 0;
    private static int failCount = 0;

    /**
     * Initialize all reflection lookups. Each is independent.
     * Returns true if at least the core (MC client access) works.
     */
    public static boolean init() {
        if (initialized) return coreReady;
        initialized = true;
        successCount = 0;
        failCount = 0;

        // === CORE: MinecraftClient class ===
        mcClientClass = tryLoadClass(
            "net.minecraft.class_310"
        );
        if (mcClientClass == null) {
            log("FATAL: Could not find MinecraftClient class - mod cannot function");
            return false;
        }
        log("Found MinecraftClient: " + mcClientClass.getName());

        // === getInstance() ===
        getInstanceMethod = tryFindMethod(mcClientClass, new String[]{
            "method_1551", "getInstance"
        });
        if (getInstanceMethod == null) {
            log("FATAL: Could not find getInstance() - mod cannot function");
            return false;
        }
        success("MinecraftClient.getInstance()");

        // Test that getInstance works
        try {
            Object mc = getInstanceMethod.invoke(null);
            if (mc == null) {
                log("WARNING: getInstance() returned null (MC not fully loaded yet)");
                initialized = false; // Allow retry
                return false;
            }
            log("getInstance() works - MC instance obtained");
        } catch (Exception e) {
            log("WARNING: getInstance() threw exception: " + e.getMessage());
            initialized = false;
            return false;
        }

        coreReady = true;

        // === Player field ===
        playerField = tryFindField(mcClientClass, new String[]{
            "field_1724", "player"
        });
        if (playerField != null) {
            playerField.setAccessible(true);
            success("player field");
        } else {
            fail("player field");
        }

        // === currentScreen field ===
        currentScreenField = tryFindField(mcClientClass, new String[]{
            "field_1755", "currentScreen"
        });
        if (currentScreenField != null) {
            currentScreenField.setAccessible(true);
            success("currentScreen field");
        } else {
            fail("currentScreen field");
        }

        // === textRenderer field ===
        textRendererField = tryFindField(mcClientClass, new String[]{
            "field_1772", "textRenderer"
        });
        if (textRendererField != null) {
            textRendererField.setAccessible(true);
            success("textRenderer field");
        } else {
            fail("textRenderer field");
        }

        // === options field ===
        optionsField = tryFindField(mcClientClass, new String[]{
            "field_1690", "options"
        });
        if (optionsField != null) {
            optionsField.setAccessible(true);
            success("options field");
        } else {
            fail("options field");
        }

        // === Other classes (non-fatal) ===
        textClass = tryLoadClass("net.minecraft.class_2561");
        if (textClass != null) success("Text class"); else fail("Text class");

        screenClass = tryLoadClass("net.minecraft.class_437");
        if (screenClass != null) success("Screen class"); else fail("Screen class");

        drawContextClass = tryLoadClass("net.minecraft.class_332");
        if (drawContextClass != null) success("DrawContext class"); else fail("DrawContext class");

        textRendererClass = tryLoadClass("net.minecraft.class_327");
        if (textRendererClass != null) success("TextRenderer class"); else fail("TextRenderer class");

        // === Window methods ===
        getWindowMethod = tryFindMethod(mcClientClass, new String[]{
            "method_22683", "getWindow"
        });
        if (getWindowMethod != null) {
            success("getWindow()");

            // Get Window class from return type
            Class<?> windowClass = getWindowMethod.getReturnType();
            getScaledWidthMethod = tryFindMethod(windowClass, new String[]{
                "method_4486", "getScaledWidth"
            });
            if (getScaledWidthMethod != null) success("getScaledWidth()"); else fail("getScaledWidth()");

            getScaledHeightMethod = tryFindMethod(windowClass, new String[]{
                "method_4502", "getScaledHeight"
            });
            if (getScaledHeightMethod != null) success("getScaledHeight()"); else fail("getScaledHeight()");
        } else {
            fail("getWindow()");
        }

        // === GameOptions.hudHidden ===
        if (optionsField != null) {
            try {
                Object mc = getInstanceMethod.invoke(null);
                Object options = optionsField.get(mc);
                if (options != null) {
                    Class<?> optionsClass = options.getClass();
                    hudHiddenField = tryFindField(optionsClass, new String[]{
                        "field_1842", "hudHidden"
                    });
                    if (hudHiddenField != null) {
                        hudHiddenField.setAccessible(true);
                        success("hudHidden field");
                    } else {
                        fail("hudHidden field (non-fatal)");
                    }
                }
            } catch (Exception e) {
                fail("hudHidden lookup: " + e.getMessage());
            }
        }

        // === setScreen method ===
        if (screenClass != null) {
            setScreenMethod = tryFindMethod(mcClientClass, new String[]{
                "method_1507", "setScreen"
            }, screenClass);
            if (setScreenMethod != null) success("setScreen()"); else fail("setScreen()");
        }

        // === Text.literal() ===
        if (textClass != null) {
            textLiteralMethod = tryFindStaticMethod(textClass, new String[]{
                "method_43470", "literal"
            }, String.class);
            if (textLiteralMethod == null) {
                // Fallback: search for static methods that take String and return Text-like
                textLiteralMethod = findStaticMethodBySignature(textClass, String.class);
            }
            if (textLiteralMethod != null) success("Text.literal()"); else fail("Text.literal()");
        }

        // === DrawContext methods ===
        if (drawContextClass != null) {
            // fill(int, int, int, int, int) - 5 ints
            drawContextFillMethod = tryFindMethod(drawContextClass, new String[]{
                "method_25294", "fill"
            }, int.class, int.class, int.class, int.class, int.class);
            if (drawContextFillMethod == null) {
                // Fallback: search for any method with 5 int params
                drawContextFillMethod = findMethodByParamTypes(drawContextClass,
                    int.class, int.class, int.class, int.class, int.class);
            }
            if (drawContextFillMethod != null) success("DrawContext.fill()"); else fail("DrawContext.fill()");

            // drawText(TextRenderer, String, int, int, int, boolean)
            if (textRendererClass != null) {
                drawContextDrawTextMethod = tryFindMethod(drawContextClass, new String[]{
                    "method_25303", "method_51433", "drawText"
                }, textRendererClass, String.class, int.class, int.class, int.class, boolean.class);
                if (drawContextDrawTextMethod == null) {
                    // Fallback: search by param signature
                    drawContextDrawTextMethod = findMethodByParamTypes(drawContextClass,
                        textRendererClass, String.class, int.class, int.class, int.class, boolean.class);
                }
                if (drawContextDrawTextMethod != null) success("DrawContext.drawText()"); else fail("DrawContext.drawText()");
            }
        }

        // === Entity position methods ===
        Class<?> entityClass = tryLoadClass("net.minecraft.class_1297");
        if (entityClass != null) {
            getXMethod = tryFindMethod(entityClass, new String[]{"method_23317", "getX"});
            getYMethod = tryFindMethod(entityClass, new String[]{"method_23318", "getY"});
            getZMethod = tryFindMethod(entityClass, new String[]{"method_23321", "getZ"});
            if (getXMethod != null) success("getX/Y/Z()"); else fail("getX/Y/Z()");
        } else {
            fail("Entity class");
        }

        // === sendMessage ===
        initSendMessage();

        log("McReflect init complete: " + successCount + " succeeded, " + failCount + " failed");
        return coreReady;
    }

    private static void initSendMessage() {
        // Try multiple classes and method names
        String[] classes = {
            "net.minecraft.class_746",  // ClientPlayerEntity
            "net.minecraft.class_1657", // PlayerEntity
            "net.minecraft.class_1297"  // Entity
        };
        String[] methodNames = {"method_9203", "method_44096", "sendMessage"};

        for (String className : classes) {
            Class<?> clazz = tryLoadClass(className);
            if (clazz == null) continue;

            // Try sendMessage(Text, boolean) first
            if (textClass != null) {
                for (String name : methodNames) {
                    sendMessageMethod = tryFindMethod(clazz, new String[]{name}, textClass, boolean.class);
                    if (sendMessageMethod != null) {
                        success("sendMessage(Text, boolean) on " + className);
                        return;
                    }
                }
                // Try sendMessage(Text) - no overlay param
                for (String name : methodNames) {
                    sendMessageMethod = tryFindMethod(clazz, new String[]{name}, textClass);
                    if (sendMessageMethod != null) {
                        success("sendMessage(Text) on " + className);
                        return;
                    }
                }
            }

            // Last resort: find any method with 1 param that's the Text class
            if (textClass != null) {
                for (Method m : clazz.getMethods()) {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(textClass)) {
                        sendMessageMethod = m;
                        success("sendMessage (found by signature) on " + className);
                        return;
                    }
                    if (m.getParameterCount() == 2 && m.getParameterTypes()[0].equals(textClass)
                            && m.getParameterTypes()[1] == boolean.class) {
                        sendMessageMethod = m;
                        success("sendMessage (found by signature, 2-param) on " + className);
                        return;
                    }
                }
            }
        }
        fail("sendMessage (tried all approaches)");
    }

    // === Reflection lookup helpers ===

    private static Class<?> tryLoadClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private static Method tryFindMethod(Class<?> clazz, String[] names, Class<?>... params) {
        for (String name : names) {
            try {
                return clazz.getMethod(name, params);
            } catch (NoSuchMethodException ignored) {}
            try {
                Method m = clazz.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private static Method tryFindStaticMethod(Class<?> clazz, String[] names, Class<?>... params) {
        Method m = tryFindMethod(clazz, names, params);
        if (m != null && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
            return m;
        }
        return null;
    }

    private static Method findStaticMethodBySignature(Class<?> clazz, Class<?>... params) {
        for (Method m : clazz.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != params.length) continue;
            boolean match = true;
            for (int i = 0; i < p.length; i++) {
                if (!p[i].equals(params[i])) { match = false; break; }
            }
            if (match) return m;
        }
        return null;
    }

    private static Method findMethodByParamTypes(Class<?> clazz, Class<?>... params) {
        for (Method m : clazz.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length != params.length) continue;
            boolean match = true;
            for (int i = 0; i < p.length; i++) {
                if (!p[i].equals(params[i])) { match = false; break; }
            }
            if (match) return m;
        }
        return null;
    }

    private static Field tryFindField(Class<?> clazz, String[] names) {
        for (String name : names) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
            try {
                return clazz.getField(name);
            } catch (NoSuchFieldException ignored) {}
        }
        // Fallback: search all fields up the hierarchy
        return null;
    }

    private static void success(String what) {
        successCount++;
        log("  OK: " + what);
    }

    private static void fail(String what) {
        failCount++;
        log("  FAIL: " + what);
    }

    // === Public API ===

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
        } catch (Exception e) {
            // Log once for debugging, then silently ignore
            log("sendChatMessage error: " + e.getMessage());
        }
    }

    public static double getPlayerX() {
        try {
            if (getXMethod == null) return 0;
            Object p = getPlayer();
            return p != null ? (double) getXMethod.invoke(p) : 0;
        } catch (Exception e) { return 0; }
    }

    public static double getPlayerY() {
        try {
            if (getYMethod == null) return 0;
            Object p = getPlayer();
            return p != null ? (double) getYMethod.invoke(p) : 0;
        } catch (Exception e) { return 0; }
    }

    public static double getPlayerZ() {
        try {
            if (getZMethod == null) return 0;
            Object p = getPlayer();
            return p != null ? (double) getZMethod.invoke(p) : 0;
        } catch (Exception e) { return 0; }
    }

    // === DrawContext rendering helpers ===

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
        return text.length() * 6; // Approximate MC font width
    }

    public static boolean isCoreReady() { return coreReady; }
    public static boolean canRender() { return drawContextFillMethod != null; }
    public static boolean canSendMessage() { return sendMessageMethod != null && textLiteralMethod != null; }
    public static Class<?> getScreenClass() { return screenClass; }
    public static Class<?> getDrawContextClass() { return drawContextClass; }

    private static void log(String msg) {
        System.out.println("[Phantom McReflect] " + msg);
    }
}
