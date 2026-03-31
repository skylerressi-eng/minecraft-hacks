package com.github.hackclient.antidetect;

import java.util.EnumMap;
import java.util.Map;

/**
 * Anti-Cheat Analyzer - Profiles major server anti-cheat systems and applies
 * server-specific bypass configurations.
 *
 * Analyzes known anti-cheat plugins and their detection methods to find
 * loopholes and configure the client's behavior accordingly.
 *
 * Supported anti-cheats:
 * - Watchdog (Hypixel) - Pattern analysis, statistical CPS detection
 * - Grim (GrimAC) - Prediction-based movement checks
 * - Vulcan - Packet timing and movement consistency checks
 * - Matrix - Combat and movement pattern analysis
 * - Spartan - Configurable checks with common bypasses
 * - NoCheatPlus (NCP) - Classic anti-cheat with known tolerances
 * - Polar - Newer AC with ML-based detection
 * - Intave - Advanced statistical analysis
 */
public class AntiCheatAnalyzer {

    public enum AntiCheatType {
        WATCHDOG,       // Hypixel's custom AC
        GRIM,           // GrimAC - prediction based
        VULCAN,         // Vulcan AC
        MATRIX,         // Matrix AC
        SPARTAN,        // Spartan AC
        NOCHEATPLUS,    // NCP
        POLAR,          // Polar AC
        INTAVE,         // Intave AC
        UNKNOWN         // Generic safe profile
    }

    /**
     * Server-specific bypass profile with tuned parameters.
     */
    public static class BypassProfile {
        public final AntiCheatType type;
        public final String name;

        // Timing constraints
        public final double maxCps;               // Max safe CPS before flag
        public final double minClickInterval;     // Min ms between clicks
        public final long minActionIntervalMs;    // Min ms between any actions

        // Movement constraints
        public final double maxReachExtra;        // Max extra reach blocks
        public final double maxRotationSpeed;     // Max degrees per tick
        public final float rotationNoiseScale;    // Scale factor for rotation noise

        // Velocity/KB constraints
        public final double minHorizontalKb;      // Min horizontal KB ratio to accept
        public final double minVerticalKb;        // Min vertical KB ratio to accept
        public final int fullKbFrequency;         // Take full KB every N hits

        // Packet timing
        public final long packetDelayBase;        // Base packet delay ms
        public final double packetVariance;       // Variance multiplier

        // Probability of acting (lower = safer but slower)
        public final double actionProbability;

        public BypassProfile(AntiCheatType type, String name, double maxCps,
                             double minClickInterval, long minActionIntervalMs,
                             double maxReachExtra, double maxRotationSpeed,
                             float rotationNoiseScale, double minHorizontalKb,
                             double minVerticalKb, int fullKbFrequency,
                             long packetDelayBase, double packetVariance,
                             double actionProbability) {
            this.type = type;
            this.name = name;
            this.maxCps = maxCps;
            this.minClickInterval = minClickInterval;
            this.minActionIntervalMs = minActionIntervalMs;
            this.maxReachExtra = maxReachExtra;
            this.maxRotationSpeed = maxRotationSpeed;
            this.rotationNoiseScale = rotationNoiseScale;
            this.minHorizontalKb = minHorizontalKb;
            this.minVerticalKb = minVerticalKb;
            this.fullKbFrequency = fullKbFrequency;
            this.packetDelayBase = packetDelayBase;
            this.packetVariance = packetVariance;
            this.actionProbability = actionProbability;
        }
    }

    private static final Map<AntiCheatType, BypassProfile> PROFILES = new EnumMap<>(AntiCheatType.class);

