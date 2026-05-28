package com.github.hackclient;

import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.ui.PhantomGui;
import com.github.hackclient.ui.HudRenderer;
import com.github.hackclient.antidetect.AntiCheatAnalyzer;
import com.github.hackclient.antidetect.StealthEngine;
import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;

public class PhantomClient implements ClientModInitializer {
    public static final String NAME = "Phantom Client";
    public static final String VERSION = "3.0.0";

    private static PhantomClient instance;
    private static ModuleManager moduleManager;
    private GameModeManager gameModeManager;
    private PhantomGui gui;
    private HudRenderer hudRenderer;
    private AntiCheatAnalyzer antiCheatAnalyzer;
    private StealthEngine stealthEngine;

    private boolean rightShiftWasDown = false;
    private boolean vKeyWasDown = false;
    private boolean[] numberKeysWereDown = new boolean[7];
    private static boolean guiOpen = false;

    private boolean mcReflectReady = false;
    private int initAttempts = 0;
    private boolean welcomeSent = false;
    private boolean titleSet = false;

    private static Method dcFill;
    private static Method dcDrawText;
    private static Object textRendererObj;
    private static boolean renderInitDone = false;

    @Override
    public void onInitializeClient() {
        instance = this;
        log("Phantom Client v" + VERSION + " initializing...");

        try {
            moduleManager = new ModuleManager();
            antiCheatAnalyzer = new AntiCheatAnalyzer();
            stealthEngine = new StealthEngine(antiCheatAnalyzer);
            gameModeManager = new GameModeManager(moduleManager);

            gameModeManager.registerAll();

            gui = new PhantomGui(moduleManager);
            hudRenderer = new HudRenderer(moduleManager);

            registerTickHandler();
            registerHudRenderHandler();

            log("Loaded " + moduleManager.getModuleCount() + " modules across " +
                    GameMode.values().length + " game modes");
            log("Press RIGHT SHIFT to open GUI");
            log("Phantom Client v" + VERSION + " ready!");
        } catch (Exception e) {
            log("FATAL ERROR during init: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerTickHandler() {
        try {
            Class<?> tickEventsClass = Class.forName(
                    "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");

            Class<?> endTickClass = null;
            for (Class<?> inner : tickEventsClass.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("EndTick")) {
                    endTickClass = inner;
                    break;
                }
            }
            if (endTickClass == null) { log("ERROR: Could not find EndTick interface"); return; }

            Object tickHandler = Proxy.newProxyInstance(
                    endTickClass.getClassLoader(),
                    new Class<?>[] { endTickClass },
                    (proxy, method, args) -> {
                        if (method.getName().equals("onEndTick")) onClientTick();
                        return null;
                    }
            );

            Field endTickField = tickEventsClass.getField("END_CLIENT_TICK");
            Object event = endTickField.get(null);
            registerOnEvent(event, tickHandler);
            log("Tick handler registered");
        } catch (Exception e) {
            log("ERROR registering tick handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerHudRenderHandler() {
        try {
            Class<?> hudCallbackClass = Class.forName(
                    "net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");

            Object hudHandler = Proxy.newProxyInstance(
                    hudCallbackClass.getClassLoader(),
                    new Class<?>[] { hudCallbackClass },
                    (proxy, method, args) -> {
                        if (method.getName().equals("onHudRender") && args != null && args.length >= 1)
                            onHudRender(args[0]);
                        return null;
                    }
            );

            Field eventField = hudCallbackClass.getField("EVENT");
            Object event = eventField.get(null);
            registerOnEvent(event, hudHandler);
            log("HUD render handler registered");
        } catch (Exception e) {
            log("ERROR registering HUD render handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerOnEvent(Object event, Object handler) throws Exception {
        for (Method m : event.getClass().getMethods()) {
            if (m.getName().equals("register") && m.getParameterCount() == 1) {
                m.invoke(event, handler);
                return;
            }
        }
        throw new RuntimeException("No register() method found on event");
    }

    public static void onKeyEvent(int key, int action) {
        if (instance == null) return;
        if (action != 1) return; // GLFW_PRESS = 1

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            instance.rightShiftWasDown = true;
            guiOpen = !guiOpen;
            if (instance.gui != null) instance.gui.setVisible(guiOpen);
            instance.log("GUI " + (guiOpen ? "OPENED" : "CLOSED") + " (via Mixin)");

            try {
                long w = GLFW.glfwGetCurrentContext();
                if (w != 0L) {
                    GLFW.glfwSetWindowTitle(w, guiOpen
                        ? "Minecraft - [Phantom Client GUI OPEN]"
                        : "Minecraft");
                }
            } catch (Exception ignored) {}

            instance.trySendChat(guiOpen
                ? "§b[Phantom] §aGUI opened §7- click modules to toggle"
                : "§b[Phantom] §cGUI closed");
        }

        if (key == GLFW.GLFW_KEY_ESCAPE && guiOpen) {
            guiOpen = false;
            if (instance.gui != null) instance.gui.setVisible(false);
            instance.log("GUI CLOSED (via ESC)");
        }

        if (key == GLFW.GLFW_KEY_V && moduleManager != null) {
            instance.vKeyWasDown = true;
            moduleManager.getAllModules().forEach(m -> {
                if (m.getName().equals("AutoBreachSwap")) {
                    m.toggle();
                    instance.log("AutoBreachSwap " + (m.isEnabled() ? "ENABLED" : "DISABLED"));
                    instance.trySendChat("§b[Phantom] §fAutoBreachSwap " + (m.isEnabled() ? "§aENABLED" : "§cDISABLED"));
                }
            });
        }

        GameMode[] modes = GameMode.values();
        for (int i = 0; i < 7 && i < modes.length; i++) {
            if (key == GLFW.GLFW_KEY_1 + i) {
                instance.numberKeysWereDown[i] = true;
                if (instance.gameModeManager != null) {
                    instance.gameModeManager.switchMode(modes[i]);
                    instance.log("Switched to " + modes[i].displayName + " mode");
                    instance.trySendChat("§b[Phantom] §fSwitched to §e" + modes[i].displayName + "§f mode");
                }
            }
        }
    }

    private void onClientTick() {
        try {
            if (!titleSet) {
                long w = GLFW.glfwGetCurrentContext();
                if (w != 0L) {
                    titleSet = true;
                    log("Phantom Client tick handler active - GLFW window found");
                }
            }

            long window = GLFW.glfwGetCurrentContext();
            if (window != 0L) {
                handleKeyInput(window);
            }

            if (!mcReflectReady) {
                initAttempts++;
                if (initAttempts < 20) return;
                mcReflectReady = McReflect.init();
                if (!mcReflectReady) {
                    if (initAttempts % 100 == 0) log("Waiting for MC reflection init... (attempt " + initAttempts + ")");
                    return;
                }
                log("McReflect initialized on tick " + initAttempts);
            }

            stealthEngine.onTick();
            checkStaffProximity();

            if (!welcomeSent && McReflect.getPlayer() != null) {
                welcomeSent = true;
                if (McReflect.canSendMessage()) {
                    McReflect.sendChatMessage("§b[Phantom] §fv" + VERSION + " loaded! Press §eRIGHT SHIFT§f to open GUI.");
                }
                log("Player joined - Phantom Client active!");
            }

            moduleManager.tickAll();
        } catch (Exception e) {
            // Silently ignore tick errors
        }
    }

    private void checkStaffProximity() {
        if (!McReflect.canGetPlayers()) return;
        try {
            List<Object> players = McReflect.getPlayers();
            Object self = McReflect.getPlayer();
            if (self == null) return;

            boolean foundStaff = false;
            for (Object p : players) {
                if (p == self) continue;
                String name = McReflect.getEntityName(p);
                if (name == null) continue;
                String lower = name.toLowerCase();
                if (lower.contains("[staff]") || lower.contains("[mod]") || lower.contains("[admin]")
                        || lower.contains("[helper]") || lower.contains("[sr.mod]") || lower.contains("[owner]")) {
                    foundStaff = true;
                    break;
                }
            }
            stealthEngine.setStaffNearby(foundStaff);
        } catch (Exception ignored) {}
    }

    private void handleKeyInput(long window) {
        boolean rightShiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (rightShiftDown && !rightShiftWasDown) {
            guiOpen = !guiOpen;
            gui.setVisible(guiOpen);
            log("GUI " + (guiOpen ? "OPENED" : "CLOSED") + " (via GLFW poll)");

            try {
                GLFW.glfwSetWindowTitle(window, guiOpen
                    ? "Minecraft - [Phantom Client GUI OPEN]"
                    : "Minecraft");
            } catch (Exception ignored) {}

            trySendChat(guiOpen
                ? "§b[Phantom] §aGUI opened §7- click modules to toggle"
                : "§b[Phantom] §cGUI closed");
        }
        rightShiftWasDown = rightShiftDown;

        boolean vKeyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
        if (vKeyDown && !vKeyWasDown) {
            moduleManager.getAllModules().forEach(m -> {
                if (m.getName().equals("AutoBreachSwap")) {
                    m.toggle();
                    log("AutoBreachSwap " + (m.isEnabled() ? "ENABLED" : "DISABLED"));
                    trySendChat("§b[Phantom] §fAutoBreachSwap " + (m.isEnabled() ? "§aENABLED" : "§cDISABLED"));
                }
            });
        }
        vKeyWasDown = vKeyDown;

        GameMode[] modes = GameMode.values();
        for (int i = 0; i < 7 && i < modes.length; i++) {
            boolean keyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS;
            if (keyDown && !numberKeysWereDown[i]) {
                gameModeManager.switchMode(modes[i]);
                log("Switched to " + modes[i].displayName + " mode");
                trySendChat("§b[Phantom] §fSwitched to §e" + modes[i].displayName + "§f mode");
            }
            numberKeysWereDown[i] = keyDown;
        }
    }

    private void trySendChat(String message) {
        try {
            if (mcReflectReady && McReflect.canSendMessage()) {
                McReflect.sendChatMessage(message);
            }
        } catch (Exception ignored) {}
    }

    private void initRendering(Object drawContext) {
        if (renderInitDone) return;
        renderInitDone = true;

        try {
            Class<?> dc = drawContext.getClass();
            log("DrawContext actual class: " + dc.getName());

            String[] fillNames = {"method_25294", "fill"};
            for (String name : fillNames) {
                try { dcFill = dc.getMethod(name, int.class, int.class, int.class, int.class, int.class); break; }
                catch (NoSuchMethodException ignored) {}
                try { Method m = dc.getDeclaredMethod(name, int.class, int.class, int.class, int.class, int.class); m.setAccessible(true); dcFill = m; break; }
                catch (NoSuchMethodException ignored) {}
            }

            if (dcFill == null) {
                for (Method m : dc.getMethods()) {
                    if (m.getReturnType() == void.class) {
                        Class<?>[] p = m.getParameterTypes();
                        if (p.length == 5 && p[0]==int.class && p[1]==int.class && p[2]==int.class && p[3]==int.class && p[4]==int.class) {
                            dcFill = m;
                            log("Found fill by signature: " + m.getName());
                            break;
                        }
                    }
                }
            }

            for (Method m : dc.getMethods()) {
                if (m.getReturnType() == int.class) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 6 && p[1]==String.class && p[2]==int.class && p[3]==int.class && p[4]==int.class && p[5]==boolean.class) {
                        dcDrawText = m;
                        log("Found drawText: " + m.getName() + " (TextRenderer type: " + p[0].getName() + ")");
                        break;
                    }
                }
            }

            if (dcDrawText != null) {
                try {
                    Class<?> trType = dcDrawText.getParameterTypes()[0];
                    Class<?> mcClass = Class.forName("net.minecraft.class_310");
                    Object mc = findStaticGetter(mcClass);
                    if (mc != null) {
                        for (Field f : mcClass.getDeclaredFields()) {
                            try {
                                f.setAccessible(true);
                                if (f.getType() == trType || trType.isAssignableFrom(f.getType())) {
                                    Object tr = f.get(mc);
                                    if (tr != null) {
                                        textRendererObj = tr;
                                        log("Found TextRenderer from field: " + f.getName());
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    log("TextRenderer lookup failed: " + e.getMessage());
                }
            }

            log("Direct render init: fill=" + (dcFill != null) + " drawText=" + (dcDrawText != null) + " textRenderer=" + (textRendererObj != null));
        } catch (Exception e) {
            log("Render init error: " + e.getMessage());
        }
    }

    private static Object findStaticGetter(Class<?> clazz) {
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 0 && Modifier.isStatic(m.getModifiers()) && m.getReturnType() == clazz) {
                try { return m.invoke(null); } catch (Exception ignored) {}
            }
        }
        String[] names = {"method_1551", "getInstance"};
        for (String name : names) {
            try { return clazz.getMethod(name).invoke(null); } catch (Exception ignored) {}
        }
        return null;
    }

    private static void directFill(Object dc, int x1, int y1, int x2, int y2, int color) {
        try { if (dcFill != null) dcFill.invoke(dc, x1, y1, x2, y2, color); } catch (Exception ignored) {}
    }

    private static void directDrawText(Object dc, String text, int x, int y, int color) {
        try {
            if (dcDrawText != null && textRendererObj != null)
                dcDrawText.invoke(dc, textRendererObj, text, x, y, color, true);
        } catch (Exception ignored) {}
    }

    private void onHudRender(Object drawContext) {
        try {
            initRendering(drawContext);
            if (dcFill == null) return;

            int screenWidth = 960;
            int screenHeight = 540;
            boolean hudHidden = false;

            if (mcReflectReady) {
                try { screenWidth = McReflect.getScaledWidth(); } catch (Exception ignored) {}
                try { screenHeight = McReflect.getScaledHeight(); } catch (Exception ignored) {}
                try { hudHidden = McReflect.isHudHidden(); } catch (Exception ignored) {}
            }

            if (hudHidden) return;

            if (guiOpen) renderGui(drawContext);

            if (mcReflectReady) {
                renderHudOverlay(drawContext, screenWidth, screenHeight);
            }
        } catch (Exception ignored) {}
    }

    private void renderGui(Object drawContext) {
        List<PhantomGui.RenderCommand> commands = gui.getRenderCommands(0, 0);
        for (PhantomGui.RenderCommand cmd : commands) {
            switch (cmd.type) {
                case RECT:
                case MODULE_BUTTON:
                    directFill(drawContext, cmd.x, cmd.y, cmd.x + cmd.width, cmd.y + cmd.height, cmd.color);
                    if (cmd.text != null)
                        directDrawText(drawContext, cmd.text, cmd.x + 4, cmd.y + (cmd.height - 8) / 2, 0xFFE0E0E0);
                    break;
                case TEXT:
                    directDrawText(drawContext, cmd.text, cmd.x, cmd.y, cmd.color);
                    break;
            }
        }
    }

    private void renderHudOverlay(Object drawContext, int screenWidth, int screenHeight) {
        try {
            double x = McReflect.getPlayerX();
            double y = McReflect.getPlayerY();
            double z = McReflect.getPlayerZ();

            String gameModeName = gameModeManager.getActiveMode() != null
                    ? gameModeManager.getActiveMode().displayName : "None";

            HudRenderer.HudData data = hudRenderer.getHudData(screenWidth, screenHeight, gameModeName, 0, 0, x, y, z);

            if (data.watermark != null) {
                int textW = estimateTextWidth(data.watermark);
                directFill(drawContext, 0, 0, textW + 8, 14, 0x80000000);
                directDrawText(drawContext, "Phantom Client v" + VERSION, data.watermarkX, data.watermarkY, data.watermarkColor);
            }

            if (data.gameModeText != null) {
                directDrawText(drawContext, data.gameModeText, data.gameModeX, data.gameModeY, data.gameModeColor);
            }

            if (data.activeModules != null) {
                for (HudRenderer.HudEntry entry : data.activeModules) {
                    int textW = estimateTextWidth(entry.text);
                    directFill(drawContext, screenWidth - textW - 6, entry.y, screenWidth, entry.y + 12, entry.bgColor);
                    directFill(drawContext, screenWidth - 2, entry.y, screenWidth, entry.y + 12, 0xFF00D4AA);
                    directDrawText(drawContext, entry.text, entry.x, entry.y + 2, entry.color);
                }
            }

            if (data.coordsText != null) {
                int textW = estimateTextWidth(data.coordsText);
                directFill(drawContext, 0, data.coordsY - 2, textW + 8, data.coordsY + 10, 0x80000000);
                directDrawText(drawContext, data.coordsText, data.coordsX, data.coordsY, data.coordsColor);
            }

            int stealthY = data.coordsY != 0 ? data.coordsY - 14 : screenHeight - 48;
            String stealthText = getStealthStatusText();
            int stealthW = estimateTextWidth(stealthText);
            directFill(drawContext, 0, stealthY - 2, stealthW + 8, stealthY + 10, 0x80000000);
            directDrawText(drawContext, stealthText, 4, stealthY, getStealthStatusColor());

            if (data.infoText != null) {
                int infoW = estimateTextWidth(data.infoText);
                directFill(drawContext, 0, data.infoY - 2, infoW + 8, data.infoY + 10, 0x80000000);
                directDrawText(drawContext, data.infoText, data.infoX, data.infoY, data.infoColor);
            }
        } catch (Exception ignored) {}
    }

    private int estimateTextWidth(String text) {
        return text.length() * 6;
    }

    private String getStealthStatusText() {
        if (stealthEngine.isPanicMode()) return "STEALTH: PANIC";
        if (stealthEngine.isStaffNearby()) return "STEALTH: STAFF DETECTED";
        if (stealthEngine.isLegitMode()) return "STEALTH: LEGIT MODE";
        int score = stealthEngine.getSuspicionScore();
        if (score > 40) return "STEALTH: CAUTION (" + score + ")";
        return "STEALTH: GHOST (" + score + ")";
    }

    private int getStealthStatusColor() {
        if (stealthEngine.isPanicMode()) return 0xFFFF3333;
        if (stealthEngine.isStaffNearby()) return 0xFFFF8800;
        if (stealthEngine.isLegitMode()) return 0xFF88FF88;
        int score = stealthEngine.getSuspicionScore();
        if (score > 40) return 0xFFFFFF00;
        return 0xFF00D4AA;
    }

    private void log(String msg) { System.out.println("[Phantom Client] " + msg); }

    public static PhantomClient getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public GameModeManager getGameModeManager() { return gameModeManager; }
    public PhantomGui getGui() { return gui; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
    public AntiCheatAnalyzer getAntiCheatAnalyzer() { return antiCheatAnalyzer; }
    public StealthEngine getStealthEngine() { return stealthEngine; }
    public static boolean isGuiOpen() { return guiOpen; }
}
