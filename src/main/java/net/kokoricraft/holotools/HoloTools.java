package net.kokoricraft.holotools;

import net.kokoricraft.holotools.commands.Commands;
import net.kokoricraft.holotools.commands.CommandsCompleter;
import net.kokoricraft.holotools.data.PlayerManager;
import net.kokoricraft.holotools.enums.GlowColor;
import net.kokoricraft.holotools.interfaces.Tickable;
import net.kokoricraft.holotools.listeners.*;
import net.kokoricraft.holotools.managers.*;
import net.kokoricraft.holotools.objects.halo.Holo;
import net.kokoricraft.holotools.objects.holobridge.Bridge;
import net.kokoricraft.holotools.utils.CraftItemsUtils;
import net.kokoricraft.holotools.utils.Metrics;
import net.kokoricraft.holotools.utils.UpdateChecker;
import net.kokoricraft.holotools.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HoloTools extends JavaPlugin {
    private TickManager tickManager;
    private Manager manager;
    private CompatManager compatManager;
    private Utils utils;
    private CraftItemsUtils craftItemsUtils;
    private HoloManager holoManager;
    private DataManager dataManager;
    private PlayerManager playerManager;
    private ConfigManager configManager;
    private LangManager langManager;
    private BridgesManager bridgeManager;
    private UpdateChecker updateChecker;
    public boolean UPDATED = true;
    public String VERSION;

    @Override
    public void onEnable() {
        // Plugin startup logic
        initCommands();
        initClass();
        initListeners();
        checkPlayers();

        updateChecker = new UpdateChecker(this, 119315);
        updateChecker.getVersion(version -> {
            VERSION = version;
            if(!getDescription().getVersion().equals(version)) {
                UPDATED = false;
                updateChecker.sendMessage(Bukkit.getConsoleSender(), UPDATED, version);
            }
        });

        Bukkit.getOnlinePlayers().forEach(player -> playerManager.getPlayer(player));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        compatManager.getCompat().removePlayers();
        for(Tickable tickable : tickManager.getTickableList()){
            if(tickable instanceof Holo holo){
                holo.setVisible(false);
            }
        }

        GlowColor.unregisterAll();
    }

    private void initCommands(){
        PluginCommand command = getCommand("holotools");
        if(command == null) return;
        command.setExecutor(new Commands(this));
        command.setTabCompleter(new CommandsCompleter());
    }

    private void initClass(){
        tickManager = new TickManager(this);
        manager = new Manager(this);
        compatManager = new CompatManager(this);
        utils = new Utils(this);
        craftItemsUtils = new CraftItemsUtils(this);
        holoManager = new HoloManager(this);
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        playerManager = new PlayerManager(this);
        langManager = new LangManager(this);
        bridgeManager = new BridgesManager(this);

        new Metrics(this, 23286);
    }

    private void initListeners(){
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new HoloListener(this), this);
        pm.registerEvents(new HoloCrafterListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new HoloBridgeListener(), this);
//        pm.registerEvents(new TestListeners(this), this);
    }

    private void checkPlayers(){
        Bukkit.getOnlinePlayers().forEach(player -> {
            compatManager.getCompat().initPacketsRegister(player);
            holoManager.check(player);
        });
    }
    public void reloadConfig(){
        configManager.loadConfig();
        langManager.loadMessages();
    }

    public TickManager getTickManager(){
        return tickManager;
    }
    public Manager getManager() {
        return manager;
    }
    public CompatManager getCompatManager(){
        return compatManager;
    }
    public Utils getUtils() {
        return utils;
    }
    public CraftItemsUtils getCraftItemsUtils(){
        return craftItemsUtils;
    }
    public HoloManager getHoloManager(){
        return holoManager;
    }
    public DataManager getDataManager(){
        return dataManager;
    }
    public ConfigManager getConfigManager(){
        return configManager;
    }
    public LangManager getLangManager() {
        return langManager;
    }
    public PlayerManager getPlayerManager(){
        return playerManager;
    }
    public BridgesManager getBridgeManager() {
        return bridgeManager;
    }

    public UpdateChecker getUpdateChecker(){
        return updateChecker;
    }

    public static HoloTools getInstance(){
        return JavaPlugin.getPlugin(HoloTools.class);
    }
}
