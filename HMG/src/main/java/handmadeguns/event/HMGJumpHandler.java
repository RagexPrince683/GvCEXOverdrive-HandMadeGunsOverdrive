package handmadeguns.event;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import handmadeguns.HMGPacketHandler;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import handmadeguns.network.PacketJumpPolicy;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/** Weight policy and prediction only. No position, velocity or ground-state writes. */
public class HMGJumpHandler {
    private static final Logger LOG = LogManager.getLogger("HMGJump");
    private static final boolean TRACE = Boolean.getBoolean("hmg.jumpTrace");
    private static final double HEAVY_THRESHOLD = 0.70D;
    private static final double LIGHT_THRESHOLD = 0.95D;
    // Separate logical-side maps: integrated client/server never share timers.
    private static final Map<EntityPlayer, State> SERVER = new WeakHashMap<>();
    private static final Map<EntityPlayer, State> CLIENT = new WeakHashMap<>();
    private static volatile Map<Integer, Double> clientPolicy = Collections.emptyMap();
    private static NetworkManager clientConnection;
    private static boolean combativesOwnsJump;

    private static final class State {
        int cooldown;
        int pending;
        boolean wasGrounded;
    }

    public static void registerCombativesPolicy() {
        if (!Loader.isModLoaded("combatives")) return;
        try {
            Class<?> api = Class.forName("com.glowingfederal.combatives.movement.JumpRestrictions");
            api.getMethod("register", String.class, Predicate.class).invoke(null,
                    "HandmadeGuns:weight", (Predicate<EntityPlayer>) HMGJumpHandler::isJumpBlockedByWeaponWeight);
            combativesOwnsJump = true;
            LOG.info("Combatives owns weight-based jump rejection at jump HEAD");
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.warn("Combatives jump API unavailable; using HMG jump HEAD fallback", e);
        }
    }

    /** Called by the common coremod hook. An accepted bridge owns the sole rejection. */
    public static boolean rejectFallbackJump(EntityLivingBase entity) {
        return !combativesOwnsJump && entity instanceof EntityPlayer
                && isJumpBlockedByWeaponWeight((EntityPlayer) entity);
    }

    public static boolean isJumpBlockedByWeaponWeight(EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        Double motion = motionPolicy(player, held);
        State state = states(player).get(player);
        boolean blocked = !player.capabilities.isFlying && !player.isRiding()
                && held != null && held.getItem() instanceof HMGItem_Unified_Guns
                && (motion == null || motion <= HEAVY_THRESHOLD
                    || (motion < LIGHT_THRESHOLD && state != null && state.cooldown > 0));
        if (TRACE) trace(player, "jump:head owner=" + (combativesOwnsJump ? "combatives" : "hmg")
                + " rejected=" + blocked, motion, state);
        return blocked;
    }

    private static Double motionPolicy(EntityPlayer player, ItemStack held) {
        if (held == null || !(held.getItem() instanceof HMGItem_Unified_Guns)) return null;
        return player.worldObj.isRemote ? clientPolicy.get(Item.getIdFromItem(held.getItem()))
                : ((HMGItem_Unified_Guns) held.getItem()).gunInfo.motion;
    }

    private static Map<EntityPlayer, State> states(EntityPlayer player) {
        return player.worldObj.isRemote ? CLIENT : SERVER;
    }

    // This event is after vanilla jump. Observe success to schedule the existing
    // medium-weight landing delay; never attempt to undo the jump here.
    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.entityLiving;
        Double motion = motionPolicy(player, player.getCurrentEquippedItem());
        State state = states(player).get(player);
        trace(player, "jump:vanilla-return", motion, state);
        if (player.capabilities.isFlying || player.isRiding() || motion == null
                || motion <= HEAVY_THRESHOLD || motion >= LIGHT_THRESHOLD) return;
        if (state == null) {
            state = new State();
            states(player).put(player, state);
        }
        state.pending = motion >= 0.90D ? 3 : 6;
        state.wasGrounded = true;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        Map<EntityPlayer, State> states = states(player);
        State state = states.get(player);
        if (TRACE) trace(player, "tick:" + event.phase,
                motionPolicy(player, player.getCurrentEquippedItem()), state);
        if (event.phase != TickEvent.Phase.END || state == null) return;
        Double motion = motionPolicy(player, player.getCurrentEquippedItem());
        if (player.capabilities.isFlying || player.isRiding() || motion == null || motion >= LIGHT_THRESHOLD) {
            states.remove(player);
            return;
        }
        if (player.onGround && !state.wasGrounded) {
            state.cooldown = state.pending;
            state.pending = 0;
        }
        if (state.cooldown > 0) state.cooldown--;
        state.wasGrounded = player.onGround;
        if (state.cooldown == 0 && state.pending == 0) states.remove(player);
    }

    public static Map<Integer, Double> serverPolicy() {
        Map<Integer, Double> policy = new HashMap<>();
        for (Object value : Item.itemRegistry) {
            if (value instanceof HMGItem_Unified_Guns) {
                HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns) value;
                policy.put(Item.getIdFromItem(gun), gun.gunInfo.motion);
            }
        }
        return policy;
    }

    /** Called on the server owning thread after pack settings are reloaded. */
    public static void syncReloadedPolicy() {
        HMGPacketHandler.INSTANCE.sendToAll(new PacketJumpPolicy(serverPolicy()));
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            HMGPacketHandler.INSTANCE.sendTo(new PacketJumpPolicy(serverPolicy()), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        states(event.player).remove(event.player);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Map<EntityPlayer, State> states = event.world.isRemote ? CLIENT : SERVER;
        Iterator<EntityPlayer> players = states.keySet().iterator();
        while (players.hasNext()) {
            if (players.next().worldObj == event.world) players.remove();
        }
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        resetClientPolicy(event.manager);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        synchronized (HMGJumpHandler.class) {
            if (clientConnection == event.manager) resetClientPolicy(null);
        }
    }

    private static synchronized void resetClientPolicy(NetworkManager connection) {
        clientConnection = connection;
        clientPolicy = Collections.emptyMap();
    }

    /** Network thread publishes immutable data only; no entity/world access or mutation. */
    public static synchronized void receivePolicy(INetHandler handler, Map<Integer, Double> policy) {
        if (clientConnection != null && clientConnection.getNetHandler() == handler) {
            clientPolicy = Collections.unmodifiableMap(new HashMap<>(policy));
        }
    }

    private static void trace(EntityPlayer player, String stage, Double motion, State state) {
        if (!TRACE) return;
        ItemStack held = player.getCurrentEquippedItem();
        LOG.info("side=" + (player.worldObj.isRemote ? "CLIENT" : "SERVER")
                + " tick=" + player.ticksExisted + " worldTick=" + player.worldObj.getTotalWorldTime()
                + " player=" + player.getUniqueID() + " stage=" + stage
                + " pos=" + player.posX + "," + player.posY + "," + player.posZ
                + " motionY=" + player.motionY + " onGround=" + player.onGround
                + " isAirBorne=" + player.isAirBorne + " fallDistance=" + player.fallDistance
                + " flying=" + player.capabilities.isFlying
                + " weapon=" + (held == null ? "none" : held.getItem().getUnlocalizedName())
                + " slot=" + player.inventory.currentItem + " motionWeight=" + motion
                + " heavyThreshold=" + HEAVY_THRESHOLD + " lightThreshold=" + LIGHT_THRESHOLD
                + " cooldown=" + (state == null ? 0 : state.cooldown)
                + " pending=" + (state == null ? 0 : state.pending));
    }
}
