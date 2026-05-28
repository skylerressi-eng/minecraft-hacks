package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

public class Xray extends Module {
    public Xray() {
        super("Xray", "See ores through blocks (render system toggle)", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "render");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        McReflect.sendChatMessage("§b[Phantom] §eXray enabled");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        McReflect.sendChatMessage("§b[Phantom] §7Xray disabled");
    }

    @Override
    public void onTick() {
        // Xray rendering handled by render pipeline; this is the toggle
    }
}
