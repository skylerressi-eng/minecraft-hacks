package com.github.hackclient;

import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.ui.PhantomGui;
import com.github.hackclient.ui.HudRenderer;
import com.github.hackclient.antidetect.AntiCheatAnalyzer;
import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Phantom Client - Minecraft PvP hack client with game-mode-specific modules.
 * Meteor Client-style UI with Right Shift to open click GUI.
 *
 * Uses reflection (McReflect) with intermediary names for all Minecraft class access.
 * Uses dynamic proxies for Fabric API event registration.
 */
public class PhantomClient implements ClientModInitializer {
    public static final String NAME = "Phantom Client";
    public static final String VERSION = "2.0.0";

    private static PhantomClient instance;
    private static ModuleManager moduleManager;
    private GameModeManager gameModeManager;
    private PhantomGui gui;
    private HudRenderer hudRenderer;
    private AntiCheatAnalyzer antiCheatAnalyzer;

    // Key state tracking for edge detection
    private boolean rightShiftWasDown = false;
    private boolean vKeyWasDown = false;
    private boolean[] numberKeysWereDown = new boolean[7];
    private boolean guiOpen = false;

    // Delayed init: McReflect needs MC to be fully loaded
    private boolean mcReflectReady = false;
    private int initAttempts = 0;
    private boolean welcomeSent = false;

    @Override
    public void onInitializeClient() {
        instance = this;
        log("Phantom Client v" + VERSION + " initializing...");

        // Core systems (no MC dependencies)
        moduleManager = new ModuleManager();
        gameModeManager = new GameModeManager(moduleManager);
        antiCheatAnalyzer = new AntiCheatAnalyzer();

        // Register all game modes and their modules
        gameModeManager.registerAll();

        // GUI systems (no MC dependencies)
        gui = new PhantomGui(moduleManager);
        hudRenderer = new HudRenderer(moduleManager);

        // Register tick handler via reflection proxy
        registerTickHandler();

        // Register HUD render callback via reflection proxy
        registerHudRenderHandler();

        log("Loaded " + moduleManager.getModuleCount() + " modules across " +
                GameMode.values().length + " game modes");
        log("Press RIGHT SHIFT to open GUI, V to toggle AutoBreachSwap");
        log("Phantom Client v" + VERSION + " ready!");
    }

    /**
     * Register a client tick handler using reflection + dynamic proxy.
     * ClientTickEvents is a Fabric API class with stable package names.
     * The EndTick interface method receives MinecraftClient (intermediary at runtime).
     */
    private void registerTickHandler() {
        try {
            Class<?> tickEventsClass = Class.forName(
                    "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");

            // Get the EndTick inner interface
            Class<?> endTickClass = null;
            for (Class<?> inner : tickEventsClass.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("EndTick")) {
                    endTickClass = inner;
                    break;
                }
            }
            if (endTickClass == null) {
                log("ERROR: Could not find EndTick interface");
                return;
            }

            // Create dynamic proxy implementing EndTick
            Object tickHandler = Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { endTickClass },
                    (proxy, method, args) -> {
                        if (method.getName().equals("onEndTick")) {
                            onClientTick();
                        }
                        return null;
                    }
            );

