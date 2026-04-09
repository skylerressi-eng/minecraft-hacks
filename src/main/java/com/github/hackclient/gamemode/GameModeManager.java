package com.github.hackclient.gamemode;

import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.module.mace.*;
import com.github.hackclient.module.legacy.*;
import com.github.hackclient.module.bedwars.*;
import com.github.hackclient.module.crystal.*;
import com.github.hackclient.module.combat.InstantPot;

/**
 * Manages game modes and registers all modules for each mode.
 */
public class GameModeManager {
    private final ModuleManager moduleManager;
    private GameMode activeMode = null;

    public GameModeManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    /**
     * Register all modules for all game modes.
     */
    public void registerAll() {
        registerMaceModules();
        registerLegacy18Modules();
        registerBedwarsModules();
        registerCrystalModules();
    }

    private void registerMaceModules() {
        moduleManager.register(new AutoWindBurst());
        moduleManager.register(new AutoPearl());
        moduleManager.register(new AutoStuntSlam());
        moduleManager.register(new MaceSwapCombo());
        moduleManager.register(new AutoShieldSwap());
        moduleManager.register(new ShieldRotation());
        moduleManager.register(new WindburstPearlMacro());
        moduleManager.register(new InstantPot()); // Instant pot works in mace mode
    }

    private void registerLegacy18Modules() {
        moduleManager.register(new AutoClicker());
        moduleManager.register(new Reach());
        moduleManager.register(new Velocity());
        moduleManager.register(new KillAura());
        moduleManager.register(new WTap());
        moduleManager.register(new AutoBreachSwap());
        moduleManager.register(new SpearCombo());
    }

    private void registerBedwarsModules() {
        moduleManager.register(new AutoBridge());
        moduleManager.register(new FireballDeflect());
        moduleManager.register(new BedAura());
        moduleManager.register(new PearlClutch());
        moduleManager.register(new AutoShop());
        moduleManager.register(new InvisDetector());
    }

    private void registerCrystalModules() {
        HoleFinder holeFinder = new HoleFinder();
        AutoPearlIntoHole autoPearl = new AutoPearlIntoHole();
        autoPearl.setHoleFinder(holeFinder);

        moduleManager.register(new AutoCrystal());
        moduleManager.register(new AutoTotem());
        moduleManager.register(holeFinder);
        moduleManager.register(autoPearl);
        moduleManager.register(new Surround());
        moduleManager.register(new AnchorAura());
        moduleManager.register(new SmartCrystalAnchor());
    }

    /**
     * Switch to a game mode - disables modules from other modes.
     */
    public void switchMode(GameMode mode) {
        moduleManager.activateGameMode(mode);
        activeMode = mode;
    }

    public GameMode getActiveMode() {
        return activeMode;
    }

    /**
     * Get a summary of all registered modules by game mode.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        for (GameMode mode : GameMode.values()) {
            sb.append(String.format("\n=== %s (%s) ===\n", mode.displayName, mode.description));
            moduleManager.getModulesForMode(mode).forEach(m ->
                    sb.append(String.format("  [%s] %s - %s\n",
                            m.isEnabled() ? "ON" : "OFF",
                            m.getName(),
                            m.getDescription())));
        }
        return sb.toString();
    }
}
