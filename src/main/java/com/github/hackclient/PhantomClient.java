package com.github.hackclient;

import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.gamemode.GameMode;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phantom Client - Minecraft PvP hack client with game-mode-specific modules.
 * School project demonstrating game automation concepts.
 */
public class PhantomClient implements ClientModInitializer {
    public static final String NAME = "Phantom Client";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static PhantomClient instance;
    private ModuleManager moduleManager;
    private GameModeManager gameModeManager;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("{} v{} initializing...", NAME, VERSION);

        moduleManager = new ModuleManager();
        gameModeManager = new GameModeManager(moduleManager);

        // Register all game modes and their modules
        gameModeManager.registerAll();

        LOGGER.info("Loaded {} modules across {} game modes",
                moduleManager.getModuleCount(),
                GameMode.values().length);
        LOGGER.info("{} ready!", NAME);
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
}
