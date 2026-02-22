package net.kokoricraft.holotools.objects.players;

import com.google.gson.JsonObject;
import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.enums.HoloType;
import net.kokoricraft.holotools.objects.holobridge.BridgeProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoloPlayer {
    private final UUID uuid;
    private final Map<String, JsonObject> data = new HashMap<>();
    private final HoloTools plugin = HoloTools.getInstance();

    public HoloPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public void setData(String id, JsonObject jsonObject) {
        this.data.put(id, jsonObject);
    }

    public Map<String, JsonObject> getData() {
        return data;
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getSlots(HoloType holoType) {
        int defaultSlots = plugin.getConfigManager().DEFAULT_SLOTS.get(holoType);
        String basePermission = holoType.name().toLowerCase() + ".slots";

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return defaultSlots;

        int maxSlots = 0;

        for (int i = 4; i <= 16; i++) {
            if (player.hasPermission(basePermission + "." + i)) {
                maxSlots = Math.max(maxSlots, i);
            }
        }

        if (maxSlots == 0) {
            return defaultSlots;
        }

        return maxSlots;
    }

    public BridgeProfile getBridgeProfile() {
        Player player = Bukkit.getPlayer(uuid);
        return plugin.getBridgeManager().getBridgeProfile(player);
    }
}
