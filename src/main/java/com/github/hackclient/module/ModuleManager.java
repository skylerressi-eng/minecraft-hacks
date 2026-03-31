package com.github.hackclient.module;

import com.github.hackclient.gamemode.GameMode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all hack modules and provides lookup by game mode.
 */
public class ModuleManager {
    private final List<Module> allModules = new ArrayList<>();
    private final Map<GameMode, List<Module>> modulesByMode = new EnumMap<>(GameMode.class);

    public ModuleManager() {
        for (GameMode mode : GameMode.values()) {
            modulesByMode.put(mode, new ArrayList<>());
        }
    }

    public void register(Module module) {
        allModules.add(module);
        modulesByMode.get(module.getGameMode()).add(module);
    }

    public List<Module> getModulesForMode(GameMode mode) {
        return modulesByMode.getOrDefault(mode, List.of());
    }

    public List<Module> getAllModules() {
        return allModules;
    }

    public int getModuleCount() {
        return allModules.size();
    }

    public Module getModule(String name) {
        return allModules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /** Enable all modules for a specific game mode, disable all others */
    public void activateGameMode(GameMode mode) {
        for (Module m : allModules) {
            if (m.isEnabled() && m.getGameMode() != mode) {
                m.onDisable();
            }
        }
    }

    /** Tick all enabled modules */
    public void tickAll() {
        for (Module m : allModules) {
            if (m.isEnabled()) {
                m.onTick();
            }
        }
    }
}
