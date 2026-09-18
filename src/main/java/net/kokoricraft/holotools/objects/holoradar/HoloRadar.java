package net.kokoricraft.holotools.objects.holoradar;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.enums.HoloActionType;
import net.kokoricraft.holotools.interfaces.HoloBase;
import net.kokoricraft.holotools.objects.halo.HaloSlot;
import net.kokoricraft.holotools.objects.halo.Holo;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.kokoricraft.holotools.version.HoloTextDisplay;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HoloRadar extends Holo implements HoloBase {
    private static final Map<Long, List<CachedOre>> chunkCache = new ConcurrentHashMap<>();
    private final HoloTools plugin = HoloTools.getInstance();
    private final HoloColor defaultUnselected;
    private final int radius;
    private final int scanInterval;
    private int scanTimer = 0;
    private int tickCount = 0;
    private final Map<String, BlipHolder> activeBlips = new ConcurrentHashMap<>();

    public record CachedOre(int x, int y, int z, OreProfile ore) {}

    private static class BlipHolder {
        HoloTextDisplay display;
        OreProfile ore;
        double distance;
        float relAngle;
        float targetY;

        BlipHolder(HoloTextDisplay display, OreProfile ore, double distance, float relAngle, float targetY) {
            this.display = display;
            this.ore = ore;
            this.distance = distance;
            this.relAngle = relAngle;
            this.targetY = targetY;
        }
    }

    public HoloRadar(Player player, ItemStack itemStack, int size) {
        super(size, -1.5f, itemStack);
        this.player = player;
        this.radius = plugin.getConfigManager().RADAR_RADIUS;
        this.scanInterval = plugin.getConfigManager().RADAR_SCAN_INTERVAL;

        HoloColor unselected = plugin.getConfigManager().RADAR_DEFAULT_UNSELECTED;
        this.defaultUnselected = (unselected != null) ? unselected : HoloColor.fromARGB(130, 16, 32, 48);

        for (int key : slots.keySet()) {
            HaloSlot slot = slots.get(key);
            slot.setColor(defaultUnselected);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (scanTimer <= 0) {
            scanOres();
            scanTimer = scanInterval;
        } else {
            scanTimer--;
        }

        tickCount++;
        boolean blink = (tickCount % 12) < 6;

        for (BlipHolder holder : activeBlips.values()) {
            if (holder.display == null || holder.ore == null) continue;

            if (blink) {
                holder.display.setScale(0.4f, 0.4f, 0.4f);
                holder.display.setTextOpacity((byte) -1);
                holder.display.setText(plugin.getUtils().color("&l■"));
                holder.display.setColor(holder.ore.color());
            } else {
                holder.display.setScale(0.0001f, 0.0001f, 0.0001f);
                holder.display.setTextOpacity((byte) 0);
                holder.display.setText("");
                holder.display.setColor(HoloColor.fromARGB(0, 0, 0, 0));
            }
            holder.display.update();
        }
    }

    private void scanOres() {
        if (player == null || !player.isOnline() || !visible || shouldRemove) return;

        World world = player.getWorld();
        Location pLoc = player.getLocation();
        double px = pLoc.getX();
        double py = pLoc.getY();
        double pz = pLoc.getZ();
        int bx = pLoc.getBlockX();
        int by = pLoc.getBlockY();
        int bz = pLoc.getBlockZ();

        List<OreProfile> oreProfiles = plugin.getConfigManager().RADAR_ORES;
        if (oreProfiles.isEmpty()) {
            clearDisplays();
            return;
        }

        int r = Math.min(radius, 28);
        int minCX = (bx - r) >> 4;
        int maxCX = (bx + r) >> 4;
        int minCZ = (bz - r) >> 4;
        int maxCZ = (bz + r) >> 4;

        List<CachedOre> candidateOres = new ArrayList<>();

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                if (!visible || shouldRemove) return;
                long key = (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
                List<CachedOre> inChunk = chunkCache.get(key);
                if (inChunk == null) {
                    if (!world.isChunkLoaded(cx, cz)) continue;
                    Chunk chunk = world.getChunkAt(cx, cz);
                    inChunk = scanChunk(chunk, world, oreProfiles);
                    chunkCache.put(key, inChunk);
                }
                candidateOres.addAll(inChunk);
            }
        }

        Set<String> disabledOres = HoloRadarMenu.getDisabledOres(player);
        Map<String, OreHit> foundOres = new HashMap<>();
        double rSq = (double) r * r;

        for (CachedOre core : candidateOres) {
            if (disabledOres.contains(core.ore().key())) continue;

            double dx = core.x() + 0.5 - px;
            double dy = core.y() + 0.5 - py;
            double dz = core.z() + 0.5 - pz;
            double distSq = dx * dx + dz * dz;

            if (distSq <= rSq && Math.abs(dy) <= 15) {
                double dist = Math.sqrt(distSq + dy * dy);
                float exactAngle = (float) Math.toDegrees(Math.atan2(-dx, dz));
                String id = core.x() + ":" + core.y() + ":" + core.z();
                foundOres.put(id, new OreHit(id, core.x(), core.y(), core.z(), core.ore(), dist, exactAngle, dy));
            }
        }

        updateRadarDisplays(foundOres);
    }

    private void updateRadarDisplays(Map<String, OreHit> foundOres) {
        if (!visible || shouldRemove) return;
        List<Player> players = plugin.getManager().getHoloPlayerView(player);
        float rRing = 1.95f;

        activeBlips.entrySet().removeIf(entry -> {
            if (!foundOres.containsKey(entry.getKey())) {
                if (entry.getValue() != null && entry.getValue().display != null) {
                    entry.getValue().display.remove();
                }
                return true;
            }
            return false;
        });

        for (Map.Entry<String, OreHit> entry : foundOres.entrySet()) {
            if (!visible || shouldRemove) return;
            String id = entry.getKey();
            OreHit hit = entry.getValue();

            float relAngle = (hit.exactAngle() - initial_yaw + 360.0f) % 360.0f;
            float tx = (float) (rRing * Math.sin(Math.toRadians(relAngle)));
            float tz = (float) (-rRing * Math.cos(Math.toRadians(relAngle)));
            float normY = Math.max(-1.0f, Math.min(1.0f, (float) hit.deltaY() / 15.0f));
            float ty = -0.50f + normY * 0.65f;

            BlipHolder holder = activeBlips.get(id);
            if (holder == null) {
                HoloTextDisplay display = plugin.getCompatManager().getCompat().createTextDisplay(
                        players, player.getLocation(), 0, initial_yaw + 180.0f
                );
                display.setScale(0.4f, 0.4f, 0.4f);
                display.setLineWidth(200);
                display.setBrightness(new Display.Brightness(15, 15));
                display.setTranslation(tx, ty, tz);
                display.setRotation(0, -relAngle, 0);
                display.setColor(hit.ore().color());
                display.setText(plugin.getUtils().color("&l■"));
                display.update();
                display.mount(player);

                holder = new BlipHolder(display, hit.ore(), hit.dist(), relAngle, ty);
                activeBlips.put(id, holder);
            } else {
                holder.ore = hit.ore();
                holder.distance = hit.dist();
                holder.relAngle = relAngle;
                holder.targetY = ty;

                holder.display.setTranslation(tx, ty, tz);
                holder.display.setRotation(0, -relAngle, 0);
                holder.display.setColor(hit.ore().color());
                holder.display.setText(plugin.getUtils().color("&l■"));
                holder.display.interpolation(-1, 3);
                holder.display.update();
            }
        }
    }

    private List<CachedOre> scanChunk(Chunk chunk, World world, List<OreProfile> oreProfiles) {
        List<CachedOre> list = new ArrayList<>();
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();
        int chunkStartX = chunk.getX() << 4;
        int chunkStartZ = chunk.getZ() << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    Block b = chunk.getBlock(x, y, z);
                    Material mat = b.getType();
                    if (mat == Material.AIR || mat == Material.STONE || mat == Material.DEEPSLATE || mat == Material.NETHERRACK) continue;

                    for (OreProfile ore : oreProfiles) {
                        if (ore.matches(mat)) {
                            list.add(new CachedOre(chunkStartX + x, y, chunkStartZ + z, ore));
                            break;
                        }
                    }
                }
            }
        }
        return list;
    }

    public static void invalidateChunk(Chunk chunk) {
        long key = (((long) chunk.getX()) << 32) | (chunk.getZ() & 0xFFFFFFFFL);
        chunkCache.remove(key);
    }

    private void clearDisplays() {
        for (BlipHolder holder : activeBlips.values()) {
            if (holder != null && holder.display != null) {
                holder.display.remove();
            }
        }
        activeBlips.clear();
    }

    private record OreHit(String id, int x, int y, int z, OreProfile ore, double dist, float exactAngle, double deltaY) {}

    @Override
    public void onChangeSlot(int fromSlot, int toSlot) {}

    @Override
    public void onClick() {
        super.onClick();
        HoloRadarMenu.open(player);
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
            scanOres();
        } else {
            this.shouldRemove = true;
            remove("remove set visible false radar");
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
        synchronized (activeBlips) {
            clearDisplays();
        }
        super.remove(reason);
    }
}
