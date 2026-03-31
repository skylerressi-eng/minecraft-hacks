package com.github.hackclient.module;

import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.gamemode.GameModeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the module manager and game mode system.
 */
class ModuleManagerTest {

    private ModuleManager moduleManager;
    private GameModeManager gameModeManager;

    @BeforeEach
    void setUp() {
        moduleManager = new ModuleManager();
        gameModeManager = new GameModeManager(moduleManager);
        gameModeManager.registerAll();
    }

    @Test
    void testAllGameModesHaveModules() {
        for (GameMode mode : GameMode.values()) {
            List<Module> modules = moduleManager.getModulesForMode(mode);
            assertFalse(modules.isEmpty(),
                    mode.displayName + " should have at least one module");
        }
    }

    @Test
    void testMaceModulesRegistered() {
        List<Module> maceModules = moduleManager.getModulesForMode(GameMode.MACE);
        assertEquals(4, maceModules.size(), "Mace should have 4 modules");

        List<String> names = maceModules.stream().map(Module::getName).toList();
        assertTrue(names.contains("AutoWindBurst"));
        assertTrue(names.contains("AutoPearl"));
        assertTrue(names.contains("AutoStuntSlam"));
        assertTrue(names.contains("MaceSwapCombo"));
    }

    @Test
    void testLegacy18ModulesRegistered() {
        List<Module> legacyModules = moduleManager.getModulesForMode(GameMode.LEGACY_1_8);
        assertEquals(5, legacyModules.size(), "1.8 PvP should have 5 modules");

        List<String> names = legacyModules.stream().map(Module::getName).toList();
        assertTrue(names.contains("AutoClicker"));
        assertTrue(names.contains("Reach"));
        assertTrue(names.contains("Velocity"));
        assertTrue(names.contains("KillAura"));
        assertTrue(names.contains("WTap"));
    }

    @Test
    void testBedwarsModulesRegistered() {
        List<Module> bwModules = moduleManager.getModulesForMode(GameMode.BEDWARS);
        assertEquals(5, bwModules.size(), "Bedwars should have 5 modules");

        List<String> names = bwModules.stream().map(Module::getName).toList();
        assertTrue(names.contains("AutoBridge"));
        assertTrue(names.contains("FireballDeflect"));
        assertTrue(names.contains("BedAura"));
        assertTrue(names.contains("PearlClutch"));
        assertTrue(names.contains("AutoShop"));
    }

    @Test
    void testCrystalModulesRegistered() {
        List<Module> crystalModules = moduleManager.getModulesForMode(GameMode.CRYSTAL);
        assertEquals(5, crystalModules.size(), "Crystal should have 5 modules");

        List<String> names = crystalModules.stream().map(Module::getName).toList();
        assertTrue(names.contains("AutoCrystal"));
        assertTrue(names.contains("AutoTotem"));
        assertTrue(names.contains("HoleFinder"));
        assertTrue(names.contains("AutoPearlHole"));
        assertTrue(names.contains("Surround"));
    }

    @Test
    void testTotalModuleCount() {
        assertEquals(19, moduleManager.getModuleCount(),
                "Should have 19 total modules (4+5+5+5)");
    }

    @Test
    void testModuleToggle() {
        Module autoClicker = moduleManager.getModule("AutoClicker");
        assertNotNull(autoClicker);
        assertFalse(autoClicker.isEnabled());

        autoClicker.toggle();
        assertTrue(autoClicker.isEnabled());

        autoClicker.toggle();
        assertFalse(autoClicker.isEnabled());
    }

    @Test
    void testModuleLookupByName() {
        assertNotNull(moduleManager.getModule("AutoCrystal"));
        assertNotNull(moduleManager.getModule("autoclicker")); // case insensitive
        assertNull(moduleManager.getModule("NonExistent"));
    }

    @Test
    void testGameModeSwitchDisablesOtherModes() {
        // Enable some modules from different modes
        Module autoClicker = moduleManager.getModule("AutoClicker"); // 1.8
        Module autoCrystal = moduleManager.getModule("AutoCrystal"); // Crystal

        autoClicker.onEnable();
        autoCrystal.onEnable();

        assertTrue(autoClicker.isEnabled());
        assertTrue(autoCrystal.isEnabled());

        // Switch to crystal mode - should disable 1.8 modules
        gameModeManager.switchMode(GameMode.CRYSTAL);
        moduleManager.activateGameMode(GameMode.CRYSTAL);

        assertFalse(autoClicker.isEnabled(), "1.8 module should be disabled after switching to Crystal");
        assertTrue(autoCrystal.isEnabled(), "Crystal module should remain enabled");
    }

    @Test
    void testAllModulesHaveTimers() {
        for (Module m : moduleManager.getAllModules()) {
            assertNotNull(m.getTimer(),
                    m.getName() + " should have a humanized timer");
        }
    }

    @Test
    void testGameModeSummaryContainsAllModules() {
        String summary = gameModeManager.getSummary();
        assertNotNull(summary);

        // Check that all game modes appear
        for (GameMode mode : GameMode.values()) {
            assertTrue(summary.contains(mode.displayName),
                    "Summary should contain " + mode.displayName);
        }

        // Check that all modules appear
        for (Module m : moduleManager.getAllModules()) {
            assertTrue(summary.contains(m.getName()),
                    "Summary should contain module " + m.getName());
        }
    }
}
