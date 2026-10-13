package net.kokoricraft.holotools.objects.holowaypoint;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.enums.HoloActionType;
import net.kokoricraft.holotools.interfaces.HoloBase;
import net.kokoricraft.holotools.objects.halo.HaloSlot;
import net.kokoricraft.holotools.objects.halo.Holo;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.kokoricraft.holotools.version.HoloTextDisplay;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HoloWaypoint extends Holo implements HoloBase {
    private final HoloTools plugin = HoloTools.getInstance();
    private final Map<Integer, WaypointData> waypoints = new ConcurrentHashMap<>();
    private final Map<Integer, HoloTextDisplay> markerDisplays = new ConcurrentHashMap<>();
    private final HoloColor defaultColor = HoloColor.fromARGB(140, 20, 35, 55);
    private int updateTimer = 0;

    public HoloWaypoint(Player player, Map<Integer, WaypointData> waypointMap, ItemStack itemStack, int size) {
        super(size, -1.5f, itemStack);
        this.player = player;

        if (waypointMap != null) {
            this.waypoints.putAll(waypointMap);
        }

        for (int key : slots.keySet()) {
            HaloSlot slot = slots.get(key);
            slot.setColor(defaultColor);
        }
    }

    @Override
    public void tick() {
        super.tick();

        updateTimer++;
        if (updateTimer % 2 == 0) {
            updateWaypointMarkers();
        }
    }

    private void updateWaypointMarkers() {
        if (player == null || !player.isOnline() || !visible || shouldRemove) return;

        Location pLoc = player.getLocation();
        World world = pLoc.getWorld();
        if (world == null) return;

        double px = pLoc.getX();
        double py = pLoc.getY();
        double pz = pLoc.getZ();

        Set<Integer> currentKeys = new HashSet<>();

        for (WaypointData wp : waypoints.values()) {
            if (wp == null) continue;
            if (!wp.getWorldName().equals(world.getName())) continue;
            if (!visible || shouldRemove) return;

            currentKeys.add(wp.getSlot());

            double dx = wp.getX() - px;
            double dy = wp.getY() - py;
            double dz = wp.getZ() - pz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            float angle = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float relAngle = (angle - initial_yaw + 360.0f) % 360.0f;
            float r = 1.95f;
            float tx = (float) (r * Math.sin(Math.toRadians(relAngle)));
            float tz = (float) (-r * Math.cos(Math.toRadians(relAngle)));
            float normY = Math.max(-1.0f, Math.min(1.0f, (float) dy / 30.0f));
            float ty = -0.50f + normY * 0.60f;

            HoloTextDisplay display = markerDisplays.get(wp.getSlot());
            if (display == null) {
                if (!visible || shouldRemove) return;
                List<Player> players = List.of(player);
                display = plugin.getCompatManager().getCompat().createTextDisplay(
                        players, player.getLocation(), 0, initial_yaw + 180.0f
                );
                display.setScale(0.35f, 0.35f, 0.35f);
                display.setLineWidth(200);
                display.setBrightness(new Display.Brightness(15, 15));
                display.setColor(HoloColor.fromARGB(150, 10, 20, 35));
                display.mount(player);
                markerDisplays.put(wp.getSlot(), display);
            }

            display.setTranslation(tx, ty, tz);
            display.setRotation(0, -relAngle, 0);
            display.setText(plugin.getUtils().color(String.format("&#00d2ff✦ &#4facfe&l%s &#00d2ff✦\n&#f5f6fa%dm &#a1c4fd(Y:%d)", wp.getName(), (int) dist, (int) wp.getY())));
            display.interpolation(-1, 2);
            display.update();
        }

        markerDisplays.entrySet().removeIf(entry -> {
            if (!currentKeys.contains(entry.getKey())) {
                if (entry.getValue() != null) {
                    entry.getValue().remove();
                }
                return true;
            }
            return false;
        });
    }

    public void addWaypoint(Location loc) {
        int nextSlot = 0;
        while (waypoints.containsKey(nextSlot)) {
            nextSlot++;
        }

        String name = "Punto #" + (nextSlot + 1);
        WaypointData wp = new WaypointData(nextSlot, name, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        waypoints.put(nextSlot, wp);
        plugin.getDataManager().saveHoloWaypoint(itemStack, this, "add waypoint");
        updateWaypointMarkers();
    }

    public void renameWaypoint(int slot, String newName) {
        WaypointData wp = waypoints.get(slot);
        if (wp != null) {
            wp.setName(newName);
            plugin.getDataManager().saveHoloWaypoint(itemStack, this, "rename waypoint");
            updateWaypointMarkers();
        }
    }

    public void removeWaypoint(int slot) {
        waypoints.remove(slot);
        plugin.getDataManager().saveHoloWaypoint(itemStack, this, "remove waypoint");
        HoloTextDisplay display = markerDisplays.remove(slot);
        if (display != null) {
            display.remove();
        }
        updateWaypointMarkers();
    }

    public Map<Integer, WaypointData> getWaypoints() {
        return waypoints;
    }

    @Override
    public void onChangeSlot(int fromSlot, int toSlot) {}

    @Override
    public void onClick() {
        super.onClick();
        HoloWaypointMenu.open(player, this);
    }

    @Override
    public void onAction(HoloActionType type) {}

    @Override
    public int getSlot() {
        return getPlayerSlot();
    }

    @Override
    public void setVisible(boolean visible) {
        if (!this.visible && visible)
            plugin.getTickManager().addTickable(this);

        this.visible = visible;

        if (visible) {
            this.shouldRemove = false;
            spawn(player);
            updateWaypointMarkers();
        } else {
            this.shouldRemove = true;
            remove("remove set visible false waypoint");
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void remove(String reason) {
        this.visible = false;
        this.shouldRemove = true;
        for (HoloTextDisplay display : markerDisplays.values()) {
            if (display != null) {
                display.remove();
            }
        }
        markerDisplays.clear();
        super.remove(reason);
    }
}
