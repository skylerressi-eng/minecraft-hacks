package net.minecraft.client;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.option.GameOptions;
public class MinecraftClient {
    public ClientPlayerEntity player;
    public Screen currentScreen;
    public TextRenderer textRenderer;
    public GameOptions options;
    public static MinecraftClient getInstance() { return null; }
    public Window getWindow() { return null; }
    public void setScreen(Screen screen) {}
}
