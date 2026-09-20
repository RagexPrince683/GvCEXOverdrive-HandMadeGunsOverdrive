package handmadeguns.tech;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/** Per-world, server-authoritative HMG technology progression. */
public final class HMGTechTierData extends WorldSavedData {
    private static final String DATA_NAME = "hmg_technology_tier";
    private int unlockedHalfSteps;

    public HMGTechTierData() { super(DATA_NAME); }
    public HMGTechTierData(String name) { super(name); }

    public static HMGTechTierData get(World world) {
        if (world == null) return null;
        MinecraftServer server = MinecraftServer.getServer();
        World primaryWorld = server == null ? null : server.worldServerForDimension(0);
        World storageWorld = primaryWorld == null ? world : primaryWorld;
        HMGTechTierData data = (HMGTechTierData) storageWorld.perWorldStorage.loadData(
                HMGTechTierData.class, DATA_NAME);
        if (data == null) {
            data = new HMGTechTierData();
            storageWorld.perWorldStorage.setData(DATA_NAME, data);
        }
        return data;
    }

    public int getUnlockedHalfSteps() { return unlockedHalfSteps; }

    public void setUnlockedHalfSteps(int value) {
        unlockedHalfSteps = Math.max(0, Math.min(10, value));
        markDirty();
    }

    @Override public void readFromNBT(NBTTagCompound tag) {
        unlockedHalfSteps = Math.max(0, Math.min(10, tag.getInteger("UnlockedHalfSteps")));
    }

    @Override public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("UnlockedHalfSteps", unlockedHalfSteps);
    }
}
