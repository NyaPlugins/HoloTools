package net.kokoricraft.holotools.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;

import joptsimple.internal.Strings;
import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.kokoricraft.holotools.version.HoloItemDisplay;
import net.kokoricraft.holotools.version.HoloTextDisplay;

public class Commands implements CommandExecutor {
    private final HoloTools plugin;
    private final boolean test = true;
    public static float test1 = 0f;
    public static float test2 = 21f;

    public Commands(HoloTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(plugin.getUtils().color(String.format("&cUse %s give / reload", label)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> giveCommand(sender, args);
            case "reload" -> reloadCommand(sender);
            case "debug" -> debugCommand(sender);
            case "test3" -> testCommand(sender, args);
            case "test2" -> test2(sender, args);
            case "test" -> test(sender, args);
        }

        return true;
    }

    private void test(CommandSender sender, String[] args) {
        if (!test) return;

//        if (sender instanceof Player player) {
////            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
//            v1_21_R5 version = (v1_21_R5) plugin.getCompatManager().getCompat();
//            version.test();
//            sender.sendMessage(".");
//        }
    }

    private void testCommand(CommandSender sender, String[] args) {
        if (!test) return;
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getUtils().color("&cYou must be a player to do that"));
            return;
        }
        Player player = (Player) sender;

        Location location = player.getLocation().clone();

        float size = 0.5f;

        int rows = 4;

        float translation_per_row = -0.015f;

        float y_translation = (translation_per_row * rows) - translation_per_row;

        HoloTextDisplay d1 = plugin.getCompatManager().createTextDisplay(List.of(player), location, 0, 0);
        d1.setText(getText(2));
        d1.setTextOpacity((byte) 10);
        d1.setLineWidth(999999999);
        d1.setColor(HoloColor.fromHex("#ee87d4", 255));
        d1.setTranslation(0, y_translation * size, -1.5f);
        d1.setScale(1 * size, (1 + (.45f / rows)) * size, 1 * size);
        d1.setAlignment(TextDisplay.TextAlignment.LEFT);
        d1.setBillboard(Display.Billboard.CENTER);
        d1.mount(player);
        d1.update();

        HoloTextDisplay d2 = plugin.getCompatManager().createTextDisplay(List.of(player), location, 0, 0);
        d2.setText(getText(0));
        d2.setLineWidth(999999999);
        d2.setColor(HoloColor.fromHex("#2e2b00", 255));
        d2.setTranslation(0, 0, -1.48f);
        d2.setAlignment(TextDisplay.TextAlignment.LEFT);
        d2.setBillboard(Display.Billboard.CENTER);
        d2.setScale(1 * size, 1 * size, 1 * size);
        d2.mount(player);
        d2.setShadowed(true);
        d2.update();


        HoloItemDisplay d3 = plugin.getCompatManager().createItemDisplay(List.of(player), location, 0, 0);
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwningPlayer(player);
        itemStack.setItemMeta(skullMeta);
        d3.setItemStack(itemStack);
        d3.setTranslation(0, 0, -1.4f);
        d3.setScale(-1 * size, 1 * size, -1 * size);
        d3.mount(player);
        d3.setBillboard(Display.Billboard.CENTER);
        d3.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
        d3.setRotation(0, 5, 0);
        d3.update();

