package com.github.hackclient.ui;

import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;
import com.github.hackclient.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Meteor Client-style click GUI model.
 *
 * This class is pure Java (no Minecraft imports) so it can be unit-compiled
 * without the game. The actual drawing/input is done by {@link PhantomScreen},
 * which translates {@link RenderCommand}s into GuiGraphics calls and routes
 * clicks back into {@link #onMouseClick}.
 *
 * Layout: one panel per game mode (ALL_HACKS is skipped to avoid duplicating
 * every module). Panels flow left-to-right and wrap to a new row when they
 * would run off the screen.
 */
public class PhantomGui {

    // GLFW_KEY_RIGHT_SHIFT = 344
    public static final int OPEN_KEY = 344;

    private boolean visible = false;
    private final ModuleManager moduleManager;
    private final List<CategoryPanel> panels = new ArrayList<>();

    // Meteor-style colors (ARGB)
    public static final int COLOR_BG = 0xC0101020;          // Dark backdrop
    public static final int COLOR_PANEL_BG = 0xE0161A2E;     // Panel background
    public static final int COLOR_HEADER = 0xFF0F3460;       // Panel header
    public static final int COLOR_ACCENT = 0xFF533483;       // Accent purple
    public static final int COLOR_ENABLED = 0xFF00D4AA;      // Enabled module - teal
    public static final int COLOR_DISABLED = 0xFFB0B0B0;     // Disabled module - light gray
    public static final int COLOR_HOVER = 0xFF2A2F4A;        // Hover highlight
    public static final int COLOR_TEXT = 0xFFFFFFFF;         // Header text
    public static final int COLOR_TEXT_DIM = 0xFF9090A0;     // Dimmed text

    // Layout constants
    public static final int PANEL_WIDTH = 96;
    public static final int PANEL_SPACING = 4;
    public static final int HEADER_HEIGHT = 15;
    public static final int BUTTON_HEIGHT = 12;
    public static final int START_X = 6;
    public static final int START_Y = 22;

    public PhantomGui(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        initPanels();
        layout(854, 480);
    }

    private void initPanels() {
        panels.clear();
        for (GameMode mode : GameMode.values()) {
            if (mode == GameMode.ALL_HACKS) continue; // skip the all-in-one duplicate
            List<Module> modules = moduleManager.getModulesForMode(mode);
            if (!modules.isEmpty()) {
                panels.add(new CategoryPanel(mode.displayName, START_X, START_Y, modules));
            }
        }
    }

    /**
     * Arrange panels into rows that fit within the given screen width.
     * Called by the screen when it opens (we then know the real size).
     */
    public void layout(int screenWidth, int screenHeight) {
        int x = START_X;
        int y = START_Y;
        int rowHeight = 0;
        for (CategoryPanel panel : panels) {
            int panelHeight = panel.totalHeight();
            if (x + PANEL_WIDTH > screenWidth - START_X && x > START_X) {
                // wrap to next row
                x = START_X;
                y += rowHeight + PANEL_SPACING;
                rowHeight = 0;
            }
            panel.x = x;
            panel.y = y;
            x += PANEL_WIDTH + PANEL_SPACING;
            rowHeight = Math.max(rowHeight, panelHeight);
        }
    }

    /** Toggle GUI visibility (used by tests). */
    public boolean onKeyPress(int keyCode) {
        if (keyCode == OPEN_KEY) {
            visible = !visible;
            return true;
        }
        return false;
    }

    /**
     * Handle a left click. Returns true if the click hit a module/header.
     */
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;
        for (CategoryPanel panel : panels) {
            Module clicked = panel.getModuleAt(mouseX, mouseY);
            if (clicked != null) {
                clicked.toggle();
                return true;
            }
            if (panel.isHeaderAt(mouseX, mouseY)) {
                panel.toggleExpanded();
                return true;
            }
        }
        return false;
    }

    /**
     * Produce the list of draw commands for the current state.
     */
    public List<RenderCommand> getRenderCommands(double mouseX, double mouseY) {
        List<RenderCommand> commands = new ArrayList<>();
        if (!visible) return commands;

        for (CategoryPanel panel : panels) {
            // Header
            commands.add(new RenderCommand(RenderCommand.Type.RECT,
                    panel.x, panel.y, PANEL_WIDTH, HEADER_HEIGHT,
                    COLOR_HEADER, panel.name));

            if (!panel.expanded) continue;

            int y = panel.y + HEADER_HEIGHT;
            for (Module module : panel.modules) {
                boolean hovered = mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH
                        && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;

                int bgColor = hovered ? COLOR_HOVER : COLOR_PANEL_BG;
                int textColor = module.isEnabled() ? COLOR_ENABLED : COLOR_DISABLED;

                commands.add(new RenderCommand(RenderCommand.Type.MODULE_BUTTON,
                        panel.x, y, PANEL_WIDTH, BUTTON_HEIGHT,
                        bgColor, null));
                commands.add(new RenderCommand(RenderCommand.Type.TEXT,
                        panel.x + 4, y + 2, 0, 0,
                        textColor, module.getName()));
                y += BUTTON_HEIGHT;
            }
        }
        return commands;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public List<CategoryPanel> getPanels() { return panels; }

    public static class CategoryPanel {
        public final String name;
        public int x;
        public int y;
        public final List<Module> modules;
        public boolean expanded = true;

        public CategoryPanel(String name, int x, int y, List<Module> modules) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.modules = modules;
        }

        public void toggleExpanded() { expanded = !expanded; }

        public int totalHeight() {
            return HEADER_HEIGHT + (expanded ? modules.size() * BUTTON_HEIGHT : 0);
        }

        public boolean isHeaderAt(double mx, double my) {
            return mx >= x && mx <= x + PANEL_WIDTH
                    && my >= y && my <= y + HEADER_HEIGHT;
        }

        public Module getModuleAt(double mx, double my) {
            if (!expanded) return null;
            int by = y + HEADER_HEIGHT;
            for (Module m : modules) {
                if (mx >= x && mx <= x + PANEL_WIDTH
                        && my >= by && my <= by + BUTTON_HEIGHT) {
                    return m;
                }
                by += BUTTON_HEIGHT;
            }
            return null;
        }
    }

    public static class RenderCommand {
        public enum Type { RECT, MODULE_BUTTON, TEXT }

        public final Type type;
        public final int x, y, width, height;
        public final int color;
        public final String text;

        public RenderCommand(Type type, int x, int y, int width, int height,
                             int color, String text) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.text = text;
        }
    }
}
