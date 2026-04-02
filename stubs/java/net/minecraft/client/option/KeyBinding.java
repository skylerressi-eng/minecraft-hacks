package net.minecraft.client.option;
import net.minecraft.client.util.InputUtil;
public class KeyBinding {
    public KeyBinding(String translationKey, InputUtil.Type type, int code, String category) {}
    public KeyBinding(String translationKey, int code, String category) {}
    public boolean wasPressed() { return false; }
}
