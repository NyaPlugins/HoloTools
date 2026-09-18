package net.kokoricraft.holotools.objects.holowaypoint;

import net.kokoricraft.holotools.HoloTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HoloWaypointMenu {

    public static String getTitle() {
        return HoloTools.getInstance().getUtils().color(HoloTools.getInstance().getLangManager().WAYPOINT_MENU_TITLE);
    }

    public static void open(Player player, HoloWaypoint holo) {
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

        ItemStack createBtn = new ItemStack(Material.NETHER_STAR);
        ItemMeta createMeta = createBtn.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_SAVE_BTN));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_SAVE_LORE));
            lore.add(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_CLICK_TO_SAVE));
            createMeta.setLore(lore);
            createBtn.setItemMeta(createMeta);
        }
        inv.setItem(4, createBtn);

        Map<Integer, WaypointData> waypoints = holo.getWaypoints();
        Location pLoc = player.getLocation();
        int invSlot = 9;

        for (Map.Entry<Integer, WaypointData> entry : waypoints.entrySet()) {
            if (invSlot >= 27) break;
            WaypointData wp = entry.getValue();
            if (wp == null) continue;

            ItemStack wpItem = new ItemStack(Material.COMPASS);
            ItemMeta wpMeta = wpItem.getItemMeta();
            if (wpMeta != null) {
                wpMeta.setDisplayName(plugin.getUtils().color("&#00d2ff&l" + wp.getName()));
                List<String> lore = new ArrayList<>();
                lore.add(plugin.getUtils().color(String.format(plugin.getLangManager().WAYPOINT_WORLD, wp.getWorldName())));
                lore.add(plugin.getUtils().color("&#74b9ffX: &#dfe6e9" + (int) wp.getX() + " &#74b9ffY: &#dfe6e9" + (int) wp.getY() + " &#74b9ffZ: &#dfe6e9" + (int) wp.getZ()));

                double dist = wp.distanceTo(pLoc);
                if (dist >= 0) {
                    lore.add(plugin.getUtils().color(String.format(plugin.getLangManager().WAYPOINT_DISTANCE, (int) dist)));
                }
                lore.add("");
                lore.add(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_RENAME_LORE));
                lore.add(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_DELETE_LORE));
                wpMeta.setLore(lore);
                wpItem.setItemMeta(wpMeta);
            }

            inv.setItem(invSlot, wpItem);
            invSlot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.4f);
    }
}
