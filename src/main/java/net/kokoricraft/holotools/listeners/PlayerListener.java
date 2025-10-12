package net.kokoricraft.holotools.listeners;

import net.kokoricraft.holotools.HoloTools;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final HoloTools plugin;

    public PlayerListener(HoloTools plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        if(!event.getPlayer().isOp() && !event.getPlayer().hasPermission("*")) return;
        plugin.getUpdateChecker().sendMessage(event.getPlayer(), plugin.UPDATED, plugin.VERSION);
    }

    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event){
        plugin.getPlayerManager().getPlayer(event.getUniqueId());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        plugin.getHoloManager().disable(event.getPlayer());
        plugin.getPlayerManager().remove(event.getPlayer().getUniqueId());
    }
}
