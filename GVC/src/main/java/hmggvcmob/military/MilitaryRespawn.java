
package hmggvcmob.military;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import hmggvcmob.tile.TileEntityFlag;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MilitaryRespawn {
    private static final String BINDING = "GVCMilitaryRespawn";
    private final Map<UUID, BedSpawn> savedBeds = new HashMap<>();
    private final Map<UUID, EntityPlayerMP> pendingRespawns = new HashMap<>();

    public static boolean bind(EntityPlayer player, TileEntityFlag flag) {
        if (MilitaryConfig.respawn == MilitaryConfig.Respawn.VANILLA) return false;
        if (player.worldObj.isRemote) return true;
        Occupation post = flag.occupation();
        if (!eligible(post, player)) {
            MilitaryRecruitment.refused(player);
            return true;
        }
        NBTTagCompound persisted = persisted(player);
        NBTTagCompound binding = new NBTTagCompound();
        binding.setString("Post", post.id.toString());
        binding.setInteger("Dimension", player.dimension);
        persisted.setTag(BINDING, binding);
        player.addChatComponentMessage(new ChatComponentTranslation("gvc.military.respawn.bound"));
        return true;
    }

    private static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound entity = player.getEntityData();
        if (!entity.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) entity.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        return entity.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    @SubscribeEvent
    public void sleep(PlayerSleepInBedEvent event) {
        if (MilitaryConfig.respawn == MilitaryConfig.Respawn.BEACON_ONLY && MilitaryConfig.blockBeaconOnlyBedSleep) {
            event.result = EntityPlayer.EnumStatus.OTHER_PROBLEM;
        }
    }

    @SubscribeEvent
    public void wake(PlayerWakeUpEvent event) {
        EntityPlayer player = event.entityPlayer;
        if (!player.worldObj.isRemote && event.setSpawn && MilitaryConfig.respawn == MilitaryConfig.Respawn.BEACON_ONLY) {
            savedBeds.putIfAbsent(player.getUniqueID(), new BedSpawn(player));
        }
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.worldObj.isRemote) restoreBed(event.player);
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        restoreBed(event.player);
    }

    private void restoreBed(EntityPlayer player) {
        BedSpawn saved = savedBeds.remove(player.getUniqueID());
        if (saved != null) player.setSpawnChunk(saved.position, saved.forced, saved.dimension);
    }

    @SubscribeEvent
    public void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.entityPlayer.worldObj.isRemote || !event.wasDeath) return;
        EntityPlayer original = event.original;
        restoreBed(original);
        NBTTagCompound transientData = event.entityPlayer.getEntityData();
        transientData.setBoolean("GVCMilitaryDied", true);
        transientData.setInteger("GVCMilitaryDeathDimension", original.worldObj.provider.dimensionId);
        transientData.setIntArray("GVCMilitaryDeathPosition", new int[]{
                MathHelper.floor_double(original.posX), MathHelper.floor_double(original.posY), MathHelper.floor_double(original.posZ)});
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (MilitaryConfig.respawn != MilitaryConfig.Respawn.VANILLA && event.player instanceof EntityPlayerMP
                && event.player.getEntityData().getBoolean("GVCMilitaryDied")) {
            pendingRespawns.put(event.player.getUniqueID(), (EntityPlayerMP)event.player);
        }
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingRespawns.isEmpty()) return;
        java.util.Iterator<EntityPlayerMP> waiting = pendingRespawns.values().iterator();
        while (waiting.hasNext()) {
            EntityPlayerMP player = waiting.next();
            waiting.remove();
            if (player.playerNetServerHandler.playerEntity == player && player.isEntityAlive()) applyRespawn(player);
        }
    }

    private void applyRespawn(EntityPlayerMP player) {
        NBTTagCompound transientData = player.getEntityData();
        transientData.removeTag("GVCMilitaryDied");
        NBTTagCompound binding = persisted(player).getCompoundTag(BINDING);
        UUID id = MilitaryEconomy.parseUuid(binding.getString("Post"));
        if (id != null) {
            WorldServer boundWorld = net.minecraftforge.common.DimensionManager.getWorld(binding.getInteger("Dimension"));
            Occupation bound = boundWorld == null ? null : WorldSavedData_Flag.get(boundWorld).byId(id);
            if (arrive(player, boundWorld, bound)) return;
        }
        if (MilitaryConfig.respawn == MilitaryConfig.Respawn.HYBRID) return;

        int[] origin = transientData.getIntArray("GVCMilitaryDeathPosition");
        WorldServer deathWorld = net.minecraftforge.common.DimensionManager.getWorld(transientData.getInteger("GVCMilitaryDeathDimension"));
        if (deathWorld != null && origin.length == 3) {
            TileEntityFlag nearest = TileEntityFlag.findNearest(deathWorld, origin[0], origin[1], origin[2],
                    MilitaryConfig.respawnDistance, false, flag -> eligible(flag.occupation(), player));
            if (nearest != null && arrive(player, deathWorld, nearest.occupation())) return;
        }
        WorldServer overworld = MinecraftServer.getServer().worldServerForDimension(0);
        ChunkCoordinates spawn = overworld.getSpawnPoint();
        int y = Math.max(spawn.posY, overworld.getTopSolidOrLiquidBlock(spawn.posX, spawn.posZ));
        teleport(player, overworld, new ChunkCoordinates(spawn.posX, Math.min(254, y), spawn.posZ));
    }

    private static boolean eligible(Occupation post, EntityPlayer player) {
        return post != null && post.occupied() && !post.attacked && post.control >= MilitaryConfig.captureStrength / 2 && post.permits(player);
    }

    private static boolean arrive(EntityPlayerMP player, WorldServer world, Occupation post) {
        if (world == null || !eligible(post, player)) return false;
        int[] position = post.flagPos;
        world.getChunkFromBlockCoords(position[0], position[2]);
        if (!(world.getTileEntity(position[0], position[1], position[2]) instanceof TileEntityFlag)) return false;
        for (int r = 1; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int x = position[0] + dx, z = position[2] + dz;
                if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) continue;
                for (int y = position[1] + 1; y >= position[1] - 1; y--) {
                    AxisAlignedBB space = AxisAlignedBB.getBoundingBox(x + .18, y, z + .18, x + .82, y + 1.82, z + .82);
                    if (world.isSideSolid(x, y - 1, z, ForgeDirection.UP)
                            && world.getCollidingBoundingBoxes(player, space).isEmpty() && !world.isAnyLiquid(space)) {
                        teleport(player, world, new ChunkCoordinates(x, y, z));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void teleport(EntityPlayerMP player, WorldServer world, ChunkCoordinates spot) {
        if (player.dimension != world.provider.dimensionId) {
            MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(player, world.provider.dimensionId,
                    new Teleporter(world) {
                        @Override
                        public void placeInPortal(Entity entity, double x, double y, double z, float facing) {
                            entity.setLocationAndAngles(spot.posX + .5, spot.posY, spot.posZ + .5, entity.rotationYaw, entity.rotationPitch);
                        }
                    });
        }
        player.playerNetServerHandler.setPlayerLocation(spot.posX + .5, spot.posY, spot.posZ + .5, player.rotationYaw, player.rotationPitch);
    }

    private static final class BedSpawn {
        final int dimension;
        final ChunkCoordinates position;
        final boolean forced;

        BedSpawn(EntityPlayer player) {
            dimension = player.dimension;
            position = player.getBedLocation(dimension);
            forced = player.isSpawnForced(dimension);
        }
    }
}
