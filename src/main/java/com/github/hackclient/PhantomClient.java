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
    private boolean guiOpen = false;

    private boolean mcReflectReady = false;
    private int initAttempts = 0;
    private boolean welcomeSent = false;

    @Override
    public void onInitializeClient() {
        instance = this;
        log("Phantom Client v" + VERSION + " initializing...");

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
                    getClass().getClassLoader(),
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
        }
    }

    private void registerHudRenderHandler() {
        try {
            Class<?> hudCallbackClass = Class.forName(
                    "net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");

            Object hudHandler = Proxy.newProxyInstance(
                    getClass().getClassLoader(),
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

    private void onClientTick() {
        try {
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

            long window = GLFW.glfwGetCurrentContext();
            if (window == 0L) return;

            Object currentScreen = McReflect.getCurrentScreen();

            if (!welcomeSent && McReflect.getPlayer() != null) {
                welcomeSent = true;
                if (McReflect.canSendMessage()) {
                    McReflect.sendChatMessage("§b[Phantom] §fv" + VERSION + " loaded! Press §eRIGHT SHIFT§f to open GUI.");
                }
                log("Player joined - Phantom Client active!");
            }

            if (currentScreen == null) handleKeyInput(window);

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
            if (guiOpen) {
                McReflect.sendChatMessage("§b[Phantom] §aGUI opened §7- click modules to toggle");
            } else {
                McReflect.sendChatMessage("§b[Phantom] §cGUI closed");
            }
        }
        rightShiftWasDown = rightShiftDown;

        boolean vKeyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
        if (vKeyDown && !vKeyWasDown) {
            moduleManager.getAllModules().forEach(m -> {
                if (m.getName().equals("AutoBreachSwap")) {
                    m.toggle();
                    String state = m.isEnabled() ? "§aENABLED" : "§cDISABLED";
                    McReflect.sendChatMessage("§b[Phantom] §fAutoBreachSwap " + state);
                }
            });
        }
        vKeyWasDown = vKeyDown;

        GameMode[] modes = GameMode.values();
        for (int i = 0; i < 7 && i < modes.length; i++) {
            boolean keyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS;
            if (keyDown && !numberKeysWereDown[i]) {
                gameModeManager.switchMode(modes[i]);
                McReflect.sendChatMessage("§b[Phantom] §fSwitched to §e" + modes[i].displayName + "§f mode");
            }
            numberKeysWereDown[i] = keyDown;
        }
    }

    private void onHudRender(Object drawContext) {
        if (!mcReflectReady || !McReflect.canRender()) return;
        try {
            int screenWidth = McReflect.getScaledWidth();
            int screenHeight = McReflect.getScaledHeight();
            if (McReflect.isHudHidden()) return;

            if (guiOpen) renderGui(drawContext, screenWidth, screenHeight);
            renderHudOverlay(drawContext, screenWidth, screenHeight);
        } catch (Exception ignored) {}
    }

    private void renderGui(Object drawContext, int screenWidth, int screenHeight) {
        List<PhantomGui.RenderCommand> commands = gui.getRenderCommands(0, 0);
        for (PhantomGui.RenderCommand cmd : commands) {
            switch (cmd.type) {
                case RECT:
                case MODULE_BUTTON:
                    McReflect.drawFill(drawContext, cmd.x, cmd.y, cmd.x + cmd.width, cmd.y + cmd.height, cmd.color);
                    if (cmd.text != null)
                        McReflect.drawText(drawContext, cmd.text, cmd.x + 4, cmd.y + (cmd.height - 8) / 2, 0xFFE0E0E0, true);
                    break;
                case TEXT:
                    McReflect.drawText(drawContext, cmd.text, cmd.x, cmd.y, cmd.color, true);
                    break;
            }
        }
    }

    private void renderHudOverlay(Object drawContext, int screenWidth, int screenHeight) {
        double x = McReflect.getPlayerX();
        double y = McReflect.getPlayerY();
        double z = McReflect.getPlayerZ();

        String gameModeName = gameModeManager.getActiveMode() != null
                ? gameModeManager.getActiveMode().displayName : "None";

        HudRenderer.HudData data = hudRenderer.getHudData(screenWidth, screenHeight, gameModeName, 0, 0, x, y, z);

        if (data.watermark != null) {
            McReflect.drawFill(drawContext, 0, 0, McReflect.getTextWidth(data.watermark) + 8, 14, 0x80000000);
            McReflect.drawText(drawContext, "Phantom Client v" + VERSION, data.watermarkX, data.watermarkY, data.watermarkColor, true);
        }

        if (data.gameModeText != null) {
            McReflect.drawText(drawContext, data.gameModeText, data.gameModeX, data.gameModeY, data.gameModeColor, true);
        }

        if (data.activeModules != null) {
            for (HudRenderer.HudEntry entry : data.activeModules) {
                int textW = McReflect.getTextWidth(entry.text);
                McReflect.drawFill(drawContext, screenWidth - textW - 6, entry.y, screenWidth, entry.y + 12, entry.bgColor);
                McReflect.drawFill(drawContext, screenWidth - 2, entry.y, screenWidth, entry.y + 12, 0xFF00D4AA);
                McReflect.drawText(drawContext, entry.text, entry.x, entry.y + 2, entry.color, true);
            }
        }

        if (data.coordsText != null) {
            McReflect.drawFill(drawContext, 0, data.coordsY - 2, McReflect.getTextWidth(data.coordsText) + 8, data.coordsY + 10, 0x80000000);
            McReflect.drawText(drawContext, data.coordsText, data.coordsX, data.coordsY, data.coordsColor, true);
        }

        int stealthY = data.coordsY != 0 ? data.coordsY - 14 : screenHeight - 48;
        String stealthText = getStealthStatusText();
        McReflect.drawFill(drawContext, 0, stealthY - 2, McReflect.getTextWidth(stealthText) + 8, stealthY + 10, 0x80000000);
        McReflect.drawText(drawContext, stealthText, 4, stealthY, getStealthStatusColor(), true);

        if (data.infoText != null) {
            McReflect.drawFill(drawContext, 0, data.infoY - 2, McReflect.getTextWidth(data.infoText) + 8, data.infoY + 10, 0x80000000);
            McReflect.drawText(drawContext, data.infoText, data.infoX, data.infoY, data.infoColor, true);
        }
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
}
