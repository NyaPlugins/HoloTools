package net.kokoricraft.holotools.managers;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.objects.holoshield.HoloShield;
import net.kokoricraft.holotools.objects.holoshield.ShieldProfile;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ShieldManager {
    private final HoloTools plugin;
    public final NamespacedKey KEY;
    private final Map<Player, HoloShield> activeShields = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> recentlyDeflected = new HashSet<>();

    public ShieldManager(HoloTools plugin) {
        this.plugin = plugin;
        this.KEY = new NamespacedKey(plugin, "holoshield");
        startTrackingTask();
    }

    public boolean isHoloShield(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(KEY, PersistentDataType.STRING);
    }

    public boolean isHoldingShield(Player player) {
        return isHoloShield(player.getInventory().getItemInMainHand()) || isHoloShield(player.getInventory().getItemInOffHand());
    }

    public ShieldProfile getShieldProfile(Player player) {
        List<ShieldProfile> profiles = plugin.getConfigManager().shieldProfiles;
        ShieldProfile selected = null;

        for (ShieldProfile profile : profiles) {
            if (profile.needPermission()) {
                if (profile.permission() == null || profile.permission().isEmpty()) continue;
                if (!player.hasPermission(profile.permission())) continue;
            }

            if (selected == null || profile.priority() > selected.priority()) {
                selected = profile;
            }
        }

        if (selected == null && !profiles.isEmpty()) {
            selected = profiles.get(0);
        }

        return selected;
    }

    public boolean isOnCooldown(Player player) {
        Long expire = cooldowns.get(player.getUniqueId());
        if (expire == null) return false;
        if (System.currentTimeMillis() >= expire) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public int getRemainingCooldown(Player player) {
        Long expire = cooldowns.get(player.getUniqueId());
        if (expire == null) return 0;
        long diff = expire - System.currentTimeMillis();
        return diff > 0 ? (int) Math.ceil(diff / 1000.0) : 0;
    }

    public boolean canActivate(Player player) {
        return !hasActiveShield(player) && !isOnCooldown(player);
    }

    public void startShield(Player player) {
        if (hasActiveShield(player)) return;

        if (isOnCooldown(player)) {
            int remaining = getRemainingCooldown(player);
            player.sendMessage(plugin.getUtils().color(String.format("&cEl HoloShield está en enfriamiento: &e%ds", remaining)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        ShieldProfile profile = getShieldProfile(player);
        if (profile == null) return;

        HoloShield shield = new HoloShield(player, profile);
        shield.spawn();
        activeShields.put(player, shield);

        if (plugin.getConfigManager().SHIELD_SOUND) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.5f);
        }
    }

    public void stopShield(Player player) {
        HoloShield shield = activeShields.remove(player);
        if (shield != null) {
            shield.remove();
            ShieldProfile profile = shield.getProfile();
            if (profile != null && profile.cooldown() > 0) {
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + profile.cooldown() * 1000L);
            }
            if (plugin.getConfigManager().SHIELD_SOUND && player.isOnline()) {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.8f);
            }
        }
    }

    public boolean hasActiveShield(Player player) {
        return activeShields.containsKey(player);
    }

    public HoloShield getActiveShield(Player player) {
        return activeShields.get(player);
    }

    public double getDamageReduction(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        if (cfg.SHIELD_PERM_REDUCTION_ENABLED && !cfg.SHIELD_PERM_REDUCTIONS.isEmpty()) {
            double highest = -1.0;
            boolean matched = false;
            for (Map.Entry<String, Double> entry : cfg.SHIELD_PERM_REDUCTIONS.entrySet()) {
                if (player.hasPermission(entry.getKey())) {
                    matched = true;
                    if (entry.getValue() > highest) {
                        highest = entry.getValue();
                    }
                }
            }
            if (matched) {
                return Math.max(0.0, Math.min(1.0, highest));
            }
        }

        ShieldProfile profile = getShieldProfile(player);
        if (profile != null) {
            return Math.max(0.0, Math.min(1.0, profile.damageReduction()));
        }

        return Math.max(0.0, Math.min(1.0, cfg.SHIELD_DEFAULT_DAMAGE_REDUCTION));
    }

    public boolean canReflectProjectiles(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.SHIELD_REFLECT_PROJECTILES) {
            return false;
        }

        ShieldProfile profile = getShieldProfile(player);
        if (profile != null && !profile.reflectProjectiles()) {
            return false;
        }

        if (cfg.SHIELD_REFLECT_NEED_PERM) {
            if (cfg.SHIELD_REFLECT_PERMISSIONS != null && !cfg.SHIELD_REFLECT_PERMISSIONS.isEmpty()) {
                boolean hasAny = false;
                for (String perm : cfg.SHIELD_REFLECT_PERMISSIONS) {
                    if (player.hasPermission(perm)) {
                        hasAny = true;
                        break;
                    }
                }
                return hasAny;
            } else if (cfg.SHIELD_REFLECT_PERM != null && !cfg.SHIELD_REFLECT_PERM.isEmpty()) {
                return player.hasPermission(cfg.SHIELD_REFLECT_PERM);
            }
        }

        return true;
    }

    public void onDamageBlocked(Player player) {
        HoloShield shield = activeShields.get(player);
        if (shield != null) {
            shield.onDamageBlocked();
        }
    }

    public void reflectProjectile(Player player, Projectile projectile) {
        if (recentlyDeflected.contains(projectile.getUniqueId())) return;
        recentlyDeflected.add(projectile.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyDeflected.remove(projectile.getUniqueId()), 20L);

        double mult = plugin.getConfigManager().SHIELD_SPEED;
        Vector reflection;

        if (projectile.getShooter() instanceof LivingEntity shooter && !shooter.equals(player)) {
            Location eye = shooter.getEyeLocation();
            reflection = eye.subtract(projectile.getLocation()).toVector().normalize();
            double speed = Math.max(1.6, projectile.getVelocity().length() * mult);
            reflection.multiply(speed);
        } else {
            reflection = projectile.getVelocity().multiply(-mult);
        }

        if (Double.isNaN(reflection.getX()) || reflection.lengthSquared() < 0.001) {
            reflection = player.getLocation().getDirection().multiply(1.6);
        }

        try {
            Location forward = projectile.getLocation().add(reflection.clone().normalize().multiply(0.6));
            projectile.teleport(forward);
        } catch (Exception ignored) {}

        projectile.setVelocity(reflection);
        projectile.setShooter(player);

        String msg = plugin.getLangManager().SHIELD_DEFLECTED;
        if (!msg.isBlank()) {
            player.sendMessage(plugin.getUtils().color(msg));
        }

        Location impact = projectile.getLocation();
        World world = player.getWorld();

        if (plugin.getConfigManager().SHIELD_SOUND) {
            world.playSound(impact, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f);
            world.playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 1.8f);
        }

        if (plugin.getConfigManager().SHIELD_PARTICLES) {
            world.spawnParticle(Particle.ELECTRIC_SPARK, impact, 15, 0.25, 0.25, 0.25, 0.1);
            world.spawnParticle(Particle.CRIT, impact, 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private void startTrackingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeShields.isEmpty()) return;

                Iterator<Map.Entry<Player, HoloShield>> it = activeShields.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Player, HoloShield> entry = it.next();
                    Player player = entry.getKey();
                    HoloShield shield = entry.getValue();

                    if (!player.isOnline() || player.isDead()) {
                        shield.remove();
                        it.remove();
                        continue;
                    }

                    shield.tick();

                    if (shield.isExpired()) {
                        shield.remove();
                        it.remove();
                        ShieldProfile profile = shield.getProfile();
                        if (profile != null && profile.cooldown() > 0) {
                            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + profile.cooldown() * 1000L);
                        }
                        if (plugin.getConfigManager().SHIELD_SOUND) {
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.8f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
