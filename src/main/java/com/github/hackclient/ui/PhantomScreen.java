package com.github.hackclient.ui;

import com.github.hackclient.PhantomClient;
import com.github.hackclient.gamemode.GameMode;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The actual Meteor-style click GUI, implemented as a real Minecraft {@link Screen}.
 *
 * Opening a real Screen (via Minecraft.setScreen) is what releases the mouse,
 * lets the panels be clicked, and makes the menu unmistakably "open". All the
 * layout/state lives in {@link PhantomGui}; this class just draws it and routes
 * input.
 */
public class PhantomScreen extends Screen {

    private final PhantomGui gui;

    public PhantomScreen(PhantomGui gui) {
        super(Component.literal("Phantom Client"));
        this.gui = gui;
    }

    @Override
    protected void init() {
        gui.setVisible(true);
        gui.layout(this.width, this.height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Dim backdrop so the menu is obvious.
        g.fill(0, 0, this.width, this.height, 0xC0101020);

        // Title bar
        String title = "Phantom Client v" + PhantomClient.VERSION;
        g.fill(0, 0, this.width, 18, 0xE00F3460);
        g.drawString(this.font, title, 6, 5, 0xFF00D4AA);

        String hint = "Mode: " + activeModeName()
                + "    Right Shift / Esc: close    1-7: game mode    Click: toggle";
        g.drawString(this.font, hint, 12 + this.font.width(title), 5, 0xFFB8C0D8);

        // Module panels
        List<PhantomGui.RenderCommand> commands = gui.getRenderCommands(mouseX, mouseY);
        for (PhantomGui.RenderCommand cmd : commands) {
            switch (cmd.type) {
                case RECT -> {
                    g.fill(cmd.x, cmd.y, cmd.x + cmd.width, cmd.y + cmd.height, cmd.color);
                    if (cmd.text != null) {
                        g.drawString(this.font, cmd.text, cmd.x + 4, cmd.y + 4, 0xFFFFFFFF);
                    }
                }
                case MODULE_BUTTON ->
                        g.fill(cmd.x, cmd.y, cmd.x + cmd.width, cmd.y + cmd.height, cmd.color);
                case TEXT -> g.drawString(this.font, cmd.text, cmd.x, cmd.y, cmd.color);
            }
        }

        // Stealth status (bottom-left)
        String stealth = stealthStatus();
        g.fill(0, this.height - 14, this.font.width(stealth) + 10, this.height, 0xC0000000);
        g.drawString(this.font, stealth, 5, this.height - 11, 0xFF00D4AA);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (gui.onMouseClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 1-7 switch game mode (note: handled here, not via global keybind, so it
        // never conflicts with the hotbar during normal play).
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_7) {
            int idx = keyCode - GLFW.GLFW_KEY_1;
            PhantomClient pc = PhantomClient.getInstance();
            GameMode[] modes = GameMode.values();
            if (pc != null && pc.getGameModeManager() != null && idx < modes.length) {
                pc.getGameModeManager().switchMode(modes[idx]);
                gui.layout(this.width, this.height);
            }
            return true;
        }
        // Right Shift closes the GUI (Esc also closes via the default handler).
        // Opening is done by the registered key binding while in-game, so these
        // two paths never fire for the same key press — no double-toggle.
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // keep the world running while the menu is open
    }

    private String activeModeName() {
        PhantomClient pc = PhantomClient.getInstance();
        if (pc != null && pc.getGameModeManager() != null
                && pc.getGameModeManager().getActiveMode() != null) {
            return pc.getGameModeManager().getActiveMode().displayName;
        }
        return "None";
    }

    private String stealthStatus() {
        PhantomClient pc = PhantomClient.getInstance();
        if (pc != null && pc.getStealthEngine() != null) {
            return "STEALTH: GHOST (" + pc.getStealthEngine().getSuspicionScore() + ")";
        }
        return "STEALTH: READY";
    }
}
