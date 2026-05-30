package com.github.hackclient.ui;

import com.github.hackclient.PhantomClient;
import com.github.hackclient.gamemode.GameMode;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The Meteor-style click GUI, implemented as a real Minecraft {@link Screen}.
 *
 * Input handling note: Minecraft 1.21.9+ changed the Screen mouse/key callback
 * signatures (they now take MouseButtonEvent / KeyEvent records instead of plain
 * ints/doubles), and those types differ between versions. To stay version-proof
 * we override ONLY the stable Screen methods (init / render / isPauseScreen) and
 * read input by polling GLFW directly — GLFW codes are constant across every
 * Minecraft version. The scaled cursor position is taken straight from render()'s
 * mouseX/mouseY, so no manual GUI-scale math is needed.
 */
public class PhantomScreen extends Screen {

    private final PhantomGui gui;

    private boolean leftWasDown = false;
    private final boolean[] numWasDown = new boolean[7];

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
        // Route clicks / number keys via GLFW polling (see class note).
        handleInput(mouseX, mouseY);

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

    /**
     * Poll GLFW for in-GUI input. We deliberately do NOT override
     * mouseClicked/keyPressed because their signatures changed in 1.21.9+
     * (MouseButtonEvent / KeyEvent). Polling keeps the GUI working on every
     * 1.21.x build. mouseX/mouseY are already in scaled GUI coordinates here.
     */
    private void handleInput(int mouseX, int mouseY) {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return;

        // Left click toggles the module/header under the cursor (edge-detected).
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (leftDown && !leftWasDown) {
            gui.onMouseClick(mouseX, mouseY, 0);
        }
        leftWasDown = leftDown;

        // 1-7 switch game mode while the menu is focused.
        for (int i = 0; i < numWasDown.length; i++) {
            boolean down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS;
            if (down && !numWasDown[i]) {
                switchMode(i);
            }
            numWasDown[i] = down;
        }
    }

    private void switchMode(int idx) {
        PhantomClient pc = PhantomClient.getInstance();
        GameMode[] modes = GameMode.values();
        if (pc != null && pc.getGameModeManager() != null && idx < modes.length) {
            pc.getGameModeManager().switchMode(modes[idx]);
            gui.layout(this.width, this.height);
        }
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
