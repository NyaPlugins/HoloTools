package net.kokoricraft.holotools.objects.holocrafter;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.objects.halo.HaloSlot;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.kokoricraft.holotools.version.HoloItemDisplay;
import net.kokoricraft.holotools.version.HoloTextDisplay;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class HoloCrafterSlot {
    private final HoloTools plugin = HoloTools.getInstance();
    private final HaloSlot haloSlot;
    private final static float text_y_crafting_table = 0f;
    private final static float text_y = 0.2f;
    private Recipe recipe;
    private final Player player;
    private final boolean isCraftingTable;
    private HoloTextDisplay textDisplay;
    private HoloItemDisplay display;
    private final int size;

    public HoloCrafterSlot(HaloSlot haloSlot, Recipe recipe, Player player, int size) {
        this.haloSlot = haloSlot;
        this.recipe = recipe;
        this.player = player;
        this.isCraftingTable = haloSlot.getSlot() == 0;
        this.size = size;
    }

    public void spawn() {
        List<Player> players = plugin.getManager().getHoloPlayerView(player);
        if (isCraftingTable) {
            HoloItemDisplay craftingTable = plugin.getCompatManager().createItemDisplay(players, player.getLocation(), getYaw(), 0);
            craftingTable.setScale(0.95f, 0.95f, 0.95f);
            craftingTable.setTranslation(0, -1 + 0.3f, 1.9f);
            craftingTable.setBrightness(new Display.Brightness(15, 15));
            craftingTable.setItemStack(new ItemStack(Material.CRAFTING_TABLE));
            craftingTable.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            craftingTable.update();
            craftingTable.mount(player);
            haloSlot.addItemDisplay("holo_crafter_crafting_table_" + haloSlot.getSlot(), craftingTable);
        } else if (recipe != null && recipe.getResult().getType() != Material.AIR) {
            HoloItemDisplay result = plugin.getCompatManager().createItemDisplay(players, player.getLocation(), getYaw(), 0);
            result.setScale(0.5f, 0.5f, 0.5f);
            result.setTranslation(0, -1 + 0.3f, 1.95f);
            result.setBrightness(new Display.Brightness(15, 15));
            result.setItemStack(recipe.getResult());
            result.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            result.update();
            result.mount(player);
            display = result;
            haloSlot.addItemDisplay("holo_crafter_result_" + haloSlot.getSlot(), result);
        }
        if (textDisplay == null) {
            textDisplay = plugin.getCompatManager().createTextDisplay(players, player.getLocation(), 0, getYaw() + 180);
            textDisplay.setScale(0.3f, 0.3f, 0.3f);
            textDisplay.setTranslation(0, 0 + 0.3f, -1.9f);
            textDisplay.setLineWidth(200);
            textDisplay.setBrightness(new Display.Brightness(15, 15));
            textDisplay.setColor(HoloColor.fromARGB(0, 0, 0, 0));
            if (isCraftingTable) {
                setText(plugin.getUtils().color(plugin.getLangManager().CRAFTER_CLICK_TO_OPEN));
            } else {
                textDisplay.update();
            }
            textDisplay.mount(player);

            haloSlot.addTextDisplay("holo_crafter_text" + haloSlot.getSlot(), textDisplay);
        }
    }

    public void setText(String text) {
        if (textDisplay == null) return;
        textDisplay.setText(text);

        int height_movement = 0;
        if (text != null && !text.isEmpty()) {
            int textSize = JavaPlugin.getPlugin(HoloTools.class).getUtils().getTextLength(text);
            height_movement = (int) Math.ceil((double) textSize / 200);
        }

        if (height_movement != 0)
            height_movement--;

        textDisplay.setTranslation(0f, (float) ((isCraftingTable ? text_y_crafting_table : text_y) - (height_movement * 0.075)), -1.9f);
        textDisplay.update();
    }

    private float getYaw() {
//        return (45 * haloSlot.getSlot() + haloSlot.getInitialYaw());
        return 360.0f / size * haloSlot.getSlot() + (haloSlot.getInitialYaw());
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;

        if (display == null) {
            spawn();
            return;
        }

        if (recipe != null)
            display.setItemStack(recipe.getResult());

        display.setViewRange(recipe == null ? 0 : 20);
        display.update();
    }
}
