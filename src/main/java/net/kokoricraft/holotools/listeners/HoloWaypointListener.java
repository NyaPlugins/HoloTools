package net.kokoricraft.holotools.listeners;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.objects.halo.Holo;
import net.kokoricraft.holotools.objects.holowaypoint.HoloWaypoint;
import net.kokoricraft.holotools.objects.holowaypoint.HoloWaypointMenu;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class HoloWaypointListener implements Listener {
    private final HoloTools plugin;
    private final Map<UUID, Integer> renamingPlayers = new HashMap<>();

    public HoloWaypointListener(HoloTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(HoloWaypointMenu.getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        Holo holo = plugin.getHoloManager().getHolo(player);
        HoloWaypoint wp;

        if (holo instanceof HoloWaypoint hw) {
            wp = hw;
        } else {
            wp = plugin.getDataManager().loadHoloWaypoint(player, item);
        }

        if (wp == null) return;

        int rawSlot = event.getRawSlot();

        if (rawSlot == 4) {
            wp.addWaypoint(player.getLocation());
            player.sendMessage(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_CREATED));
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.8f, 1.5f);
            HoloWaypointMenu.open(player, wp);
            return;
        }

        if (rawSlot >= 9 && rawSlot < 27) {
            List<Integer> keys = new ArrayList<>(wp.getWaypoints().keySet());
            int index = rawSlot - 9;
            if (index < keys.size()) {
                int targetSlot = keys.get(index);
                if (event.isShiftClick()) {
                    wp.removeWaypoint(targetSlot);
                    player.sendMessage(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_DELETED));
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.2f);
                    HoloWaypointMenu.open(player, wp);
                } else {
                    player.closeInventory();
                    renamingPlayers.put(player.getUniqueId(), targetSlot);
                    player.sendMessage(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_RENAME_PROMPT));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!renamingPlayers.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        int slot = renamingPlayers.remove(player.getUniqueId());
        String msg = event.getMessage().trim();

        if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getUtils().color(plugin.getLangManager().WAYPOINT_RENAME_CANCEL));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack item = player.getInventory().getItemInMainHand();
            Holo holo = plugin.getHoloManager().getHolo(player);
            HoloWaypoint wp;
            if (holo instanceof HoloWaypoint hw) {
                wp = hw;
            } else {
                wp = plugin.getDataManager().loadHoloWaypoint(player, item);
            }
            if (wp != null) {
                wp.renameWaypoint(slot, msg);
                player.sendMessage(plugin.getUtils().color(String.format(plugin.getLangManager().WAYPOINT_RENAME_SUCCESS, msg)));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.8f);
                HoloWaypointMenu.open(player, wp);
            }
        });
    }
}
