package com.github.hackclient.gamemode;

import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.module.mace.*;
import com.github.hackclient.module.legacy.*;
import com.github.hackclient.module.bedwars.*;
import com.github.hackclient.module.crystal.*;
import com.github.hackclient.module.combat.InstantPot;
import com.github.hackclient.module.meteor.*;
import com.github.hackclient.module.vape.*;

public class GameModeManager {
    private final ModuleManager moduleManager;
    private GameMode activeMode = null;

    public GameModeManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void registerAll() {
        registerMaceModules();
        registerLegacy18Modules();
        registerBedwarsModules();
        registerCrystalModules();
        registerMeteorModules();
        registerVapeModules();
    }

    private void registerMaceModules() {
        moduleManager.register(new AutoWindBurst());
        moduleManager.register(new AutoPearl());
        moduleManager.register(new AutoStuntSlam());
        moduleManager.register(new MaceSwapCombo());
        moduleManager.register(new AutoShieldSwap());
        moduleManager.register(new ShieldRotation());
        moduleManager.register(new WindburstPearlMacro());
        moduleManager.register(new InstantPot());
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

    private void registerMeteorModules() {
        moduleManager.register(new Speed());
        moduleManager.register(new Fly());
        moduleManager.register(new NoFall());
        moduleManager.register(new ESP());
        moduleManager.register(new Xray());
        moduleManager.register(new Nuker());
        moduleManager.register(new Jesus());
        moduleManager.register(new Scaffold());
        moduleManager.register(new Step());
        moduleManager.register(new FullBright());
        moduleManager.register(new AntiHunger());
        moduleManager.register(new FastBreak());
        moduleManager.register(new AutoArmor());
        moduleManager.register(new AutoEat());
        moduleManager.register(new Criticals());
        moduleManager.register(new NoSlow());
        moduleManager.register(new Sprint());
        moduleManager.register(new Tracers());
        moduleManager.register(new StorageESP());
        moduleManager.register(new FreeCam());
    }

    private void registerVapeModules() {
        moduleManager.register(new AimAssist());
        moduleManager.register(new ClickAssist());
        moduleManager.register(new AutoBlock());
        moduleManager.register(new BackTrack());
        moduleManager.register(new HitSelect());
        moduleManager.register(new TimerHack());
        moduleManager.register(new Blink());
        moduleManager.register(new AntiBot());
        moduleManager.register(new Chams());
        moduleManager.register(new NameTags());
    }

    public void switchMode(GameMode mode) {
        moduleManager.activateGameMode(mode);
        activeMode = mode;
    }

    public GameMode getActiveMode() {
        return activeMode;
    }

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
