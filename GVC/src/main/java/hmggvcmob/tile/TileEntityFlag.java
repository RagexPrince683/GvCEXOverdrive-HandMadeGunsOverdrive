package hmggvcmob.tile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import hmggvcmob.block.GVCBlockFlag;
import hmggvcmob.camp.CampObj;
import hmggvcmob.camp.CampObjAndPos;
import hmggvcmob.entity.IGVCmob;
import hmggvcmob.entity.IflagBattler;
import hmggvcmob.entity.guerrilla.EntityGBases;
import hmggvcmob.military.MilitaryConfig;
import hmggvcmob.military.MilitaryOccupation;
import hmggvcmob.military.Occupation;
import hmggvcmob.world.WorldSavedData_Flag;
import littleMaidMobX.LMM_LittleMaidMobX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.vecmath.Vector3d;
import java.util.function.Predicate;

import static handmadeguns.HandmadeGunsCore.islmmloaded;
import static hmggvcmob.GVCMobPlus.campsHash;
import static hmggvcmob.GVCMobPlus.forPlayer;

public class TileEntityFlag extends TileEntity {
    private static final int UPDATE_INTERVAL = 10;
    private static final int REINFORCEMENT_LIMIT = 16;
    private static final int RAID_LIMIT = 48;
    private static final int RAID_ATTEMPTS = 50;
    private static final int RAID_RADIUS = 160;

    public int flagHeight;
    public int respawncycle;
    public boolean raidMode = true;
    public boolean vehicleBaseType;
    public CampObj campObj;
    private Occupation territory;
    private String militaryType = "";
    private String militaryFactionName = "";
    private boolean wasCountingDown;

    public TileEntityFlag() {}

    public TileEntityFlag(CampObj camp) {
        campObj = camp;
        flagHeight = camp.maxFlagHeight;
        respawncycle = camp.flagSpawnInterval;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        flagHeight = tag.getInteger("flagHeight");
        respawncycle = tag.getInteger("respawncycle");
        raidMode = !tag.hasKey("raidMode") || tag.getBoolean("raidMode");
        vehicleBaseType = tag.getBoolean("vehicleBaseType");
        campObj = campsHash.get(tag.getString("campObj"));
        militaryType = tag.getString("MilitaryType");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("flagHeight", flagHeight);
        tag.setInteger("respawncycle", respawncycle);
        tag.setBoolean("raidMode", raidMode);
        tag.setBoolean("vehicleBaseType", vehicleBaseType);
        if (campObj != null) tag.setString("campObj", campObj.campName);
        if (!militaryType.isEmpty()) tag.setString("MilitaryType", militaryType);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        if (territory != null && !territory.campObj.isRaidType) {
            tag.setString("MilitaryFactionName", hmggvcmob.military.MilitaryRelations.displayName(territory.faction()));
        }
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
        NBTTagCompound tag = packet.func_148857_g();
        readFromNBT(tag);
        militaryFactionName = tag.getString("MilitaryFactionName");
    }

    public String militaryFactionName() { return militaryFactionName; }

    @Override
    public void onChunkUnload() {
        clearAttacked();
    }

    @Override
    public void invalidate() {
        clearAttacked();
        super.invalidate();
    }

    private void clearAttacked() {
        territory = null;
    }

    public void designate(String type) {
        militaryType = type;
        markDirty();
    }

    public Occupation occupation() {
        if (territory == null) setFlagData();
        return territory;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || isInvalid()) return;
        if (campObj == null && getBlockType() instanceof GVCBlockFlag) {
            campObj = ((GVCBlockFlag) getBlockType()).campObj;
        }
        if (campObj == null) return;
        if (territory == null) setFlagData();
        if (!territory.occupied() && !MilitaryConfig.occupation) return;
        if (!vehicleBaseType && !campObj.isOneTime && !campObj.isRaidType
                && (MilitaryConfig.occupation || MilitaryConfig.recruitment)) {
            MilitaryOccupation.update(this, territory, worldObj);
            if (MilitaryConfig.occupation) return;
        }
        if (worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
            territory.attacked = false;
            return;
        }
        long staggeredTick = worldObj.getTotalWorldTime() + xCoord + zCoord;
        if (Math.floorMod(staggeredTick, UPDATE_INTERVAL) != 0) return;

