package net.kokoricraft.holotools.version;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

public interface HoloItemDisplay {
    void update(Location location);
    void remove();
    void setGlowing(boolean glowing);
    void setScale(float x, float y, float z);
    void setRotation(float x, float y, float z);
    void setTranslation(float x, float y, float z);
    void setRotation(float v, float v2);
    void update();
    void mount(Entity target);
    void setItemStack(ItemStack itemStack);
    Location getLocation();
    void setItemDisplayTransform(ItemDisplay.ItemDisplayTransform transform);
    void setBillboard(Display.Billboard billboard);
    void setViewRange(float range);
    void setBrightness(Display.Brightness brightness);
    void interpolation(int delay, int duration);
}
