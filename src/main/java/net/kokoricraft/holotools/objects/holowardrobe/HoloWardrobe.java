package net.kokoricraft.holotools.objects.holowardrobe;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.enums.HoloActionType;
import net.kokoricraft.holotools.enums.HoloType;
import net.kokoricraft.holotools.interfaces.HoloBase;
import net.kokoricraft.holotools.objects.colors.HoloPanelsColors;
import net.kokoricraft.holotools.objects.halo.HaloSlot;
import net.kokoricraft.holotools.objects.halo.Holo;
import net.kokoricraft.holotools.objects.tooltip.TooltipDisplay;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import org.bukkit.entity.Player;
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
        tooltip.setX(0.3f);
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
            if (!color.getSequence().isEmpty())
                from.setIgnoreColorChange(5);
        }

        if (to != null) {
            to.addExtraYSize(0.45f);
            to.setColor(colors.getColor(toSlot).selected().clone());
            to.setIgnoreColorChange(5);
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

        if (wardrobeSlot.getContent() == null && !saveArmor.isEmpty()) {
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
            player.updateInventory();
            wardrobeSlot.setContent(saveArmor);
            plugin.getDataManager().saveHoloWardrobe(itemStack, this, "save wardrobe armor");
        } else {
            WardrobeContent loadArmor = wardrobeSlot.getContent();
            if ((loadArmor == null || loadArmor.isEmpty()) && saveArmor.isEmpty()) return;
            wardrobeSlot.setContent(saveArmor);
            if (loadArmor != null)
                loadArmor.apply(player);
            plugin.getDataManager().saveHoloWardrobe(itemStack, this, "swap wardrobe armor");
        }

        if (plugin.getConfigManager().TOOLTIP_ENABLED)
            onVerticalSlot(0, getVerticalSlot());
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
                ItemStack stack = content == null ? null : wardrobeSlots.get(getPlayerSlot()).getContent().getHelmet();
                if (stack != null) {
                    tooltip.setY(y + (separation * 1));
                    tooltip.setItemStack(stack);
                } else {
                    tooltip.setVisible(false);
                }
            }
            case 1 -> {
                ItemStack stack = content == null ? null : wardrobeSlots.get(getPlayerSlot()).getContent().getChestplate();
                if (stack != null) {
                    tooltip.setY(y + (separation * 2));
                    tooltip.setItemStack(stack);
                } else {
                    tooltip.setVisible(false);
                }
            }
            case 2 -> {
                ItemStack stack = content == null ? null : wardrobeSlots.get(getPlayerSlot()).getContent().getLeggings();
                if (stack != null) {
                    tooltip.setY(y + (separation * 3));
                    tooltip.setItemStack(stack);
                } else {
                    tooltip.setVisible(false);
                }
            }
            case 3 -> {
                ItemStack stack = content == null ? null : wardrobeSlots.get(getPlayerSlot()).getContent().getBoots();
                if (stack != null) {
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
