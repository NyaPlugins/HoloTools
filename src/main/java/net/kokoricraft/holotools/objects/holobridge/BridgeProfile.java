package net.kokoricraft.holotools.objects.holobridge;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record BridgeProfile(ItemStack itemStack, boolean need_permission, String permission, int duration, int priority, Material material) {
}