            // Get END_CLIENT_TICK event and call register()
            Field endTickField = tickEventsClass.getField("END_CLIENT_TICK");
            Object event = endTickField.get(null);
            registerOnEvent(event, tickHandler);
            log("Tick handler registered successfully");
        } catch (Exception e) {
            log("ERROR registering tick handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Register a HUD render callback using reflection + dynamic proxy.
     * HudRenderCallback is a Fabric API class. The onHudRender method receives
     * DrawContext and RenderTickCounter (intermediary names at runtime).
     */
    private void registerHudRenderHandler() {
        try {
            Class<?> hudCallbackClass = Class.forName(
                    "net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");

            // Create dynamic proxy implementing HudRenderCallback
            Object hudHandler = Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { hudCallbackClass },
                    (proxy, method, args) -> {
                        if (method.getName().equals("onHudRender") && args != null && args.length >= 1) {
                            onHudRender(args[0]); // args[0] = DrawContext instance
                        }
                        return null;
                    }
            );

            // Get EVENT field and call register()
            Field eventField = hudCallbackClass.getField("EVENT");
            Object event = eventField.get(null);
            registerOnEvent(event, hudHandler);
            log("HUD render handler registered successfully");
        } catch (Exception e) {
            log("ERROR registering HUD render handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper: call event.register(handler) via reflection.
     * Works for any Fabric Event object.
     */
    private void registerOnEvent(Object event, Object handler) throws Exception {
        for (Method m : event.getClass().getMethods()) {
            if (m.getName().equals("register") && m.getParameterCount() == 1) {
                m.invoke(event, handler);
                return;
            }
        }
        throw new RuntimeException("No register() method found on event: " + event.getClass());
    }

    /**
     * Called every client tick. Uses GLFW directly for key input.
     */
    private void onClientTick() {
        try {
            // Lazy-init McReflect (MC must be fully loaded first)
            if (!mcReflectReady) {
                initAttempts++;
                if (initAttempts < 20) return; // Wait ~1 second for MC to load
                mcReflectReady = McReflect.init();
                if (!mcReflectReady) {
                    if (initAttempts % 100 == 0) {
                        log("Waiting for MC reflection init... (attempt " + initAttempts + ")");
                    }
                    return;
                }
                log("McReflect initialized on tick " + initAttempts);
            }

            long window = GLFW.glfwGetCurrentContext();
            if (window == 0L) return;

            // Don't process keybinds when a screen is open (like chat)
            Object currentScreen = McReflect.getCurrentScreen();

            // Send welcome message once when player is available
            if (!welcomeSent && McReflect.getPlayer() != null) {
                welcomeSent = true;
                if (McReflect.canSendMessage()) {
                    McReflect.sendChatMessage("\u00a7b[Phantom Client] \u00a7fv" + VERSION + " loaded! Press \u00a7eRIGHT SHIFT\u00a7f to open GUI.");
                }
                log("Player joined - Phantom Client active!");
            }

            // Only process keybinds when no screen is open
            if (currentScreen == null) {
                handleKeyInput(window);
            }

            // Tick all enabled modules
            moduleManager.tickAll();
        } catch (Exception e) {
            // Silently ignore tick errors to prevent spam
        }
    }

    /**
     * Handle key input using GLFW (stable class, no remapping needed).
     */
    private void handleKeyInput(long window) {
        // Right Shift - toggle GUI
        boolean rightShiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (rightShiftDown && !rightShiftWasDown) {
            guiOpen = !guiOpen;
            gui.setVisible(guiOpen);
            if (guiOpen) {
                McReflect.sendChatMessage("\u00a7b[Phantom] \u00a7aGUI opened \u00a77- click modules to toggle");
            } else {
                McReflect.sendChatMessage("\u00a7b[Phantom] \u00a7cGUI closed");
            }
        }
        rightShiftWasDown = rightShiftDown;

        // V key - toggle AutoBreachSwap
        boolean vKeyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
        if (vKeyDown && !vKeyWasDown) {
            moduleManager.getAllModules().forEach(m -> {
                if (m.getName().equals("AutoBreachSwap")) {
                    m.toggle();
                    String state = m.isEnabled() ? "\u00a7aENABLED" : "\u00a7cDISABLED";
                    McReflect.sendChatMessage("\u00a7b[Phantom] \u00a7fAutoBreachSwap " + state);
                }
            });
        }
        vKeyWasDown = vKeyDown;

        // Number keys 1-7 to switch game modes
        // 1=All Hacks 2=Mace 3=1.8 4=Bedwars 5=Crystal 6=Meteor 7=Vape
        GameMode[] modes = GameMode.values();
        for (int i = 0; i < 7 && i < modes.length; i++) {
            boolean keyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS;
            if (keyDown && !numberKeysWereDown[i]) {
                gameModeManager.switchMode(modes[i]);
                McReflect.sendChatMessage("\u00a7b[Phantom] \u00a7fSwitched to \u00a7e" + modes[i].displayName + "\u00a7f mode");
            }
            numberKeysWereDown[i] = keyDown;
        }
    }

    /**
     * Called every frame to render the HUD overlay.
     * Receives the DrawContext object from the HUD render event.
     */
    private void onHudRender(Object drawContext) {
        if (!mcReflectReady || !McReflect.canRender()) return;

        try {
            int screenWidth = McReflect.getScaledWidth();
            int screenHeight = McReflect.getScaledHeight();

            // Don't render HUD if it's hidden in game options
            if (McReflect.isHudHidden()) return;

            // Render GUI panels if open
            if (guiOpen) {
                renderGui(drawContext, screenWidth, screenHeight);
            }

            // Always render HUD overlay (active modules list, watermark, coords)
            renderHudOverlay(drawContext, screenWidth, screenHeight);

        } catch (Exception e) {
            // Silently ignore render errors
        }
    }

    /**
     * Render the click GUI panels.
     */
    private void renderGui(Object drawContext, int screenWidth, int screenHeight) {
        // Get mouse position via GLFW
        // (We can't easily get cursor pos without MC's mouse handler, so use 0,0 for now)
        List<PhantomGui.RenderCommand> commands = gui.getRenderCommands(0, 0);

        for (PhantomGui.RenderCommand cmd : commands) {
            switch (cmd.type) {
                case RECT:
                case MODULE_BUTTON:
                    McReflect.drawFill(drawContext,
                            cmd.x, cmd.y,
                            cmd.x + cmd.width, cmd.y + cmd.height,
                            cmd.color);
                    // Draw text on header/button
                    if (cmd.text != null) {
                        McReflect.drawText(drawContext, cmd.text,
                                cmd.x + 4, cmd.y + (cmd.height - 8) / 2,
                                0xFFE0E0E0, true);
                    }
                    break;
                case TEXT:
                    McReflect.drawText(drawContext, cmd.text,
                            cmd.x, cmd.y, cmd.color, true);
                    break;
            }
        }
    }

    /**
     * Render the HUD overlay (watermark, active modules, coords, info bar).
     */
    private void renderHudOverlay(Object drawContext, int screenWidth, int screenHeight) {
        // Get player coords
        double x = McReflect.getPlayerX();
        double y = McReflect.getPlayerY();
        double z = McReflect.getPlayerZ();

        // Get current game mode name
        String gameModeName = gameModeManager.getActiveMode() != null
                ? gameModeManager.getActiveMode().displayName : "None";

        // Use HudRenderer to compute layout
        HudRenderer.HudData data = hudRenderer.getHudData(
                screenWidth, screenHeight, gameModeName, 0, 0, x, y, z);

        // Watermark (top-left)
        if (data.watermark != null) {
            // Background bar
            McReflect.drawFill(drawContext, 0, 0,
                    McReflect.getTextWidth(data.watermark) + 8, 14, 0x80000000);
            McReflect.drawText(drawContext, "Phantom Client v" + VERSION,
                    data.watermarkX, data.watermarkY, data.watermarkColor, true);
        }

        // Game mode indicator
        if (data.gameModeText != null) {
            McReflect.drawText(drawContext, data.gameModeText,
                    data.gameModeX, data.gameModeY, data.gameModeColor, true);
        }

        // Active modules list (top-right, Meteor style)
        if (data.activeModules != null) {
            for (HudRenderer.HudEntry entry : data.activeModules) {
                // Background for each module entry
                int textW = McReflect.getTextWidth(entry.text);
                McReflect.drawFill(drawContext,
                        screenWidth - textW - 6, entry.y,
                        screenWidth, entry.y + 12,
                        entry.bgColor);
                // Accent bar on right edge (Meteor style)
                McReflect.drawFill(drawContext,
                        screenWidth - 2, entry.y,
                        screenWidth, entry.y + 12,
                        0xFF00D4AA);
                McReflect.drawText(drawContext, entry.text,
                        entry.x, entry.y + 2, entry.color, true);
            }
        }

        // Coordinates (bottom-left)
        if (data.coordsText != null) {
            McReflect.drawFill(drawContext, 0, data.coordsY - 2,
                    McReflect.getTextWidth(data.coordsText) + 8, data.coordsY + 10,
                    0x80000000);
            McReflect.drawText(drawContext, data.coordsText,
                    data.coordsX, data.coordsY, data.coordsColor, true);
        }

        // Info bar (bottom-left, below coords)
        if (data.infoText != null) {
            McReflect.drawFill(drawContext, 0, data.infoY - 2,
                    McReflect.getTextWidth(data.infoText) + 8, data.infoY + 10,
                    0x80000000);
            McReflect.drawText(drawContext, data.infoText,
                    data.infoX, data.infoY, data.infoColor, true);
        }
    }

    private void log(String msg) {
        System.out.println("[Phantom Client] " + msg);
    }

    // --- Accessors ---
    public static PhantomClient getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public GameModeManager getGameModeManager() { return gameModeManager; }
    public PhantomGui getGui() { return gui; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
    public AntiCheatAnalyzer getAntiCheatAnalyzer() { return antiCheatAnalyzer; }
}
