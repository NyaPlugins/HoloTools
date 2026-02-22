package net.kokoricraft.holotools.version;

import com.mojang.math.Transformation;
import io.netty.channel.*;
import net.kokoricraft.holotools.events.InventoryUpdateEvent;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.monster.EntityShulker;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R7.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_21_R7.entity.*;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R7.util.CraftChatMessage;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class v1_21_R7 implements Compat{
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
    public HoloBlockDisplay createBlockDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayBlock(players, location, yaw, pitch, this);
    }

    public HoloShulker createShulker(List<Player> players, Location location, float yaw, float pitch) {
        return new ShulkerHolo(players, location, yaw, pitch, this);
    }


    public void test() {

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
                            onPacketSend(player);
                        }

                        if(name.endsWith("ClientboundSetPassengersPacket") || name.endsWith("PacketPlayOutMount")){
                            onMountPacketSend(msg, player);
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
            ServerCommonPacketListenerImpl serverCommonPacketListener = player.getHandle().g;

            Field networkManagerField = ServerCommonPacketListenerImpl.class.getDeclaredField("e");// connection
            networkManagerField.setAccessible(true);

            NetworkManager networkManager = (NetworkManager) networkManagerField.get(serverCommonPacketListener);

            return networkManager.k;
        }catch (Exception ignore){}
        return null;
    }

    private void onPacketSend(Player player) {
        InventoryUpdateEvent event = new InventoryUpdateEvent(player);
        Bukkit.getPluginManager().callEvent(event);
    }

    private void onMountPacketSend(Object msg, Player player){
        boolean isPaper = msg.getClass().getName().endsWith("ClientboundSetPassengersPacket");
        String vehicleFieldName = isPaper ? "vehicle" : "b";
        String passengersFieldName = isPaper ? "passengers" : "c";

        try{
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
            for(Entity entity : entities){
                int entityID = getEntityID(entity);
                newPassengersID[index++] = entityID;
            }

//            if(!player.getName().equals("FavioMC19")){
//                player.sendMessage("modified array: "+Arrays.toString(newPassengersID));
//            }

            passengersField.set(msg, newPassengersID);
        }catch(Exception exception) {
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
        World world = ((CraftWorld) player.getWorld()).getHandle();

        EntityHuman entityPlayer =  ((CraftPlayer)player).getHandle();

        List<IChatBaseComponent> list = nmsItemStack.a(Item.b.a(world), entityPlayer, advanced ? TooltipFlag.b : TooltipFlag.a);

        List<BaseComponent> components = new ArrayList<>();

        for(IChatBaseComponent baseComponent : list){
            String json = CraftChatMessage.toJSON(baseComponent);
            components.add(ComponentSerializer.deserialize(json));
        }
        return components;
    }

    public int getEntityID(Entity entity){
        return entity.aA();
    }

    public DataWatcher getDataWatcher(Entity entity){
        return entity.aD();
    }

    public void sendPacket(List<Player> players, Packet<?> packet){
        players.forEach(player -> {
            ((CraftPlayer)player).getHandle().g.b(packet);
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

//        if(target.getName().equals("FavioMC19")){
//            target.sendMessage("mount packet id: "+getEntityID(passenger));
//        }
        PacketPlayOutMount packet = new PacketPlayOutMount(((CraftEntity)target).getHandle());
        sendPacket(players, packet);
    }

    public static class HoloDisplayText implements HoloTextDisplay {
        private final List<Player> players;
        private final Display.TextDisplay displayEntity;
        private Location location;
        private org.bukkit.entity.Entity target;
        private final v1_21_R7 manager;

        public HoloDisplayText(List<Player> players, Location location, float yaw, float pitch, v1_21_R7 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.displayEntity = new Display.TextDisplay(EntityTypes.bD, world);
            manager.getEntityID(displayEntity);
            displayEntity.cT();
            location.getX();
            location.getY();
            location.getZ();
            displayEntity.ax();
            displayEntity.dI();
            displayEntity.cN();
            PacketPlayOutSpawnEntity spawnPacket =  new PacketPlayOutSpawnEntity(manager.getEntityID(displayEntity), displayEntity.cY(), location.getX(), location.getY(), location.getZ(), yaw, pitch, displayEntity.ay(), 0, displayEntity.dN(), displayEntity.cS());

            manager.sendPacket(players, spawnPacket);
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
//            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(textDisplay);
//            manager.sendPacket(players, teleport);

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(manager.getEntityID(displayEntity));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, displayEntity);
        }

        @Override
        public void setText(String text) {
            displayEntity.a(CraftChatMessage.fromString(text, true)[0]);
        }

        @Override
        public void setText(List<BaseComponent> components) {
            List<IChatBaseComponent> iChatBaseComponents = new ArrayList<>();

            for(BaseComponent baseComponent : components){
                String json = ComponentSerializer.toJson(baseComponent).toString();
                iChatBaseComponents.add(CraftChatMessage.fromJSONOrString(json, true));
            }

            IChatBaseComponent empty = IChatBaseComponent.i();

            IChatBaseComponent mutableComponent = ChatComponentUtils.a(iChatBaseComponents, empty);

            displayEntity.a(mutableComponent);
        }

        @Override
        public void interpolation(int delay, int duration) {
            Display display = displayEntity;
            display.b(delay);
            display.a(duration);
        }

        @Override
        public void setColor(HoloColor color) {
            int colorValue = color == null ? -1 : color.asARGB();
            manager.getDataWatcher(displayEntity).a(Display.TextDisplay.aY, colorValue);
        }

        @Override
        public void setGlowing(boolean glowing) {
            displayEntity.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Transformation transformation = new Transformation(nms.e(), nms.f(), new Vector3f(x, y, z), nms.h());
            displayEntity.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.e(), quaternionf, nms.g(), nms.h());
            displayEntity.a(transformation);
        }

        @Override
        public void setRotation(float v, float v2) {
            if(Float.isNaN(v))
                v = 0.0F;

            if(Float.isNaN(v2))
                v2 = 0.0F;

            displayEntity.t(v % 360.0F);
            displayEntity.u(v2 % 360.0F);
        }

        @Override
        public void setSeeThrough(boolean seeThrough) {
            setFlag(2, seeThrough);
        }

        @Override
        public void setLineWidth(int width) {
            manager.getDataWatcher(displayEntity).a(Display.TextDisplay.aX, width);
        }

        @Override
        public void setOpacity(byte opacity) {
            displayEntity.c(opacity);
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
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.f(), nms.g(), nms.h());
            displayEntity.a(transformation);
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
            displayEntity.a(brightness);
        }

        @Override
        public void setViewRange(float range) {
            displayEntity.b(range);
        }

        @Override
        public void setTextOpacity(byte opacity) {
            displayEntity.c(opacity);
        }

        @Override
        public void mount(org.bukkit.entity.Entity target) {
            this.target = target;
            manager.mount(players, target, displayEntity);
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            displayEntity.a(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(manager.getEntityID(displayEntity), manager.getDataWatcher(displayEntity).c());
            manager.sendPacket(players, metadata);
        }

        public void setFlag(int flag, boolean set){
            byte flagBits = this.displayEntity.r();
            if (set) {
                flagBits = (byte)(flagBits | flag);
            } else {
                flagBits = (byte)(flagBits & ~flag);
            }

            displayEntity.d(flagBits);
        }
    }

    public static class HoloDisplayItem implements HoloItemDisplay{

        private final List<Player> players;
        private final Display.ItemDisplay displayEntity;
        private Location location;
        private org.bukkit.entity.Entity target;
        private final v1_21_R7 manager;

        public HoloDisplayItem(List<Player> players, Location location, float yaw, float pitch, v1_21_R7 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.displayEntity = new Display.ItemDisplay(EntityTypes.aw, world);

            PacketPlayOutSpawnEntity spawnPacket =  new PacketPlayOutSpawnEntity(manager.getEntityID(displayEntity), displayEntity.cY(), location.getX(), location.getY(), location.getZ(), pitch, yaw, displayEntity.ay(), 0, displayEntity.dN(), displayEntity.cS());

            manager.sendPacket(players, spawnPacket);
        }

        public Entity getEntity(){
            return displayEntity;
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
//            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(itemDisplay);
//            manager.sendPacket(players, teleport);

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(manager.getEntityID(displayEntity));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, displayEntity);
        }

        @Override
        public void setGlowing(boolean glowing) {
            displayEntity.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Transformation transformation = new Transformation(nms.e(), nms.f(), new Vector3f(x, y, z), nms.h());
            displayEntity.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.e(), quaternionf, nms.g(), nms.h());
            displayEntity.a(transformation);
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.f(), nms.g(), nms.h());
            displayEntity.a(transformation);
        }

        @Override
        public void setRotation(float pitch, float yaw) {
            NumberConversions.checkFinite(pitch, "pitch not finite");
            NumberConversions.checkFinite(yaw, "yaw not finite");

            yaw = Location.normalizeYaw(yaw);
            pitch = Location.normalizePitch(pitch);

            displayEntity.v(yaw);
            displayEntity.w(pitch);
            displayEntity.aa = yaw;
            displayEntity.ab = pitch;
            displayEntity.r(yaw);
        }

        @Override
        public void update() {
            internalUpdate();
        }

        @Override
        public void mount(org.bukkit.entity.Entity target) {
            this.target = target;
            manager.mount(players, target, displayEntity);
        }

        @Override
        public void setItemStack(ItemStack itemStack) {
            displayEntity.a(CraftItemStack.asNMSCopy(itemStack));
            internalUpdate();
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public void setItemDisplayTransform(ItemDisplay.ItemDisplayTransform transform) {
            displayEntity.a(ItemDisplayContext.l.apply(transform.ordinal()));
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            displayEntity.a(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        @Override
        public void setViewRange(float range) {
            displayEntity.b(range);
        }

        @Override
        public void setBrightness(org.bukkit.entity.Display.Brightness bukkitBrightness) {
            Brightness brightness = new Brightness(bukkitBrightness.getBlockLight(), bukkitBrightness.getSkyLight());
            displayEntity.a(brightness);
        }

        @Override
        public void interpolation(int delay, int duration) {
            displayEntity.b(delay);
            displayEntity.a(duration);
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(manager.getEntityID(displayEntity), manager.getDataWatcher(displayEntity).c());
            manager.sendPacket(players, metadata);
        }
    }

    public static class ShulkerHolo implements HoloShulker {

        private final List<Player> players;
        private final EntityShulker entity;
        private final Display.ItemDisplay displayEntity;
        private Location location;
        private org.bukkit.entity.Entity target;
        private final v1_21_R7 manager;

        public ShulkerHolo(List<Player> players, Location location, float yaw, float pitch, v1_21_R7 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.entity = new EntityShulker(EntityTypes.bk, world);
            PacketPlayOutSpawnEntity spawnPacket = new PacketPlayOutSpawnEntity(manager.getEntityID(entity), entity.cY(), location.getX(), location.getY(), location.getZ(), pitch, yaw, entity.ay(), 0, entity.dN(), entity.cS());


            manager.sendPacket(players, spawnPacket);

            displayEntity = new Display.ItemDisplay(EntityTypes.aw, world);

            PacketPlayOutSpawnEntity itemDisplay =  new PacketPlayOutSpawnEntity(manager.getEntityID(displayEntity), displayEntity.cY(), location.getX(), location.getY(), location.getZ(), pitch, yaw, displayEntity.ay(), 0, displayEntity.dN(), displayEntity.cS());
            manager.sendPacket(players, itemDisplay);

            PacketPlayOutMount mount = new PacketPlayOutMount(displayEntity);

            boolean isPaper = mount.getClass().getName().endsWith("ClientboundSetPassengersPacket");
            String vehicleFieldName = isPaper ? "vehicle" : "b";
            String passengersFieldName = isPaper ? "passengers" : "c";

            try{
                Field targetField = mount.getClass().getDeclaredField(vehicleFieldName);
                targetField.setAccessible(true);

                Field passengersField = mount.getClass().getDeclaredField(passengersFieldName);
                passengersField.setAccessible(true);

                int[] newPassengersID = new int[]{manager.getEntityID(entity)};

                passengersField.set(mount, newPassengersID);
            }catch(Exception exception) {
                exception.printStackTrace();
            }

            manager.sendPacket(players, mount);
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;

            Vec3D position = new Vec3D(location.getX(), location.getY(), location.getZ());
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            PositionMoveRotation pos = new PositionMoveRotation(position, new Vec3D(0, 0, 0), yaw, pitch);
            Set<Relative> relativeSet = Set.of();
            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(manager.getEntityID(entity), pos, relativeSet, false);
            manager.sendPacket(players, teleport);

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(manager.getEntityID(entity));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, entity);

            PacketPlayOutEntityDestroy destroyDisplay = new PacketPlayOutEntityDestroy(manager.getEntityID(displayEntity));
            manager.sendPacket(players, destroyDisplay);
        }

        @Override
        public void setGlowing(boolean glowing) {
            entity.i(glowing);
        }

        @Override
        public void setScale(float scale) {
            AttributeMapBase instance = entity.fw();
            Objects.requireNonNull(instance.a(GenericAttributes.A)).a(scale);
        }

        @Override
        public void update() {
            internalUpdate();
        }

        @Override
        public void mount(org.bukkit.entity.Entity target) {
            this.target = target;
            manager.mount(players, target, entity);
        }

        @Override
        public void setPeek(float peek) {
            entity.b((int)(peek * 100.0F));
        }

        public void internalUpdate() {
            entity.persistentInvisibility = true;
            entity.b(5, true);
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(manager.getEntityID(entity), manager.getDataWatcher(entity).c());
            manager.sendPacket(players, metadata);
            updateAttributes();
        }

        public void updateAttributes() {
            AttributeMapBase instance = entity.fw();
            Collection<AttributeModifiable> collection = instance.a();
            PacketPlayOutUpdateAttributes packet = new PacketPlayOutUpdateAttributes(manager.getEntityID(entity), collection);
            manager.sendPacket(players, packet);
        }

        @Override
        public Location getLocation() {
            return null;
        }
    }

    public static class HoloDisplayBlock implements HoloBlockDisplay {

        private final List<Player> players;
        private final Display.BlockDisplay displayEntity;
        private Location location;
        private org.bukkit.entity.Entity target;
        private final v1_21_R7 manager;

        public HoloDisplayBlock(List<Player> players, Location location, float yaw, float pitch, v1_21_R7 manager) {
            this.manager = manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.displayEntity = new Display.BlockDisplay(EntityTypes.r, world);

            PacketPlayOutSpawnEntity spawnPacket =  new PacketPlayOutSpawnEntity(manager.getEntityID(displayEntity), displayEntity.cY(), location.getX(), location.getY(), location.getZ(), pitch, yaw, displayEntity.ay(), 0, displayEntity.dN(), displayEntity.cS());

            manager.sendPacket(players, spawnPacket);
        }

        public Entity getEntity(){
            return displayEntity;
        }

        @Override
        public void update(Location location) {
            if(players.isEmpty()) return;
            this.location = location;
//            PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(itemDisplay);
//            manager.sendPacket(players, teleport);

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(manager.getEntityID(displayEntity));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, displayEntity);
        }

        @Override
        public void setGlowing(boolean glowing) {
            displayEntity.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Transformation transformation = new Transformation(nms.e(), nms.f(), new Vector3f(x, y, z), nms.h());
            displayEntity.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.e(), quaternionf, nms.g(), nms.h());
            displayEntity.a(transformation);
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.f(), nms.g(), nms.h());
            displayEntity.a(transformation);
        }

        @Override
        public void setRotation(float pitch, float yaw) {
            NumberConversions.checkFinite(pitch, "pitch not finite");
            NumberConversions.checkFinite(yaw, "yaw not finite");

            yaw = Location.normalizeYaw(yaw);
            pitch = Location.normalizePitch(pitch);

            displayEntity.v(yaw);
            displayEntity.w(pitch);
            displayEntity.aa = yaw;
            displayEntity.ab = pitch;
            displayEntity.r(yaw);
        }

        @Override
        public void update() {
            internalUpdate();
        }

        @Override
        public void mount(org.bukkit.entity.Entity target) {
            this.target = target;
            manager.mount(players, target, displayEntity);
        }

        @Override
        public void setItemStack(ItemStack itemStack) {

        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public void setBlock(Material material) {
            BlockData blockData = material.createBlockData();
            CraftBlockData craftBlockData = ((CraftBlockData) blockData);
            try {
                Method setBlockData = Display.BlockDisplay.class.getMethod("c", craftBlockData.getState().getClass());
                setBlockData.invoke(displayEntity, craftBlockData.getState());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            displayEntity.a(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        @Override
        public void setViewRange(float range) {
            displayEntity.b(range);
        }

        @Override
        public void setBrightness(org.bukkit.entity.Display.Brightness bukkitBrightness) {
            Brightness brightness = new Brightness(bukkitBrightness.getBlockLight(), bukkitBrightness.getSkyLight());
            displayEntity.a(brightness);
        }

        @Override
        public void interpolation(int delay, int duration) {
            displayEntity.b(delay);
            displayEntity.a(duration);
        }

        @Override
        public void setLeftRotation(Quaternionf rotation) {
            Transformation nms = Display.a(manager.getDataWatcher(displayEntity));
            Transformation transformation = new Transformation(nms.e(), rotation, nms.g(), nms.h());
            displayEntity.a(transformation);
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(manager.getEntityID(displayEntity), manager.getDataWatcher(displayEntity).c());
            manager.sendPacket(players, metadata);
        }
    }
}