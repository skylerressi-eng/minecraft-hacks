package com.github.hackclient.ui;

import com.github.hackclient.module.Module;
import com.github.hackclient.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Meteor Client-style HUD overlay renderer.
 *
 * Displays:
 * - Active module list (top-right, sorted by name length like Meteor)
 * - Watermark with client name/version (top-left)
 * - Active game mode indicator
 * - FPS/ping/TPS info bar (bottom-left)
 * - Coordinates display (bottom-left)
 */
public class HudRenderer {

    private final ModuleManager moduleManager;
    private boolean showWatermark = true;
    private boolean showActiveModules = true;
    private boolean showCoords = true;
    private boolean showInfoBar = true;

    // Meteor-style colors
    private static final int WATERMARK_COLOR = 0xFF00D4AA;
    private static final int MODULE_COLOR = 0xFFE0E0E0;
    private static final int SHADOW_COLOR = 0x80000000;

    public HudRenderer(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    /**
     * Get HUD render data. The actual drawing is done by the Fabric mixin renderer.
     */
    public HudData getHudData(int screenWidth, int screenHeight,
                               String gameModeName, int fps, int ping, double x, double y, double z) {
        HudData data = new HudData();

        // Watermark (top-left, Meteor style)
        if (showWatermark) {
            data.watermark = "Phantom Client v3.0.0";
            data.watermarkX = 4;
            data.watermarkY = 4;
            data.watermarkColor = WATERMARK_COLOR;
        }

        // Active modules list (top-right, sorted by string length descending - Meteor style)
        if (showActiveModules) {
            List<Module> active = new ArrayList<>();
            for (Module m : moduleManager.getAllModules()) {
                if (m.isEnabled()) active.add(m);
            }
            // Sort by name length descending (Meteor style)
            active.sort((a, b) -> b.getName().length() - a.getName().length());

            data.activeModules = new ArrayList<>();
            int moduleY = 2;
            for (Module m : active) {
                HudEntry entry = new HudEntry();
                entry.text = m.getName();
                entry.x = screenWidth - getTextWidth(m.getName()) - 4;
                entry.y = moduleY;
                entry.color = MODULE_COLOR;
                entry.bgColor = SHADOW_COLOR;
                data.activeModules.add(entry);
                moduleY += 12;
            }
        }

        // Game mode indicator (below watermark)
        if (gameModeName != null) {
            data.gameModeText = "[" + gameModeName + "]";
            data.gameModeX = 4;
            data.gameModeY = 16;
            data.gameModeColor = 0xFF909090;
        }

        // Info bar (bottom-left)
        if (showInfoBar) {
            data.infoText = String.format("FPS: %d | Ping: %dms", fps, ping);
            data.infoX = 4;
            data.infoY = screenHeight - 22;
            data.infoColor = 0xFFB0B0B0;
        }

        // Coordinates (bottom-left, above info bar)
        if (showCoords) {
            data.coordsText = String.format("XYZ: %.1f / %.1f / %.1f", x, y, z);
            data.coordsX = 4;
            data.coordsY = screenHeight - 34;
            data.coordsColor = 0xFFD0D0D0;
        }

        return data;
    }

    private int getTextWidth(String text) {
        return text.length() * 6; // Approximate MC font width
    }

    public ModuleManager getModuleManager() { return moduleManager; }

    // Settings
    public void setShowWatermark(boolean show) { this.showWatermark = show; }
    public void setShowActiveModules(boolean show) { this.showActiveModules = show; }
    public void setShowCoords(boolean show) { this.showCoords = show; }
    public void setShowInfoBar(boolean show) { this.showInfoBar = show; }

    /**
     * HUD data container - holds all info the renderer needs to draw.
     */
    public static class HudData {
        public String watermark;
        public int watermarkX, watermarkY, watermarkColor;
        public List<HudEntry> activeModules;
        public String gameModeText;
        public int gameModeX, gameModeY, gameModeColor;
        public String infoText;
        public int infoX, infoY, infoColor;
        public String coordsText;
        public int coordsX, coordsY, coordsColor;
    }

    public static class HudEntry {
        public String text;
        public int x, y, color, bgColor;
    }
}
