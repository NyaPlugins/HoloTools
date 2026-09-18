package net.kokoricraft.holotools.listeners;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.managers.ShieldManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class HoloShieldListener implements Listener {
    private final HoloTools plugin;
    private final ShieldManager shieldManager;

    public HoloShieldListener(HoloTools plugin) {
        this.plugin = plugin;
        this.shieldManager = plugin.getShieldManager();
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (shieldManager.isHoloShield(item)) {
            if (shieldManager.canActivate(player)) {
                shieldManager.startShield(player);
            } else if (shieldManager.isOnCooldown(player)) {
                int rem = shieldManager.getRemainingCooldown(player);
                player.sendMessage(plugin.getUtils().color(String.format(plugin.getLangManager().SHIELD_COOLDOWN, rem)));
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (shieldManager.isHoloShield(event.getMainHandItem()) || shieldManager.isHoloShield(event.getOffHandItem())) {
            if (shieldManager.canActivate(player)) {
                shieldManager.startShield(player);
            } else if (shieldManager.isOnCooldown(player)) {
                int rem = shieldManager.getRemainingCooldown(player);
                player.sendMessage(plugin.getUtils().color(String.format(plugin.getLangManager().SHIELD_COOLDOWN, rem)));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!shieldManager.hasActiveShield(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FALL ||
            cause == EntityDamageEvent.DamageCause.VOID ||
            cause == EntityDamageEvent.DamageCause.STARVATION ||
            cause == EntityDamageEvent.DamageCause.SUFFOCATION ||
            cause == EntityDamageEvent.DamageCause.DROWNING) {
            return;
        }

        event.setCancelled(true);
        shieldManager.onDamageBlocked(player);

        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            if (byEntityEvent.getDamager() instanceof Projectile projectile) {
                shieldManager.reflectProjectile(player, projectile);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        shieldManager.stopShield(event.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        shieldManager.stopShield(event.getPlayer());
    }
}
