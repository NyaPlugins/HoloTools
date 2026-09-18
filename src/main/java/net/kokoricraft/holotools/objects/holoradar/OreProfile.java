package net.kokoricraft.holotools.objects.holoradar;

import net.kokoricraft.holotools.utils.objects.HoloColor;
import org.bukkit.Material;

import java.util.Set;

public record OreProfile(String key, String name, Material icon, int priority, HoloColor color, Set<Material> materials) {
    public boolean matches(Material material) {
        return materials.contains(material);
    }
}
