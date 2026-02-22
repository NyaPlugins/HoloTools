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
