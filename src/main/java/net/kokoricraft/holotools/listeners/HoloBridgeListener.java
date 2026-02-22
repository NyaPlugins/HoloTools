package net.kokoricraft.holotools.listeners;

import net.kokoricraft.holotools.HoloTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class HoloBridgeListener implements Listener {
    private final HoloTools plugin = HoloTools.getInstance();

    @EventHandler
    public void onTest(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        if (event.getHitBlock() == null) return;
        Location to = event.getHitBlock().getLocation().clone().add(0.5, 1, 0.5);

        if (event.getEntity() instanceof Trident trident) {
            plugin.getBridgeManager().hitEntity(player, trident, to);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) return;
        PersistentDataContainer persistentDataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        NamespacedKey namespacedKey = plugin.getBridgeManager().KEY;
        if (persistentDataContainer.has(namespacedKey)) {
            event.getEntity().getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, "yes");
        }
    }
}
