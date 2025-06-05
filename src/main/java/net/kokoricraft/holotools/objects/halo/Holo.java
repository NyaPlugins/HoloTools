package net.kokoricraft.holotools.objects.halo;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.commands.Commands;
import net.kokoricraft.holotools.enums.HoloSize;
import net.kokoricraft.holotools.interfaces.HoloBase;
import net.kokoricraft.holotools.interfaces.Tickable;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class Holo implements Tickable, HoloBase {
    private final HoloTools plugin = HoloTools.getInstance();
    protected Player player;
    protected final int size;
    protected final float height;
    protected boolean shouldRemove = false;
    protected int last_slot = -1;
    protected boolean visible = false;
    protected final Map<Integer, HaloSlot> slots = new HashMap<>();
    protected final ItemStack itemStack;
    protected float initial_yaw = 0.0f;

    public Holo(int size, float height, ItemStack itemStack) {
        this.size = size;
        this.height = height;
        this.itemStack = itemStack;

        HoloSize sizeData = HoloSize.getBySlots(size);

        for (int i = 0; i < size; i++) {//-0.012f
            float rotation = ((360.0f / size * i - 90.0f) + 360) % 360;

            float panelSize = sizeData.getXSize();
            float x = (-0.012553802f * panelSize);
            HaloSlot slot = new HaloSlot(i, x/*sizeData.getX()*/, height, sizeData.getZ(), panelSize/*sizeData.getXSize()*/, sizeData.getYSize(), sizeData.getZSize(), rotation, this);
            slots.put(i, slot);
        }
    }

    public void spawn(Player player) {
        this.initial_yaw = player.getLocation().getYaw();
        slots.values().forEach(slot -> slot.spawn(player));
    }

    public void remove(String reason) {
        slots.values().forEach(haloSlot -> haloSlot.remove(reason));
    }

    public int getMaxSlots() {
        return size;
    }

    @Override
    public boolean shouldRemove() {
        return shouldRemove;
    }

    @Override
    public void onChangeSlot(int fromSlot, int toSlot) {
        HaloSlot from = slots.get(fromSlot);
        HaloSlot to = slots.get(toSlot);

        if (fromSlot == -1)
            player.playSound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.1f, 10);

        if (from != to && fromSlot != -1)
            player.playSound(player, Sound.ENTITY_CHICKEN_STEP, 0.1f, -2);
    }

    @Override
    public void onClick() {
        player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.8f, 3f);
    }

    @Override
    public void tick() {
        int slot = getPlayerSlot();

        if (last_slot != slot) {
            onChangeSlot(last_slot, slot);
            last_slot = slot;
        }

        slots.values().forEach(HaloSlot::tick);
    }

    public int getPlayerSlot() {
        if (player == null)
            return -1;

        float yaw = (player.getLocation().getYaw() + 360.0f) % 360.0f;
        yaw = (yaw - initial_yaw + 360.0f) % 360.0f;

        float degreesPerSlot = 360f / this.size;

        return (int) (((yaw + degreesPerSlot / 2.0f) % 360.0f) / degreesPerSlot);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Player getPlayer() {
        return player;
    }

    public float getHeight() {
        return height;
    }

    public float getInitialYaw() {
        return initial_yaw;
    }
}
