package com.github.hackclient;

import com.github.hackclient.antidetect.AntiCheatAnalyzer;
import com.github.hackclient.antidetect.StealthEngine;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.gamemode.GameModeManager;
import com.github.hackclient.module.ModuleManager;
import com.github.hackclient.ui.HudRenderer;
import com.github.hackclient.ui.PhantomGui;
import com.github.hackclient.ui.PhantomScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Phantom Client — Fabric entrypoint (Minecraft 1.21.x).
 *
 * Design notes:
 *  - The GUI is a real {@link PhantomScreen} opened with Minecraft.setScreen,
 *    so it reliably "opens" (mouse freed, clickable) on any 1.21.x build.
 *  - The open key (Right Shift) and the AutoBreachSwap key (V) are real,
 *    rebindable {@link KeyMapping}s registered with Fabric's KeyBindingHelper.
 *    Minecraft's own input system delivers the presses (via consumeClick), so
 *    they appear in Options ▸ Controls and don't depend on fragile raw GLFW
 *    polling that could silently no-op.
 *  - The 58 hack modules drive the game through {@link McReflect} (reflection
 *    against stable intermediary names) and are fully decoupled from the GUI —
 *    even if reflection fails on a given build, the menu still opens.
 */
public class PhantomClient implements ClientModInitializer {
    public static final String NAME = "Phantom Client";
    public static final String VERSION = "3.0.0";

    private static PhantomClient instance;

    private ModuleManager moduleManager;
    private GameModeManager gameModeManager;
    private AntiCheatAnalyzer antiCheatAnalyzer;
    private StealthEngine stealthEngine;
    private PhantomGui gui;
    private HudRenderer hudRenderer;

    private static KeyMapping openGuiKey;
    private static KeyMapping autoBreachSwapKey;

    private boolean mcReflectReady = false;
    private int initAttempts = 0;
    private boolean welcomeSent = false;

    @Override
    public void onInitializeClient() {
        instance = this;
        log("Phantom Client v" + VERSION + " initializing for Minecraft 1.21.x...");

        moduleManager = new ModuleManager();
        antiCheatAnalyzer = new AntiCheatAnalyzer();
        stealthEngine = new StealthEngine(antiCheatAnalyzer);
        gameModeManager = new GameModeManager(moduleManager);
        gameModeManager.registerAll();

        gui = new PhantomGui(moduleManager);
        hudRenderer = new HudRenderer(moduleManager);

        // Register key bindings through Minecraft itself. This makes them show up
        // (and be rebindable) in Options > Controls, and lets Minecraft's own input
        // system deliver the presses — far more reliable than raw GLFW polling,
        // which could silently no-op when no GL context was current on the tick
        // thread. Right Shift opens the GUI; V toggles AutoBreachSwap.
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.phantom.open_gui", GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.phantom"));
        autoBreachSwapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.phantom.autobreachswap", GLFW.GLFW_KEY_V, "key.categories.phantom"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        log("Loaded " + moduleManager.getModuleCount() + " modules across "
                + GameMode.values().length + " game modes");
        log("Press RIGHT SHIFT in-game to open the GUI. Ready!");
    }

    private void onClientTick(Minecraft client) {
        try {
            handleKeys(client);
        } catch (Exception e) {
            log("Key handling error: " + e.getMessage());
        }

        // Module engine runs independently of the GUI.
        try {
            if (!mcReflectReady) {
                initAttempts++;
                if (initAttempts >= 20) {
                    mcReflectReady = McReflect.init();
                    if (mcReflectReady) log("McReflect initialized (modules active).");
                }
            }

            if (mcReflectReady) {
                stealthEngine.onTick();
                checkStaffProximity();

                if (!welcomeSent && McReflect.getPlayer() != null) {
                    welcomeSent = true;
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal(
                                "§b[Phantom] §fv" + VERSION
                                        + " loaded. Press §eRIGHT SHIFT§f to open the GUI "
                                        + "§7(rebindable in Options ▸ Controls)§f."));
                    }
                }
                moduleManager.tickAll();
            }
        } catch (Exception ignored) {
            // Module errors must never crash the client.
        }
    }

    private void handleKeys(Minecraft client) {
        // Right Shift (rebindable): open the GUI. consumeClick() only fires for
        // presses Minecraft delivered while in-game (no screen capturing input),
        // so this path only ever opens. Closing is handled inside PhantomScreen,
        // which means the two never trigger for the same press (no double-toggle).
        while (openGuiKey.consumeClick()) {
            if (client.screen == null) {
                client.setScreen(new PhantomScreen(gui));
                log("GUI opened.");
            }
        }

        // V (rebindable): toggle AutoBreachSwap.
        while (autoBreachSwapKey.consumeClick()) {
            toggleAutoBreachSwap(client);
        }
    }

    private void toggleAutoBreachSwap(Minecraft client) {
        moduleManager.getAllModules().stream()
                .filter(m -> m.getName().equals("AutoBreachSwap"))
                .findFirst()
                .ifPresent(m -> {
                    m.toggle();
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal(
                                "§b[Phantom] §fAutoBreachSwap "
                                        + (m.isEnabled() ? "§aENABLED" : "§cDISABLED")));
                    }
                });
    }

    private void checkStaffProximity() {
        if (!McReflect.canGetPlayers()) return;
        try {
            List<Object> players = McReflect.getPlayers();
            Object self = McReflect.getPlayer();
            if (self == null) return;
            boolean foundStaff = false;
            for (Object p : players) {
                if (p == self) continue;
                String name = McReflect.getEntityName(p);
                if (name == null) continue;
                String lower = name.toLowerCase();
                if (lower.contains("[staff]") || lower.contains("[mod]") || lower.contains("[admin]")
                        || lower.contains("[helper]") || lower.contains("[owner]")) {
                    foundStaff = true;
                    break;
                }
            }
            stealthEngine.setStaffNearby(foundStaff);
        } catch (Exception ignored) {
        }
    }

    private void log(String msg) {
        System.out.println("[Phantom Client] " + msg);
    }

    public static PhantomClient getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public GameModeManager getGameModeManager() { return gameModeManager; }
    public PhantomGui getGui() { return gui; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
    public AntiCheatAnalyzer getAntiCheatAnalyzer() { return antiCheatAnalyzer; }
    public StealthEngine getStealthEngine() { return stealthEngine; }
}
