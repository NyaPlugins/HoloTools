package net.kokoricraft.holotools.managers;

import com.google.gson.JsonObject;
import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.data.StorageConfig;
import net.kokoricraft.holotools.enums.HoloColors;
import net.kokoricraft.holotools.enums.HoloType;
import net.kokoricraft.holotools.enums.StorageMode;
import net.kokoricraft.holotools.enums.StorageType;
import net.kokoricraft.holotools.objects.colors.DualColor;
import net.kokoricraft.holotools.objects.NekoConfig;
import net.kokoricraft.holotools.objects.NekoItem;
import net.kokoricraft.holotools.objects.colors.HoloPanelsColors;
import net.kokoricraft.holotools.objects.holobridge.BridgeProfile;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final HoloTools plugin;
    public String LANG = "en";
    public boolean HOLO_VISIBLE_FOR_EVERYONE;
    public boolean UPDATE_CHECKER;
    public NekoItem CRAFTER_ITEM;
    public List<HoloPanelsColors> CRAFTER_PANELS_COLORS;
    public NekoItem WARDROBE_ITEM;
    public List<HoloPanelsColors> WARDROBE_PANELS_COLORS;
    public boolean TOOLTIP_ENABLED;
    public StorageMode STORAGE_MODE = StorageMode.ITEM;
    public StorageType STORAGE_TYPE = StorageType.YAML;
    public StorageConfig STORAGE_CONFIG;
    public Map<HoloType, Integer> DEFAULT_SLOTS = new HashMap<>();

    public List<BridgeProfile> bridgeProfiles = new ArrayList<>();
    public NekoItem BRIDGE_ITEM;
    public int BRIDGE_DURATION = 30;

    public static final DualColor DUAL_COLOR_DEF = new DualColor(HoloColors.WHITE.getColor(), HoloColors.WHITE_SELECTED.getColor());

    public ConfigManager(HoloTools plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        // Config.yml configuration.
        NekoConfig config = new NekoConfig("config.yml", plugin);

        LANG = config.getString("lang", "en");
        HOLO_VISIBLE_FOR_EVERYONE = config.getBoolean("holos.visible_for_everyone", false);

        STORAGE_MODE = StorageMode.valueOf(config.getString("storage.mode", "item").toUpperCase());
        STORAGE_TYPE = StorageType.valueOf(config.getString("storage.type", "yaml").toUpperCase());

        if (!config.contains("storage.config"))
            generateDefaultStorageConfig(config);

        STORAGE_CONFIG = new StorageConfig(config.getConfigurationSection("storage.config"));

        UPDATE_CHECKER = config.getBoolean("update_checker", true);
        config.update();

        //Crafter configuration.

        NekoConfig crafter = new NekoConfig("holocrafter.yml", plugin);
        CRAFTER_ITEM = new NekoItem(plugin, crafter.getConfigurationSection("item"));
        CRAFTER_ITEM.setTag("holo", "yes");
        CRAFTER_ITEM.setTag("holo_crafter", new JsonObject().toString());

        DEFAULT_SLOTS.put(HoloType.HOLOCRAFTER, crafter.getInt("default_slots", 8));

        if (!crafter.contains("colors_lists"))
            generateDefaultConfig(crafter, HoloType.HOLOCRAFTER);

        CRAFTER_PANELS_COLORS = getPanelsColors(crafter);

        crafter.update();

        //Wardrobe configuration.

        NekoConfig wardrobe = new NekoConfig("holowardrobe.yml", plugin);
        WARDROBE_ITEM = new NekoItem(plugin, wardrobe.getConfigurationSection("item"));
        WARDROBE_ITEM.setTag("holo", "yes");
        WARDROBE_ITEM.setTag("holo_wardrobe", new JsonObject().toString());

        DEFAULT_SLOTS.put(HoloType.HOLOWARDROBE, crafter.getInt("default_slots", 8));

        TOOLTIP_ENABLED = wardrobe.getBoolean("tooltip.enabled", true);

        if (!wardrobe.contains("colors_lists"))
            generateDefaultConfig(wardrobe, HoloType.HOLOWARDROBE);

        WARDROBE_PANELS_COLORS = getPanelsColors(wardrobe);

        wardrobe.update();

        //Bridge configuration.

        NekoConfig bridge = new NekoConfig("holobridge.yml", plugin);

        BRIDGE_ITEM = new NekoItem(plugin, bridge.getConfigurationSection("item"));
        BRIDGE_ITEM.setTag("holobridge", "yes");
        BRIDGE_ITEM.setDefaultMaterial(Material.TRIDENT);
        BRIDGE_ITEM.addEnchant(Enchantment.LOYALTY, 3);
        BRIDGE_ITEM.setUnbreakable(true);

        BRIDGE_DURATION = bridge.getInt("default_duration", 30);
        bridgeProfiles.clear();

        ConfigurationSection bridgeSection = bridge.getConfigurationSection("profiles");
        if (bridgeSection != null) {
            for (String key : bridgeSection.getKeys(false)) {
                int priority = config.getConfig().getInt("profiles." + key + ".priority", 0);
                boolean needPermission = config.getConfig().getBoolean("profiles." + key + ".need_permission", false);
                String permission = config.contains("profiles." + key + ".permission") ? config.getString("profiles." + key + ".permission") : "holotools.holobridge." + key;
                int duration = config.contains("profiles." + key + ".duration") ? config.getInt("profiles." + key + ".duration", 0) : BRIDGE_DURATION;
                Material material = Material.valueOf(config.getString("profiles." + key + ".material", "WHITE_STAINED_GLASS").toUpperCase());
                BridgeProfile bridgeProfile = new BridgeProfile(BRIDGE_ITEM.getItem(), needPermission, permission, duration, priority, material);
                bridgeProfiles.add(bridgeProfile);
            }
        }

        bridge.update();
    }

    private void generateDefaultConfig(NekoConfig config, HoloType type) {
        //generate colors default config
        String default_color_path = "colors.default.";
        config.set(default_color_path + "need_permission", false);
        config.set(default_color_path + "permission", "holotools." + type.name().toLowerCase() + ".color.default");
        config.set(default_color_path + "slots.default_slot", "blue");

        for (int i = 0; i < 8; i++) {
            if (i != 0 && i != 2 && i != 4 && i != 6) continue;
            config.set(default_color_path + "slots.slot_" + i, "light_blue");
        }

        config.set("colors_lists.blue.unselected.hex", "#19a7d2");
        config.set("colors_lists.blue.unselected.alpha", 210);
        config.set("colors_lists.blue.selected.hex", "#28bae6");
        config.set("colors_lists.blue.selected.alpha", 230);

        config.set("colors_lists.light_blue.unselected.hex", "#24dce5");
        config.set("colors_lists.light_blue.unselected.alpha", 229);
        config.set("colors_lists.light_blue.selected.hex", "#5be4ec");
        config.set("colors_lists.light_blue.selected.alpha", 236);

        config.forceUpdate();
    }

    private void generateDefaultStorageConfig(NekoConfig config) {
        String path = "storage.config.";

        config.set(path + "host", "localhost");
        config.set(path + "port", "3306");
        config.set(path + "user", "user");
        config.set(path + "password", "password");
        config.set(path + "database", "database");
        config.forceUpdate();
    }

    private List<HoloPanelsColors> getPanelsColors(NekoConfig config) {
        List<HoloPanelsColors> colors = new ArrayList<>();

        Map<String, DualColor> dualColorMap = getColors(config);

        ConfigurationSection section = config.getConfigurationSection("colors");

        if (section != null) {
            for (String name : section.getKeys(false)) {
                Map<String, DualColor> slots_colors = new HashMap<>();
                boolean need_permission = section.getBoolean("need_permission", false);
                String permission = section.getString("permission");
                ConfigurationSection slotsSection = section.getConfigurationSection(name + ".slots");
                if (slotsSection != null) {
                    for (String slot : slotsSection.getKeys(false)) {
                        slots_colors.put(slot, dualColorMap.getOrDefault(slotsSection.getString(slot), null));
                    }
                }

                HoloPanelsColors holoPanelsColors = new HoloPanelsColors(name, slots_colors, need_permission, permission);
                colors.add(holoPanelsColors);
            }
        }

        return colors;
    }

    private Map<String, DualColor> getColors(NekoConfig config) {
        Map<String, DualColor> colorMap = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("colors_lists");

        if (section != null) {
            for (String name : section.getKeys(false)) {
                ConfigurationSection selectedSection = section.getConfigurationSection(name + ".selected");
                ConfigurationSection unselectedSection = section.getConfigurationSection(name + ".unselected");

                HoloColor selectedColor = parseColorSection(selectedSection);
                HoloColor unselectedColor = parseColorSection(unselectedSection);

                colorMap.put(name, new DualColor(selectedColor, unselectedColor));
            }
        }

        return colorMap;
    }

    private HoloColor parseColorSection(ConfigurationSection section) {
        if (section == null) return HoloColor.fromHex("#ffffff", 3);

        if (section.contains("sequence")) {
            List<Map<?, ?>> sequenceList = section.getMapList("sequence");
            if (sequenceList.isEmpty()) return HoloColor.fromHex("#ffffff", 3);

            Map<?, ?> first = sequenceList.get(0);
            String hex = String.valueOf(first.get("hex"));
            int alpha = parseInt(first.get("alpha"));
            HoloColor color = HoloColor.fromHex(hex, alpha);

            for (Map<?, ?> entry : sequenceList) {
                String entryHex = String.valueOf(entry.get("hex"));
                int entryAlpha = parseInt(entry.get("alpha"));
                int duration = parseInt(entry.get("duration"));

                HoloColor timed = HoloColor.fromHex(entryHex, entryAlpha);
                color.addToSequence(timed, duration);
            }

            return color;
        } else {
            String hex = section.getString("hex", "#ffffff");
            int alpha = section.getInt("alpha", 255);
            return HoloColor.fromHex(hex, alpha);
        }
    }

    private int parseInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return 255;
    }
}
