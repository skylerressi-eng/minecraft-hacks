package net.minecraft.client.gui.screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
public class Screen {
    public int width;
    public int height;
    protected TextRenderer textRenderer;
    protected Screen(Text title) {}
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {}
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    public boolean shouldPause() { return true; }
    public void close() {}
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
}
