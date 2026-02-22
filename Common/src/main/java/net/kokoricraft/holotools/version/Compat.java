package net.kokoricraft.holotools.version;


import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface Compat {
    HoloTextDisplay createTextDisplay(List<Player> players, Location location, float yaw, float pitch);
    HoloItemDisplay createItemDisplay(List<Player> players, Location location, float yaw, float pitch);
    HoloBlockDisplay createBlockDisplay(List<Player> players, Location location, float yaw, float pitch);
    HoloShulker createShulker(List<Player> players, Location location, float yaw, float pitch);
    void initPacketsRegister(Player player);
    void removePlayers();
    List<BaseComponent> getToolTip(ItemStack itemStack, Player player, boolean advanced);
}
