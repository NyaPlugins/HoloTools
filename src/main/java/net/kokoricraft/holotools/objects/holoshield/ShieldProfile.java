package net.kokoricraft.holotools.objects.holoshield;

import net.kokoricraft.holotools.utils.objects.HoloColor;

public record ShieldProfile(
        boolean needPermission,
        String permission,
        int priority,
        int duration,
        int cooldown,
        double damageReduction,
        boolean reflectProjectiles,
        HoloColor color,
        HoloColor warningColor
) {
    public ShieldProfile(boolean needPermission, String permission, int priority, int duration, int cooldown, HoloColor color, HoloColor warningColor) {
        this(needPermission, permission, priority, duration, cooldown, 1.0, true, color, warningColor);
    }
}
