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
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.TooltipFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;

public class v1_20_R1 implements Compat{
    private final Map<Integer, List<Entity>> passengers = new HashMap<>();

    @Override
    public HoloTextDisplay createTextDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayText(players, location, yaw, pitch, this);
    }

    @Override
    public HoloItemDisplay createItemDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayItem(players, location, yaw, pitch, this);
    }

    @Override
    public void initPacketsRegister(Player player){
        try{
            ChannelPipeline pipeline = getPipeline((CraftPlayer) player);

            pipeline.addBefore("packet_handler", String.format("Holo_%s", player.getName()), new ChannelDuplexHandler(){
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if(msg instanceof Packet<?> packet){
                        String name = packet.getClass().getName();
                        if(name.endsWith("PacketPlayOutSetSlot") || name.endsWith("ClientboundContainerSetSlotPacket")){
                            onPacketSend(player, packet);
                        }

                        if(name.endsWith("ClientboundSetPassengersPacket") || name.endsWith("PacketPlayOutMount")){
                            onMountPacketSend(msg);
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
            PlayerConnection serverCommonPacketListener = player.getHandle().c;

            Field networkManagerField = PlayerConnection.class.getDeclaredField("h");// connection
            networkManagerField.setAccessible(true);

            NetworkManager networkManager = (NetworkManager) networkManagerField.get(serverCommonPacketListener);

            return networkManager.m;
        }catch (Exception ignore){}
        return null;
    }

    private void onPacketSend(Player player, Packet<?> packet) {
        InventoryUpdateEvent event = new InventoryUpdateEvent(player);
        Bukkit.getPluginManager().callEvent(event);
    }

    private void onMountPacketSend(Object msg){
        boolean isPaper = msg.getClass().getName().endsWith("ClientboundSetPassengersPacket");
        String vehicleFieldName = isPaper ? "vehicle" : "a";
        String passengersFieldName = isPaper ? "passengers" : "b";

        try {
            Field targetField = msg.getClass().getDeclaredField(vehicleFieldName);
            targetField.setAccessible(true);
            int targetID = targetField.getInt(msg);

            if(!passengers.containsKey(targetID)) return;

            Field passengersField = msg.getClass().getDeclaredField(passengersFieldName);
            passengersField.setAccessible(true);
            int[] passengersID = (int[]) passengersField.get(msg);

            List<Entity> entities = new ArrayList<>(passengers.get(targetID));

            int[] newPassengersID = new int[passengersID.length + entities.size()];

            System.arraycopy(passengersID, 0, newPassengersID, 0, passengersID.length);

            int index = passengersID.length;
            for (Entity entity : entities) {
                int entityID = getEntityID(entity);
                newPassengersID[index++] = entityID;
            }

            passengersField.set(msg, newPassengersID);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void removePlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> getPipeline((CraftPlayer) player).remove(String.format("Holo_%s", player.getName())));
    }

    @Override
    public List<BaseComponent> getToolTip(ItemStack itemStack, Player player, boolean advanced) {
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);

        EntityHuman entityPlayer =  ((CraftPlayer)player).getHandle();

        List<IChatBaseComponent> list = nmsItemStack.a(entityPlayer, advanced ? TooltipFlag.b : TooltipFlag.a);

        List<BaseComponent> components = new ArrayList<>();

        for(IChatBaseComponent baseComponent : list){
            String json = CraftChatMessage.toJSON(baseComponent);
            components.add(ComponentSerializer.parse(json)[0]);
        }
        return components;
    }

    public int getEntityID(Entity entity){
        return entity.af();
    }

    public DataWatcher getDataWatcher(Entity entity){
        return entity.aj();
    }

    public void sendPacket(List<Player> players, Packet<?> packet){
        players.forEach(player -> {
            ((CraftPlayer)player).getHandle().c.a(packet);
        });
    }

    public void removePassengers(org.bukkit.entity.Entity target, Entity passenger){
        if(target == null) return;
        List<Entity> entities = new ArrayList<>(passengers.getOrDefault(target.getEntityId(), new ArrayList<>()));
        entities.remove(passenger);
        passengers.put(target.getEntityId(), entities);
    }

    public void mount(List<Player> players, org.bukkit.entity.Entity target, Entity passenger){
        List<Entity> entities = passengers.getOrDefault(target.getEntityId(), new ArrayList<>());
        if(!entities.contains(passenger))
            entities.add(passenger);

        passengers.put(target.getEntityId(), entities);

        PacketPlayOutMount packet = new PacketPlayOutMount(((CraftPlayer)target).getHandle());
        sendPacket(players, packet);
    }

    public static class HoloDisplayText implements HoloTextDisplay{
        private final List<Player> players;
        private final Display.TextDisplay textDisplay;
        private Location location;
        private final Packet<?> spawnPacket;
        private org.bukkit.entity.Entity target;
        private final v1_20_R1 manager;

        public HoloDisplayText(List<Player> players, Location location, float yaw, float pitch, v1_20_R1 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.textDisplay = new Display.TextDisplay(EntityTypes.aX, world);
            spawnPacket =  new PacketPlayOutSpawnEntity(textDisplay.af(), textDisplay.ct(), location.getX(), location.getY(), location.getZ(), yaw, pitch, textDisplay.ae(), 0, textDisplay.dl(), textDisplay.cm());

            manager.sendPacket(players, spawnPacket);
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(textDisplay);
            manager.sendPacket(players, teleport);

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(manager.getEntityID(textDisplay)); //textDisplay.al() = getId()
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, textDisplay);
        }

        @Override
        public void setText(String text) {
            textDisplay.c(CraftChatMessage.fromString(text, true)[0]);
        }

        @Override
        public void setColor(HoloColor color) {
            int colorValue = color == null ? -1 : color.asARGB();
            manager.getDataWatcher(textDisplay).b(Display.TextDisplay.aN, colorValue); //CraftTextDisplay.setBackgroundColor
        }

        @Override
        public void setGlowing(boolean glowing) {
            textDisplay.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(textDisplay)); // ar = getDataWatcher
            Transformation transformation = new Transformation(nms.d(), nms.e(), new Vector3f(x, y, z), nms.g());
            textDisplay.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(textDisplay));
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
            manager.getDataWatcher(textDisplay).b(Display.TextDisplay.aM, width);
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
            Transformation nms = Display.a(manager.getDataWatcher(textDisplay));
            Transformation transformation = new Transformation(new Vector3f(x, y+.43f, z), nms.e(), nms.f(), nms.g());
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
            textDisplay.s(range);
        }

        @Override
        public void setTextOpacity(byte opacity) {
            textDisplay.c(opacity);
        }

        @Override
        public void setText(List<BaseComponent> components) {
            List<IChatBaseComponent> iChatBaseComponents = new ArrayList<>();

            for(BaseComponent baseComponent : components){
                String json = ComponentSerializer.toString(baseComponent);
                iChatBaseComponents.add(CraftChatMessage.fromJSONOrString(json, true));
            }

            IChatBaseComponent empty = IChatBaseComponent.h();

            IChatBaseComponent mutableComponent = ChatComponentUtils.a(iChatBaseComponents, empty);

            textDisplay.c(mutableComponent);
        }

        @Override
        public void interpolation(int delay, int duration) {
            Display display = textDisplay;
            display.c(delay);
            display.b(duration);
        }

        @Override
        public void mount(org.bukkit.entity.Entity target) {
            this.target = target;
            manager.mount(players, target, textDisplay);
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            textDisplay.a(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(manager.getEntityID(textDisplay), manager.getDataWatcher(textDisplay).c());
            manager.sendPacket(players, metadata);
        }

        public void setFlag(int flag, boolean set){
            byte flagBits = textDisplay.t();
            if (set) {
                flagBits = (byte)(flagBits | flag);
            } else {
                flagBits = (byte)(flagBits & (~flag));
            }
            textDisplay.d(flagBits);
        }
    }

    public static class HoloDisplayItem implements HoloItemDisplay{

        private final List<Player> players;
        private final Display.ItemDisplay itemDisplay;
        private Location location;
        private final Packet<?> spawnPacket;
        private org.bukkit.entity.Entity target;
        private final v1_20_R1 manager;

        public HoloDisplayItem(List<Player> players, Location location, float yaw, float pitch, v1_20_R1 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.itemDisplay = new Display.ItemDisplay(EntityTypes.ae, world);
            spawnPacket = new PacketPlayOutSpawnEntity(itemDisplay.af(), itemDisplay.ct(), location.getX(), location.getY(), location.getZ(), pitch, yaw, itemDisplay.ae(), 0, itemDisplay.dl(), itemDisplay.cm());

            manager.sendPacket(players, spawnPacket);
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(itemDisplay);
            manager.sendPacket(players, teleport);

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(manager.getEntityID(itemDisplay));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, itemDisplay);
        }

        @Override
        public void setGlowing(boolean glowing) {
            itemDisplay.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(itemDisplay));
            Transformation transformation = new Transformation(nms.d(), nms.e(), new Vector3f(x, y, z), nms.g());
            itemDisplay.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(itemDisplay));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.d(), quaternionf, nms.f(), nms.g());
            itemDisplay.a(transformation);
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(itemDisplay));
            Transformation transformation = new Transformation(new Vector3f(x, y+.43f, z), nms.e(), nms.f(), nms.g());
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
        public void mount(org.bukkit.entity.Entity target) {
            this.target = target;
            manager.mount(players, target, itemDisplay);
        }

        @Override
        public void setItemStack(ItemStack itemStack) {
            net.minecraft.world.item.ItemStack nmsgItemStack = CraftItemStack.asNMSCopy(itemStack);
            itemDisplay.a(nmsgItemStack);
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(manager.getEntityID(itemDisplay), manager.getDataWatcher(itemDisplay).c());
            manager.sendPacket(players, metadata);
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
            itemDisplay.s(range);
        }

        @Override
        public void setBrightness(org.bukkit.entity.Display.Brightness bukkitBrightness) {
            Brightness brightness = new Brightness(bukkitBrightness.getBlockLight(), bukkitBrightness.getSkyLight());
            itemDisplay.a(brightness);
        }

        @Override
        public void interpolation(int delay, int duration) {
            Display display = itemDisplay;
            display.c(delay);
            display.b(duration);
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(manager.getEntityID(itemDisplay), manager.getDataWatcher(itemDisplay).c());
            manager.sendPacket(players, metadata);
        }
    }
}
