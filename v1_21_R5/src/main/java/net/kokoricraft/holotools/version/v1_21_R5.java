package net.kokoricraft.holotools.version;

import com.mojang.math.Transformation;
import io.netty.channel.*;
import net.kokoricraft.holotools.events.InventoryUpdateEvent;
import net.kokoricraft.holotools.utils.objects.HoloColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.dialog.Dialog;
import net.md_5.bungee.api.dialog.DialogBase;
import net.md_5.bungee.api.dialog.NoticeDialog;
import net.md_5.bungee.api.dialog.action.ActionButton;
import net.md_5.bungee.api.dialog.action.RunCommandAction;
import net.md_5.bungee.api.dialog.body.PlainMessageBody;
import net.md_5.bungee.api.dialog.chat.ShowDialogClickEvent;
import net.md_5.bungee.api.dialog.input.BooleanInput;
import net.md_5.bungee.api.dialog.input.TextInput;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutMount;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R5.util.CraftChatMessage;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;

public class v1_21_R5 implements Compat{
    private final Map<Integer, List<Entity>> passengers = new HashMap<>();

    @Override
    public HoloTextDisplay createTextDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayText(players, location, yaw, pitch, this);
    }

    @Override
    public HoloItemDisplay createItemDisplay(List<Player> players, Location location, float yaw, float pitch) {
        return new HoloDisplayItem(players, location, yaw, pitch, this);
    }

    public void test() {
        Player player = Bukkit.getPlayerExact("FavioMC19");
        if (player == null) return;

        DialogBase base = new DialogBase(new TextComponent("Hello")).body(List.of(new PlainMessageBody(new TextComponent("Test"), 64))).canCloseWithEscape(true);

        base.inputs(Arrays.asList(new TextInput("first", new TextComponent("Insert your name")),
                new BooleanInput("pro", new TextComponent("Your are pro?"), false, "Yes", "No")));

        Dialog notice = new NoticeDialog(base)
                .action(new ActionButton(new TextComponent("test button"), new TextComponent("tooltip"), 128, new RunCommandAction("holotools $(first)")));

        player.showDialog(notice);

        player.spigot().sendMessage( new ComponentBuilder( "click me" ).event( new ShowDialogClickEvent( notice ) ).build() );

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Input input = player.getCurrentInput();
                if (input != null) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("%s, %s, %s, %s".formatted(input.isBackward(), input.isForward(), input.isJump(), input.isSneak(), input.isRight())));
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugins()[0], 0, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.clearDialog();
                task.cancel();
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugins()[0], 20 * 30);
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

            return networkManager.n;
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
        return entity.ar();
    }

    public DataWatcher getDataWatcher(Entity entity){
        return entity.au();
    }

    public void sendPacket(List<Player> players, Packet<?> packet){
        players.forEach(player -> {
            ((CraftPlayer)player).getHandle().g.sendPacket(packet);
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

    public static class HoloDisplayText implements HoloTextDisplay{
        private final List<Player> players;
        private final Display.TextDisplay textDisplay;
        private Location location;
        private final Packet<?> spawnPacket;
        private org.bukkit.entity.Entity target;
        private final v1_21_R5 manager;

        public HoloDisplayText(List<Player> players, Location location, float yaw, float pitch, v1_21_R5 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.textDisplay = new Display.TextDisplay(EntityTypes.bx, world);
            spawnPacket =  new PacketPlayOutSpawnEntity(manager.getEntityID(textDisplay), textDisplay.cK(), location.getX(), location.getY(), location.getZ(), yaw, pitch, textDisplay.ap(), 0, textDisplay.dA(), textDisplay.cE());
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
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(manager.getEntityID(textDisplay));
            manager.sendPacket(players, destroy);
            manager.removePassengers(target, textDisplay);
        }

        @Override
        public void setText(String text) {
            textDisplay.a(CraftChatMessage.fromString(text, true)[0]);
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

            textDisplay.a(mutableComponent);
        }

        @Override
        public void interpolation(int delay, int duration) {
            Display display = textDisplay;
            display.c(delay);
            display.b(duration);
        }

        @Override
        public void setColor(HoloColor color) {
            int colorValue = color == null ? -1 : color.asARGB();
            manager.getDataWatcher(textDisplay).a(Display.TextDisplay.aX, colorValue);
        }

        @Override
        public void setGlowing(boolean glowing) {
            textDisplay.i(glowing);
        }

        @Override
        public void setScale(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(textDisplay));
            Transformation transformation = new Transformation(nms.e(), nms.f(), new Vector3f(x, y, z), nms.h());
            textDisplay.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(textDisplay));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.e(), quaternionf, nms.g(), nms.h());
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
            manager.getDataWatcher(textDisplay).a(Display.TextDisplay.aW, width);
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
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.f(), nms.g(), nms.h());
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
            textDisplay.c(opacity);
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
            byte flagBits = this.textDisplay.s();
            if (set) {
                flagBits = (byte)(flagBits | flag);
            } else {
                flagBits = (byte)(flagBits & ~flag);
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
        private final v1_21_R5 manager;

        public HoloDisplayItem(List<Player> players, Location location, float yaw, float pitch, v1_21_R5 manager){
            this.manager = manager;
            this.players = players;
            this.location = location;
            WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            this.itemDisplay = new Display.ItemDisplay(EntityTypes.at, world);
            spawnPacket =  new PacketPlayOutSpawnEntity(manager.getEntityID(itemDisplay), itemDisplay.cK(), location.getX(), location.getY(), location.getZ(), pitch, yaw, itemDisplay.ap(), 0, itemDisplay.dA(), itemDisplay.cE());
            manager.sendPacket(players, spawnPacket);
        }

        public Entity getEntity(){
            return itemDisplay;
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
            Transformation transformation = new Transformation(nms.e(), nms.f(), new Vector3f(x, y, z), nms.h());
            itemDisplay.a(transformation);
        }

        @Override
        public void setRotation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(itemDisplay));
            Quaternionf quaternionf = new Quaternionf();
            quaternionf.rotateXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
            Transformation transformation = new Transformation(nms.e(), quaternionf, nms.g(), nms.h());
            itemDisplay.a(transformation);
        }

        @Override
        public void setTranslation(float x, float y, float z) {
            Transformation nms = Display.a(manager.getDataWatcher(itemDisplay));
            Transformation transformation = new Transformation(new Vector3f(x, y, z), nms.f(), nms.g(), nms.h());
            itemDisplay.a(transformation);
        }

        @Override
        public void setRotation(float pitch, float yaw) {
            NumberConversions.checkFinite(pitch, "pitch not finite");
            NumberConversions.checkFinite(yaw, "yaw not finite");

            yaw = Location.normalizeYaw(yaw);
            pitch = Location.normalizePitch(pitch);

            itemDisplay.v(yaw);
            itemDisplay.w(pitch);
            itemDisplay.aa = yaw;
            itemDisplay.ab = pitch;
            itemDisplay.r(yaw);
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
            itemDisplay.a(CraftItemStack.asNMSCopy(itemStack));
            internalUpdate();
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

        @Override
        public void interpolation(int delay, int duration) {
            itemDisplay.c(delay);
            itemDisplay.b(duration);
        }

        public void internalUpdate(){
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(manager.getEntityID(itemDisplay), manager.getDataWatcher(itemDisplay).c());
            manager.sendPacket(players, metadata);
        }
    }
}