    static {
        // === WATCHDOG (Hypixel) ===
        // Detection: Statistical CPS analysis, pattern matching on click intervals,
        //            movement prediction, combat consistency scoring
        // Loopholes: Allows up to ~15 CPS with sufficient variance, doesn't flag
        //            Gaussian-distributed clicks, tolerates 3.1 reach, rotation
        //            checks have 2-degree tolerance
        PROFILES.put(AntiCheatType.WATCHDOG, new BypassProfile(
                AntiCheatType.WATCHDOG, "Hypixel Watchdog",
                15.0,       // maxCps - watchdog flags consistent 16+ CPS
                62.0,       // minClickInterval - 1000/16 ≈ 62ms
                45,         // minActionIntervalMs
                0.1,        // maxReachExtra - very strict on reach (3.1 max)
                15.0,       // maxRotationSpeed - flags >15 deg/tick snap rotations
                1.0f,       // rotationNoiseScale - standard noise
                0.75,       // minHorizontalKb - flags <70% reduction
                0.80,       // minVerticalKb - strict vertical KB check
                8,          // fullKbFrequency - take full KB every 8 hits
                50,         // packetDelayBase
                0.2,        // packetVariance - ±20%
                0.88        // actionProbability
        ));

        // === GRIM (GrimAC) ===
        // Detection: Prediction-based - simulates client physics server-side,
        //            compares predicted vs actual position. Very strict on movement.
        // Loopholes: Focuses on movement prediction, less aggressive on combat timing.
        //            Rotation checks have small tolerance for natural aim noise.
        //            Packet order matters more than timing.
        PROFILES.put(AntiCheatType.GRIM, new BypassProfile(
                AntiCheatType.GRIM, "GrimAC",
                14.0,       // maxCps
                70.0,       // minClickInterval - be more conservative
                50,         // minActionIntervalMs
                0.05,       // maxReachExtra - extremely strict, basically vanilla only
                12.0,       // maxRotationSpeed - strict rotation speed limit
                0.8f,       // rotationNoiseScale - less noise (Grim flags erratic aim)
                0.80,       // minHorizontalKb - Grim checks KB very precisely
                0.85,       // minVerticalKb
                6,          // fullKbFrequency
                55,         // packetDelayBase
                0.15,       // packetVariance - less variance (Grim checks consistency)
                0.82        // actionProbability - act less often, be safe
        ));

        // === VULCAN ===
        // Detection: Packet timing consistency, movement smoothness,
        //            hit consistency analysis, auto-clicker pattern detection
        // Loopholes: Vulcan has tick-based checks - acting once per tick is safe.
        //            Combat checks focus on hit consistency over time, not individual hits.
        //            Rotation checks are less strict than Grim.
        PROFILES.put(AntiCheatType.VULCAN, new BypassProfile(
                AntiCheatType.VULCAN, "Vulcan AC",
                16.0,       // maxCps - Vulcan is more lenient on CPS
                60.0,       // minClickInterval
                48,         // minActionIntervalMs
                0.15,       // maxReachExtra - moderate reach tolerance
                18.0,       // maxRotationSpeed - more lenient rotations
                1.2f,       // rotationNoiseScale - can have more noise
                0.70,       // minHorizontalKb
                0.75,       // minVerticalKb
                10,         // fullKbFrequency - less frequent full KB needed
                45,         // packetDelayBase
                0.25,       // packetVariance
                0.90        // actionProbability - can act more often
        ));

        // === MATRIX ===
        // Detection: Combat pattern analysis with scoring, checks aim consistency
        //            and click pattern entropy
        // Loopholes: Score-based system means occasional flags are fine as long as
        //            score stays below threshold. Intermittent clean behavior resets score.
        PROFILES.put(AntiCheatType.MATRIX, new BypassProfile(
                AntiCheatType.MATRIX, "Matrix AC",
                15.0,       // maxCps
                65.0,       // minClickInterval
                48,         // minActionIntervalMs
                0.2,        // maxReachExtra - moderate
                16.0,       // maxRotationSpeed
                1.0f,       // rotationNoiseScale
                0.65,       // minHorizontalKb - more lenient
                0.70,       // minVerticalKb
                12,         // fullKbFrequency
                50,         // packetDelayBase
                0.2,        // packetVariance
                0.85        // actionProbability
        ));

        // === SPARTAN ===
        // Detection: Configurable check system, depends on server config.
        //            Default configs have known tolerances.
        // Loopholes: Many servers run with default config which is lenient.
        //            Check thresholds are configurable so we use conservative defaults.
        PROFILES.put(AntiCheatType.SPARTAN, new BypassProfile(
                AntiCheatType.SPARTAN, "Spartan AC",
                16.0,       // maxCps
                58.0,       // minClickInterval
                45,         // minActionIntervalMs
                0.25,       // maxReachExtra - quite lenient default
                20.0,       // maxRotationSpeed - lenient
                1.3f,       // rotationNoiseScale
                0.60,       // minHorizontalKb
                0.65,       // minVerticalKb
                15,         // fullKbFrequency
                45,         // packetDelayBase
                0.25,       // packetVariance
                0.92        // actionProbability
        ));

        // === NOCHEATPLUS (NCP) ===
        // Detection: Classic checks - fight speed, reach, direction, angle.
        //            Well-documented tolerance values.
        // Loopholes: Vanilla-like behavior passes all checks. Has known reach
        //            tolerance of ~3.1 blocks. Direction check has ±10 degree cone.
        PROFILES.put(AntiCheatType.NOCHEATPLUS, new BypassProfile(
                AntiCheatType.NOCHEATPLUS, "NoCheatPlus",
                18.0,       // maxCps - NCP is lenient on CPS
                50.0,       // minClickInterval
                40,         // minActionIntervalMs
                0.1,        // maxReachExtra - 3.1 total reach
                25.0,       // maxRotationSpeed - very lenient
                1.5f,       // rotationNoiseScale
                0.60,       // minHorizontalKb
                0.60,       // minVerticalKb
                20,         // fullKbFrequency
                40,         // packetDelayBase
                0.3,        // packetVariance
                0.95        // actionProbability
        ));

        // === POLAR ===
        // Detection: Machine learning-based pattern analysis, very hard to bypass
        //            consistently. Analyzes behavioral patterns over time.
        // Loopholes: ML models struggle with truly human-like variance.
        //            Key is high entropy in timing and small modifications only.
        PROFILES.put(AntiCheatType.POLAR, new BypassProfile(
                AntiCheatType.POLAR, "Polar AC",
                13.0,       // maxCps - be very conservative
                75.0,       // minClickInterval
                55,         // minActionIntervalMs
                0.03,       // maxReachExtra - basically vanilla
                10.0,       // maxRotationSpeed - slow, human-like
                0.6f,       // rotationNoiseScale - minimal noise (ML flags noise patterns)
                0.85,       // minHorizontalKb - take most KB
                0.90,       // minVerticalKb
                5,          // fullKbFrequency - take full KB often
                60,         // packetDelayBase
                0.12,       // packetVariance - consistent timing
                0.75        // actionProbability - act less, be safe
        ));

        // === INTAVE ===
        // Detection: Statistical analysis on timing distributions,
        //            checks for inhuman consistency in combat metrics
        // Loopholes: High variance in timing is actually expected by Intave.
        //            Consistent low-variance timing gets flagged faster than
        //            high-variance. Our Gaussian distribution naturally bypasses.
        PROFILES.put(AntiCheatType.INTAVE, new BypassProfile(
                AntiCheatType.INTAVE, "Intave AC",
                14.0,       // maxCps
                68.0,       // minClickInterval
                50,         // minActionIntervalMs
                0.08,       // maxReachExtra
                14.0,       // maxRotationSpeed
                0.9f,       // rotationNoiseScale
                0.78,       // minHorizontalKb
                0.82,       // minVerticalKb
                7,          // fullKbFrequency
                52,         // packetDelayBase
                0.18,       // packetVariance
                0.84        // actionProbability
        ));

        // === UNKNOWN (Safe defaults) ===
        // Most conservative settings - should pass any anti-cheat
        PROFILES.put(AntiCheatType.UNKNOWN, new BypassProfile(
                AntiCheatType.UNKNOWN, "Unknown / Safe Default",
                12.0,       // maxCps
                80.0,       // minClickInterval
                60,         // minActionIntervalMs
                0.0,        // maxReachExtra - vanilla reach only
                8.0,        // maxRotationSpeed
                0.5f,       // rotationNoiseScale
                0.90,       // minHorizontalKb
                0.95,       // minVerticalKb
                3,          // fullKbFrequency
                65,         // packetDelayBase
                0.1,        // packetVariance
                0.70        // actionProbability
        ));
    }

