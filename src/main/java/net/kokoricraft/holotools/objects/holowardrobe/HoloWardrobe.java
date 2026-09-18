package net.kokoricraft.holotools.objects.holowardrobe;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.enums.HoloActionType;
import net.kokoricraft.holotools.enums.HoloType;
import net.kokoricraft.holotools.events.HoloWardrobeSaveItemEvent;
import net.kokoricraft.holotools.interfaces.HoloBase;
import net.kokoricraft.holotools.objects.colors.HoloPanelsColors;
import net.kokoricraft.holotools.objects.halo.HaloSlot;
import net.kokoricraft.holotools.objects.halo.Holo;
import net.kokoricraft.holotools.objects.tooltip.TooltipDisplay;
import net.kokoricraft.holotools.objects.tooltip.TooltipPosition;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class HoloWardrobe extends Holo implements HoloBase {
    private final HoloTools plugin = HoloTools.getInstance();
    private final Map<Integer, HoloWardrobeSlot> wardrobeSlots = new HashMap<>();
    private final HoloPanelsColors colors;
    private final TooltipDisplay tooltip;
    private int last_vertical_slot = -1;

    public HoloWardrobe(Player player, ItemStack itemStack, Map<Integer, WardrobeContent> contentMap, int size) {
        super(size, -1.5f, itemStack);
        this.player = player;
        this.colors = plugin.getHoloManager().getHoloColor(player, HoloType.HOLOWARDROBE);

        for (int key : slots.keySet()) {
            HaloSlot slot = slots.get(key);
            slot.setColor(colors.getColor(key).unselected());

            HoloWardrobeSlot wardrobeSlot = new HoloWardrobeSlot(slot, contentMap.get(key), player, size);

            wardrobeSlots.put(key, wardrobeSlot);
        }

        tooltip = new TooltipDisplay(player);
        tooltip.setPosition(TooltipPosition.RIGHT);
    }

    @Override
    public void onChangeSlot(int fromSlot, int toSlot) {
        super.onChangeSlot(fromSlot, toSlot);

        HaloSlot from = slots.get(fromSlot);
        HaloSlot to = slots.get(toSlot);

        if (from != null) {
            HoloColor color = colors.getColor(fromSlot).unselected().clone();
            from.removeExtraY();
            from.setColor(color);
        }

        if (to != null) {
            to.addExtraYSize(0.45f);
            to.setColor(colors.getColor(toSlot).selected().clone());
        }

        if (plugin.getConfigManager().TOOLTIP_ENABLED)
            onVerticalSlot(0, getVerticalSlot());
    }

    @Override
    public void onClick() {
        super.onClick();
        int slot = getPlayerSlot();

        HoloWardrobeSlot wardrobeSlot = wardrobeSlots.get(slot);

        WardrobeContent saveArmor = WardrobeContent.fromPlayer(player);
        WardrobeContent loadArmor = wardrobeSlot.getContent();

        if (!saveArmor.isEmpty()) {
            if (!handleSaveItemEvents(saveArmor, loadArmor, slot)) return;
        }

        if (wardrobeSlot.getContent() == null && !saveArmor.isEmpty()) {
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
            player.updateInventory();
            wardrobeSlot.setContent(saveArmor);
            plugin.getDataManager().saveHoloWardrobe(itemStack, this, "save wardrobe armor");
        } else {
            if ((loadArmor == null || loadArmor.isEmpty()) && saveArmor.isEmpty()) return;
            wardrobeSlot.setContent(saveArmor);
            if (loadArmor != null)
                loadArmor.apply(player);
            plugin.getDataManager().saveHoloWardrobe(itemStack, this, "swap wardrobe armor");
        }

        if (plugin.getConfigManager().TOOLTIP_ENABLED)
            onVerticalSlot(0, getVerticalSlot());
    }

    private boolean handleSaveItemEvents(WardrobeContent saveArmor, WardrobeContent loadArmor, int slot) {
        ItemStack realHead = saveArmor.getHelmet();
        if (realHead != null && realHead.getType() != Material.AIR) {
            ItemStack oldHead = loadArmor != null && loadArmor.getHelmet() != null && loadArmor.getHelmet().getType() != Material.AIR ? loadArmor.getHelmet() : null;
            ItemStack oldDisplayHead = loadArmor != null ? loadArmor.getDisplayHelmet() : null;
            HoloWardrobeSaveItemEvent event = new HoloWardrobeSaveItemEvent(player, this, slot, EquipmentSlot.HEAD, realHead, oldHead, oldDisplayHead);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
            saveArmor.setDisplayHelmet(event.getDisplayItem());
        }

        ItemStack realChest = saveArmor.getChestplate();
        if (realChest != null && realChest.getType() != Material.AIR) {
            ItemStack oldChest = loadArmor != null && loadArmor.getChestplate() != null && loadArmor.getChestplate().getType() != Material.AIR ? loadArmor.getChestplate() : null;
            ItemStack oldDisplayChest = loadArmor != null ? loadArmor.getDisplayChestplate() : null;
            HoloWardrobeSaveItemEvent event = new HoloWardrobeSaveItemEvent(player, this, slot, EquipmentSlot.CHEST, realChest, oldChest, oldDisplayChest);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
            saveArmor.setDisplayChestplate(event.getDisplayItem());
        }

        ItemStack realLegs = saveArmor.getLeggings();
        if (realLegs != null && realLegs.getType() != Material.AIR) {
            ItemStack oldLegs = loadArmor != null && loadArmor.getLeggings() != null && loadArmor.getLeggings().getType() != Material.AIR ? loadArmor.getLeggings() : null;
            ItemStack oldDisplayLegs = loadArmor != null ? loadArmor.getDisplayLeggings() : null;
            HoloWardrobeSaveItemEvent event = new HoloWardrobeSaveItemEvent(player, this, slot, EquipmentSlot.LEGS, realLegs, oldLegs, oldDisplayLegs);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
            saveArmor.setDisplayLeggings(event.getDisplayItem());
        }

        ItemStack realBoots = saveArmor.getBoots();
        if (realBoots != null && realBoots.getType() != Material.AIR) {
            ItemStack oldBoots = loadArmor != null && loadArmor.getBoots() != null && loadArmor.getBoots().getType() != Material.AIR ? loadArmor.getBoots() : null;
            ItemStack oldDisplayBoots = loadArmor != null ? loadArmor.getDisplayBoots() : null;
            HoloWardrobeSaveItemEvent event = new HoloWardrobeSaveItemEvent(player, this, slot, EquipmentSlot.FEET, realBoots, oldBoots, oldDisplayBoots);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return false;
            saveArmor.setDisplayBoots(event.getDisplayItem());
        }

        return true;
    }

    @Override
    public void onAction(HoloActionType type) {

    }

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
            spawn(player);
            wardrobeSlots.values().forEach(HoloWardrobeSlot::spawnContent);

            if (plugin.getConfigManager().TOOLTIP_ENABLED)
                tooltip.spawn();
        } else {
            this.shouldRemove = true;
            remove("remove set visible false wardrobe");
            tooltip.remove();
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void tick() {
        super.tick();

        if (!plugin.getConfigManager().TOOLTIP_ENABLED) return;

        int vertical_slot = getVerticalSlot();

        if (vertical_slot != last_vertical_slot) {
            onVerticalSlot(last_vertical_slot, vertical_slot);
            last_vertical_slot = vertical_slot;
        }
    }

    public void onVerticalSlot(int fromSlot, int toSlot) {
        float separation = -0.2f;
        float y = 0.2f;
        WardrobeContent content = getContent(getPlayerSlot());

        switch (toSlot) {
            case 0 -> {
                ItemStack stack = content == null ? null : wardrobeSlots.get(getPlayerSlot()).getContent().getDisplayHelmet();
                if (stack != null && stack.getType() != Material.AIR) {
                    tooltip.setY(y + (separation * 1));
                    tooltip.setItemStack(stack);
                } else {
                    tooltip.setVisible(false);
                }
            }
            case 1 -> {
                ItemStack stack = content == null ? null : wardrobeSlots.get(getPlayerSlot()).getContent().getDisplayChestplate();
                if (stack != null && stack.getType() != Material.AIR) {
                    tooltip.setY(y + (separation * 2));
                    tooltip.setItemStack(stack);
                } else {
                    tooltip.setVisible(false);
                }
            }
            case 2 -> {
                ItemStack stack = content == null ? null : wardrobeSlots.get(getPlayerSlot()).getContent().getDisplayLeggings();
                if (stack != null && stack.getType() != Material.AIR) {
                    tooltip.setY(y + (separation * 3));
                    tooltip.setItemStack(stack);
                } else {
                    tooltip.setVisible(false);
                }
            }
            case 3 -> {
                ItemStack stack = content == null ? null : wardrobeSlots.get(getPlayerSlot()).getContent().getDisplayBoots();
                if (stack != null && stack.getType() != Material.AIR) {
                    tooltip.setY(y + (separation * 4));
                    tooltip.setItemStack(stack);
                } else {
                    tooltip.setVisible(false);
                }
            }
            default -> {
                tooltip.setVisible(false);
            }
        }
    }

    private WardrobeContent getContent(int slot) {
        return wardrobeSlots.get(slot).getContent();
    }

    public int getVerticalSlot() {
        if (player == null)
            return -1;

        float yaw = player.getLocation().getPitch();

        if (yaw > -23 && yaw < -10.68)
            return 0;

        if (yaw > -10.68 && yaw < 5.19)
            return 1;

        if (yaw > 5.19 && yaw < 19)
            return 2;

        if (yaw > 19 && yaw < 32)
            return 3;

        return -1;
    }

    public Map<Integer, HoloWardrobeSlot> getWardrobeSlots() {
        return wardrobeSlots;
    }
}
