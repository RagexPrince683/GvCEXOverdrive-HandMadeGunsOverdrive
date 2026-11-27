
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

    public EntityMilitaryTrader(World world) {
        super(world);
        setSize(.6F, 1.8F);
        tasks.addTask(1, new EntityAIWatchClosest(this, EntityPlayer.class, 8F));
        tasks.addTask(2, new EntityAILookIdle(this));
    }

    void bind(Occupation post) {
        postId = post.id;
    }

    Occupation post() {
        if (postId == null || worldObj.isRemote) return null;
        return WorldSavedData_Flag.get(worldObj).byId(postId);
    }

    String faction() {
        Occupation post = post();
        return post == null ? null : post.faction();
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
        if (!worldObj.isRemote && (ticksExisted + getEntityId()) % 10 == 0) {
            Occupation post = post();
            if (post == null || !getUniqueID().equals(post.trader)) {
                setDead();
                return;
            }
            double x = post.traderX + .5;
            double z = post.traderZ + .5;
            if (getDistanceSq(x, post.traderY, z) > .15) setPositionAndUpdate(x, post.traderY, z);
        }
        super.onLivingUpdate();
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (worldObj.isRemote) return;
        Occupation post = post();
        if (post == null || !getUniqueID().equals(post.trader)) return;
        post.trader = null;
        post.nextTrader = MilitaryConfig.traderReplacement == 0 ? Long.MAX_VALUE
                : worldObj.getTotalWorldTime() + MilitaryConfig.traderReplacement;
        WorldSavedData_Flag.get(worldObj).markDirty();
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        postId = MilitaryEconomy.parseUuid(tag.getString("MilitaryPost"));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (postId != null) tag.setString("MilitaryPost", postId.toString());
    }
}
