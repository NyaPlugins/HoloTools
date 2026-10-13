package net.kokoricraft.holotools.managers;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.enums.HoloType;
import net.kokoricraft.holotools.objects.colors.HoloPanelsColors;
import net.kokoricraft.holotools.objects.halo.Holo;
import net.kokoricraft.holotools.objects.holocrafter.HoloCrafter;
import net.kokoricraft.holotools.objects.holoradar.HoloRadar;
import net.kokoricraft.holotools.objects.holowardrobe.HoloWardrobe;
import net.kokoricraft.holotools.objects.holowaypoint.HoloWaypoint;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HoloManager {
    private final HoloTools plugin;
    private final Map<Player, Holo> opened = new HashMap<>();
    public static NamespacedKey HOLO_KEY;
    private final Map<Player, BukkitTask> tasks = new HashMap<>();

    public HoloManager(HoloTools plugin) {
        this.plugin = plugin;
        HOLO_KEY = new NamespacedKey(plugin, "holo");
    }

    long time;

    public void check(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        time = System.currentTimeMillis();
        boolean hasHolo = isHolo(itemStack);
        boolean preHolo = opened.containsKey(player);

        if (hasHolo && preHolo) {
            Holo holo = opened.get(player);
            if (!isSameHolo(holo, itemStack)) {
                holo.remove("remove check player 0");
                enable(player);
                return;
            }
        }

        if (!hasHolo && preHolo) {
            disable(player);
            return;
        }

        if (hasHolo && !preHolo) {
            enable(player);
        }
    }

    public void enable(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        if (opened.containsKey(player)) {
            Holo holo = opened.get(player);
            holo.setVisible(false);
        }

        if (meta.getPersistentDataContainer().has(plugin.getDataManager().CRAFTER_KEY, PersistentDataType.STRING)) {
            HoloCrafter holoCrafter = plugin.getDataManager().loadHoloCrafter(player, itemStack);
            holoCrafter.setVisible(true);
            opened.put(player, holoCrafter);
            return;
        }

        if (meta.getPersistentDataContainer().has(plugin.getDataManager().WARDROBE_KEY, PersistentDataType.STRING)) {
            HoloWardrobe holoWardrobe = plugin.getDataManager().loadHoloWardrobe(player, itemStack);
            holoWardrobe.setVisible(true);
            opened.put(player, holoWardrobe);
            return;
        }

        if (meta.getPersistentDataContainer().has(plugin.getDataManager().WAYPOINT_KEY, PersistentDataType.STRING)) {
            HoloWaypoint holoWaypoint = plugin.getDataManager().loadHoloWaypoint(player, itemStack);
            if (holoWaypoint != null) {
                holoWaypoint.setVisible(true);
                opened.put(player, holoWaypoint);
            }
            return;
        }

        if (meta.getPersistentDataContainer().has(plugin.getDataManager().RADAR_KEY, PersistentDataType.STRING)) {
            int slots = plugin.getPlayerManager().getPlayer(player).getSlots(HoloType.HOLORADAR);
            HoloRadar holoRadar = new HoloRadar(player, itemStack, slots);
            holoRadar.setVisible(true);
            opened.put(player, holoRadar);
        }
    }

    public void disable(Player player) {
        Holo holo = opened.get(player);
        if (holo == null) return;

        holo.setVisible(false);
        opened.remove(player);
    }

    public boolean isHolo(ItemStack itemStack) {
        if (itemStack == null) return false;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        return dataContainer.has(HOLO_KEY, PersistentDataType.STRING);
    }

    public Holo getHolo(Player player) {
        return opened.get(player);
    }

    public HoloPanelsColors getHoloColor(Player player, HoloType type) {
        List<HoloPanelsColors> colors;
        switch (type) {
            case HOLOCRAFTER -> colors = plugin.getConfigManager().CRAFTER_PANELS_COLORS;
            case HOLOWARDROBE -> colors = plugin.getConfigManager().WARDROBE_PANELS_COLORS;
            case HOLOWAYPOINT -> colors = plugin.getConfigManager().WAYPOINT_PANELS_COLORS;
            default -> colors = null;
        }
        if (colors == null) return null;
        return colors.stream().filter(color -> color.canUse(player)).findFirst().orElse(null);
    }

    public void update(Player player, long time) {
        if (tasks.containsKey(player)) return;

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getHoloManager().check(player);
            tasks.remove(player);
        }, time);

        tasks.put(player, task);
    }

    public boolean isSameHolo(Holo holo, ItemStack newItem) {
        if (holo == null || newItem == null || !newItem.hasItemMeta()) return false;
        ItemMeta newMeta = newItem.getItemMeta();
        if (newMeta == null) return false;
        PersistentDataContainer newPdc = newMeta.getPersistentDataContainer();

        ItemStack oldItem = holo.getItemStack();
        if (oldItem == null || !oldItem.hasItemMeta()) return false;
        ItemMeta oldMeta = oldItem.getItemMeta();
        if (oldMeta == null) return false;
        PersistentDataContainer oldPdc = oldMeta.getPersistentDataContainer();

        if (holo instanceof HoloCrafter && newPdc.has(plugin.getDataManager().CRAFTER_KEY, PersistentDataType.STRING)) {
            return matchHoloId(oldPdc, newPdc);
        }

        if (holo instanceof HoloWardrobe && newPdc.has(plugin.getDataManager().WARDROBE_KEY, PersistentDataType.STRING)) {
            return matchHoloId(oldPdc, newPdc);
        }

        if (holo instanceof HoloWaypoint && newPdc.has(plugin.getDataManager().WAYPOINT_KEY, PersistentDataType.STRING)) {
            return matchHoloId(oldPdc, newPdc);
        }

        if (holo instanceof HoloRadar && newPdc.has(plugin.getDataManager().RADAR_KEY, PersistentDataType.STRING)) {
            return true;
        }

        return false;
    }

    private boolean matchHoloId(PersistentDataContainer oldPdc, PersistentDataContainer newPdc) {
        NamespacedKey idKey = new NamespacedKey(plugin, "id");
        Integer oldId = oldPdc.get(idKey, PersistentDataType.INTEGER);
        Integer newId = newPdc.get(idKey, PersistentDataType.INTEGER);
        if (oldId != null && newId != null) {
            return oldId.equals(newId);
        }
        return oldId == null && newId == null;
    }
}
