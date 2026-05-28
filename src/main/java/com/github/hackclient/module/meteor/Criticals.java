package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.List;

public class Criticals extends Module {
    private long lastJumpTime = 0;
    private boolean jumped = false;

    public Criticals() {
        super("Criticals", "Auto micro-jump before hits for critical damage", GameMode.METEOR, HumanizedTimer.SkillLevel.SKILLED, "combat");
    }

    @Override
    public void onTick() {
        if (!shouldAct(0.88)) return;
        boolean onGround = McReflect.isPlayerOnGround();
        long now = System.currentTimeMillis();

        List<Object> players = McReflect.getPlayers();
        Object self = McReflect.getPlayer();
        if (self == null) return;

        boolean targetNearby = false;
        for (Object p : players) {
            if (p == self) continue;
            if (!McReflect.isEntityAlive(p)) continue;
            if (McReflect.distanceTo(p) < 4.0) { targetNearby = true; break; }
        }

        if (!targetNearby) { jumped = false; return; }

        if (onGround && !jumped && now - lastJumpTime > 500) {
            double[] vel = McReflect.getPlayerVelocity();
            McReflect.setPlayerVelocity(vel[0], 0.1, vel[2]);
            jumped = true;
            lastJumpTime = now;
            recordAction();
        } else if (!onGround) {
            jumped = false;
        }
    }
}
