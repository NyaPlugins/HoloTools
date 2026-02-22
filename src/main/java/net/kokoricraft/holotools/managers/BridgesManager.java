package net.kokoricraft.holotools.managers;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.objects.holobridge.Bridge;
import net.kokoricraft.holotools.objects.holobridge.BridgeProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BridgesManager {
    private final HoloTools plugin;
    public final NamespacedKey KEY;
    private Map<Player, Bridge> bridges = new HashMap<>();

    public BridgesManager(HoloTools holoTools) {
        this.plugin = holoTools;
        KEY = new NamespacedKey(plugin, "holobridge");
    }

    public void hitEntity(Player player, Trident trident, Location to) {
        PersistentDataContainer persistentDataContainer = trident.getPersistentDataContainer();

        if (!persistentDataContainer.has(KEY, PersistentDataType.STRING)) {
            return;
        }

        if (bridges.containsKey(player)) {
            bridges.get(player).remove();
        }

        BridgeProfile bridgeProfile = getBridgeProfile(player);
        if (bridgeProfile == null) return;

        Bridge bridge = new Bridge(player, player.getLocation().getBlock().getLocation().clone().add(0.5, -0.5, 0.5), to, bridgeProfile.material());
        bridge.spawn();

        bridges.put(player, bridge);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            bridge.remove();
            bridges.remove(player);
        }, 20L * bridgeProfile.duration());
    }

    public BridgeProfile getBridgeProfile(Player player) {
        List<BridgeProfile> profiles = plugin.getConfigManager().bridgeProfiles;

        BridgeProfile selected = null;

        for (BridgeProfile profile : profiles) {
            if (profile.need_permission()) {
                if (profile.permission() == null || profile.permission().isEmpty()) continue;
                if (!player.hasPermission(profile.permission())) continue;
            }

            if (selected == null || profile.priority() > selected.priority()) {
                selected = profile;
            }
        }

        return selected;
    }
}
