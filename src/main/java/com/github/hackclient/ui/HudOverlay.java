package com.github.hackclient.ui;

import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD overlay renderer - draws the Meteor-style HUD on screen.
 *
 * Registered as a HudRenderCallback to Fabric API.
 * Renders:
 * - Watermark (top-left)
 * - Active module list (top-right, sorted by name length)
 * - Game mode indicator
 * - Player coordinates (bottom-left)
 */
public class HudOverlay implements HudRenderCallback {

    private final HudRenderer hudRenderer;
    private final GameModeManager gameModeManager;

    // Meteor-style colors
    private static final int WATERMARK_COLOR = 0xFF00D4AA;
    private static final int MODULE_BG = 0x80000000;
    private static final int MODULE_TEXT = 0xFFE0E0E0;
    private static final int INFO_COLOR = 0xFFB0B0B0;

    public HudOverlay(HudRenderer hudRenderer, GameModeManager gameModeManager) {
        this.hudRenderer = hudRenderer;
        this.gameModeManager = gameModeManager;
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        // Don't draw HUD when the click GUI screen is open
        if (client.currentScreen instanceof PhantomGuiScreen) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Watermark (top-left)
        context.drawText(client.textRenderer,
                "Phantom Client v2.0.0",
                4, 4, WATERMARK_COLOR, true);

        // Game mode indicator
        if (gameModeManager.getActiveMode() != null) {
            String modeText = "[" + gameModeManager.getActiveMode().displayName + "]";
            context.drawText(client.textRenderer, modeText, 4, 16, 0xFF909090, false);
        }

        // Active modules list (top-right, Meteor style - sorted by name length)
        List<Module> activeModules = new ArrayList<>();
        for (Module m : hudRenderer.getModuleManager().getAllModules()) {
            if (m.isEnabled()) activeModules.add(m);
        }
        activeModules.sort((a, b) -> b.getName().length() - a.getName().length());

        int moduleY = 2;
        for (Module m : activeModules) {
            int textWidth = client.textRenderer.getWidth(m.getName());
            int x = screenWidth - textWidth - 4;

            // Background bar behind module name
            context.fill(x - 2, moduleY, screenWidth, moduleY + 12, MODULE_BG);
            // Module name
            context.drawText(client.textRenderer, m.getName(), x, moduleY + 2, MODULE_TEXT, false);

            moduleY += 12;
        }

        // Coordinates (bottom-left)
        String coords = String.format("XYZ: %.1f / %.1f / %.1f",
                client.player.getX(), client.player.getY(), client.player.getZ());
        context.drawText(client.textRenderer, coords, 4, screenHeight - 22, 0xFFD0D0D0, false);
    }
}
