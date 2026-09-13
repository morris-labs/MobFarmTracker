package com.mobfarmtracker;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/** Which mobs to count: hostile only, passive/neutral only, or both. */
public enum MobFilter {
    HOSTILE("mobfarmtracker.filter.hostile"),
    PASSIVE("mobfarmtracker.filter.passive"),
    BOTH("mobfarmtracker.filter.both");

    private final String translationKey;

    MobFilter(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public boolean matches(Mob mob) {
        boolean hostile = mob instanceof Enemy;
        return switch (this) {
            case HOSTILE -> hostile;
            case PASSIVE -> !hostile;
            case BOTH -> true;
        };
    }

    public static MobFilter byOrdinalSafe(int ordinal) {
        MobFilter[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : BOTH;
    }
}
