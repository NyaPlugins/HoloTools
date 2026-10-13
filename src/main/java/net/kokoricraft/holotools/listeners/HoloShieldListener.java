package net.kokoricraft.holotools.listeners;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.managers.ShieldManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class HoloShieldListener implements Listener {
    private final HoloTools plugin;
    private final ShieldManager shieldManager;

    public HoloShieldListener(HoloTools plugin) {
        this.plugin = plugin;
        this.shieldManager = plugin.getShieldManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getHand() == EquipmentSlot.OFF_HAND && shieldManager.isHoloShield(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (!shieldManager.isHoloShield(item)) return;

        event.setCancelled(true);

        if (shieldManager.hasActiveShield(player)) {
            shieldManager.stopShield(player);
            player.sendMessage(plugin.getUtils().color(plugin.getLangManager().SHIELD_DEACTIVATED));
        } else {
            if (shieldManager.isOnCooldown(player)) {
                int rem = shieldManager.getRemainingCooldown(player);
                player.sendMessage(plugin.getUtils().color(String.format(plugin.getLangManager().SHIELD_COOLDOWN, rem)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            } else {
                shieldManager.startShield(player);
                player.sendMessage(plugin.getUtils().color(plugin.getLangManager().SHIELD_ACTIVATED));
            }
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (shieldManager.hasActiveShield(player) && !shieldManager.isHoldingShield(player)) {
                shieldManager.stopShield(player);
                player.sendMessage(plugin.getUtils().color(plugin.getLangManager().SHIELD_DEACTIVATED));
            }
        });
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (shieldManager.hasActiveShield(player) && !shieldManager.isHoldingShield(player)) {
                shieldManager.stopShield(player);
                player.sendMessage(plugin.getUtils().color(plugin.getLangManager().SHIELD_DEACTIVATED));
            }
        });
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (shieldManager.isHoloShield(event.getItemDrop().getItemStack())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (shieldManager.hasActiveShield(player) && !shieldManager.isHoldingShield(player)) {
                    shieldManager.stopShield(player);
                    player.sendMessage(plugin.getUtils().color(plugin.getLangManager().SHIELD_DEACTIVATED));
                }
            });
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

        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            if (byEntityEvent.getDamager() instanceof Projectile projectile) {
                if (shieldManager.canReflectProjectiles(player)) {
                    shieldManager.reflectProjectile(player, projectile);
                }
            }
        }

        double reduction = shieldManager.getDamageReduction(player);
        if (reduction >= 1.0) {
            event.setCancelled(true);
            shieldManager.onDamageBlocked(player);
        } else if (reduction > 0.0) {
            double currentDamage = event.getDamage();
            double newDamage = Math.max(0.0, currentDamage * (1.0 - reduction));
            event.setDamage(newDamage);
            shieldManager.onDamageBlocked(player);
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
