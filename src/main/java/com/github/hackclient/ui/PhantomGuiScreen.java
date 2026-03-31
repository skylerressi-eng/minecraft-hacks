package com.github.hackclient.ui;

import com.github.hackclient.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Meteor Client-style click GUI screen.
 *
 * This is an actual Minecraft Screen that opens when you press Right Shift.
 * Renders the category panels with module toggle buttons.
 * Click a module name to toggle it on/off.
 * Click a category header to collapse/expand.
 * Press Right Shift or Escape to close.
 */
public class PhantomGuiScreen extends Screen {

    private final PhantomGui gui;

    public PhantomGuiScreen(PhantomGui gui) {
        super(Text.literal("Phantom Client"));
        this.gui = gui;
        gui.setVisible(true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent dark background overlay
        context.fill(0, 0, this.width, this.height, PhantomGui.COLOR_BG);

        // Render each category panel
        for (PhantomGui.CategoryPanel panel : gui.getPanels()) {
            // Panel header
            context.fill(panel.x, panel.y,
                    panel.x + 110, panel.y + 22, PhantomGui.COLOR_HEADER);
            context.drawText(this.textRenderer, panel.name,
                    panel.x + 4, panel.y + 6, PhantomGui.COLOR_TEXT, true);

            if (!panel.expanded) continue;

            // Module buttons
            int y = panel.y + 22;
            for (Module module : panel.modules) {
                boolean hovered = mouseX >= panel.x && mouseX <= panel.x + 110
                        && mouseY >= y && mouseY <= y + 16;

                // Button background
                int bgColor = hovered ? PhantomGui.COLOR_HOVER : PhantomGui.COLOR_PANEL_BG;
                context.fill(panel.x, y, panel.x + 110, y + 16, bgColor);

                // Module name (teal if enabled, gray if disabled)
                int textColor = module.isEnabled() ? PhantomGui.COLOR_ENABLED : PhantomGui.COLOR_DISABLED;
                context.drawText(this.textRenderer, module.getName(),
                        panel.x + 4, y + 4, textColor, false);

                y += 16;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check module clicks
            if (gui.onMouseClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game when GUI is open (like Meteor)
    }

    @Override
    public void close() {
        gui.setVisible(false);
        super.close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Right Shift closes the GUI too
        if (keyCode == PhantomGui.OPEN_KEY) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
