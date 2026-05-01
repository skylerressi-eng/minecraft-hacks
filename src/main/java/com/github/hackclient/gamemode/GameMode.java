package com.github.hackclient.gamemode;

/**
 * Supported game modes, each with a curated set of hack modules.
 */
public enum GameMode {
    ALL_HACKS("All Hacks", "Every hack module in one place"),
    MACE("Mace PvP", "Mace combat with wind burst and slam mechanics"),
    LEGACY_1_8("1.8 PvP", "Classic 1.8-style combat with click timing and reach"),
    BEDWARS("Bedwars", "Bedwars-specific utility and combat hacks"),
    CRYSTAL("Crystal PvP", "End crystal combat with totems and hole mechanics"),
    METEOR("Meteor", "Meteor Client-style movement, render and utility hacks"),
    VAPE("Vape", "Vape Client-style closet/legit cheats");

    public final String displayName;
    public final String description;

    GameMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