        int chunkX = xCoord >> 4;
        int chunkZ = zCoord >> 4;
        int minY = Math.max(0, ((yCoord >> 4) - 1) * 16);
        int maxY = Math.min(256, ((yCoord >> 4) + 2) * 16);
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox((chunkX - 1) * 16, minY, (chunkZ - 1) * 16,
                (chunkX + 2) * 16, maxY, (chunkZ + 2) * 16);
        int allies = 0;
        int enemies = 0;
        int captureRate = 0;
        int supplyBoxes = worldObj.getEntitiesWithinAABB(hmggvcmob.entity.EntitySupplyBox.class, area).size();
        CampObj nextCamp = campObj;
        for (Object candidate : worldObj.getEntitiesWithinAABB(EntityLiving.class, area)) {
            if (!(candidate instanceof IflagBattler) || !((EntityLiving) candidate).isEntityAlive()) continue;
            IflagBattler battler = (IflagBattler) candidate;
            if (battler.isThisFriendCamp(campObj)) {
                allies++;
                captureRate++;
            } else if (battler.isThisAttackAbleCamp(campObj)) {
                enemies++;
                captureRate--;
                nextCamp = battler.getCampObj();
            }
        }
        for (Object nearby : worldObj.getEntitiesWithinAABB(EntityPlayer.class, area)) {
            if (!((EntityPlayer) nearby).isEntityAlive()) continue;
            if (campObj.playerIsFriend) {
                allies += 10;
                captureRate += 10;
            } else {
                enemies++;
                captureRate -= 10;
                if (nextCamp == campObj) nextCamp = forPlayer;
            }
        }
        if (islmmloaded) {
            for (Object nearby : worldObj.getEntitiesWithinAABB(LMM_LittleMaidMobX.class, area)) {
                if (!((EntityLiving) nearby).isEntityAlive()) continue;
                if (campObj.playerIsFriend) {
                    allies += 10;
                    captureRate += 10;
                } else {
                    enemies++;
                    captureRate -= 10;
                    if (nextCamp == campObj) nextCamp = forPlayer;
                }
            }
        }
        territory.attacked = enemies > 0;
        int previousHeight = flagHeight;
        flagHeight = Math.min(campObj.maxFlagHeight, flagHeight + captureRate * UPDATE_INTERVAL);
        if (flagHeight < 0) {
            if (campObj.isOneTime) {
                worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            } else if (nextCamp != campObj) {
                worldObj.setBlock(xCoord, yCoord, zCoord, nextCamp.campsBlock);
                TileEntity tile = worldObj.getTileEntity(xCoord, yCoord, zCoord);
                if (tile instanceof TileEntityFlag) {
                    TileEntityFlag replacement = (TileEntityFlag) tile;
                    replacement.flagHeight = 0;
                    replacement.setFlagData();
                    replacement.markDirty();
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
            }
            return;
        }

        boolean spawnable = !vehicleBaseType && campObj.teamEntityClasses != null && allies >= enemies;
        int room = REINFORCEMENT_LIMIT - allies - enemies - supplyBoxes;
        boolean launchingRaid = campObj.isRaidType && raidMode;
        boolean spawned = false;
        boolean countingDown = spawnable && (launchingRaid || room > 0);
        if (countingDown) {
            respawncycle -= UPDATE_INTERVAL;
            if (respawncycle <= 0) {
                if (launchingRaid) {
                    spawnRaid();
                    raidMode = false;
                } else {
                    int count = Math.min(room, groupSize(campObj.spawnEntitiesAve));
                    for (int attempt = 0; attempt < count; attempt++) {
                        spawn(campObj.teamEntityClasses, xCoord + 0.5, yCoord + 1, zCoord + 0.5, false);
                    }
                    if (campObj.isRaidType) raidMode = true;
                }
                respawncycle = Math.max(UPDATE_INTERVAL, campObj.flagSpawnInterval);
                spawned = true;
            }
            markDirty();
        }
        if (previousHeight != flagHeight || spawned || wasCountingDown != countingDown
                || (countingDown && Math.floorMod(staggeredTick, 20) == 0)) {
            markDirty();
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        wasCountingDown = countingDown;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(xCoord - 2, yCoord, zCoord - 2, xCoord + 3, yCoord + 6, zCoord + 3);
    }

    public void setFlagData() {
        if (worldObj == null || worldObj.isRemote || campObj == null) return;
        WorldSavedData_Flag data = WorldSavedData_Flag.get(worldObj);
        String type = militaryType.isEmpty() ? campObj.isRaidType ? "raid" : "base" : militaryType;
        territory = data.getOrCreate(xCoord, yCoord, zCoord, campObj, type);
        if (!territory.established) {
            territory.established = true;
            territory.control = MathHelper.clamp_int(flagHeight * MilitaryConfig.captureStrength
                    / Math.max(1, campObj.maxFlagHeight), 0, MilitaryConfig.captureStrength);
            territory.nextReinforcement = worldObj.getTotalWorldTime() + MilitaryConfig.reinforcementInterval;
            data.markDirty();
        }
        campObj = territory.campObj;
    }

    public void removeFlagData() {
        if (worldObj == null || worldObj.isRemote || campObj == null) return;
        WorldSavedData_Flag.get(worldObj).remove(xCoord, yCoord, zCoord);
    }

    private int groupSize(int average) {
        return Math.max(0, Math.min(RAID_LIMIT, average) - 4 + worldObj.rand.nextInt(8));
    }

    private void spawnRaid() {
        if (campObj.raiderEntityClasses == null || campObj.raiderEntityClasses.length == 0) return;
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(xCoord - RAID_RADIUS, 0, zCoord - RAID_RADIUS,
                xCoord + RAID_RADIUS + 1, 256, zCoord + RAID_RADIUS + 1);
        int remaining = RAID_LIMIT - worldObj.getEntitiesWithinAABB(EntityGBases.class, area).size();
        int groups = Math.min(campObj.raiderGroupNum, 8);
        for (int group = 0; group < groups && remaining > 0; group++) {
            Vector3d location = findRaiderSpawnPoint();
            if (location == null) break;
            int count = Math.min(groupSize(campObj.spawnRaiderEntitiesAve), remaining);
            int spawned = 0;
            for (int attempt = 0; attempt < count; attempt++) {
                if (spawn(campObj.raiderEntityClasses, location.x, location.y, location.z, true)) spawned++;
            }
            if (spawned == 0) continue;
            remaining -= spawned;
            for (Object candidate : worldObj.playerEntities) {
                EntityPlayer player = (EntityPlayer) candidate;
                if (player.getDistanceSq(xCoord, yCoord, zCoord) < RAID_RADIUS * RAID_RADIUS) {
                    player.addChatComponentMessage(new ChatComponentTranslation("gvc.raid.spawned",
                            MathHelper.floor_double(location.x), MathHelper.floor_double(location.z)));
                }
            }
        }
    }

    private boolean spawn(Class<? extends Entity>[] types, double x, double y, double z, boolean raider) {
        if (types.length == 0) return false;
        x += worldObj.rand.nextDouble() * 4 - 2;
        z += worldObj.rand.nextDouble() * 4 - 2;
        int bx = MathHelper.floor_double(x);
        int bz = MathHelper.floor_double(z);
        if (!worldObj.getChunkProvider().chunkExists(bx >> 4, bz >> 4)) return false;
        if (raider) y = worldObj.getTopSolidOrLiquidBlock(bx, bz);
        if (y < 1 || y >= 255) return false;
        try {
            Entity entity = types[worldObj.rand.nextInt(types.length)].getConstructor(World.class).newInstance(worldObj);
            entity.setLocationAndAngles(x, y, z, worldObj.rand.nextFloat() * 360, 0);
            if (entity.isEntityInsideOpaqueBlock() || !worldObj.checkNoEntityCollision(entity.boundingBox)
                    || worldObj.isAnyLiquid(entity.boundingBox)) return false;
            if (entity instanceof EntityLiving) ((EntityLiving) entity).onSpawnWithEgg(null);
            if (raider && entity instanceof IGVCmob) ((IGVCmob) entity).setCanDespawn(false);
            return worldObj.spawnEntityInWorld(entity);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Invalid registered camp spawn type", failure);
        }
    }

    public Vector3d findRaiderSpawnPoint() {
        WorldSavedData_Flag data = WorldSavedData_Flag.get(worldObj);
        for (int attempt = 0; attempt < RAID_ATTEMPTS; attempt++) {
            double angle = worldObj.rand.nextDouble() * Math.PI * 2;
            double radius = 32 + (RAID_RADIUS - 34) * (attempt + 1) / (double) RAID_ATTEMPTS;
            int x = MathHelper.floor_double(xCoord + Math.cos(angle) * radius);
            int z = MathHelper.floor_double(zCoord + Math.sin(angle) * radius);
            if (!worldObj.getChunkProvider().chunkExists(x >> 4, z >> 4)) continue;
            CampObjAndPos claimed = data.atTerritory(x + .5, z + .5, false);
            if (claimed != null && campObj.raiderIgnoreList != null) {
                boolean ignored = false;
                for (CampObj ignoredCamp : campObj.raiderIgnoreList) ignored |= claimed.campObj == ignoredCamp;
                if (ignored) continue;
            }
            int y = worldObj.getTopSolidOrLiquidBlock(x, z);
            if (y > 0 && y < 255 && !worldObj.getBlock(x, y - 1, z).getMaterial().isLiquid()) {
                return new Vector3d(x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    public static TileEntityFlag findNearest(World world, double x, double y, double z, int range,
                                              boolean horizontal, Predicate<TileEntityFlag> filter) {
        TileEntityFlag closest = null;
        double best = range * range;
        int minX = MathHelper.floor_double(x - range) >> 4;
        int minZ = MathHelper.floor_double(z - range) >> 4;
        int maxX = MathHelper.floor_double(x + range) >> 4;
        int maxZ = MathHelper.floor_double(z + range) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                if (!world.getChunkProvider().chunkExists(cx, cz)) continue;
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                for (Object candidate : chunk.chunkTileEntityMap.values()) {
                    if (!(candidate instanceof TileEntityFlag)) continue;
                    TileEntityFlag flag = (TileEntityFlag) candidate;
                    if (flag.isInvalid() || flag.campObj == null) continue;
                    double distance = flag.getDistanceFrom(x, horizontal ? flag.yCoord + 0.5 : y, z);
                    if (distance < best && filter.test(flag)) {
                        best = distance;
                        closest = flag;
                    }
                }
            }
        }
        return closest;
    }

    public boolean isAttacked() {
        return territory != null && territory.attacked;
    }

    public boolean isActiveBeacon() {
        return campObj != null && flagHeight >= campObj.maxFlagHeight / 2;
    }
}
