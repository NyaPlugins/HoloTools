package net.kokoricraft.holotools.managers;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.objects.NekoConfig;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class LangManager {
    private final HoloTools plugin;
    private final Map<String, YamlConfiguration> temporal_languages = new HashMap<>();

    public String CRAFTER_CLICK_TO_OPEN;
    public String CRAFTER_AUTO_ON;
    public String CRAFTER_AUTO_OFF;
    public String CRAFTER_CLICK_TO_CRAFT;
    public String CRAFTER_CLICK_TO_SAVE;
    public String CRAFTER_CLICK_TO_REMOVE;
    public String WARDROBE_CLICK_TO_SAVE;
    public String WARDROBE_CLICK_TO_SWAP;
    public String NO_PERMISSION;
    public String CONFIG_RELOADED;
    public String BRIDGE_FAR_DISTANCE;
    public String BRIDGE_STEEP_ANGLE;
    public String WAYPOINT_EMPTY_SLOT;
    public String WAYPOINT_CLICK_TO_SAVE;
    public String WAYPOINT_CLICK_TO_TRACK;
    public String WAYPOINT_CLICK_TO_DELETE;
    public String WAYPOINT_CREATED;
    public String WAYPOINT_DELETED;
    public String WAYPOINT_TRACKING;
    public String WAYPOINT_DISTANCE;
    public String WAYPOINT_WORLD;
    public String WAYPOINT_MENU_TITLE;
    public String WAYPOINT_SAVE_BTN;
    public String WAYPOINT_SAVE_LORE;
    public String WAYPOINT_RENAME_PROMPT;
    public String WAYPOINT_RENAME_CANCEL;
    public String WAYPOINT_RENAME_SUCCESS;
    public String WAYPOINT_RENAME_LORE;
    public String WAYPOINT_DELETE_LORE;
    public String RADAR_NO_ORE;
    public String RADAR_ORE_FOUND;
    public String RADAR_DISTANCE;
    public String RADAR_DEPTH;
    public String RADAR_MENU_TITLE;
    public String RADAR_FILTER_DETECTING;
    public String RADAR_FILTER_DISABLE_CLICK;
    public String RADAR_FILTER_IGNORED;
    public String RADAR_FILTER_ENABLE_CLICK;
    public String SHIELD_DEFLECTED;
    public String SHIELD_COOLDOWN;

    public LangManager(HoloTools plugin){
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages(){
        NekoConfig messages = new NekoConfig("languages/"+plugin.getConfigManager().LANG+".yml", plugin);

        CRAFTER_CLICK_TO_OPEN = getStringWithDefault(messages, "crafter.click_to_open");
        CRAFTER_AUTO_ON = getStringWithDefault(messages, "crafter.auto_craft_on");
        CRAFTER_AUTO_OFF = getStringWithDefault(messages, "crafter.auto_craft_off");
        CRAFTER_CLICK_TO_CRAFT = getStringWithDefault(messages, "crafter.click_to_craft");
        CRAFTER_CLICK_TO_SAVE = getStringWithDefault(messages, "crafter.click_to_save");
        CRAFTER_CLICK_TO_REMOVE = getStringWithDefault(messages, "crafter.click_to_remove");

        WARDROBE_CLICK_TO_SAVE = getStringWithDefault(messages, "wardrobe.click_to_save");
        WARDROBE_CLICK_TO_SWAP = getStringWithDefault(messages, "wardrobe.click_to_swap");

        BRIDGE_FAR_DISTANCE = getStringWithDefault(messages, "bridge.far_distance");
        BRIDGE_STEEP_ANGLE = getStringWithDefault(messages, "bridge.step_angle");

        WAYPOINT_EMPTY_SLOT = getStringWithDefault(messages, "waypoint.empty_slot");
        WAYPOINT_CLICK_TO_SAVE = getStringWithDefault(messages, "waypoint.click_to_save");
        WAYPOINT_CLICK_TO_TRACK = getStringWithDefault(messages, "waypoint.click_to_track");
        WAYPOINT_CLICK_TO_DELETE = getStringWithDefault(messages, "waypoint.click_to_delete");
        WAYPOINT_CREATED = getStringWithDefault(messages, "waypoint.created");
        WAYPOINT_DELETED = getStringWithDefault(messages, "waypoint.deleted");
        WAYPOINT_TRACKING = getStringWithDefault(messages, "waypoint.tracking");
        WAYPOINT_DISTANCE = getStringWithDefault(messages, "waypoint.distance");
        WAYPOINT_WORLD = getStringWithDefault(messages, "waypoint.world");
        WAYPOINT_MENU_TITLE = getStringWithDefault(messages, "waypoint.menu_title");
        WAYPOINT_SAVE_BTN = getStringWithDefault(messages, "waypoint.save_btn");
        WAYPOINT_SAVE_LORE = getStringWithDefault(messages, "waypoint.save_lore");
        WAYPOINT_RENAME_PROMPT = getStringWithDefault(messages, "waypoint.rename_prompt");
        WAYPOINT_RENAME_CANCEL = getStringWithDefault(messages, "waypoint.rename_cancel");
        WAYPOINT_RENAME_SUCCESS = getStringWithDefault(messages, "waypoint.rename_success");
        WAYPOINT_RENAME_LORE = getStringWithDefault(messages, "waypoint.rename_lore");
        WAYPOINT_DELETE_LORE = getStringWithDefault(messages, "waypoint.delete_lore");

        RADAR_NO_ORE = getStringWithDefault(messages, "radar.no_ore");
        RADAR_ORE_FOUND = getStringWithDefault(messages, "radar.ore_found");
        RADAR_DISTANCE = getStringWithDefault(messages, "radar.distance");
        RADAR_DEPTH = getStringWithDefault(messages, "radar.depth");
        RADAR_MENU_TITLE = getStringWithDefault(messages, "radar.menu_title");
        RADAR_FILTER_DETECTING = getStringWithDefault(messages, "radar.filter_detecting");
        RADAR_FILTER_DISABLE_CLICK = getStringWithDefault(messages, "radar.filter_disable_click");
        RADAR_FILTER_IGNORED = getStringWithDefault(messages, "radar.filter_ignored");
        RADAR_FILTER_ENABLE_CLICK = getStringWithDefault(messages, "radar.filter_enable_click");

        SHIELD_DEFLECTED = getStringWithDefault(messages, "shield.deflected");
        SHIELD_COOLDOWN = getStringWithDefault(messages, "shield.cooldown");

        NO_PERMISSION = getStringWithDefault(messages, "others.no_permission");
        CONFIG_RELOADED = getStringWithDefault(messages, "others.config_reloaded");

        messages.update();
    }

    private String getStringWithDefault(NekoConfig config, String path){
        return config.getString(path, getDefaultString(path));
    }

    private YamlConfiguration getDefaultLangFile(String name){
        if(temporal_languages.containsKey(name))
            return temporal_languages.get(name);

        try{
            InputStream inputStream = plugin.getResource("languages/"+name+".yml");
            if(inputStream == null) return null;

           return temporal_languages.put(name, YamlConfiguration.loadConfiguration(NekoConfig.convertInputStreamToFile(inputStream, "temp_config_"+name)));
        }catch(Exception ignore){
            return null;
        }
    }

    private String getDefaultString(String path){
        String text = "Error in generate new option";
        YamlConfiguration configuration = getDefaultLangFile(plugin.getConfigManager().LANG);
        if(configuration != null)
            text = configuration.getString(path);

        return text;
    }

}
