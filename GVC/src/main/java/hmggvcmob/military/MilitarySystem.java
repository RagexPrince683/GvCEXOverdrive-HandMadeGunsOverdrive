
package hmggvcmob.military;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import hmggvcmob.GVCMobPlus;
import handmadeguns.guide.FieldManualItem;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.ChunkEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MilitarySystem implements IGuiHandler {
    private static volatile Thread serverThread;
    private static final Field MERCHANT = ReflectionHelper.findField(ContainerMerchant.class, "theMerchant", "field_75178_e");
    private static Path directory;

    private MilitarySystem() {}

    public static void registerItems(File mainConfig) {
        directory = mainConfig.toPath().getParent().resolve("gvc-military");
        MilitarySupplies.registerItems();
        MilitaryMaterials.register();
        cpw.mods.fml.common.registry.GameRegistry.registerItem(new ItemStructurePlan(), "structure_plan");
        FieldManualItem.register("FieldManual", "gvco_field_manual", GVCMobPlus.tabgvcm, net.minecraft.init.Items.paper,
                true, () -> GVCMobPlus.proxy.openFieldManual());
        MilitarySupplies.load();
    }

    public static void register(GVCMobPlus mod) {
        EntityRegistry.registerModEntity(EntityMilitaryTrader.class, "MilitaryTrader", 90, mod, 80, 3, true);
        EntityRegistry.registerModEntity(EntityMilitaryUnit.class, "MilitaryUnit", 91, mod, 250, 3, true);
        EntityRegistry.registerModEntity(EntityTraderMarker.class, "TraderMarker", 92, mod, 48, 10, false);
        cpw.mods.fml.common.registry.GameRegistry.registerWorldGenerator(new MilitarySites(), 15);
        MilitaryNetwork.init();
        VillageOutpost.register();
        MilitarySystem events = new MilitarySystem();
        MilitaryRespawn respawn = new MilitaryRespawn();
        NetworkRegistry.INSTANCE.registerGuiHandler(mod, events);
        MinecraftForge.EVENT_BUS.register(events);
        MinecraftForge.EVENT_BUS.register(respawn);
        FMLCommonHandler.instance().bus().register(events);
        FMLCommonHandler.instance().bus().register(respawn);
    }

    public static void loadDefinitions() {
        try {
            Files.createDirectories(directory);
        } catch (IOException failure) {
            FMLLog.warning("[GVC military] Could not create configuration directory %s: %s", directory, failure.getMessage());
        }
        MilitaryRelations.load(directory, MilitaryConfig.factionRelations);
        MilitaryReputation.configure();
        WeaponCatalog.load(directory);
        UnitPresets.load(directory);
        MilitarySites.load(directory);
        MilitaryCommerce.load(directory);
        MilitaryQuests.load(directory);
    }

    public static void start(FMLServerStartingEvent event) {
        serverThread = Thread.currentThread();
        event.registerServerCommand(new MilitaryCommand(false));
        event.registerServerCommand(new MilitaryCommand(true));
    }

    static void requireServerThread() {
        if (Thread.currentThread() != serverThread) throw new IllegalStateException("GVCO military data requires the Minecraft server thread");
    }

    public static void stop() {
        MilitaryReputation.stop();
        MilitaryNetwork.clear();
        serverThread = null;
    }

    @SubscribeEvent
    public void playerLogin(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) MilitaryNetwork.syncAllegiance((EntityPlayerMP)event.player);
    }

    @SubscribeEvent
    public void playerLogout(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        MilitaryNetwork.removeReputationHud(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void playerChangedDimension(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) MilitaryNetwork.syncAllegiance((EntityPlayerMP)event.player);
    }

    @SubscribeEvent
    public void playerRespawn(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) MilitaryNetwork.syncAllegiance((EntityPlayerMP)event.player);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Thread.currentThread() != serverThread) return;
        MilitaryNetwork.drain();
        long now = MilitaryEconomy.now();
        for (Object entry : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP)entry;
            MilitaryQuests.visit(player);
            if (now % 20 == 0) MilitaryNetwork.updateReputationHud(player, false);
        }
        if (now % MilitaryConfig.tradeInterval == 0) closeHostileTrade();
        if (now % 20 == 0) MilitaryReputation.sendNotices(now);
        if (now % MilitaryConfig.upkeepProcessing == 0) MilitaryEconomy.get().processUpkeep(now);
    }

    @SubscribeEvent
    public void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Thread.currentThread() == serverThread) MilitaryRoads.tick(event.world);
    }

    @SubscribeEvent
    public void load(ChunkEvent.Load event) {
        if (Thread.currentThread() == serverThread) {
            MilitaryRoads.chunkReady(event.world, event.getChunk().xPosition, event.getChunk().zPosition);
        }
    }

    @SubscribeEvent
    public void unload(ChunkEvent.Unload event) {
        if (event.world.isRemote || Thread.currentThread() != serverThread) return;
        for (List section : event.getChunk().entityLists) {
            for (Object entity : section) {
                if (entity instanceof EntityMilitaryUnit && ((EntityMilitaryUnit)entity).owner() != null) {
                    MilitaryEconomy.get().unload((EntityMilitaryUnit)entity);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interact(EntityInteractEvent event) {
        if (!event.entityPlayer.worldObj.isRemote && event.target instanceof EntityVillager
                && deniesTrade(event.entityPlayer, (EntityVillager)event.target)) {
            event.setCanceled(true);
            MilitaryRecruitment.refused(event.entityPlayer);
        }
    }

    @SubscribeEvent
    public void drops(LivingDropsEvent event) {
        if (event.entityLiving.worldObj.isRemote) return;
        if (event.entityLiving instanceof EntityMilitaryTrader) {
            event.drops.clear();
            return;
        }
        if (event.entityLiving instanceof EntityMilitaryUnit) {
            MilitaryLoot.drop(event, (EntityMilitaryUnit)event.entityLiving);
            return;
        }
        if (MilitaryConfig.hostileCoinDrop <= 0 || event.entityLiving.worldObj.isRemote
                || !event.recentlyHit || !(event.source.getEntity() instanceof EntityPlayer)
                || !(event.entityLiving instanceof hmggvcmob.entity.IflagBattler)) return;
        EntityPlayer player = (EntityPlayer) event.source.getEntity();
        if (!MilitaryRelations.hostile(event.entityLiving, player)) return;
        int count = 1 + event.entityLiving.getRNG().nextInt(MilitaryConfig.hostileCoinDrop);
        event.drops.add(new EntityItem(event.entityLiving.worldObj, event.entityLiving.posX, event.entityLiving.posY,
                event.entityLiving.posZ, new ItemStack(MilitarySupplies.coinItem(0), count)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void militaryReputation(LivingDeathEvent event) {
        if (!event.entityLiving.worldObj.isRemote) MilitaryReputation.death(event.entityLiving, event.source);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void militaryDamage(LivingHurtEvent event) {
        if (!event.entityLiving.worldObj.isRemote && event.ammount > 0) MilitaryReputation.hurt(event.entityLiving, event.source);
    }

    static void closeHostileTrade() {
        if (!MilitaryConfig.restrictVillagers || !MilitaryConfig.occupation) return;
        for (Object entry : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) entry;
            if (!(player.openContainer instanceof ContainerMerchant)) continue;
            IMerchant merchant;
            try {
                merchant = (IMerchant) MERCHANT.get(player.openContainer);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException(failure);
            }
            if (merchant instanceof EntityVillager && deniesTrade(player, (EntityVillager)merchant)) {
                player.closeScreen();
                MilitaryRecruitment.refused(player);
            }
        }
    }

    private static boolean deniesTrade(EntityPlayer player, EntityVillager villager) {
        if (!MilitaryConfig.restrictVillagers || !MilitaryConfig.occupation) return false;
        Occupation post = WorldSavedData_Flag.get(villager.worldObj).atOccupiedTerritory(villager.posX, villager.posZ, true);
        return post != null && !player.getUniqueID().equals(post.owner)
                && MilitaryRelations.hostileToPlayer(player.getUniqueID(), post.faction());
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == MilitaryNetwork.GUI ? new MilitaryContainer(player, x) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == MilitaryNetwork.GUI ? GVCMobPlus.proxy.militaryGui(player, x) : null;
    }
}
