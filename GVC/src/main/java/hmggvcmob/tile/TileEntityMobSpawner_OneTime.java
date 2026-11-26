package hmggvcmob.tile;

import hmggvcmob.entity.IGVCmob;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;

import javax.vecmath.Vector3d;


public class TileEntityMobSpawner_OneTime extends TileEntity {
    private static final String __OBFID = "CL_00000360";

    public String mobName;
    public String vehicleName;
    public Vector3d offset = new Vector3d(0,0,0);
    public float performDist = 256;

    @Override
    public void updateEntity() {
        if (worldObj == null || isInvalid() || worldObj.isRemote
                || (!hmggvcmob.GVCMobPlus.VEHICLES_ENABLED && vehicleName != null && !vehicleName.isEmpty())
                || Math.floorMod(worldObj.getTotalWorldTime() + xCoord + zCoord, 20) != 0
                || mobName == null || !mobName.startsWith("GVCMob.")) return;
        EntityPlayer player = worldObj.getClosestPlayer(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, performDist);
        if (player == null || player.capabilities.isCreativeMode) return;
        double x = xCoord + 0.5 + offset.x;
        double y = yCoord + 0.5 + offset.y;
        double z = zCoord + 0.5 + offset.z;
        if (y < 0 || y >= 256 || !worldObj.blockExists(MathHelper.floor_double(x),
                MathHelper.floor_double(y), MathHelper.floor_double(z))) return;
        Entity entity = EntityList.createEntityByName(mobName, worldObj);
        if (!(entity instanceof EntityLiving)) return;
        entity.setLocationAndAngles(x, y, z, worldObj.rand.nextFloat() * 360, 0);
        if (entity.isEntityInsideOpaqueBlock()) return;
        ((EntityLiving) entity).onSpawnWithEgg(null);
        if (entity instanceof IGVCmob) ((IGVCmob) entity).setCanDespawn(false);
        if (worldObj.spawnEntityInWorld(entity)) worldObj.setBlockToAir(xCoord, yCoord, zCoord);
    }

    public TileEntityMobSpawner_OneTime(){
    }

    public void readFromNBT(NBTTagCompound p_145839_1_)
    {
        super.readFromNBT(p_145839_1_);
        mobName = p_145839_1_.getString("mobName");
        vehicleName = p_145839_1_.getString("vehicleName");
        int[] pos = p_145839_1_.getIntArray("offset");
        if (pos.length == 3) offset = new Vector3d(pos[0], pos[1], pos[2]);
        if (p_145839_1_.hasKey("performDist")) performDist = p_145839_1_.getFloat("performDist");
    }
    public void writeToNBT(NBTTagCompound p_145841_1_)
    {
        super.writeToNBT(p_145841_1_);
        p_145841_1_.setString("mobName", mobName == null ? "" : mobName);
        p_145841_1_.setString("vehicleName", vehicleName == null ? "" : vehicleName);
        p_145841_1_.setFloat("performDist", performDist);
        p_145841_1_.setIntArray("offset",new int[]{(int)offset.x,(int)offset.y,(int)offset.z});
    }
}
