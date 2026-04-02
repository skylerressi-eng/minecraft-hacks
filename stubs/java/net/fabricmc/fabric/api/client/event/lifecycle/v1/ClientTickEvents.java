package net.fabricmc.fabric.api.client.event.lifecycle.v1;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.MinecraftClient;
public class ClientTickEvents {
    public static final Event<EndTick> END_CLIENT_TICK = new Event<>();
    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftClient client);
    }
}
