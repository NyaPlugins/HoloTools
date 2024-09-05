package net.kokoricraft.holotools.version;

import com.google.common.collect.ImmutableList;
import com.mojang.math.Transformation;
import io.netty.channel.*;
import net.kokoricraft.holotools.events.InventoryUpdateEvent;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftChatMessage;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class v1_19_R3 implements Compat{

    @Override
    public HoloTextDisplay createTextDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayText(players, location, yaw, pitch);
    }

    @Override
    public HoloItemDisplay createItemDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayItem(players, location, yaw, pitch);
    }

    @Override
    public void initPacketsRegister(Player player){
        try{
            ChannelPipeline pipeline = getPipeline((CraftPlayer) player);
            pipeline.addBefore("packet_handler", player.getName(), new ChannelDuplexHandler(){
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if(msg instanceof Packet<?> packet){
                        String name = packet.getClass().getName();
                        if(name.endsWith("PacketPlayOutSetSlot") || name.endsWith("ClientboundContainerSetSlotPacket")){
                            onPacketSend(player);
                        }
                    }
                    super.write(ctx, msg, promise);
                }
            });
        }catch (Exception exception){
            exception.printStackTrace();
        }
    }

    private ChannelPipeline getPipeline(CraftPlayer player){
        return getChannel(player).pipeline();
    }

    public Channel getChannel(CraftPlayer player){
        try{
            PlayerConnection playerConnection = player.getHandle().b;

            Field networkManagerField = PlayerConnection.class.getDeclaredField("h");// connection
            networkManagerField.setAccessible(true);

            NetworkManager networkManager = (NetworkManager) networkManagerField.get(playerConnection);

            return networkManager.m;
        }catch (Exception ignore){}
        return null;
    }

    private void onPacketSend(Player player) {
        InventoryUpdateEvent event = new InventoryUpdateEvent(player);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Override
    public void removePlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> getPipeline((CraftPlayer) player).remove(((CraftPlayer) player).getName()));
    }

    public static class HoloDisplayText implements HoloTextDisplay{
        private final List<Player> players;
        private final Display.TextDisplay textDisplay;
        private Location location;
        private final Packet<?> spawnPacket;

        public HoloDisplayText(List<Player> players, Location location, float yaw, float pitch){
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.textDisplay = new Display.TextDisplay(EntityTypes.aX, world);
            spawnPacket =  new PacketPlayOutSpawnEntity(textDisplay.af(), textDisplay.cs(), location.getX(), location.getY(), location.getZ(), yaw, pitch, textDisplay.ae(), 0, textDisplay.dj(), textDisplay.ck());
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(spawnPacket);
            });
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
            textDisplay.p(location.getX(), location.getY(), location.getZ()); //setPosRaw
            textDisplay.a(location.getX(), location.getY(), location.getZ()); //setPos
            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(textDisplay);
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(teleport);
            });

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(textDisplay.af()); //textDisplay.aj() = getId()
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(destroy);
            });
        }

        @Override
        public void setText(String text) {
            textDisplay.c(CraftChatMessage.fromString(text, true)[0]);
        }

        @Override
        public void setColor(HoloColor color) {
            int colorValue = color == null ? -1 : color.asARGB();
            textDisplay.c(colorValue);
        }

        @Override
        public void setGlowing(boolean glowing) {
            textDisplay.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(textDisplay.aj()); // ar = getDataWatcher
            Transformation transformation = new Transformation(nms.d(), nms.e(), new Vector3f(x, y, z), nms.g());
            textDisplay.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(textDisplay.aj());
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.d(), quaternionf, nms.f(), nms.g());
            textDisplay.a(transformation);
        }

        @Override
        public void setRotation(float v, float v2) {
            if(Float.isNaN(v))
                v = 0.0F;

            if(Float.isNaN(v2))
                v2 = 0.0F;

            textDisplay.t(v % 360.0F);
            textDisplay.u(v2 % 360.0F);
        }

        @Override
        public void setSeeThrough(boolean seeThrough) {
            setFlag(2, seeThrough);
        }

        @Override
        public void setLineWidth(int width) {
            textDisplay.b(width);
        }

        @Override
        public void setOpacity(byte opacity) {
            textDisplay.c(opacity);
        }

        @Override
        public void setShadowed(boolean shadow) {
            setFlag(1, shadow);
        }

        @Override
        public void setAlignment(TextDisplay.TextAlignment alignment) {
            switch (alignment) {
                case LEFT:
                    setFlag(8, true);
                    setFlag(16, false);
                    return;
                case RIGHT:
                    setFlag(8, false);
                    setFlag(16, true);
                    return;
                case CENTER:
                    setFlag(8, false);
                    setFlag(16, false);
            }
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.a(textDisplay.aj());
            Transformation transformation = new Transformation(new Vector3f(x, y+.3f, z), nms.e(), nms.f(), nms.g());
            textDisplay.a(transformation);
        }

        @Override
        public void update() {
            internalUpdate();
        }


        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public void setBrightness(org.bukkit.entity.Display.Brightness bukkitBrightness) {
            Brightness brightness = new Brightness(bukkitBrightness.getBlockLight(), bukkitBrightness.getSkyLight());
            textDisplay.a(brightness);
        }

        @Override
        public void mount(Player target) {
            Entity entityPlayer = ((CraftPlayer)target).getHandle();
            List<Entity> list = new ArrayList<>(entityPlayer.r);
            if(!list.contains(textDisplay))
                list.add(textDisplay);

            entityPlayer.r = ImmutableList.copyOf(list);
            PacketPlayOutMount packet = new PacketPlayOutMount(entityPlayer);

            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(packet);
            });
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            textDisplay.a(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(textDisplay.af(), textDisplay.aj().c());
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(metadata);
            });
        }

        public void setFlag(int flag, boolean set){
            byte flagBits = textDisplay.q();
            if (set) {
                flagBits = (byte)(flagBits | flag);
            } else {
                flagBits = (byte)(flagBits & (~flag));
            }
            textDisplay.d(flagBits);
        }
    }

    public static class HoloDisplayItem implements HoloItemDisplay{

        private List<Player> players = new ArrayList<>();
        private final Display.ItemDisplay itemDisplay;
        private Location location;
        private final Packet<?> spawnPacket;

        public HoloDisplayItem(List<Player> players, Location location, float yaw, float pitch){
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.itemDisplay = new Display.ItemDisplay(EntityTypes.ae, world);
            
            spawnPacket = new PacketPlayOutSpawnEntity(itemDisplay.af(), itemDisplay.cs(), location.getX(), location.getY(), location.getZ(), pitch, yaw, itemDisplay.ae(), 0, itemDisplay.dj(), itemDisplay.ck());

            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(spawnPacket);
            });
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
            itemDisplay.p(location.getX(), location.getY(), location.getZ()); //setPosRaw
            itemDisplay.a(location.getX(), location.getY(), location.getZ());
            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(itemDisplay);
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(teleport);
            });

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(itemDisplay.af());
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(destroy);
            });
        }

        @Override
        public void setGlowing(boolean glowing) {
            itemDisplay.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(itemDisplay.aj());
            Transformation transformation = new Transformation(nms.d(), nms.e(), new Vector3f(-x, y, -z), nms.g());
            itemDisplay.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(itemDisplay.aj());
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.d(), quaternionf, nms.f(), nms.g());
            itemDisplay.a(transformation);
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.a(itemDisplay.aj());
            Transformation transformation = new Transformation(new Vector3f(x, y+.3f, z), nms.e(), nms.f(), nms.g());
            itemDisplay.a(transformation);
        }

        @Override
        public void setRotation(float v, float v2) {
            if(Float.isNaN(v))
                v = 0.0F;

            if(Float.isNaN(v2))
                v2 = 0.0F;

            itemDisplay.t(v % 360.0F);
            itemDisplay.u(v2 % 360.0F);
        }

        @Override
        public void update() {
            internalUpdate();
        }

        @Override
        public void mount(Player target) {
            Entity entityPlayer = ((CraftPlayer)target).getHandle();
            List<Entity> list = new ArrayList<>(entityPlayer.r);
            if(!list.contains(itemDisplay))
                list.add(itemDisplay);

            entityPlayer.r = ImmutableList.copyOf(list);
            PacketPlayOutMount packet = new PacketPlayOutMount(entityPlayer);

            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(packet);
            });
        }

        @Override
        public void setItemStack(ItemStack itemStack) {
            net.minecraft.world.item.ItemStack nmsgItemStack = CraftItemStack.asNMSCopy(itemStack);
            itemDisplay.a(nmsgItemStack);
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(itemDisplay.af(), itemDisplay.aj().c());
            players.forEach(player -> ((CraftPlayer)player).getHandle().b.a(metadata));
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public void setItemDisplayTransform(ItemDisplay.ItemDisplayTransform transform) {
            itemDisplay.a(ItemDisplayContext.k.apply(transform.ordinal()));
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            itemDisplay.a(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        @Override
        public void setViewRange(float range) {
            itemDisplay.g(range);
        }

        @Override
        public void setBrightness(org.bukkit.entity.Display.Brightness bukkitBrightness) {
            Brightness brightness = new Brightness(bukkitBrightness.getBlockLight(), bukkitBrightness.getSkyLight());
            itemDisplay.a(brightness);
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(itemDisplay.af(), itemDisplay.aj().c());
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().b.a(metadata);
            });
        }
    }
}
