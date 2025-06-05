package net.kokoricraft.holotools.objects.holowardrobe;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.enums.HoloSize;
import net.kokoricraft.holotools.objects.halo.HaloSlot;
import net.kokoricraft.holotools.version.HoloItemDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HoloWardrobeSlot {
    private final HoloTools plugin = HoloTools.getInstance();
    private final HaloSlot slot;
    private WardrobeContent content;
    private final Player player;
    private final Map<EquipmentSlot, HoloItemDisplay> entities = new HashMap<>();
    private final float separation = 0.5f;
    private final float y_translation = 1.9f - 0.3f;
    private final int size;

    public HoloWardrobeSlot(HaloSlot slot, WardrobeContent content, Player player, int size) {
        this.slot = slot;
        this.content = content;
        this.player = player;
        this.size = size;
    }

    public void spawnContent() {
        if (!entities.isEmpty()) return;

        List<Player> players = plugin.getManager().getHoloPlayerView(player);
        HoloSize holoSize = HoloSize.getBySlots(size);
        float z = (float) (Math.abs(holoSize.getZ()) - 0.15f);

        HoloItemDisplay head = plugin.getCompatManager().createItemDisplay(players, player.getLocation(), getYaw(), 0);
        head.setTranslation(0, separation * 4 - y_translation, z);
        head.setScale(0.5f, 0.5f, 0.5f);
        head.setBrightness(new Display.Brightness(15, 15));

        if (content != null && content.getHelmet() != null)
            head.setItemStack(content.getHelmet());

        head.update();
        head.mount(player);
        entities.put(EquipmentSlot.HEAD, head);
        slot.addItemDisplay("wardrobe_content_head", head);


        HoloItemDisplay chestplate = plugin.getCompatManager().createItemDisplay(players, player.getLocation(), getYaw(), 0);
        chestplate.setTranslation(0, separation * 3 - y_translation, z);
        chestplate.setScale(0.5f, 0.5f, 0.5f);
        chestplate.setBrightness(new Display.Brightness(15, 15));

        if (content != null && content.getChestplate() != null)
            chestplate.setItemStack(content.getChestplate());

        chestplate.update();
        chestplate.mount(player);
        entities.put(EquipmentSlot.CHEST, chestplate);
        slot.addItemDisplay("wardrobe_content_chestplate", chestplate);

        HoloItemDisplay leggings = plugin.getCompatManager().createItemDisplay(players, player.getLocation(), getYaw(), 0);
        leggings.setTranslation(0, separation * 2 - y_translation, z);
        leggings.setScale(0.5f, 0.5f, 0.5f);
        leggings.setBrightness(new Display.Brightness(15, 15));

        if (content != null && content.getLeggings() != null)
            leggings.setItemStack(content.getLeggings());

        leggings.update();
        leggings.mount(player);
        entities.put(EquipmentSlot.LEGS, leggings);
        slot.addItemDisplay("wardrobe_content_leggings", leggings);

        HoloItemDisplay boots = plugin.getCompatManager().createItemDisplay(players, player.getLocation(), getYaw(), 0);
        boots.setTranslation(0, separation * 1 - y_translation, z);
        boots.setScale(0.5f, 0.5f, 0.5f);
        boots.setBrightness(new Display.Brightness(15, 15));

        if (content != null && content.getBoots() != null)
            boots.setItemStack(content.getBoots());

        boots.update();
        boots.mount(player);
        entities.put(EquipmentSlot.FEET, boots);
        slot.addItemDisplay("wardrobe_content_boots", boots);
    }

    public void setContent(WardrobeContent content) {
        this.content = content;

        if (entities.isEmpty()) {
            spawnContent();
            return;
        }

        HoloItemDisplay head = entities.get(EquipmentSlot.HEAD);
        HoloItemDisplay chest = entities.get(EquipmentSlot.CHEST);
        HoloItemDisplay legs = entities.get(EquipmentSlot.LEGS);
        HoloItemDisplay feet = entities.get(EquipmentSlot.FEET);

        if (content == null || content.isEmpty()) {
            for (HoloItemDisplay display : entities.values())
                display.setViewRange(0);
            updateAll();
        } else {
            head.setViewRange(content.getHelmet() == null ? 0 : 20);
            head.setItemStack(content.getHelmet());
            chest.setViewRange(content.getChestplate() == null ? 0 : 20);
            chest.setItemStack(content.getChestplate());
            legs.setViewRange(content.getLeggings() == null ? 0 : 20);
            legs.setItemStack(content.getLeggings());
            feet.setViewRange(content.getBoots() == null ? 0 : 20);
            feet.setItemStack(content.getBoots());
            updateAll();
        }
    }

    private void updateAll() {
        entities.values().forEach(HoloItemDisplay::update);
    }

    public WardrobeContent getContent() {
        return content;
    }

    private float getYaw() {
//        return (45 * slot.getSlot() + slot.getInitialYaw());
        return 360.0f / size * slot.getSlot() + (slot.getInitialYaw());
    }

}
