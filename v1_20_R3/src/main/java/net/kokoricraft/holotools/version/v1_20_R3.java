package net.kokoricraft.holotools.version;

import com.google.common.collect.ImmutableList;
import com.mojang.math.Transformation;
import io.netty.channel.*;
import net.kokoricraft.holotools.events.InventoryUpdateEvent;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;

public class v1_20_R3 implements Compat{
    public final Map<Integer, Map<Integer, Entity>> passengers = new HashMap<>();

    @Override
    public HoloTextDisplay createTextDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayText(players, location, yaw, pitch, this);
    }

    @Override
    public HoloItemDisplay createItemDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayItem(players, location, yaw, pitch, this);
    }
    public void remove(int targetID, int entity){
        Map<Integer, Entity> entities = passengers.getOrDefault(targetID, new HashMap<>());
        entities.remove(entity);
        passengers.put(targetID, entities);
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

                        if(name.endsWith("ClientboundSetPassengersPacket") || name.endsWith("PacketPlayOutMount")){
                            boolean isPaper = name.endsWith("ClientboundSetPassengersPacket");
                            String vehicleFieldName = isPaper ? "vehicle" : "a";
                            String passengersFieldName = isPaper ? "passengers" : "b";

                            try {
                                Field targetField = msg.getClass().getDeclaredField(vehicleFieldName);
                                targetField.setAccessible(true);
                                int targetID = targetField.getInt(msg);

                                Field passengersField = msg.getClass().getDeclaredField(passengersFieldName);
                                passengersField.setAccessible(true);
                                int[] passengersID = (int[]) passengersField.get(msg);

                                Map<Integer, Entity> entities = passengers.getOrDefault(targetID, new HashMap<>());

                                int[] newPassengersID = new int[passengersID.length + entities.size()];

                                System.arraycopy(passengersID, 0, newPassengersID, 0, passengersID.length);

                                int index = passengersID.length;
                                for (Integer entityID : entities.keySet()) {
                                    newPassengersID[index++] = entityID;
                                }

                                passengersField.set(msg, newPassengersID);

                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
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
            ServerCommonPacketListenerImpl serverCommonPacketListener = player.getHandle().c;

            Field networkManagerField = ServerCommonPacketListenerImpl.class.getDeclaredField("c");// connection
            networkManagerField.setAccessible(true);

            NetworkManager networkManager = (NetworkManager) networkManagerField.get(serverCommonPacketListener);

            return networkManager.n;
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

    @Override
    public List<BaseComponent> getToolTip(ItemStack itemStack, Player player, boolean advanced) {
        return null;
    }

    public static class HoloDisplayText implements HoloTextDisplay{
        private final List<Player> players;
        private final Display.TextDisplay textDisplay;
        private Location location;
        private final Packet<?> spawnPacket;
        private final v1_20_R3 manager;
        private Player target;

        public HoloDisplayText(List<Player> players, Location location, float yaw, float pitch, Compat manager){
            this.manager = (v1_20_R3) manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.textDisplay = new Display.TextDisplay(EntityTypes.aY, world);
            spawnPacket =  new PacketPlayOutSpawnEntity(textDisplay.aj(), textDisplay.cw(), location.getX(), location.getY(), location.getZ(), yaw, pitch, textDisplay.ai(), 0, textDisplay.dp(), textDisplay.cp());
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(spawnPacket);
            });
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(textDisplay);
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(teleport);
            });

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(textDisplay.aj()); //textDisplay.al() = getId()
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(destroy);
            });
            manager.remove(target.getEntityId(), textDisplay.aj());
        }

        @Override
        public void setText(String text) {
            textDisplay.c(CraftChatMessage.fromString(text, true)[0]);
        }

        @Override
        public void setColor(HoloColor color) {
            int colorValue = color == null ? -1 : color.asARGB();
            textDisplay.an().b(Display.TextDisplay.aO, colorValue); //CraftTextDisplay.setBackgroundColor
        }

        @Override
        public void setGlowing(boolean glowing) {
            textDisplay.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(textDisplay.an()); // ar = getDataWatcher
            Transformation transformation = new Transformation(nms.d(), nms.e(), new Vector3f(x, y, z), nms.g());
            textDisplay.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(textDisplay.an());
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
            textDisplay.an().b(Display.TextDisplay.aO, width);
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
            Transformation nms = Display.a(textDisplay.an());
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.e(), nms.f(), nms.g());
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
        public void setViewRange(float range) {
            textDisplay.b(range);
        }

        @Override
        public void setTextOpacity(byte opacity) {

        }

        @Override
        public void setText(List<BaseComponent> components) {
            ComponentBuilder builder = new ComponentBuilder();
            for(BaseComponent component : components){
                builder.append(component).append("\n");
            }

            String string = ComponentSerializer.toString(builder.create()[0]);
            textDisplay.c(CraftChatMessage.fromString(string, true)[0]);
        }

        @Override
        public void mount(Player target) {
            this.target =  target;
            Entity entityPlayer = ((CraftPlayer)target).getHandle();
            List<Entity> list = new ArrayList<>(entityPlayer.r);
            List<Entity> backup = new ArrayList<>(list);
            Map<Integer, Entity> entities = manager.passengers.getOrDefault(target.getEntityId(), new HashMap<>());
            entities.put(textDisplay.aj(), textDisplay);
            manager.passengers.put(target.getEntityId(), entities);

            if(!list.contains(textDisplay))
                list.add(textDisplay);

            entityPlayer.r = ImmutableList.copyOf(list);
            PacketPlayOutMount packet = new PacketPlayOutMount(entityPlayer);

            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(packet);
            });

            entityPlayer.r = ImmutableList.copyOf(backup);
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            textDisplay.a(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(textDisplay.aj(), textDisplay.an().c());
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(metadata);
            });
        }

        public void setFlag(int flag, boolean set){
            byte flagBits = textDisplay.z();
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
        private final v1_20_R3 manager;
        private Player target;

        public HoloDisplayItem(List<Player> players, Location location, float yaw, float pitch, Compat manager){
            this.manager = (v1_20_R3) manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.itemDisplay = new Display.ItemDisplay(EntityTypes.af, world);
            spawnPacket = new PacketPlayOutSpawnEntity(itemDisplay.aj(), itemDisplay.cw(), location.getX(), location.getY(), location.getZ(), pitch, yaw, itemDisplay.ai(), 0, itemDisplay.dp(), itemDisplay.cp());

            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(spawnPacket);
            });
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(itemDisplay);
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(teleport);
            });

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(itemDisplay.aj());
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(destroy);
            });
            manager.remove(target.getEntityId(), itemDisplay.aj());
        }

        @Override
        public void setGlowing(boolean glowing) {
            itemDisplay.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(itemDisplay.an());
            Transformation transformation = new Transformation(nms.d(), nms.e(), new Vector3f(x, y, z), nms.g());
            itemDisplay.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(itemDisplay.an());
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.d(), quaternionf, nms.f(), nms.g());
            itemDisplay.a(transformation);
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.a(itemDisplay.an());
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.e(), nms.f(), nms.g());
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
            this.target =  target;
            Entity entityPlayer = ((CraftPlayer)target).getHandle();
            List<Entity> list = new ArrayList<>(entityPlayer.r);
            List<Entity> backup = new ArrayList<>(list);
            Map<Integer, Entity> entities = manager.passengers.getOrDefault(target.getEntityId(), new HashMap<>());
            entities.put(itemDisplay.aj(), itemDisplay);
            manager.passengers.put(target.getEntityId(), entities);

            if(!list.contains(itemDisplay))
                list.add(itemDisplay);

            entityPlayer.r = ImmutableList.copyOf(list);
            PacketPlayOutMount packet = new PacketPlayOutMount(entityPlayer);

            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(packet);
            });

            entityPlayer.r = ImmutableList.copyOf(backup);
        }

        @Override
        public void setItemStack(ItemStack itemStack) {
            net.minecraft.world.item.ItemStack nmsgItemStack = CraftItemStack.asNMSCopy(itemStack);
            itemDisplay.a(nmsgItemStack);
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(itemDisplay.aj(), itemDisplay.an().c());
            players.forEach(player -> ((CraftPlayer)player).getHandle().c.b(metadata));
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
            itemDisplay.b(range);
        }

        @Override
        public void setBrightness(org.bukkit.entity.Display.Brightness bukkitBrightness) {
            Brightness brightness = new Brightness(bukkitBrightness.getBlockLight(), bukkitBrightness.getSkyLight());
            itemDisplay.a(brightness);
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(itemDisplay.aj(), itemDisplay.an().c());
            players.forEach(player -> {
                ((CraftPlayer)player).getHandle().c.b(metadata);
            });
        }
    }
}
