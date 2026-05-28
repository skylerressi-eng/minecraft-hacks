package com.github.hackclient.module.vape;

import com.github.hackclient.McReflect;
import com.github.hackclient.antidetect.HumanizedTimer;
import com.github.hackclient.gamemode.GameMode;
import com.github.hackclient.module.Module;

import java.util.ArrayList;
import java.util.List;

public class NameTags extends Module {
    private final List<NameTagInfo> nameTagData = new ArrayList<>();

    public NameTags() {
        super("NameTags", "Enhanced nametags with health display visible at any distance", GameMode.VAPE, HumanizedTimer.SkillLevel.AVERAGE, "render");
    }

    @Override
    public void onTick() {
        nameTagData.clear();
        Object self = McReflect.getPlayer();
        if (self == null) return;

        List<Object> players = McReflect.getPlayers();
        for (Object p : players) {
            if (p == self) continue;
            if (!McReflect.isEntityAlive(p)) continue;

            String name = McReflect.getEntityName(p);
            float health = McReflect.getEntityHealth(p);
            double dist = McReflect.distanceTo(p);

            String healthColor;
            if (health > 15) healthColor = "§a";
            else if (health > 8) healthColor = "§e";
            else healthColor = "§c";

            String display = name + " " + healthColor + String.format("%.1f", health) + "§f HP §7[" + String.format("%.0f", dist) + "m]";

            nameTagData.add(new NameTagInfo(name, display, health, dist,
                McReflect.getEntityX(p), McReflect.getEntityY(p), McReflect.getEntityZ(p)));
        }
    }

    public List<NameTagInfo> getNameTagData() { return nameTagData; }

    public static class NameTagInfo {
        public final String name;
        public final String displayText;
        public final float health;
        public final double distance;
        public final double x, y, z;

        public NameTagInfo(String name, String displayText, float health, double distance, double x, double y, double z) {
            this.name = name;
            this.displayText = displayText;
            this.health = health;
            this.distance = distance;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
