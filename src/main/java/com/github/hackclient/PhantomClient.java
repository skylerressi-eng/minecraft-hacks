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

/**
 * Phantom Client - Minecraft PvP hack client with game-mode-specific modules.
 * Meteor Client-style UI with Right Shift to open click GUI.
 *
 * Uses reflection for all Minecraft class access to avoid
 * intermediary mapping issues (compiled without Fabric Loom).
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
    private boolean guiOpen = false;

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

        // Register tick handler via reflection (avoids MC class imports)
        registerTickHandler();

        log("Loaded " + moduleManager.getModuleCount() + " modules across " +
                GameMode.values().length + " game modes");
        log("Press RIGHT SHIFT to toggle modules, V to toggle AutoBreachSwap");
        log("Phantom Client v" + VERSION + " ready!");
    }

    /**
     * Register a client tick handler using reflection + dynamic proxy.
     * This avoids importing any net.minecraft.* classes directly.
     */
    private void registerTickHandler() {
        try {
            // Load Fabric API tick events class (stable package name)
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

            // Create a dynamic proxy that implements EndTick
            final Class<?> finalEndTickClass = endTickClass;
            Object tickHandler = Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] { finalEndTickClass },
                    (proxy, method, args) -> {
                        if (method.getName().equals("onEndTick")) {
                            onClientTick();
                        }
                        return null;
                    }
            );

            // Get END_CLIENT_TICK field
            Field endTickField = tickEventsClass.getField("END_CLIENT_TICK");
            Object event = endTickField.get(null);

            // Call event.register(handler) - type erasure makes this work with Object
            Method registerMethod = null;
            for (Method m : event.getClass().getMethods()) {
                if (m.getName().equals("register") && m.getParameterCount() == 1) {
                    registerMethod = m;
                    break;
                }
            }
            if (registerMethod != null) {
                registerMethod.invoke(event, tickHandler);
                log("Tick handler registered successfully");
            }
        } catch (Exception e) {
            log("ERROR: Failed to register tick handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called every client tick. Uses GLFW directly for key input
     * (LWJGL classes have stable names, no remapping needed).
     */
    private void onClientTick() {
        try {
            long window = GLFW.glfwGetCurrentContext();
            if (window == 0L) return;

            // Right Shift - toggle GUI / cycle modules display
            boolean rightShiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (rightShiftDown && !rightShiftWasDown) {
                guiOpen = !guiOpen;
                gui.setVisible(guiOpen);
                if (guiOpen) {
                    log("GUI opened - modules visible");
                } else {
                    log("GUI closed");
                }
            }
            rightShiftWasDown = rightShiftDown;

            // V key - toggle AutoBreachSwap
            boolean vKeyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
            if (vKeyDown && !vKeyWasDown) {
                moduleManager.getAllModules().forEach(m -> {
                    if (m instanceof com.github.hackclient.module.legacy.AutoBreachSwap) {
                        ((com.github.hackclient.module.legacy.AutoBreachSwap) m)
                                .onKeyPress(GLFW.GLFW_KEY_V);
                    }
                });
            }
            vKeyWasDown = vKeyDown;

            // Number keys 1-4 to switch game modes
            for (int i = 0; i < 4; i++) {
                boolean keyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS;
                if (keyDown) {
                    GameMode[] modes = GameMode.values();
                    if (i < modes.length) {
                        gameModeManager.switchMode(modes[i]);
                    }
                }
            }

            // Tick all enabled modules
            moduleManager.tickAll();
        } catch (Exception e) {
            // Silently ignore tick errors to prevent spam
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
