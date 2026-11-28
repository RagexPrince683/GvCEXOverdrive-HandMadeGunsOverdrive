package hmggvcmob.military;

import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.UUID;

public final class EntityTraderMarker extends Entity {
    private UUID postId;
    private String station = "";

    public EntityTraderMarker(World world) {
        super(world);
        setSize(.45F, .45F);
    }

    void bind(Occupation post, Occupation.TraderLocation location) {
        postId = post.id;
        station = location.key;
        setPosition(location.x + .5, location.y + 1.1, location.z + .5);
        syncTime(location);
    }

    boolean atStation(UUID id, String key) {
        return !isDead && id.equals(postId) && key.equals(station);
    }

    @Override protected void entityInit() { dataWatcher.addObject(16, 0); }

    public int secondsRemaining() { return dataWatcher.getWatchableObjectInt(16); }

    private void syncTime(Occupation.TraderLocation location) {
        long ticks = Math.max(0, location.nextTrader - worldObj.getTotalWorldTime());
        dataWatcher.updateObject(16, (int)Math.min(Integer.MAX_VALUE, (ticks + 19) / 20));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote || ticksExisted % 10 != 0) return;
        Occupation post = WorldSavedData_Flag.get(worldObj).byId(postId);
        Occupation.TraderLocation location = post == null ? null : post.trader(station);
        if (!MilitaryConfig.traderMarkers || !MilitaryConfig.recruitment || location == null || !location.waiting
                || location.entity != null || location.nextTrader == Long.MAX_VALUE || post.traderFaction(location).isEmpty()) {
            setDead();
            return;
        }
        syncTime(location);
    }

    // Its station is persisted. The marker is reconstructed from it when that site next ticks.
    @Override public boolean writeToNBTOptional(NBTTagCompound tag) { return false; }
    @Override protected void readEntityFromNBT(NBTTagCompound tag) {}
    @Override protected void writeEntityToNBT(NBTTagCompound tag) {}
}
