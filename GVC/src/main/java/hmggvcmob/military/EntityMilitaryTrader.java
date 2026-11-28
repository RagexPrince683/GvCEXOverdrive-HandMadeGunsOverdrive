
package hmggvcmob.military;

import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.UUID;

public final class EntityMilitaryTrader extends EntityCreature {
    private UUID postId;
    private String station = "main";

    public EntityMilitaryTrader(World world) {
        super(world);
        setSize(.6F, 1.8F);
        tasks.addTask(1, new EntityAIWatchClosest(this, EntityPlayer.class, 8F));
        tasks.addTask(2, new EntityAILookIdle(this));
    }

    void bind(Occupation post, Occupation.TraderLocation location) {
        postId = post.id;
        station = location.key;
    }

    Occupation post() {
        if (postId == null || worldObj.isRemote) return null;
        return WorldSavedData_Flag.get(worldObj).byId(postId);
    }

    Occupation.TraderLocation location() {
        Occupation post = post();
        return post == null ? null : post.trader(station);
    }

    String faction() {
        Occupation post = post();
        Occupation.TraderLocation location = post == null ? null : post.trader(station);
        return location == null ? "" : post.traderFaction(location);
    }

    @Override
    protected boolean isAIEnabled() {
        return true;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource damage, float amount) {
        return !MilitaryConfig.invulnerableTraders && super.attackEntityFrom(damage, amount);
    }

    @Override
    public boolean interact(EntityPlayer player) {
        if (!worldObj.isRemote) MilitaryRecruitment.open(player, this);
        return true;
    }

    @Override
    public void onLivingUpdate() {
        if (!worldObj.isRemote && isEntityAlive() && (ticksExisted + getEntityId()) % 10 == 0) {
            Occupation.TraderLocation location = location();
            if (location == null || !getUniqueID().equals(location.entity)) {
                setDead();
                return;
            }
            double x = location.x + .5;
            double z = location.z + .5;
            if (getDistanceSq(x, location.y, z) > .15) setPositionAndUpdate(x, location.y, z);
        }
        super.onLivingUpdate();
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (worldObj.isRemote) return;
        Occupation.TraderLocation location = location();
        if (location == null || !getUniqueID().equals(location.entity)) return;
        location.entity = null;
        location.waiting = true;
        location.nextTrader = MilitaryConfig.traderReplacement == 0 ? Long.MAX_VALUE
                : worldObj.getTotalWorldTime() + MilitaryConfig.traderReplacement;
        WorldSavedData_Flag.get(worldObj).markDirty();
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        postId = MilitaryEconomy.parseUuid(tag.getString("MilitaryPost"));
        if (tag.hasKey("MilitaryStation", 8)) station = tag.getString("MilitaryStation");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (postId != null) tag.setString("MilitaryPost", postId.toString());
        tag.setString("MilitaryStation", station);
    }
}
