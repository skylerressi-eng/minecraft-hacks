package com.github.hackclient.mixin;

import com.github.hackclient.PhantomClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_309", remap = false)
public class KeyboardMixin {
    @Inject(method = {"method_1454"}, at = {@At("HEAD")}, remap = false)
    private void phantomOnKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        PhantomClient.onKeyEvent(key, action);
    }
}
