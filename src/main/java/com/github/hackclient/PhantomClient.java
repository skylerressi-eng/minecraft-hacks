package com.github.hackclient;

import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.ui.PhantomGui;
import com.github.hackclient.ui.PhantomGuiScreen;
import com.github.hackclient.ui.HudOverlay;
import com.github.hackclient.ui.HudRenderer;
import com.github.hackclient.antidetect.AntiCheatAnalyzer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phantom Client - Minecraft PvP hack client with game-mode-specific modules.
 * Meteor Client-style UI with Right Shift to open click GUI.
 *
 * Fabric integration:
 * - Registers keybinds via Fabric KeyBindingHelper
 * - Hooks into ClientTickEvents for module ticking and key checking
 * - Hooks into HudRenderCallback for overlay drawing
 */
public class PhantomClient implements ClientModInitializer {
    public static final String NAME = "Phantom Client";
    public static final String VERSION = "2.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static PhantomClient instance;
    private ModuleManager moduleManager;
    private GameModeManager gameModeManager;
    private PhantomGui gui;
    private HudRenderer hudRenderer;
    private AntiCheatAnalyzer antiCheatAnalyzer;

    // Fabric keybinds
    private KeyBinding openGuiKey;
    private KeyBinding breachSwapToggleKey;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("{} v{} initializing...", NAME, VERSION);

        // Core systems
        moduleManager = new ModuleManager();
        gameModeManager = new GameModeManager(moduleManager);
        antiCheatAnalyzer = new AntiCheatAnalyzer();

        // Register all game modes and their modules
        gameModeManager.registerAll();

        // GUI systems
        gui = new PhantomGui(moduleManager);
        hudRenderer = new HudRenderer(moduleManager);

        // Register keybinds with Fabric API
        registerKeybinds();

        // Register event callbacks
        registerEvents();

        LOGGER.info("Loaded {} modules across {} game modes",
                moduleManager.getModuleCount(),
                GameMode.values().length);
        LOGGER.info("Press RIGHT SHIFT to open the GUI");
        LOGGER.info("{} v{} ready!", NAME, VERSION);
    }

    /**
     * Register all keybinds with Fabric's KeyBindingHelper.
     * This makes them show up in Minecraft's Controls settings too.
     */
    private void registerKeybinds() {
        // Right Shift to open GUI
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.phantom.openGui",           // Translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,       // Default: Right Shift
                "category.phantom.general"        // Category in controls menu
        ));

        // V to toggle AutoBreachSwap
        breachSwapToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.phantom.breachSwapToggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.phantom.combat"
        ));
    }

    /**
     * Register Fabric event callbacks for tick processing, key handling, and HUD rendering.
     */
    private void registerEvents() {
        // Client tick: check keybinds + tick all enabled modules
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Check GUI keybind
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen instanceof PhantomGuiScreen) {
                    client.setScreen(null); // Close GUI
                } else if (client.currentScreen == null) {
                    client.setScreen(new PhantomGuiScreen(gui)); // Open GUI
                }
            }

            // Check breach swap toggle
            while (breachSwapToggleKey.wasPressed()) {
                moduleManager.getAllModules().forEach(m -> {
                    if (m instanceof com.github.hackclient.module.legacy.AutoBreachSwap) {
                        ((com.github.hackclient.module.legacy.AutoBreachSwap) m)
                                .onKeyPress(GLFW.GLFW_KEY_V);
                    }
                });
            }

            // Tick all enabled modules
            moduleManager.tickAll();
        });

        // HUD render: draw overlay on screen
        HudRenderCallback.EVENT.register(new HudOverlay(hudRenderer, gameModeManager));
    }

    // --- Accessors ---

    public static PhantomClient getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public GameModeManager getGameModeManager() { return gameModeManager; }
    public PhantomGui getGui() { return gui; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
    public AntiCheatAnalyzer getAntiCheatAnalyzer() { return antiCheatAnalyzer; }
}
