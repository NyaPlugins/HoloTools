package net.kokoricraft.holotools.objects.holoshield;

import net.kokoricraft.holotools.utils.objects.HoloColor;

public record ShieldProfile(boolean needPermission, String permission, int priority, int duration, int cooldown, HoloColor color, HoloColor warningColor) {
}
