package com.github.hackclient;

import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.ui.PhantomGui;
import com.github.hackclient.ui.HudRenderer;
import com.github.hackclient.antidetect.AntiCheatAnalyzer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phantom Client - Minecraft PvP hack client with game-mode-specific modules.
 * Meteor Client-style UI with Right Shift to open click GUI.
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

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("{} v{} initializing...", NAME, VERSION);

        moduleManager = new ModuleManager();
        gameModeManager = new GameModeManager(moduleManager);
        antiCheatAnalyzer = new AntiCheatAnalyzer();

        // Register all game modes and their modules
        gameModeManager.registerAll();

        // Initialize Meteor-style GUI (Right Shift to open)
        gui = new PhantomGui(moduleManager);
        hudRenderer = new HudRenderer(moduleManager);

        LOGGER.info("Loaded {} modules across {} game modes",
                moduleManager.getModuleCount(),
                GameMode.values().length);
        LOGGER.info("Press RIGHT SHIFT to open the GUI");
        LOGGER.info("{} v{} ready!", NAME, VERSION);
    }

    /**
     * Handle key press events. Called from Fabric key event mixin.
     * Right Shift opens the Meteor-style click GUI.
     */
    public void onKeyPress(int keyCode) {
        if (gui != null) {
            gui.onKeyPress(keyCode);
        }
        // Forward to modules that have keybinds
        moduleManager.getAllModules().forEach(m -> {
            if (m instanceof com.github.hackclient.module.legacy.AutoBreachSwap) {
                ((com.github.hackclient.module.legacy.AutoBreachSwap) m).onKeyPress(keyCode);
            }
        });
    }

    public static PhantomClient getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public GameModeManager getGameModeManager() {
        return gameModeManager;
    }

    public PhantomGui getGui() {
        return gui;
    }

    public HudRenderer getHudRenderer() {
        return hudRenderer;
    }

    public AntiCheatAnalyzer getAntiCheatAnalyzer() {
        return antiCheatAnalyzer;
    }
}
