package net.kokoricraft.holotools.version;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;

public interface HoloBlockDisplay {
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
    void setBlock(Material material);
    void setBillboard(Display.Billboard billboard);
    void setViewRange(float range);
    void setBrightness(Display.Brightness brightness);
    void interpolation(int delay, int duration);
    void setLeftRotation(Quaternionf rotation);
}
