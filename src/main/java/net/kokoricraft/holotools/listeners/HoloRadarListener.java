package net.kokoricraft.holotools.listeners;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.objects.holoradar.HoloRadar;
import net.kokoricraft.holotools.objects.holoradar.HoloRadarMenu;
import net.kokoricraft.holotools.objects.holoradar.OreProfile;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class HoloRadarListener implements Listener {
    private final HoloTools plugin;

    public HoloRadarListener(HoloTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(HoloRadarMenu.getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int rawSlot = event.getRawSlot();
        if (rawSlot < 10 || rawSlot > 16) return;

        List<OreProfile> ores = plugin.getConfigManager().RADAR_ORES;
        int index = rawSlot - 10;
        if (index >= ores.size()) return;

        OreProfile ore = ores.get(index);
        HoloRadarMenu.toggleOre(player, ore.key());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        HoloRadarMenu.open(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        HoloRadar.invalidateChunk(event.getBlock().getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        HoloRadar.invalidateChunk(event.getBlock().getChunk());
    }
}
