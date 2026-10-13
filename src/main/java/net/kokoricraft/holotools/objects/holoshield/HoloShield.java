package net.kokoricraft.holotools.objects.holoshield;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.kokoricraft.holotools.version.HoloTextDisplay;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class HoloShield {
    private final Player player;
    private final ShieldProfile profile;
    private final List<HoloTextDisplay> displays = new ArrayList<>();
    private final HoloTools plugin = HoloTools.getInstance();
    private boolean active = false;
    private int remainingTicks;
    private final int warningTicks = 60;
    private final float radius = 1.3f;
    private final float scaleX = 20.8f;
    private final float scaleY = 10.4f;

    public HoloShield(Player player, ShieldProfile profile) {
        this.player = player;
        this.profile = profile;
        this.remainingTicks = profile.duration() * 20;
    }

    public void spawn() {
        if (active) return;
        Location loc = player.getLocation();
        List<Player> players = plugin.getManager().getHoloPlayerView(player);
        float baseYaw = loc.getYaw();
        float yCenter = -0.6f;
        float eps = 0.001f;

        spawnFace(players, loc, baseYaw, 0, yCenter, radius + eps, 0, 0, 0);
        spawnFace(players, loc, baseYaw, 0, yCenter, radius - eps, 0, 180f, 0);

        spawnFace(players, loc, baseYaw, 0, yCenter, -radius - eps, 0, 180f, 0);
        spawnFace(players, loc, baseYaw, 0, yCenter, -radius + eps, 0, 0, 0);

        spawnFace(players, loc, baseYaw, radius + eps, yCenter, 0, 0, -90f, 0);
        spawnFace(players, loc, baseYaw, radius - eps, yCenter, 0, 0, 90f, 0);

        spawnFace(players, loc, baseYaw, -radius - eps, yCenter, 0, 0, 90f, 0);
        spawnFace(players, loc, baseYaw, -radius + eps, yCenter, 0, 0, -90f, 0);

        spawnFace(players, loc, baseYaw, 0, yCenter + radius + eps, 0, -90f, 0, 0);
        spawnFace(players, loc, baseYaw, 0, yCenter + radius - eps, 0, 90f, 0, 0);

        spawnFace(players, loc, baseYaw, 0, yCenter - radius - eps, 0, 90f, 0, 0);
        spawnFace(players, loc, baseYaw, 0, yCenter - radius + eps, 0, -90f, 0, 0);

        active = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active && player.isOnline()) {
                displays.forEach(d -> d.mount(player));
            }
        }, 1L);
    }

    private void spawnFace(List<Player> players, Location loc, float yaw, float targetX, float targetY, float targetZ, float rx, float ry, float rz) {
        Quaternionf q = new Quaternionf().rotateXYZ(
                (float) Math.toRadians(rx),
                (float) Math.toRadians(ry),
                (float) Math.toRadians(rz)
        );
        Vector3f cScaled = new Vector3f(0.1f * 2.6f, 0.5f * 2.6f, 0.0f);
        Vector3f cRot = q.transform(new Vector3f(cScaled));

        float tx = targetX - cRot.x();
        float ty = targetY - cRot.y();
        float tz = targetZ - cRot.z();

        HoloTextDisplay display = plugin.getCompatManager().getCompat().createTextDisplay(
                players, loc, 0, yaw
        );
        display.setText(" ");
        display.setBrightness(new Display.Brightness(15, 15));
        display.setScale(scaleX, scaleY, 1.0f);
        display.setRotation(rx, ry, rz);
        display.setTranslation(tx, ty, tz);
        display.setColor(profile.color());
        display.update();
        display.mount(player);
        displays.add(display);
    }

    public void tick() {
        if (!active) return;
        remainingTicks--;

        if (remainingTicks <= warningTicks) {
            HoloColor c = ((remainingTicks / 6) % 2 == 0) ? profile.warningColor() : profile.color();
            for (HoloTextDisplay d : displays) {
                d.setColor(c);
                d.interpolation(-1, 4);
                d.update();
            }
        }
    }

    public void onDamageBlocked() {
        World w = player.getWorld();
        Location loc = player.getLocation().add(0, 1, 0);
        if (plugin.getConfigManager().SHIELD_PARTICLES) {
            w.spawnParticle(Particle.ELECTRIC_SPARK, loc, 20, 0.8, 0.8, 0.8, 0.15);
            w.spawnParticle(Particle.CRIT, loc, 12, 0.5, 0.5, 0.5, 0.1);
        }
        if (plugin.getConfigManager().SHIELD_SOUND) {
            w.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.3f);
            w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 1.8f);
        }
    }

    public void remove() {
        if (!active) return;
        displays.forEach(HoloTextDisplay::remove);
        displays.clear();
        active = false;
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    public boolean isActive() {
        return active;
    }

    public Player getPlayer() {
        return player;
    }

    public ShieldProfile getProfile() {
        return profile;
    }
}
