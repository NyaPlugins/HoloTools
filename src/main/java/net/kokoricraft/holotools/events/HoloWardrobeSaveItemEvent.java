package net.kokoricraft.holotools.events;

import net.kokoricraft.holotools.objects.holowardrobe.HoloWardrobe;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class HoloWardrobeSaveItemEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean cancelled;
    private final Player player;
    private final HoloWardrobe wardrobe;
    private final int slot;
    private final EquipmentSlot equipmentSlot;
    private final ItemStack item;
    private final ItemStack previousItem;
    private final ItemStack previousDisplayItem;
    private ItemStack displayItem;

    public HoloWardrobeSaveItemEvent(Player player, HoloWardrobe wardrobe, int slot, EquipmentSlot equipmentSlot, ItemStack item, ItemStack previousItem) {
        this(player, wardrobe, slot, equipmentSlot, item, previousItem, previousItem);
    }

    public HoloWardrobeSaveItemEvent(Player player, HoloWardrobe wardrobe, int slot, EquipmentSlot equipmentSlot, ItemStack item, ItemStack previousItem, ItemStack previousDisplayItem) {
        this.player = player;
        this.wardrobe = wardrobe;
        this.slot = slot;
        this.equipmentSlot = equipmentSlot;
        this.item = item;
        this.previousItem = previousItem;
        this.previousDisplayItem = previousDisplayItem;
        this.displayItem = item != null ? item.clone() : null;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Player getPlayer() {
        return player;
    }

    public HoloWardrobe getWardrobe() {
        return wardrobe;
    }

    public int getSlot() {
        return slot;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemStack getSavedItem() {
        return item;
    }

    public ItemStack getPreviousItem() {
        return previousItem;
    }

    public ItemStack getPreviousDisplayItem() {
        return previousDisplayItem;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public void setDisplayItem(ItemStack displayItem) {
        this.displayItem = displayItem;
    }
}
