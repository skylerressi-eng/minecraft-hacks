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
 *  - The open key (Right Shift) and the AutoBreachSwap key (V) are read by
 *    polling GLFW directly. GLFW key codes are constant across versions, so this
 *    avoids the 1.21.9+ KeyMapping/Category and KeyEvent API churn entirely.
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

    private boolean rightShiftDownLast = false;
    private boolean vDownLast = false;

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
                        client.player.displayClientMessage(Component.literal(
                                "§b[Phantom] §fv" + VERSION
                                        + " loaded. Press §eRIGHT SHIFT§f to open the GUI."), false);
                    }
                }
                moduleManager.tickAll();
            }
        } catch (Exception ignored) {
            // Module errors must never crash the client.
        }
    }

    private void handleKeys(Minecraft client) {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return;

        // Right Shift: toggle the GUI screen open/closed.
        boolean rsDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (rsDown && !rightShiftDownLast) {
            if (client.screen instanceof PhantomScreen) {
                client.setScreen(null);
                log("GUI closed.");
            } else if (client.screen == null) {
                client.setScreen(new PhantomScreen(gui));
                log("GUI opened.");
            }
        }
        rightShiftDownLast = rsDown;

        // V: toggle AutoBreachSwap (V is unbound in vanilla).
        boolean vDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
        if (vDown && !vDownLast && client.screen == null) {
            toggleAutoBreachSwap(client);
        }
        vDownLast = vDown;
    }

    private void toggleAutoBreachSwap(Minecraft client) {
        moduleManager.getAllModules().stream()
                .filter(m -> m.getName().equals("AutoBreachSwap"))
                .findFirst()
                .ifPresent(m -> {
                    m.toggle();
                    if (client.player != null) {
                        client.player.displayClientMessage(Component.literal(
                                "§b[Phantom] §fAutoBreachSwap "
                                        + (m.isEnabled() ? "§aENABLED" : "§cDISABLED")), false);
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
