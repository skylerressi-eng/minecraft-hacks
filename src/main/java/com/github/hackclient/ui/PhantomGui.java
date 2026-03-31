package com.github.hackclient.ui;

import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;
import com.github.hackclient.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Meteor Client-style click GUI.
 *
 * Opens with Right Shift key. Displays module categories as panels
 * in a horizontal layout, each containing toggleable module buttons.
 * Styled to match Meteor Client's dark theme with accent colors.
 *
 * Layout: horizontal panels for each game mode category, each panel
 * has a header and a list of module buttons that toggle on click.
 */
public class PhantomGui {

    // Keybind: GLFW_KEY_RIGHT_SHIFT = 344
    public static final int OPEN_KEY = 344;

    private boolean visible = false;
    private final ModuleManager moduleManager;
    private final List<CategoryPanel> panels = new ArrayList<>();

    // Meteor-style colors (ARGB)
    public static final int COLOR_BG = 0xCC1A1A2E;         // Dark navy background
    public static final int COLOR_PANEL_BG = 0xDD16213E;   // Panel background
    public static final int COLOR_HEADER = 0xFF0F3460;     // Panel header
    public static final int COLOR_ACCENT = 0xFF533483;     // Accent purple
    public static final int COLOR_ENABLED = 0xFF00D4AA;    // Enabled module - teal
    public static final int COLOR_DISABLED = 0xFF808080;   // Disabled module - gray
    public static final int COLOR_HOVER = 0xFF2A2A4A;      // Hover highlight
    public static final int COLOR_TEXT = 0xFFE0E0E0;       // Light text
    public static final int COLOR_TEXT_DIM = 0xFF909090;   // Dimmed text

    // Layout constants (Meteor-style)
    private static final int PANEL_WIDTH = 110;
    private static final int PANEL_SPACING = 4;
    private static final int HEADER_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 16;
    private static final int START_X = 10;
    private static final int START_Y = 10;

    public PhantomGui(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        initPanels();
    }

    private void initPanels() {
        panels.clear();
        int x = START_X;

        for (GameMode mode : GameMode.values()) {
            List<Module> modules = moduleManager.getModulesForMode(mode);
            if (!modules.isEmpty()) {
                CategoryPanel panel = new CategoryPanel(mode.displayName, x, START_Y, modules);
                panels.add(panel);
                x += PANEL_WIDTH + PANEL_SPACING;
            }
        }
    }

    /**
     * Handle key press - toggle GUI visibility on Right Shift.
     */
    public boolean onKeyPress(int keyCode) {
        if (keyCode == OPEN_KEY) {
            visible = !visible;
            return true;
        }
        return false;
    }

    /**
     * Handle mouse click - toggle modules when clicking buttons.
     * Returns true if click was consumed by the GUI.
     */
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;

        for (CategoryPanel panel : panels) {
            Module clicked = panel.getModuleAt(mouseX, mouseY);
            if (clicked != null) {
                clicked.toggle();
                return true;
            }

            // Check if header clicked (collapse/expand)
            if (panel.isHeaderAt(mouseX, mouseY)) {
                panel.toggleExpanded();
                return true;
            }
        }
        return false;
    }

    /**
     * Render the GUI. Called from the HUD render event.
     * In actual Fabric implementation, this draws to the MatrixStack.
     *
     * @return Render data for each panel/button (for the actual renderer to draw)
     */
    public List<RenderCommand> getRenderCommands(double mouseX, double mouseY) {
        List<RenderCommand> commands = new ArrayList<>();
        if (!visible) return commands;

        for (CategoryPanel panel : panels) {
            // Panel header
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
                        bgColor, module.getName()));
                commands.add(new RenderCommand(RenderCommand.Type.TEXT,
                        panel.x + 4, y + 4, 0, 0,
                        textColor, module.getName()));

                y += BUTTON_HEIGHT;
            }
        }

        return commands;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public List<CategoryPanel> getPanels() { return panels; }

    /**
     * A category panel containing modules for a game mode.
     */
    public static class CategoryPanel {
        public final String name;
        public final int x;
        public final int y;
        public final List<Module> modules;
        public boolean expanded = true;

        public CategoryPanel(String name, int x, int y, List<Module> modules) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.modules = modules;
        }

        public void toggleExpanded() {
            expanded = !expanded;
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

    /**
     * Render command for the actual GUI renderer to process.
     */
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
