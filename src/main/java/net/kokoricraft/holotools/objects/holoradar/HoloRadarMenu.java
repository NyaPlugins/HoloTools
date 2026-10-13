package net.kokoricraft.holotools.objects.holoradar;

import net.kokoricraft.holotools.HoloTools;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

public class HoloRadarMenu {
    private static final Map<UUID, Set<String>> disabledOres = new ConcurrentHashMap<>();

    public static String getTitle() {
        return HoloTools.getInstance().getUtils().color(HoloTools.getInstance().getLangManager().RADAR_MENU_TITLE);
    }

    public static Set<String> getDisabledOres(Player player) {
        return disabledOres.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    public static boolean isOreEnabled(Player player, String oreKey) {
        Set<String> disabled = disabledOres.get(player.getUniqueId());
        return disabled == null || !disabled.contains(oreKey);
    }

    public static void toggleOre(Player player, String oreKey) {
        Set<String> disabled = disabledOres.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        if (disabled.contains(oreKey)) {
            disabled.remove(oreKey);
        } else {
            disabled.add(oreKey);
        }
    }

    public static final List<Integer> ORE_SLOTS = List.of(
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24, 25
    );

    public static int getOreIndex(int rawSlot) {
        return ORE_SLOTS.indexOf(rawSlot);
    }

    public static void open(Player player) {
        HoloTools plugin = HoloTools.getInstance();
        Inventory inv = Bukkit.createInventory(null, 27, getTitle());

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        List<OreProfile> ores = plugin.getConfigManager().RADAR_ORES;

        for (int i = 0; i < ores.size() && i < ORE_SLOTS.size(); i++) {
            OreProfile ore = ores.get(i);
            int slot = ORE_SLOTS.get(i);

            boolean enabled = isOreEnabled(player, ore.key());
            ItemStack item = new ItemStack(ore.icon() != null ? ore.icon() : Material.STONE);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(plugin.getUtils().color(ore.name()));
                List<String> lore = new ArrayList<>();
                lore.add("");
                if (enabled) {
                    lore.add(plugin.getUtils().color(plugin.getLangManager().RADAR_FILTER_DETECTING));
                    lore.add(plugin.getUtils().color(plugin.getLangManager().RADAR_FILTER_DISABLE_CLICK));
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(plugin.getUtils().color(plugin.getLangManager().RADAR_FILTER_IGNORED));
                    lore.add(plugin.getUtils().color(plugin.getLangManager().RADAR_FILTER_ENABLE_CLICK));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.4f);
    }
}
