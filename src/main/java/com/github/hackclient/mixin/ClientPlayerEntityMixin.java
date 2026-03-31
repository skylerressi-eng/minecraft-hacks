package com.github.hackclient.mixin;

import com.github.hackclient.PhantomClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ClientPlayerEntity to hook into the player tick.
 * This ensures module logic runs in sync with the player's game tick,
 * which is critical for anti-cheat bypass (actions must align with game ticks).
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        PhantomClient client = PhantomClient.getInstance();
        if (client != null && client.getModuleManager() != null) {
            client.getModuleManager().tickAll();
        }
    }
}