        sender.sendMessage("test");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            d1.remove();
            d2.remove();
            d3.remove();
        }, 20 * 15);
    }

    public void test3(CommandSender sender, String[] args) {
        if (!test) return;
        if (!(sender instanceof Player player)) return;
        Location location = player.getLocation();

        List<HoloTextDisplay> displays = new ArrayList<>();

        float scale = 0.5f;

        for (int i = 0; i < 30; i++) {
            HoloTextDisplay main = plugin.getCompatManager().createTextDisplay(List.of(player), location, 0, 0);
            String separators = StringUtils.repeat("....", i);
            main.setBillboard(Display.Billboard.VERTICAL);
            main.setText("A\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA".replaceAll("A", separators));
            main.setColor(HoloColor.fromARGB(26, 255, 0, 0));
            main.setTextOpacity((byte) 20);
            main.setBrightness(new Display.Brightness(15, 15));
            main.setScale(0.03f, 1 * scale, .3f);
            main.setTranslation(0, 0.003f * i, 0.0001f * i);
            main.setLineWidth(99999999);
            main.update();
            displays.add(main);
        }


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            displays.forEach(HoloTextDisplay::remove);
        }, 20 * 8);
    }

    public void test2(CommandSender sender, String[] args) {
        if (!test) return;
        if (!(sender instanceof Player player)) return;
        Location location = player.getLocation();

        ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class);

        display.setItemStack(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        Transformation transformation = display.getTransformation();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            display.setInterpolationDelay(1);
            display.setInterpolationDuration(20 * 10);
            transformation.getLeftRotation().rotateXYZ((float) Math.toRadians(0), (float) Math.toRadians(90), (float) Math.toRadians(0));
            display.setTransformation(transformation);
        }, 10);
    }

    public String getText(int i) {
        List<String> list = new ArrayList<>();
        String points = Strings.repeat('.', i);
        if (i == 0)
            points = "";

        list.add(points + "&#2e2b00Esto solo es una &#2e2b00prueba&#2e2b00 para ver si" + points);
        list.add(points + "&#2e2b00Sirve hacer un plugin de cinematicas" + points);
        list.add(points + "&#2e2b00o abandonarlo y yas. de todas formas" + points);
        list.add(points + "&#2e2b00no pasa nada por probar asi que &o:cat:" + points);

        return plugin.getUtils().color(String.join("\n", list));
    }

    private void debugCommand(CommandSender sender) {
        if (!(sender.hasPermission("holotools.command.debug"))) {
            sender.sendMessage(plugin.getUtils().color(plugin.getLangManager().NO_PERMISSION));
            return;
        }

        boolean enabled = plugin.getUtils().toggleDebug(sender);
        sender.sendMessage(plugin.getUtils().color(enabled ? "&aDebug mode enabled!" : "&cDebug mode disabled!"));
    }

    private void giveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("holotools.command.give")) {
            sender.sendMessage(plugin.getUtils().color(plugin.getLangManager().NO_PERMISSION));
            return;
        }

        if (args.length < 2 || !(sender instanceof Player) && args.length < 3) {
            sender.sendMessage(plugin.getUtils().color("&cIncorrect usage. Use: /holotools give <item> [player] [amount]"));
            return;
        }

        String itemName = args[1].toLowerCase();
        Player target = null;
        int amount = 0;
        if (args.length == 2 && sender instanceof Player) {
            target = (Player) sender;
            amount = 1;
        }
        if (args.length == 3 && sender instanceof Player) {
            target = (Player) sender;
            try {
                amount = Integer.valueOf(args[2]);
            } catch (NumberFormatException e) {
                target = Bukkit.getPlayer(args[2]);
                amount = 1;
            }
        }
        if (args.length == 3 && !(sender instanceof Player)) {
            target = Bukkit.getPlayer(args[2]);
            amount = 1;
        }
        if (args.length == 4) {
            target = Bukkit.getPlayer(args[2]);
            try {
                amount = Integer.valueOf(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getUtils().color("&cIncorrect usage. Use: /holotools give <item> <player> <amount>"));
                return;
            }
        }
        if (target == null) {
            sender.sendMessage(plugin.getUtils().color("&cPlayer not found."));
            return;
        }
        if (amount <= 0) {
            sender.sendMessage(plugin.getUtils().color("&cAmount cannot be 0 or negative."));
            return;
        }

        ItemStack item;
        switch (itemName) {
            case "holocrafter" ->
                    item = plugin.getConfigManager().CRAFTER_ITEM.getItem("id", plugin.getDataManager().getStorage().getNextID());
            case "holowardrobe" ->
                    item = plugin.getConfigManager().WARDROBE_ITEM.getItem("id", plugin.getDataManager().getStorage().getNextID());
            case "holobridge" ->
                item = plugin.getConfigManager().BRIDGE_ITEM.getItem("id", plugin.getDataManager().getStorage().getNextID());
            default -> {
                sender.sendMessage(plugin.getUtils().color("&cTool does not exist."));
                return;
            }
        }

        item.setAmount(amount);

        target.getInventory().addItem(item);
        target.sendMessage(plugin.getUtils().color(String.format("&aYou have received %d %s(s).", amount, itemName)));
        if (sender != target)
            sender.sendMessage(plugin.getUtils().color(String.format("&aYou have given %d %s(s) to %s.", amount, itemName, target.getName())));
    }

    private void reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("holotools.command.reload")) {
            sender.sendMessage(plugin.getUtils().color(plugin.getLangManager().NO_PERMISSION));
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage(plugin.getUtils().color(plugin.getLangManager().CONFIG_RELOADED));
    }
}