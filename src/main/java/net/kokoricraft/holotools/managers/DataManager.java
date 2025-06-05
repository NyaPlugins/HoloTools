package net.kokoricraft.holotools.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.data.Storage;
import net.kokoricraft.holotools.data.types.MySqlStorage;
import net.kokoricraft.holotools.data.types.YamlStorage;
import net.kokoricraft.holotools.enums.HoloType;
import net.kokoricraft.holotools.enums.StorageMode;
import net.kokoricraft.holotools.objects.holowardrobe.WardrobeContent;
import net.kokoricraft.holotools.objects.holocrafter.HoloCrafter;
import net.kokoricraft.holotools.objects.holocrafter.HoloCrafterSlot;
import net.kokoricraft.holotools.objects.holowardrobe.HoloWardrobe;
import net.kokoricraft.holotools.objects.holowardrobe.HoloWardrobeSlot;
import net.kokoricraft.holotools.objects.players.HoloPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataManager {
    private final HoloTools plugin;
    public final NamespacedKey CRAFTER_KEY;
    public final NamespacedKey WARDROBE_KEY;
    private final Storage storage;

    public DataManager(HoloTools plugin) {
        this.plugin = plugin;
        CRAFTER_KEY = new NamespacedKey(plugin, "holo_crafter");
        WARDROBE_KEY = new NamespacedKey(plugin, "holo_wardrobe");

        storage = switch (plugin.getConfigManager().STORAGE_TYPE) {
            case YAML -> new YamlStorage();
            case MYSQL -> new MySqlStorage();
        };
    }

    public void saveHoloCrafter(ItemStack itemStack, HoloCrafter holoCrafter, String reason) {
        Player player = holoCrafter.getPlayer();
        //Bukkit.broadcastMessage("crafter save for: "+reason);
        Map<Integer, HoloCrafterSlot> slots = holoCrafter.getCrafterSlots();

        JsonObject jsonObject = new JsonObject();

        if (slots.isEmpty()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        int id;

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey idKey = new NamespacedKey(plugin, "id");

        if (!dataContainer.has(idKey, PersistentDataType.INTEGER)) {
            id = storage.getNextID();
        } else {
            Integer storedID = dataContainer.get(idKey, PersistentDataType.INTEGER);
            id = Objects.requireNonNullElseGet(storedID, storage::getNextID);
        }

        meta.getPersistentDataContainer().set(idKey, PersistentDataType.INTEGER, id);

        for (int slot : slots.keySet()) {
            if (!slots.containsKey(slot)) continue;
            if (slots.get(slot).getRecipe() == null) continue;
            if (!(slots.get(slot).getRecipe() instanceof Keyed)) continue;

            jsonObject.addProperty("slot_" + slot, ((Keyed) slots.get(slot).getRecipe()).getKey().toString());
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.ITEM) {
            meta.getPersistentDataContainer().set(CRAFTER_KEY, PersistentDataType.STRING, jsonObject.toString());
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.PLAYER) {
            HoloPlayer holoPlayer = plugin.getPlayerManager().getPlayer(player);
            holoPlayer.setData("holo_crafter", jsonObject);
            storage.savePlayer(holoPlayer);
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.UUID) {
            storage.saveHolo(id, jsonObject);
        }

        itemStack.setItemMeta(meta);

        if (!plugin.getHoloManager().isHolo(player.getInventory().getItemInMainHand())) return;

        player.getInventory().setItemInMainHand(itemStack);
    }

    public HoloCrafter loadHoloCrafter(Player player, ItemStack itemStack) {
        if (itemStack == null) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        HoloPlayer holoPlayer = plugin.getPlayerManager().getPlayer(player);

        int slots = holoPlayer.getSlots(HoloType.HOLOCRAFTER);

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        int id;

        NamespacedKey idKey = new NamespacedKey(plugin, "id");

        if (!dataContainer.has(idKey, PersistentDataType.INTEGER)) {
            id = storage.getNextID();
        } else {
            Integer storedID = dataContainer.get(idKey, PersistentDataType.INTEGER);
            id = Objects.requireNonNullElseGet(storedID, storage::getNextID);
        }

        if (!dataContainer.has(CRAFTER_KEY, PersistentDataType.STRING)) return null;

        dataContainer.set(idKey, PersistentDataType.INTEGER, id);

        JsonObject jsonObject = new JsonObject();

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.ITEM) {
            jsonObject = JsonParser.parseString(Objects.requireNonNull(dataContainer.get(CRAFTER_KEY, PersistentDataType.STRING))).getAsJsonObject();
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.PLAYER) {
            jsonObject = holoPlayer.getData().getOrDefault("holo_crafter", new JsonObject());
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.UUID) {
            jsonObject = storage.getHolo(id);
        }

        Map<Integer, Recipe> recipeMap = new HashMap<>();

        for (int slot = 1; slot < 16; slot++) {
            String slot_key = "slot_" + slot;

            if (!jsonObject.has(slot_key)) continue;
            String key = jsonObject.get(slot_key).getAsString();

            NamespacedKey namespacedKey = NamespacedKey.fromString(key);
            if (namespacedKey == null) continue;

            Recipe recipe = Bukkit.getRecipe(namespacedKey);

            if (recipe == null) continue;

            recipeMap.put(slot, recipe);
        }

        itemStack.setItemMeta(meta);

        return new HoloCrafter(player, recipeMap, itemStack, slots);
    }

    public void saveHoloWardrobe(ItemStack itemStack, HoloWardrobe wardrobe, String reason) {
        Player player = wardrobe.getPlayer();
        Map<Integer, HoloWardrobeSlot> slots = wardrobe.getWardrobeSlots();

        if (itemStack == null || slots.isEmpty()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        JsonObject jsonObject = new JsonObject();
        int id;

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey idKey = new NamespacedKey(plugin, "id");

        if (!dataContainer.has(idKey, PersistentDataType.INTEGER)) {
            id = storage.getNextID();
        } else {
            Integer storedID = dataContainer.get(idKey, PersistentDataType.INTEGER);
            id = Objects.requireNonNullElseGet(storedID, storage::getNextID);
        }

        meta.getPersistentDataContainer().set(idKey, PersistentDataType.INTEGER, id);

        for (int slot : slots.keySet()) {
            if (!slots.containsKey(slot)) continue;
            WardrobeContent content = slots.get(slot).getContent();

            if (content == null) continue;

            jsonObject.addProperty("slot_" + slot, content.getToJson().toString());
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.PLAYER) {
            HoloPlayer holoPlayer = plugin.getPlayerManager().getPlayer(player);
            holoPlayer.setData("holo_wardrobe", jsonObject);
            storage.savePlayer(holoPlayer);
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.UUID) {
            storage.saveHolo(id, jsonObject);

            itemStack.setItemMeta(meta);

            if (plugin.getHoloManager().isHolo(player.getInventory().getItemInMainHand()))
                player.getInventory().setItemInMainHand(itemStack);
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.ITEM) {
            meta.getPersistentDataContainer().set(WARDROBE_KEY, PersistentDataType.STRING, jsonObject.toString());

            itemStack.setItemMeta(meta);

            if (plugin.getHoloManager().isHolo(player.getInventory().getItemInMainHand()))
                player.getInventory().setItemInMainHand(itemStack);
        }
    }

    public HoloWardrobe loadHoloWardrobe(Player player, ItemStack itemStack) {
        if (itemStack == null) return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        HoloPlayer holoPlayer = plugin.getPlayerManager().getPlayer(player);

        int slots = holoPlayer.getSlots(HoloType.HOLOWARDROBE);

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        int id;

        NamespacedKey idKey = new NamespacedKey(plugin, "id");

        if (!dataContainer.has(idKey, PersistentDataType.INTEGER)) {
            id = storage.getNextID();
        } else {
            Integer storedID = dataContainer.get(idKey, PersistentDataType.INTEGER);
            id = Objects.requireNonNullElseGet(storedID, storage::getNextID);
        }

        if (!dataContainer.has(WARDROBE_KEY, PersistentDataType.STRING)) return null;

        dataContainer.set(idKey, PersistentDataType.INTEGER, id);

        JsonObject jsonObject = new JsonObject();

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.ITEM) {
            jsonObject = JsonParser.parseString(Objects.requireNonNull(dataContainer.get(WARDROBE_KEY, PersistentDataType.STRING))).getAsJsonObject();
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.PLAYER) {
            jsonObject = holoPlayer.getData().getOrDefault("holo_wardrobe", new JsonObject());
        }

        if (plugin.getConfigManager().STORAGE_MODE == StorageMode.UUID) {
            jsonObject = storage.getHolo(id);
        }

        Map<Integer, WardrobeContent> contentMap = new HashMap<>();

        for (int slot = 0; slot < 16; slot++) {
            String slot_key = "slot_" + slot;

            if (!jsonObject.has(slot_key)) continue;
            String stringContent = jsonObject.get(slot_key).getAsString();
            if (stringContent == null) continue;

            JsonObject jsonContent = JsonParser.parseString(stringContent).getAsJsonObject();
            WardrobeContent content = WardrobeContent.fromJson(jsonContent);

            contentMap.put(slot, content);
        }

        itemStack.setItemMeta(meta);

        return new HoloWardrobe(player, itemStack, contentMap, slots);
    }

    public Storage getStorage() {
        return storage;
    }
}
