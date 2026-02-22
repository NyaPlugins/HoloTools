package net.kokoricraft.holotools.managers;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.version.Compat;
import net.kokoricraft.holotools.version.HoloItemDisplay;
import net.kokoricraft.holotools.version.HoloTextDisplay;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompatManager {
    public String VERSION = "";
    private final HoloTools plugin;
    private final Compat compat;
    private final Map<String, String> versions = new HashMap<>(){
        {
            this.put("1.20.6", "v1_20_R2");
            this.put("1.21", "v1_21_R1");
            this.put("1.21.1", "v1_21_R1");
            this.put("1.21.2", "v1_21_R1");
            this.put("1.21.3", "v1_21_R2");
            this.put("1.21.4", "v1_21_R3");
            this.put("1.21.5", "v1_21_R4");
            this.put("1.21.6", "v1_21_R5");
            this.put("1.21.7", "v1_21_R5");
            this.put("1.21.8", "v1_21_R5");
            this.put("1.21.9", "v1_21_R6");
            this.put("1.21.10", "v1_21_R6");
            this.put("1.21.11", "v1_21_R7");
        }
    };

    public CompatManager(HoloTools plugin) {
        this.plugin = plugin;
        setVersionCompat();
        compat = loadCompat();
    }

    private void setVersionCompat() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        if(packageName.contains("v")){
            VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
        }else {
            String bukkitVersion = Bukkit.getBukkitVersion().split("-")[0];
            this.VERSION = versions.getOrDefault(bukkitVersion, "-[NYA]-");
        }
    }

    private Compat loadCompat() {
        Compat compat = tryCompat(VERSION);

        if(compat == null){
            plugin.getLogger().severe(String.format("Version no support. report to author. version not supported: [%s]", VERSION));
            Bukkit.getPluginManager().disablePlugin(plugin);
            return null;
        }

        plugin.getLogger().info(String.format("HoloTools loaded!. Selected version: [%s]", VERSION));
        return compat;
    }

    private Compat tryCompat(String version){
        try{
            Class<?> compatClass = Class.forName("net.kokoricraft.holotools.version." + version);
            return Compat.class.isAssignableFrom(compatClass) ?  (Compat) compatClass.getConstructor(new Class[0]).newInstance(new Object[0]) : null;
        }catch (Exception exception){
            return null;
        }
    }

    public HoloTextDisplay createTextDisplay(List<Player> players, Location location, float yaw, float pitch){
        return compat.createTextDisplay(players, location, yaw, pitch);
    }

    public HoloItemDisplay createItemDisplay(List<Player> players, Location location, float yaw, float pitch){
        return compat.createItemDisplay(players, location, yaw, pitch);
    }

    public Compat getCompat() {
        return compat;
    }
}
