package net.kokoricraft.holotools.objects;

import net.kokoricraft.holotools.HoloTools;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NekoItem {
    private final HoloTools plugin;
    private String name;
    private Integer custom_model_data;
    private List<String> lore;
    private Material material = Material.AMETHYST_SHARD;
    private ItemStack itemStack;
    private ItemStack head;
    private boolean unbreakable = false;
    private final Map<String, String> tags = new HashMap<>();
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();

    public NekoItem(HoloTools plugin, ConfigurationSection config){
        this.plugin = plugin;

        if(config.contains("name"))
            name = plugin.getUtils().color(config.getString("name"));

        if(config.contains("lore"))
            lore = plugin.getUtils().color(config.getStringList("lore"));

        if(config.contains("custom_model_data"))
            custom_model_data = config.getInt("custom_model_data");

        if(config.contains("material"))
            material = Material.valueOf(Objects.requireNonNull(config.getString("material")).toUpperCase());

        if(config.contains("texture") && material != null && material.equals(Material.PLAYER_HEAD))
            head = plugin.getUtils().getHeadFromURL(config.getString("texture"));
    }

    public void setTag(String key, String value){
        tags.put(key, value);
    }

    public ItemStack getItem(){
        if(itemStack != null) return itemStack.clone();

        ItemStack itemStack = new ItemStack(material);

        if(head != null)
            itemStack = head;

        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        if(name != null)
            meta.setDisplayName(name);

        if(lore != null)
            meta.setLore(lore);

        if(custom_model_data != null)
            meta.setCustomModelData(custom_model_data);

        if (!enchantments.isEmpty()) {
            enchantments.entrySet().forEach(entry -> meta.addEnchant(entry.getKey(), entry.getValue(), true));
        }

        if (unbreakable) {
            meta.setUnbreakable(true);
        }

        if(!tags.isEmpty()){
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            for(Map.Entry<String, String> entry : tags.entrySet()){
                dataContainer.set(new NamespacedKey(plugin, entry.getKey()), PersistentDataType.STRING, entry.getValue());
            }
        }

        itemStack.setItemMeta(meta);
        this.itemStack = itemStack;

        return itemStack;
    }

    public ItemStack getItem(String key, Object value){
        ItemStack item = getItem();
        ItemMeta meta = item.getItemMeta();

        assert meta != null;
        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);

        if(value instanceof Integer integer){
            container.set(namespacedKey, PersistentDataType.INTEGER, integer);
        }

        if(value instanceof String string){
            container.set(namespacedKey, PersistentDataType.STRING, string);
        }
        item.setItemMeta(meta);
        return item;
    }

    public void setDefaultMaterial(Material material) {
        this.material = material;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public void addEnchant(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
    }
}
