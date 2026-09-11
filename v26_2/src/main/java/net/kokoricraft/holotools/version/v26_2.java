package net.kokoricraft.holotools.version;

import com.mojang.math.Transformation;
import io.netty.channel.*;
import net.kokoricraft.holotools.events.InventoryUpdateEvent;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;

public class v26_2 implements Compat {
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
            ServerCommonPacketListenerImpl serverCommonPacketListener = player.getHandle().connection;

            Field networkManagerField = ServerCommonPacketListenerImpl.class.getDeclaredField("connection");
            networkManagerField.setAccessible(true);

            Connection networkManager = (Connection) networkManagerField.get(serverCommonPacketListener);

            return networkManager.channel;
        }catch (Exception ignore){}
        return null;
    }

    private void onPacketSend(Player player) {
        InventoryUpdateEvent event = new InventoryUpdateEvent(player);
        Bukkit.getPluginManager().callEvent(event);
    }

    private void onMountPacketSend(Object msg, Player player) {
        boolean isPaper = msg.getClass().getName().endsWith("ClientboundSetPassengersPacket");
        String vehicleFieldName = isPaper ? "vehicle" : "b";
        String passengersFieldName = isPaper ? "passengers" : "c";

        try {
            Field targetField = msg.getClass().getDeclaredField(vehicleFieldName);
            targetField.setAccessible(true);
            int targetID = targetField.getInt(msg);

            if (!passengers.containsKey(targetID)) return;

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

//            if(!player.getName().equals("FavioMC19")){
//                player.sendMessage("modified array: "+Arrays.toString(newPassengersID));
//            }

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
        Level world = ((CraftWorld) player.getWorld()).getHandle();

        ServerPlayer entityPlayer =  ((CraftPlayer)player).getHandle();

        List<Component> list = nmsItemStack.getTooltipLines(Item.TooltipContext.of(world), entityPlayer, advanced ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);

        List<BaseComponent> components = new ArrayList<>();

        for(Component baseComponent : list){
            String json = CraftChatMessage.toJSON(baseComponent);
            components.add(ComponentSerializer.deserialize(json));
        }
        return components;
    }

    public int getEntityID(Entity entity){
        return entity.getId();
    }

    public SynchedEntityData getSynchedEntityData(Entity entity){
        return entity.getEntityData();
    }

    public void sendPacket(List<Player> players, Packet<?> packet){
        players.forEach(player -> {
            ((CraftPlayer)player).getHandle().connection.send(packet);
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

        ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(((CraftEntity)target).getHandle());
        sendPacket(players, packet);
    }

    public static class HoloDisplayText implements HoloTextDisplay {
        private final List<Player> players;
        private final Display.TextDisplay displayEntity;
        private Location location;
        private org.bukkit.entity.Entity target;
        private final v26_2 manager;

        public HoloDisplayText(List<Player> players, Location location, float yaw, float pitch, v26_2 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            ServerLevel world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.displayEntity = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, world);
            manager.getEntityID(displayEntity);

            ClientboundAddEntityPacket spawnPacket =  new ClientboundAddEntityPacket(manager.getEntityID(displayEntity), displayEntity.getUUID(), location.getX(), location.getY(), location.getZ(), yaw, pitch, displayEntity.getType(), 0, displayEntity.getDeltaMovement(), displayEntity.getYHeadRot());

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
            ClientboundRemoveEntitiesPacket destroy = new ClientboundRemoveEntitiesPacket(manager.getEntityID(displayEntity));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, displayEntity);
        }

        @Override
        public void setText(String text) {
            displayEntity.setText(CraftChatMessage.fromString(text, true)[0]);
        }

        @Override
        public void setText(List<BaseComponent> components) {
            List<Component> iChatBaseComponents = new ArrayList<>();

            for(BaseComponent baseComponent : components){
                String json = ComponentSerializer.toJson(baseComponent).toString();
                iChatBaseComponents.add(CraftChatMessage.fromJSONOrString(json, true));
            }

            Component empty = Component.empty();

            Component mutableComponent = ComponentUtils.formatList(iChatBaseComponents, empty);

            displayEntity.setText(mutableComponent);
        }

        @Override
        public void interpolation(int delay, int duration) {
            Display display = displayEntity;
            display.setTransformationInterpolationDelay(delay);
            display.setTransformationInterpolationDuration(duration);
        }

        @Override
        public void setColor(HoloColor color) {
            int colorValue = color == null ? -1 : color.asARGB();
            manager.getSynchedEntityData(displayEntity).set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, colorValue);
        }

        @Override
        public void setGlowing(boolean glowing) {
            displayEntity.setGlowingTag(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Transformation transformation = new Transformation(nms.translation(), nms.leftRotation(), new Vector3f(x, y, z), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.translation(), quaternionf, nms.scale(), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        @Override
        public void setRotation(float v, float v2) {
            if(Float.isNaN(v))
                v = 0.0F;

            if(Float.isNaN(v2))
                v2 = 0.0F;

            displayEntity.setXRot(v % 360.0F);
            displayEntity.setYRot(v2 % 360.0F);
        }

        @Override
        public void setSeeThrough(boolean seeThrough) {
            setFlag(2, seeThrough);
        }

        @Override
        public void setLineWidth(int width) {
            manager.getSynchedEntityData(displayEntity).set(Display.TextDisplay.DATA_LINE_WIDTH_ID, width);
        }

        @Override
        public void setOpacity(byte opacity) {
            displayEntity.setTextOpacity(opacity);
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
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.leftRotation(), nms.scale(), nms.rightRotation());
            displayEntity.setTransformation(transformation);
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
            displayEntity.setBrightnessOverride(brightness);
        }

        @Override
        public void setViewRange(float range) {
            displayEntity.setViewRange(range);
        }

        @Override
        public void setTextOpacity(byte opacity) {
            displayEntity.setTextOpacity(opacity);
        }

        @Override
        public void mount(org.bukkit.entity.Entity target) {
            this.target = target;
            manager.mount(players, target, displayEntity);
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            displayEntity.setBillboardConstraints(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        public void internalUpdate(){
            ClientboundSetEntityDataPacket metadata = new ClientboundSetEntityDataPacket(manager.getEntityID(displayEntity), manager.getSynchedEntityData(displayEntity).getNonDefaultValues());
            manager.sendPacket(players, metadata);
        }

        public void setFlag(int flag, boolean set){
            byte flagBits = this.displayEntity.getFlags();
            if (set) {
                flagBits = (byte)(flagBits | flag);
            } else {
                flagBits = (byte)(flagBits & ~flag);
            }

            displayEntity.setFlags(flagBits);
        }
    }

    public static class HoloDisplayItem implements HoloItemDisplay{

        private final List<Player> players;
        private final Display.ItemDisplay displayEntity;
        private Location location;
        private org.bukkit.entity.Entity target;
        private final v26_2 manager;

        public HoloDisplayItem(List<Player> players, Location location, float yaw, float pitch, v26_2 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            ServerLevel world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.displayEntity = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, world);

            ClientboundAddEntityPacket spawnPacket =  new ClientboundAddEntityPacket(manager.getEntityID(displayEntity), displayEntity.getUUID(), location.getX(), location.getY(), location.getZ(), pitch, yaw, displayEntity.getType(), 0, displayEntity.getDeltaMovement(), displayEntity.getYHeadRot());

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
            ClientboundRemoveEntitiesPacket destroy = new ClientboundRemoveEntitiesPacket(manager.getEntityID(displayEntity));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, displayEntity);
        }

        @Override
        public void setGlowing(boolean glowing) {
            displayEntity.setGlowingTag(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Transformation transformation = new Transformation(nms.translation(), nms.leftRotation(), new Vector3f(x, y, z), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.translation(), quaternionf, nms.scale(), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.leftRotation(), nms.scale(), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        @Override
        public void setRotation(float v, float v2) {
            if(Float.isNaN(v))
                v = 0.0F;

            if(Float.isNaN(v2))
                v2 = 0.0F;

            displayEntity.setXRot(v % 360.0F);
            displayEntity.setYRot(v2 % 360.0F);
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
            displayEntity.setItemStack(CraftItemStack.asNMSCopy(itemStack));
            internalUpdate();
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public void setItemDisplayTransform(ItemDisplay.ItemDisplayTransform transform) {
            displayEntity.setItemTransform(ItemDisplayContext.BY_ID.apply(transform.ordinal()));
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            displayEntity.setBillboardConstraints(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        @Override
        public void setViewRange(float range) {
            displayEntity.setViewRange(range);
        }

        @Override
        public void setBrightness(org.bukkit.entity.Display.Brightness bukkitBrightness) {
            Brightness brightness = new Brightness(bukkitBrightness.getBlockLight(), bukkitBrightness.getSkyLight());
            displayEntity.setBrightnessOverride(brightness);
        }

        @Override
        public void interpolation(int delay, int duration) {
            displayEntity.setTransformationInterpolationDelay(delay);
            displayEntity.setTransformationInterpolationDuration(duration);
        }

        public void internalUpdate(){
            ClientboundSetEntityDataPacket metadata = new ClientboundSetEntityDataPacket(manager.getEntityID(displayEntity), manager.getSynchedEntityData(displayEntity).getNonDefaultValues());
            manager.sendPacket(players, metadata);
        }
    }

    public static class ShulkerHolo implements HoloShulker {

        private final List<Player> players;
        private final Shulker entity;
        private final Display.ItemDisplay displayEntity;
        private Location location;
        private org.bukkit.entity.Entity target;
        private final v26_2 manager;

        public ShulkerHolo(List<Player> players, Location location, float yaw, float pitch, v26_2 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            ServerLevel world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.entity = new Shulker(EntityTypes.SHULKER, world);
            ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(manager.getEntityID(entity), entity.getUUID(), location.getX(), location.getY(), location.getZ(), pitch, yaw, entity.getType(), 0, entity.getDeltaMovement(), entity.getYHeadRot());

            manager.sendPacket(players, spawnPacket);

            displayEntity = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, world);

            ClientboundAddEntityPacket itemDisplay =  new ClientboundAddEntityPacket(manager.getEntityID(displayEntity), displayEntity.getUUID(), location.getX(), location.getY(), location.getZ(), pitch, yaw, displayEntity.getType(), 0, displayEntity.getDeltaMovement(), displayEntity.getYHeadRot());
            manager.sendPacket(players, itemDisplay);

            ClientboundSetPassengersPacket mount = new ClientboundSetPassengersPacket(displayEntity);

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

            Vec3 position = new Vec3(location.getX(), location.getY(), location.getZ());
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            PositionMoveRotation pos = new PositionMoveRotation(position, new Vec3(0, 0, 0), yaw, pitch);
            Set<Relative> relativeSet = Set.of();
            ClientboundTeleportEntityPacket teleport = new ClientboundTeleportEntityPacket(manager.getEntityID(entity), pos, relativeSet, false);
            manager.sendPacket(players, teleport);

            internalUpdate();
        }

        @Override
        public void remove() {
            if(players.isEmpty()) return;
            ClientboundRemoveEntitiesPacket destroy = new ClientboundRemoveEntitiesPacket(manager.getEntityID(entity));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, entity);

            ClientboundRemoveEntitiesPacket destroyDisplay = new ClientboundRemoveEntitiesPacket(manager.getEntityID(displayEntity));
            manager.sendPacket(players, destroyDisplay);
        }

        @Override
        public void setGlowing(boolean glowing) {
            entity.setGlowingTag(glowing);
        }

        @Override
        public void setScale(float scale) {
            AttributeMap instance = entity.getAttributes();
            Objects.requireNonNull(instance.getInstance(Attributes.SCALE)).setBaseValue(scale);
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
            entity.setRawPeekAmount((int)(peek * 100.0F));
        }

        public void internalUpdate() {
            entity.persistentInvisibility = true;
            entity.setSharedFlag(5, true);
            ClientboundSetEntityDataPacket metadata = new ClientboundSetEntityDataPacket(manager.getEntityID(entity), manager.getSynchedEntityData(entity).getNonDefaultValues());
            manager.sendPacket(players, metadata);
            updateAttributes();
        }

        public void updateAttributes() {
            AttributeMap instance = entity.getAttributes();
            Collection<AttributeInstance> collection = instance.getAttributesToSync();
            ClientboundUpdateAttributesPacket packet = new ClientboundUpdateAttributesPacket(manager.getEntityID(entity), collection);
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
        private final v26_2 manager;

        public HoloDisplayBlock(List<Player> players, Location location, float yaw, float pitch, v26_2 manager) {
            this.manager = manager;
            this.players = players;
            this.location = location;
            ServerLevel world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.displayEntity = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, world);

            ClientboundAddEntityPacket spawnPacket =  new ClientboundAddEntityPacket(manager.getEntityID(displayEntity), displayEntity.getUUID(), location.getX(), location.getY(), location.getZ(), pitch, yaw, displayEntity.getType(), 0, displayEntity.getDeltaMovement(), displayEntity.getYHeadRot());

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
            ClientboundRemoveEntitiesPacket destroy = new ClientboundRemoveEntitiesPacket(manager.getEntityID(displayEntity));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, displayEntity);
        }

        @Override
        public void setGlowing(boolean glowing) {
            displayEntity.setGlowingTag(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Transformation transformation = new Transformation(nms.translation(), nms.leftRotation(), new Vector3f(x, y, z), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.translation(), quaternionf, nms.scale(), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.leftRotation(), nms.scale(), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        @Override
        public void setRotation(float v, float v2) {
            if(Float.isNaN(v))
                v = 0.0F;

            if(Float.isNaN(v2))
                v2 = 0.0F;

            displayEntity.setXRot(v % 360.0F);
            displayEntity.setYRot(v2 % 360.0F);
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
            displayEntity.setBlockState(craftBlockData.getState());
        }

        @Override
        public void setBillboard(org.bukkit.entity.Display.Billboard billboard) {
            displayEntity.setBillboardConstraints(Display.BillboardConstraints.valueOf(billboard.name()));
        }

        @Override
        public void setViewRange(float range) {
            displayEntity.setViewRange(range);
        }

        @Override
        public void setBrightness(org.bukkit.entity.Display.Brightness bukkitBrightness) {
            Brightness brightness = new Brightness(bukkitBrightness.getBlockLight(), bukkitBrightness.getSkyLight());
            displayEntity.setBrightnessOverride(brightness);
        }

        @Override
        public void interpolation(int delay, int duration) {
            displayEntity.setTransformationInterpolationDelay(delay);
            displayEntity.setTransformationInterpolationDuration(duration);
        }

        @Override
        public void setLeftRotation(Quaternionf rotation) {
            Transformation nms = Display.createTransformation(manager.getSynchedEntityData(displayEntity));
            Transformation transformation = new Transformation(nms.translation(), rotation, nms.scale(), nms.rightRotation());
            displayEntity.setTransformation(transformation);
        }

        public void internalUpdate(){
            ClientboundSetEntityDataPacket metadata = new ClientboundSetEntityDataPacket(manager.getEntityID(displayEntity), manager.getSynchedEntityData(displayEntity).getNonDefaultValues());
            manager.sendPacket(players, metadata);
        }
    }
}