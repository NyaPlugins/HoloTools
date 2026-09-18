package net.kokoricraft.holotools.objects.holowaypoint;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class WaypointData {
    private final int slot;
    private String name;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    public WaypointData(int slot, String name, String worldName, double x, double y, double z) {
        this.slot = slot;
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getSlot() {
        return slot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public double distanceTo(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return -1;
        }
        return loc.distance(new Location(loc.getWorld(), x, y, z));
    }

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("slot", slot);
        json.addProperty("name", name);
        json.addProperty("world", worldName);
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("z", z);
        return json;
    }

    public static WaypointData fromJsonObject(JsonObject json) {
        if (json == null) return null;
        int slot = json.has("slot") ? json.get("slot").getAsInt() : 0;
        String name = json.has("name") ? json.get("name").getAsString() : "Waypoint";
        String world = json.has("world") ? json.get("world").getAsString() : "world";
        double x = json.has("x") ? json.get("x").getAsDouble() : 0;
        double y = json.has("y") ? json.get("y").getAsDouble() : 64;
        double z = json.has("z") ? json.get("z").getAsDouble() : 0;
        return new WaypointData(slot, name, world, x, y, z);
    }
}
