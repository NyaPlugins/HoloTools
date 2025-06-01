package net.kokoricraft.holotools.utils;

import net.kokoricraft.holotools.HoloTools;
import net.kokoricraft.holotools.commands.Commands;
import net.kokoricraft.holotools.enums.DefaultFontInfo;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Utils {
    private final HoloTools plugin;
    private final Pattern hex_pattern = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
    private List<CommandSender> debuggers = new ArrayList<>();
    public Utils(HoloTools plugin){
        this.plugin = plugin;
    }

    public int getTextLength(String text) {
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for(char c : text.toCharArray()) {
            if(c == '§'){
                previousCode = true;
            } else if(previousCode){
                previousCode = false;
                isBold = (c == 'l' || c == 'L');
            }else {
                DefaultFontInfo defaultFontInfo = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? defaultFontInfo.getBoldLength() : defaultFontInfo.getLength();
                messagePxSize++;
            }
        }
        return messagePxSize;
    }

    public String serializeItemStack(ItemStack itemStack) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);

            BukkitObjectOutputStream objectStream = new BukkitObjectOutputStream(gzipStream);

            objectStream.writeObject(itemStack);
            objectStream.close();

            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (Exception exception) {
            //debug("Error transforming ItemStack to base64. "+exception);
            throw new IllegalStateException("Error transforming ItemStack to base64.", exception);
        }
    }

    public ItemStack deserializeItemStack(String serializedItem) {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(serializedItem));
            GZIPInputStream gzipStream = new GZIPInputStream(byteStream);

            BukkitObjectInputStream objectStream = new BukkitObjectInputStream(gzipStream);

            ItemStack itemStack = (ItemStack) objectStream.readObject();
            objectStream.close();

            return itemStack;
        } catch (IOException | ClassNotFoundException exception) {
            //debug("Error transforming base64 to ItemStack. "+exception);
            throw new IllegalStateException("Error transforming base64 to ItemStack.", exception);
        }
    }

    public ItemStack getHeadFromURL(String texture) {
        PlayerProfile profile =  Bukkit.createPlayerProfile(UUID.randomUUID(), "");
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerTextures textures = profile.getTextures();
        try{
            URL url = new URL(texture);
            textures.setSkin(url);
            profile.setTextures(textures);
        }catch(Exception ignored) {
        }

        assert meta != null;
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    public ItemStack getHeadFromURLDirect(String texture){
        return getHeadFromURL("http://textures.minecraft.net/texture/"+texture);
    }

    public String color(String text){
        return ChatColor.translateAlternateColorCodes('&', colorHex(text));
    }

    public List<String> color(List<String> list){
        List<String> parsed = new ArrayList<>();

        for(String line : list)
            parsed.add(color(line));

        return parsed;
    }

    private String colorHex(String text) {
        StringBuilder message = new StringBuilder();

        Matcher matcher = hex_pattern.matcher(text);

        int index = 0;
        while(matcher.find()) {
            message.append(text, index, matcher.start()).append(net.md_5.bungee.api.ChatColor.of((matcher.group().startsWith("&") ? matcher.group().substring(1) : matcher.group())));
            index = matcher.end();
        }
        return message.append(text.substring(index)).toString();
    }

    public void sendMessage(CommandSender sender, String message){
        sender.sendMessage(color(message));
    }

    public TextComponent getTextComponent(String text) {
        TextComponent base = new TextComponent();
        base.setItalic(false);

        String color = null;
        for(String part : hexSeparator(text)) {
            if(isHex(part)) {
                color = part;
            }else {
                TextComponent component = new TextComponent(color(part));
                if(color != null)
                    component.setColor(net.md_5.bungee.api.ChatColor.of(color.startsWith("&") ? color.substring(1) : color));
                base.addExtra(component);
            }
        }
        return base;
    }

    public Boolean isHex(String text) {
        if(text == null)
            return false;

        if(text.startsWith("&"))
            text = text.substring(1);

        Matcher matcher = hex_pattern.matcher(text);

        return matcher.matches();
    }


    public List<String> hexSeparator(String text) {
        List<String> texts = new ArrayList<>();

        Matcher matcher = hex_pattern.matcher(text);

        int index = 0;

        while(matcher.find()) {
            texts.add(text.substring(index, matcher.start()));
            texts.add(matcher.group().startsWith("&") ? matcher.group() : "&"+matcher.group());
            index = matcher.end();
        }

        texts.add(text.substring(index));
        return texts;
    }

    public void debug(String message){
        debuggers.forEach(sender -> sender.sendMessage(message));
    }

    public boolean toggleDebug(CommandSender sender){
        if(debuggers.contains(sender)){
            debuggers.remove(sender);
            return false;
        }

        debuggers.add(sender);
        return true;
    }

    public List<Player> getNearbyPlayers(Location location, double radius){
        List<Player> nearbyPlayers = new ArrayList<>();

        for(Player player : Bukkit.getOnlinePlayers()){
            if(player.getWorld().equals(location.getWorld())){
                if(player.getLocation().distance(location) <= radius)
                    nearbyPlayers.add(player);
            }
        }

        return nearbyPlayers;
    }

}