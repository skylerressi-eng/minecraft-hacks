package com.github.hackclient;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.HumanizedTimer.SkillLevel;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.AntiCheatAnalyzer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.module.Module;
import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.ui.PhantomGui;
import com.github.hackclient.ui.HudRenderer;

import java.util.*;

public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Phantom Client v3.0.0 Test Suite ===\n");

        testDelaysWithinBounds();
        testMeanDelayMatchesSkillLevel();
        testSufficientVariance();
        testNoConsecutiveDuplicates();
        testDistributionApproxGaussian();
        testSessionReset();
        testSkillLevelsDifferent();
        testAntiPatternVariance();

        testRotationNoise();
        testSmoothRotationConverges();
        testMousePath();
        testShouldActProbability();
        testPacketDelayVariance();

        testAllGameModesHaveModules();
        testModuleCount();
        testModuleToggle();
        testModuleLookup();
        testGameModeSwitchDisablesOthers();
        testAllModulesHaveTimers();

        testGuiToggle();
        testGuiPanels();
        testHudRenderer();
        testAntiCheatAnalyzer();
        testAntiCheatProfiles();
        testStealthEngine();

        testMeteorModules();
        testVapeModules();
        testCrystalModules();
        testMaceModules();
        testLegacyModules();
        testBedwarsModules();
        testAllModulesTickSafely();

        System.out.println("\n=== RESULTS ===");
        System.out.printf("PASSED: %d / %d%n", passed, passed + failed);
        if (failed > 0) {
            System.out.printf("FAILED: %d%n", failed);
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED!");
        }
    }

    static void testDelaysWithinBounds() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED, 50, 500);
        boolean ok = true;
        for (int i = 0; i < 1000; i++) {
            long d = timer.getNextDelayMs();
            if (d < 50 || d > 500) { ok = false; break; }
        }
        check("Delays within bounds [50-500ms]", ok);
    }

    static void testMeanDelayMatchesSkillLevel() {
        boolean allOk = true;
        for (SkillLevel level : SkillLevel.values()) {
            HumanizedTimer timer = new HumanizedTimer(level, 30, 800);
            double sum = 0;
            int n = 5000;
            for (int i = 0; i < n; i++) sum += timer.getNextDelayMs();
            double mean = sum / n;
            double tolerance = level.meanMs * 0.4;
            if (Math.abs(mean - level.meanMs) >= tolerance) {
                System.out.printf("  WARN: %s mean=%.1f expected=%.1f%n", level, mean, level.meanMs);
                allOk = false;
            }
        }
        check("Mean delay matches skill level (within 40%)", allOk);
    }

    static void testSufficientVariance() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED, 50, 500);
        double[] delays = new double[1000];
        double sum = 0;
        for (int i = 0; i < delays.length; i++) {
            delays[i] = timer.getNextDelayMs();
            sum += delays[i];
        }
        double mean = sum / delays.length;
        double varSum = 0;
        for (double d : delays) varSum += (d - mean) * (d - mean);
        double stdDev = Math.sqrt(varSum / delays.length);
        check("Sufficient variance (stddev > 10ms): " + String.format("%.1f", stdDev), stdDev > 10);
    }

    static void testNoConsecutiveDuplicates() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED);
        long prev = -1;
        int dupes = 0;
        for (int i = 0; i < 2000; i++) {
            long d = timer.getNextDelayMs();
            if (d == prev) dupes++;
            prev = d;
        }
        check("No consecutive duplicates (< 20 out of 2000): " + dupes, dupes < 20);
    }

    static void testDistributionApproxGaussian() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.AVERAGE, 50, 600);
        int n = 3000;
        double[] delays = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            delays[i] = timer.getNextDelayMs();
            sum += delays[i];
        }
        double mean = sum / n;
        double varSum = 0;
        for (double d : delays) varSum += (d - mean) * (d - mean);
        double stdDev = Math.sqrt(varSum / n);
        int within1 = 0, within2 = 0;
        for (double d : delays) {
            if (Math.abs(d - mean) <= stdDev) within1++;
            if (Math.abs(d - mean) <= 2 * stdDev) within2++;
        }
        double p1 = (double) within1 / n;
        double p2 = (double) within2 / n;
        check("Gaussian-like: 1SD=" + String.format("%.0f%%", p1 * 100) +
              " 2SD=" + String.format("%.0f%%", p2 * 100),
              p1 > 0.45 && p1 < 0.85 && p2 > 0.80);
    }

    static void testSessionReset() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED);
        for (int i = 0; i < 100; i++) timer.getNextDelayMs();
        timer.resetSession();
        check("Session reset clears action count", timer.getActionCount() == 0);
    }

    static void testSkillLevelsDifferent() {
        Map<SkillLevel, Double> means = new EnumMap<>(SkillLevel.class);
        for (SkillLevel level : SkillLevel.values()) {
            HumanizedTimer timer = new HumanizedTimer(level, 30, 800);
            double sum = 0;
            for (int i = 0; i < 2000; i++) sum += timer.getNextDelayMs();
            means.put(level, sum / 2000);
        }
        boolean ok = means.get(SkillLevel.BEGINNER) > means.get(SkillLevel.AVERAGE)
                  && means.get(SkillLevel.AVERAGE) > means.get(SkillLevel.SKILLED)
                  && means.get(SkillLevel.SKILLED) > means.get(SkillLevel.EXPERT);
        check("Skill levels are ordered (BEGINNER > AVG > SKILLED > EXPERT)", ok);
    }

    static void testAntiPatternVariance() {
        HumanizedTimer timer = new HumanizedTimer(SkillLevel.SKILLED);
        long prev = timer.getNextDelayMs();
        int tooClose = 0;
        for (int i = 0; i < 500; i++) {
            long cur = timer.getNextDelayMs();
            if (Math.abs(cur - prev) < 3) tooClose++;
            prev = cur;
        }
        check("Anti-pattern variance (< 25 close pairs): " + tooClose, tooClose < 25);
    }

    static void testRotationNoise() {
        boolean ok = true;
        for (int i = 0; i < 1000; i++) {
            float n = AntiCheatBypass.addRotationNoise(90.0f);
            if (Math.abs(n - 90.0f) >= 5.0f) { ok = false; break; }
        }
        check("Rotation noise is small (< 5 degrees)", ok);
    }

    static void testSmoothRotationConverges() {
        float cur = 0;
        for (int i = 0; i < 20; i++)
            cur = AntiCheatBypass.smoothRotation(cur, 90, 0.5f);
        check("Smooth rotation converges to target", Math.abs(cur - 90) < 5.0f);
    }

    static void testMousePath() {
        float[][] path = AntiCheatBypass.generateMousePath(0, 0, 100, 100, 10);
        boolean correctLen = path.length == 10;
        boolean nearTarget = Math.abs(path[9][0] - 100) < 5 && Math.abs(path[9][1] - 100) < 5;
        check("Mouse path: length=10, near target", correctLen && nearTarget);
    }

    static void testShouldActProbability() {
        int acted = 0;
        for (int i = 0; i < 10000; i++)
            if (AntiCheatBypass.shouldActThisTick(0.5)) acted++;
        double ratio = (double) acted / 10000;
        check("shouldAct(0.5) probability: " + String.format("%.2f", ratio),
              ratio > 0.45 && ratio < 0.55);
    }

    static void testPacketDelayVariance() {
        Set<Long> unique = new HashSet<>();
        for (int i = 0; i < 100; i++)
            unique.add(AntiCheatBypass.getPacketDelay(100));
        check("Packet delay variance (> 20 unique): " + unique.size(), unique.size() > 20);
    }

    static void testAllGameModesHaveModules() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        boolean ok = true;
        for (GameMode mode : GameMode.values()) {
            if (mode == GameMode.ALL_HACKS) continue;
            if (mm.getModulesForMode(mode).isEmpty()) {
                System.out.printf("  WARN: %s has no modules%n", mode);
                ok = false;
            }
        }
        check("All game modes have modules", ok);
    }

    static void testModuleCount() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        int count = mm.getModuleCount();
        check("Total module count = 58: " + count, count == 58);
    }

    static void testModuleToggle() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        Module m = mm.getModule("AutoClicker");
        boolean before = !m.isEnabled();
        m.toggle();
        boolean after = m.isEnabled();
        m.toggle();
        boolean final_ = !m.isEnabled();
        check("Module toggle on/off works", before && after && final_);
    }

    static void testModuleLookup() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        check("Module lookup by name", mm.getModule("AutoCrystal") != null
              && mm.getModule("autoclicker") != null
              && mm.getModule("NonExistent") == null);
    }

    static void testGameModeSwitchDisablesOthers() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        Module ac = mm.getModule("AutoClicker");
        Module cr = mm.getModule("AutoCrystal");
        ac.onEnable();
        cr.onEnable();
        gmm.switchMode(GameMode.CRYSTAL);
        mm.activateGameMode(GameMode.CRYSTAL);
        check("Game mode switch disables other modes",
              !ac.isEnabled() && cr.isEnabled());
    }

    static void testAllModulesHaveTimers() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        boolean ok = true;
        for (Module m : mm.getAllModules()) {
            if (m.getTimer() == null) { ok = false; break; }
        }
        check("All modules have humanized timers", ok);
    }

    static void testGuiToggle() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        PhantomGui gui = new PhantomGui(mm);
        check("GUI starts invisible", !gui.isVisible());
        gui.onKeyPress(344);
        check("GUI visible after Right Shift", gui.isVisible());
        gui.onKeyPress(344);
        check("GUI hidden after second Right Shift", !gui.isVisible());
    }

    static void testGuiPanels() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        PhantomGui gui = new PhantomGui(mm);
        int panelCount = gui.getPanels().size();
        check("GUI has 7 panels (one per game mode): " + panelCount, panelCount == 7);
        int totalInPanels = 0;
        for (PhantomGui.CategoryPanel panel : gui.getPanels()) {
            totalInPanels += panel.modules.size();
        }
        // ALL_HACKS panel shows all 58, plus each mode panel shows its own modules = 58 + 58 = 116
        check("GUI panels contain modules (All Hacks + per-mode): " + totalInPanels, totalInPanels == 116);
    }

    static void testHudRenderer() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        HudRenderer hud = new HudRenderer(mm);
        HudRenderer.HudData data = hud.getHudData(1920, 1080, "Crystal PvP", 60, 30, 100.5, 64.0, -200.3);
        check("HUD has watermark", data.watermark != null && data.watermark.contains("Phantom"));
        check("HUD has coords", data.coordsText != null && data.coordsText.contains("100.5"));
        check("HUD has game mode", data.gameModeText != null && data.gameModeText.contains("Crystal"));
    }

    static void testAntiCheatAnalyzer() {
        AntiCheatAnalyzer analyzer = new AntiCheatAnalyzer();
        analyzer.detectAntiCheat("mc.hypixel.net");
        check("Detects Watchdog on Hypixel",
              analyzer.getDetectedType() == AntiCheatAnalyzer.AntiCheatType.WATCHDOG);
        analyzer.detectFromBrand("GrimAC v2.3.67");
        check("Detects Grim from brand",
              analyzer.getDetectedType() == AntiCheatAnalyzer.AntiCheatType.GRIM);
        analyzer.detectAntiCheat("some.random.server");
        check("Unknown server gets safe defaults",
              analyzer.getDetectedType() == AntiCheatAnalyzer.AntiCheatType.UNKNOWN);
    }

    static void testAntiCheatProfiles() {
        boolean allValid = true;
        for (AntiCheatAnalyzer.AntiCheatType type : AntiCheatAnalyzer.AntiCheatType.values()) {
            AntiCheatAnalyzer.BypassProfile profile = AntiCheatAnalyzer.getProfile(type);
            if (profile == null) { allValid = false; continue; }
            if (profile.maxCps <= 0 || profile.maxCps > 25) allValid = false;
            if (profile.actionProbability <= 0 || profile.actionProbability > 1.0) allValid = false;
            if (profile.maxReachExtra < 0 || profile.maxReachExtra > 1.0) allValid = false;
        }
        check("All anti-cheat profiles have valid parameters", allValid);
        AntiCheatAnalyzer.BypassProfile polar = AntiCheatAnalyzer.getProfile(AntiCheatAnalyzer.AntiCheatType.POLAR);
        AntiCheatAnalyzer.BypassProfile ncp = AntiCheatAnalyzer.getProfile(AntiCheatAnalyzer.AntiCheatType.NOCHEATPLUS);
        check("Polar is stricter than NCP", polar.actionProbability < ncp.actionProbability);
    }

    static void testStealthEngine() {
        AntiCheatAnalyzer analyzer = new AntiCheatAnalyzer();
        StealthEngine engine = new StealthEngine(analyzer);
        check("StealthEngine starts with 0 suspicion", engine.getSuspicionScore() == 0);
        check("StealthEngine not in panic mode", !engine.isPanicMode());
        engine.addSuspicion(50);
        check("Suspicion can be added", engine.getSuspicionScore() == 50);
        engine.triggerPanic();
        check("Panic mode can be triggered", engine.isPanicMode());
        engine.setStaffNearby(true);
        check("Staff detection works", engine.isStaffNearby());
    }

    static void testMeteorModules() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        String[] names = {"Speed", "Fly", "NoFall", "ESP", "Xray", "Nuker", "Jesus",
            "Scaffold", "Step", "FullBright", "AntiHunger", "FastBreak", "AutoArmor",
            "AutoEat", "Criticals", "NoSlow", "Sprint", "Tracers", "StorageESP", "FreeCam"};
        boolean ok = true;
        for (String n : names) {
            Module m = mm.getModule(n);
            if (m == null) { System.out.println("  MISSING: " + n); ok = false; }
            else if (m.getGameMode() != GameMode.METEOR) { System.out.println("  WRONG MODE: " + n); ok = false; }
        }
        check("All 20 Meteor modules exist with correct mode", ok);
    }

    static void testVapeModules() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        String[] names = {"AimAssist", "ClickAssist", "AutoBlock", "BackTrack",
            "HitSelect", "TimerHack", "Blink", "AntiBot", "Chams", "NameTags"};
        boolean ok = true;
        for (String n : names) {
            Module m = mm.getModule(n);
            if (m == null) { System.out.println("  MISSING: " + n); ok = false; }
            else if (m.getGameMode() != GameMode.VAPE) { System.out.println("  WRONG MODE: " + n); ok = false; }
        }
        check("All 10 Vape modules exist with correct mode", ok);
    }

    static void testCrystalModules() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        String[] names = {"AutoCrystal", "AutoTotem", "HoleFinder",
            "AutoPearlHole", "Surround", "AnchorAura", "SmartCrystalAnchor"};
        boolean ok = true;
        for (String n : names) {
            Module m = mm.getModule(n);
            if (m == null) { System.out.println("  MISSING: " + n); ok = false; }
            else if (m.getGameMode() != GameMode.CRYSTAL) { System.out.println("  WRONG MODE: " + n); ok = false; }
        }
        check("All 7 Crystal modules exist with correct mode", ok);
    }

    static void testMaceModules() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        String[] names = {"AutoWindBurst", "AutoPearl", "AutoStuntSlam",
            "MaceSwapCombo", "AutoShieldSwap", "ShieldRotation", "WindburstPearlMacro"};
        boolean ok = true;
        for (String n : names) {
            Module m = mm.getModule(n);
            if (m == null) { System.out.println("  MISSING: " + n); ok = false; }
            else if (m.getGameMode() != GameMode.MACE) { System.out.println("  WRONG MODE: " + n); ok = false; }
        }
        check("All 7 Mace modules exist with correct mode", ok);
    }

    static void testLegacyModules() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        String[] names = {"AutoClicker", "Reach", "Velocity", "KillAura",
            "WTap", "AutoBreachSwap", "SpearCombo"};
        boolean ok = true;
        for (String n : names) {
            Module m = mm.getModule(n);
            if (m == null) { System.out.println("  MISSING: " + n); ok = false; }
            else if (m.getGameMode() != GameMode.LEGACY_1_8) { System.out.println("  WRONG MODE: " + n); ok = false; }
        }
        check("All 7 Legacy modules exist with correct mode", ok);
    }

    static void testBedwarsModules() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        String[] names = {"AutoBridge", "FireballDeflect", "BedAura",
            "PearlClutch", "AutoShop", "InvisDetector"};
        boolean ok = true;
        for (String n : names) {
            Module m = mm.getModule(n);
            if (m == null) { System.out.println("  MISSING: " + n); ok = false; }
            else if (m.getGameMode() != GameMode.BEDWARS) { System.out.println("  WRONG MODE: " + n); ok = false; }
        }
        check("All 6 Bedwars modules exist with correct mode", ok);
    }

    static void testAllModulesTickSafely() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        boolean ok = true;
        for (Module m : mm.getAllModules()) {
            try {
                m.onEnable();
                m.onTick();
                m.onTick();
                m.onDisable();
            } catch (Exception e) {
                System.out.println("  CRASH: " + m.getName() + " - " + e.getMessage());
                ok = false;
            }
        }
        check("All 58 modules tick without crashing", ok);
    }

    static void check(String name, boolean result) {
        if (result) {
            passed++;
            System.out.println("  PASS: " + name);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
        }
    }
}
