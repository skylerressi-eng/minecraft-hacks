package com.github.hackclient;

import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.antidetect.HumanizedTimer.SkillLevel;
import com.github.hackclient.antidetect.AntiCheatBypass;
import com.github.hackclient.antidetect.AntiCheatAnalyzer;
import com.github.hackclient.module.Module;
import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.ui.PhantomGui;
import com.github.hackclient.ui.HudRenderer;

import java.util.*;

/**
 * Standalone test runner - no JUnit needed.
 * Validates all anti-detection and module systems.
 */
public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Phantom Client Test Suite ===\n");

        // Anti-detection timing tests
        testDelaysWithinBounds();
        testMeanDelayMatchesSkillLevel();
        testSufficientVariance();
        testNoConsecutiveDuplicates();
        testDistributionApproxGaussian();
        testSessionReset();
        testSkillLevelsDifferent();
        testAntiPatternVariance();

        // Anti-cheat bypass tests
        testRotationNoise();
        testSmoothRotationConverges();
        testMousePath();
        testShouldActProbability();
        testPacketDelayVariance();

        // Module system tests
        testAllGameModesHaveModules();
        testModuleCount();
        testModuleToggle();
        testModuleLookup();
        testGameModeSwitchDisablesOthers();
        testAllModulesHaveTimers();

        // New feature tests
        testGuiToggle();
        testGuiPanels();
        testHudRenderer();
        testAntiCheatAnalyzer();
        testAntiCheatProfiles();
        testShieldRotationModule();
        testWindburstPearlMacro();
        testInstantPotModule();
        testSmartCrystalAnchor();
        testAutoBreachSwapToggle();

        System.out.println("\n=== RESULTS ===");
        System.out.printf("PASSED: %d / %d%n", passed, passed + failed);
        if (failed > 0) {
            System.out.printf("FAILED: %d%n", failed);
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED!");
        }
    }

    // === ANTI-DETECTION TIMER TESTS ===

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

    // === ANTI-CHEAT BYPASS TESTS ===

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
        boolean hasDeviation = false;
        for (float[] p : path) {
            if (Math.abs(p[0] - p[1]) > 3.0f) { hasDeviation = true; break; }
        }
        check("Mouse path: length=10, near target, curved",
              correctLen && nearTarget && hasDeviation);
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

    // === MODULE SYSTEM TESTS ===

    static void testAllGameModesHaveModules() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        boolean ok = true;
        for (GameMode mode : GameMode.values()) {
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
        // 5+3 mace + 7 legacy + 6 bedwars + 6+1 crystal = 28
        check("Total module count = 28: " + mm.getModuleCount(), mm.getModuleCount() == 28);
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

    // === NEW FEATURE TESTS ===

    static void testGuiToggle() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        PhantomGui gui = new PhantomGui(mm);

        check("GUI starts invisible", !gui.isVisible());
        gui.onKeyPress(344); // Right Shift
        check("GUI visible after Right Shift", gui.isVisible());
        gui.onKeyPress(344);
        check("GUI hidden after second Right Shift", !gui.isVisible());
    }

    static void testGuiPanels() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        PhantomGui gui = new PhantomGui(mm);

        check("GUI has 4 panels (one per game mode)", gui.getPanels().size() == 4);

        // Check panels have correct module counts
        int totalInPanels = 0;
        for (PhantomGui.CategoryPanel panel : gui.getPanels()) {
            totalInPanels += panel.modules.size();
        }
        check("GUI panels contain all modules: " + totalInPanels, totalInPanels == 28);
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

        // Test auto-detection by server IP
        analyzer.detectAntiCheat("mc.hypixel.net");
        check("Detects Watchdog on Hypixel",
              analyzer.getDetectedType() == AntiCheatAnalyzer.AntiCheatType.WATCHDOG);

        // Test brand detection
        analyzer.detectFromBrand("GrimAC v2.3.67");
        check("Detects Grim from brand",
              analyzer.getDetectedType() == AntiCheatAnalyzer.AntiCheatType.GRIM);

        // Test unknown defaults
        analyzer.detectAntiCheat("some.random.server");
        check("Unknown server gets safe defaults",
              analyzer.getDetectedType() == AntiCheatAnalyzer.AntiCheatType.UNKNOWN);
    }

    static void testAntiCheatProfiles() {
        // Test that all profiles have sane values
        boolean allValid = true;
        for (AntiCheatAnalyzer.AntiCheatType type : AntiCheatAnalyzer.AntiCheatType.values()) {
            AntiCheatAnalyzer.BypassProfile profile = AntiCheatAnalyzer.getProfile(type);
            if (profile == null) { allValid = false; continue; }
            if (profile.maxCps <= 0 || profile.maxCps > 25) allValid = false;
            if (profile.actionProbability <= 0 || profile.actionProbability > 1.0) allValid = false;
            if (profile.maxReachExtra < 0 || profile.maxReachExtra > 1.0) allValid = false;
        }
        check("All anti-cheat profiles have valid parameters", allValid);

        // Test that stricter ACs have lower action probabilities
        AntiCheatAnalyzer.BypassProfile polar = AntiCheatAnalyzer.getProfile(AntiCheatAnalyzer.AntiCheatType.POLAR);
        AntiCheatAnalyzer.BypassProfile ncp = AntiCheatAnalyzer.getProfile(AntiCheatAnalyzer.AntiCheatType.NOCHEATPLUS);
        check("Polar is stricter than NCP (lower action prob)",
              polar.actionProbability < ncp.actionProbability);
    }

    static void testShieldRotationModule() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        Module m = mm.getModule("ShieldRotation");
        check("ShieldRotation module exists", m != null);
        check("ShieldRotation is in MACE mode", m != null && m.getGameMode() == GameMode.MACE);
        if (m != null) {
            m.toggle();
            check("ShieldRotation can be toggled on", m.isEnabled());
            m.onTick(); // Should not crash
            m.toggle();
            check("ShieldRotation can be toggled off", !m.isEnabled());
        }
    }

    static void testWindburstPearlMacro() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        Module m = mm.getModule("WindburstPearlMacro");
        check("WindburstPearlMacro module exists", m != null);
        check("WindburstPearlMacro is in MACE mode", m != null && m.getGameMode() == GameMode.MACE);
    }

    static void testInstantPotModule() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        Module m = mm.getModule("InstantPot");
        check("InstantPot module exists", m != null);
        if (m != null) {
            m.toggle();
            m.onTick(); // Should not crash
            m.toggle();
        }
        check("InstantPot toggles correctly", m != null && !m.isEnabled());
    }

    static void testSmartCrystalAnchor() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        Module m = mm.getModule("SmartCrystalAnchor");
        check("SmartCrystalAnchor module exists", m != null);
        check("SmartCrystalAnchor is in CRYSTAL mode", m != null && m.getGameMode() == GameMode.CRYSTAL);
        if (m != null) {
            m.toggle();
            m.onTick(); // Should not crash
            m.toggle();
        }
        check("SmartCrystalAnchor toggles correctly", m != null && !m.isEnabled());
    }

    static void testAutoBreachSwapToggle() {
        ModuleManager mm = new ModuleManager();
        GameModeManager gmm = new GameModeManager(mm);
        gmm.registerAll();
        Module m = mm.getModule("AutoBreachSwap");
        check("AutoBreachSwap module exists", m != null);
        if (m != null) {
            m.toggle(); // enable module
            // Simulate V key press for toggle
            com.github.hackclient.module.legacy.AutoBreachSwap abs =
                    (com.github.hackclient.module.legacy.AutoBreachSwap) m;
            check("AutoBreachSwap starts with toggle off", !abs.isActiveToggle());
            abs.onKeyPress(86); // V key
            check("AutoBreachSwap toggle activates on V press", abs.isActiveToggle());
            abs.onKeyPress(86);
            check("AutoBreachSwap toggle deactivates on second V", !abs.isActiveToggle());
            m.toggle(); // disable
        }
    }

    // === HELPER ===

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
