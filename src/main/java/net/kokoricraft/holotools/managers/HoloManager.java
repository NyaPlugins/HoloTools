package net.kokoricraft.holotools.managers;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.enums.HoloType;
import net.kokoricraft.holotools.objects.colors.HoloPanelsColors;
import net.kokoricraft.holotools.objects.halo.Holo;
import net.kokoricraft.holotools.objects.holocrafter.HoloCrafter;
import net.kokoricraft.holotools.objects.holowardrobe.HoloWardrobe;
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
            ItemStack old = holo.getItemStack();
            if (!old.equals(itemStack)) {
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
        List<HoloPanelsColors> colors = type == HoloType.HOLOCRAFTER ? plugin.getConfigManager().CRAFTER_PANELS_COLORS : plugin.getConfigManager().WARDROBE_PANELS_COLORS;
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
}
