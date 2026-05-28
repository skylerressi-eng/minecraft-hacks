package com.github.hackclient.module.meteor;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.ArrayList;
import java.util.List;

public class Tracers extends Module {
    private final List<TracerTarget> targets = new ArrayList<>();

    public Tracers() {
        super("Tracers", "Draw lines to nearby players through walls", GameMode.METEOR, HumanizedTimer.SkillLevel.AVERAGE, "render");
    }

    @Override
    public void onTick() {
        targets.clear();
        Object self = McReflect.getPlayer();
        if (self == null) return;

        List<Object> players = McReflect.getPlayers();
        for (Object p : players) {
            if (p == self) continue;
            if (!McReflect.isEntityAlive(p)) continue;
            double dist = McReflect.distanceTo(p);
            if (dist > 256) continue;

            targets.add(new TracerTarget(
                McReflect.getEntityName(p),
                McReflect.getEntityX(p),
                McReflect.getEntityY(p),
                McReflect.getEntityZ(p),
                dist,
                McReflect.getEntityHealth(p)
            ));
        }
    }

    public List<TracerTarget> getTargets() { return targets; }

    public static class TracerTarget {
        public final String name;
        public final double x, y, z, distance;
        public final float health;

        public TracerTarget(String name, double x, double y, double z, double distance, float health) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.distance = distance;
            this.health = health;
        }
    }
}
