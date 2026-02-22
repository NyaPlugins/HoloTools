package net.kokoricraft.holotools.objects.holobridge;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.version.HoloBlockDisplay;
import net.kokoricraft.holotools.version.HoloShulker;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Bridge {

    private final Player player;
    private final Location from;
    private final Location to;
    private final List<HoloBlockDisplay> entities = new ArrayList<>();
    private final List<HoloShulker> shulkers = new ArrayList<>();
    private BukkitRunnable particleTask;
    private final HoloTools plugin = HoloTools.getInstance();
    private final Material material;

    public Bridge(Player player, Location from, Location to, Material material) {
        this.player = player;
        this.from = from.clone();
        this.to = to.clone();
        this.material = material;
    }

    public boolean isPossible() {
        if (from == null || to == null) return false;
        if (!Objects.equals(from.getWorld(), to.getWorld())) return false;

        double distance = from.distance(to);
        if (distance > 50) {
            player.sendMessage(plugin.getUtils().color(plugin.getLangManager().BRIDGE_FAR_DISTANCE));
            return false;
        }

        Location l1 = from.clone();
        l1.setY(0);

        Location l2 = to.clone();
        l2.setY(0);

        double horizontal = l1.distance(l2);
        double height = Math.abs(from.getY() - to.getY());
        double heightPerStep = height / Math.max(1, horizontal);

        if (heightPerStep > 0.7) {
            player.sendMessage(plugin.getUtils().color(plugin.getLangManager().BRIDGE_STEEP_ANGLE));
        }

        return heightPerStep <= 0.7;
    }

    public void spawn() {
        if (!isPossible()) {
            return;
        }

        World world = from.getWorld();

        Vector dir = to.clone().subtract(from).toVector();
        double totalDist = dir.length();
        dir.normalize();

        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        Location start = from.clone().add(right.multiply(1.0));

        double stepLength = 1.0;
        int steps = (int) Math.ceil(totalDist / stepLength);

        double heightDiff = to.getY() - start.getY();
        double heightPerStep = heightDiff / steps;

        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));

        player.setVelocity(player.getVelocity().add(new Vector(0, 0.4, 0)));

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;

            Location loc = start.clone().add(dir.clone().multiply(i * stepLength));

            double baseY = start.getY() + heightPerStep * i;
            double curveY = Math.abs(heightPerStep) < 0.3 ? -Math.sin(t * Math.PI) * 0.3 : 0;

            loc.setY(baseY + curveY);
            loc.setYaw(yaw);

            HoloBlockDisplay display = plugin.getCompatManager().getCompat().createBlockDisplay(plugin.getManager().getHoloPlayerView(player), loc, 0, 0);
            display.setBrightness(new Brightness(15, 15));
            display.setBillboard(Billboard.FIXED);
            display.setScale(2, 0.6f, 1);
            display.setLeftRotation(new Quaternionf().rotationY((float) Math.toRadians(-yaw)));
            display.setTranslation(0, 3, 0);
            display.update();

            entities.add(display);

            Location center = loc.clone()
                    .add(right.clone().multiply(-1.0))
                    .add(dir.clone());

            Location shulkerSpawn = center.clone();
            shulkerSpawn.setY(loc.getY() - 1.4);

            HoloShulker shulker = plugin.getCompatManager().getCompat().createShulker(plugin.getManager().getHoloPlayerView(player), shulkerSpawn, 0, 0);
            shulker.setScale(2);
            shulker.update();

            shulkers.add(shulker);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                startStepAnimation(shulkers.size() * 3L);
            }
        }.runTaskLater(plugin, 2L);

        startParticleTask(world, start, dir, right, totalDist, steps, heightPerStep);
    }

    private void startStepAnimation(long totalTicks) {
        if (entities.isEmpty()) return;

        int count = entities.size();
        long ticksPerStep = Math.max(1, totalTicks / count);

        new BukkitRunnable() {

            int index = 0;

            @Override
            public void run() {
                if (index >= count) {
                    cancel();
                    return;
                }

                HoloBlockDisplay display = entities.get(index);

                display.setBlock(material);
                display.setTranslation(0, 0.01f, 0);
                display.interpolation(-1, (int) ticksPerStep);
                display.update();

                to.getWorld().playSound(display.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1f, 1f);

                index++;
            }

        }.runTaskTimer(plugin, 0L, ticksPerStep);
    }

    private void startParticleTask(World world, Location start, Vector dir, Vector right,
                                   double totalDist, int steps, double heightPerStep) {

        if (particleTask != null) particleTask.cancel();

        particleTask = new BukkitRunnable() {

            final double offsetY = 0.5;
            final double lateralDistance = 1;
            final int density = 2;

            @Override
            public void run() {
                if (entities.isEmpty() || !player.isOnline()) {
                    cancel();
                    return;
                }

                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i <= steps * density; i++) {

                        double t = i / (double) (steps * density);

                        Location loc = start.clone().add(dir.clone().multiply(t * totalDist));

                        double baseY = start.getY() + heightPerStep * (t * steps);
                        double curveY = Math.abs(heightPerStep) < 0.3
                                ? -Math.sin(t * Math.PI) * 0.3
                                : 0;

                        loc.setY(baseY + curveY + offsetY);

                        loc.add(right.clone().multiply(lateralDistance * side));
                        loc.add(right.clone().multiply(-1.0));

                        world.spawnParticle(Particle.DRAGON_BREATH, loc, 1, 0, 0, 0, 0, 1f);
                    }
                }
            }
        };

        particleTask.runTaskTimer(plugin, 0L, 10L);
    }

    public void remove() {
        entities.forEach(HoloBlockDisplay::remove);
        shulkers.forEach(HoloShulker::remove);

        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }

        entities.clear();
        shulkers.clear();
    }
}