    private AntiCheatType detectedType = AntiCheatType.UNKNOWN;
    private BypassProfile activeProfile;

    public AntiCheatAnalyzer() {
        this.activeProfile = PROFILES.get(AntiCheatType.UNKNOWN);
    }

    /**
     * Auto-detect the server's anti-cheat based on plugin messages and behavior.
     * Call this after joining a server.
     */
    public AntiCheatType detectAntiCheat(String serverIp) {
        // Known server mappings
        if (serverIp != null) {
            String lower = serverIp.toLowerCase();
            if (lower.contains("hypixel")) return setDetected(AntiCheatType.WATCHDOG);
            if (lower.contains("minemen") || lower.contains("pvpland"))
                return setDetected(AntiCheatType.GRIM);
            if (lower.contains("lunar")) return setDetected(AntiCheatType.GRIM);
        }

        // TODO: Runtime detection via:
        // 1. Plugin channel messages (MC|Brand, etc.)
        // 2. Server response patterns to edge-case movements
        // 3. Known plugin list from /plugins (if accessible)
        return setDetected(AntiCheatType.UNKNOWN);
    }

    /**
     * Detect anti-cheat from plugin brand/channel data.
     */
    public AntiCheatType detectFromBrand(String brand) {
        if (brand == null) return AntiCheatType.UNKNOWN;
        String lower = brand.toLowerCase();

        if (lower.contains("grim")) return setDetected(AntiCheatType.GRIM);
        if (lower.contains("vulcan")) return setDetected(AntiCheatType.VULCAN);
        if (lower.contains("matrix")) return setDetected(AntiCheatType.MATRIX);
        if (lower.contains("spartan")) return setDetected(AntiCheatType.SPARTAN);
        if (lower.contains("nocheatplus") || lower.contains("ncp"))
            return setDetected(AntiCheatType.NOCHEATPLUS);
        if (lower.contains("polar")) return setDetected(AntiCheatType.POLAR);
        if (lower.contains("intave")) return setDetected(AntiCheatType.INTAVE);

        return setDetected(AntiCheatType.UNKNOWN);
    }

    private AntiCheatType setDetected(AntiCheatType type) {
        this.detectedType = type;
        this.activeProfile = PROFILES.get(type);
        return type;
    }

    /**
     * Manually set the anti-cheat profile.
     */
    public void setAntiCheatType(AntiCheatType type) {
        setDetected(type);
    }

    public AntiCheatType getDetectedType() { return detectedType; }
    public BypassProfile getActiveProfile() { return activeProfile; }

    /**
     * Get a profile for a specific anti-cheat type.
     */
    public static BypassProfile getProfile(AntiCheatType type) {
        return PROFILES.get(type);
    }

    /**
     * Get a summary of the active profile's constraints.
     */
    public String getProfileSummary() {
        return String.format("[%s] maxCPS=%.0f reach=+%.2f rotSpeed=%.0f KB=%.0f%%H/%.0f%%V prob=%.0f%%",
                activeProfile.name, activeProfile.maxCps, activeProfile.maxReachExtra,
                activeProfile.maxRotationSpeed, activeProfile.minHorizontalKb * 100,
                activeProfile.minVerticalKb * 100, activeProfile.actionProbability * 100);
    }
}
