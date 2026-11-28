
package hmggvcmob.military;

import cpw.mods.fml.common.registry.VillagerRegistry;
import hmggvcmob.camp.CampObj;
import hmggvcmob.tile.TileEntityFlag;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import java.util.List;
import java.util.Random;

public final class VillageOutpost extends StructureVillagePieces.Village {
    private int ground = -1;
    private int width = 13;
    private String faction = "union";
    private boolean redesigned;

    public VillageOutpost() {}

    private VillageOutpost(StructureVillagePieces.Start start, int type, StructureBoundingBox box,
                           int orientation, int width, Random random) {
        super(start, type);
        this.boundingBox = box;
        this.coordBaseMode = orientation;
        this.width = width;
        redesigned = true;
        boolean insurgent = MilitaryRelations.available("allies") && random.nextInt(100) < MilitaryConfig.villageOccupation;
        if (insurgent && (!MilitaryConfig.regionalSites || !MilitarySites.protectedStartingArea(box.minX, box.minZ))) faction = "allies";
        else if (MilitaryRelations.available(MilitaryConfig.initialVillageFaction)) faction = MilitaryConfig.initialVillageFaction;
        else faction = MilitaryRelations.factions().stream().filter(MilitaryRelations::available).findFirst().orElse("");
    }

    static void register() {
        MapGenStructureIO.func_143031_a(VillageOutpost.class, "GVCVillageOutpost");
        if (MilitaryConfig.villages && MilitaryConfig.villageWeight > 0) {
            VillagerRegistry.instance().registerVillageCreationHandler(new Handler());
        }
    }

    @Override
    protected void func_143012_a(NBTTagCompound tag) {
        super.func_143012_a(tag);
        tag.setInteger("GVCGround", ground);
        tag.setInteger("GVCWidth", width);
        tag.setString("GVCFaction", faction);
        tag.setBoolean("GVCOfficeDesign", redesigned);
    }

    @Override
    protected void func_143011_b(NBTTagCompound tag) {
        super.func_143011_b(tag);
        ground = tag.getInteger("GVCGround");
        width = tag.getInteger("GVCWidth");
        faction = tag.getString("GVCFaction");
        redesigned = tag.getBoolean("GVCOfficeDesign");
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox clip) {
        if (MilitarySites.isShowcase(world)) return true;
        if (ground < 0) {
            ground = getAverageGroundLevel(world, clip);
            if (ground < 0) return true;
            boundingBox.offset(0, ground - boundingBox.minY, 0);
            if (MilitaryConfig.regionalSites && "allies".equals(faction) && !"allies".equals(MilitaryConfig.initialVillageFaction)
                    && MilitaryRelations.available(MilitaryConfig.initialVillageFaction)
                    && MilitarySites.protectedStartingArea(world, boundingBox.minX, boundingBox.minZ)) {
                faction = MilitaryConfig.initialVillageFaction;
            }
        }
        int end = width - 1;
        int center = width / 2;
        int top = redesigned ? 10 : 6;
        for (int x = 0; x <= end; x++) for (int z = 0; z <= end; z++) {
            for (int y = 1; y <= top; y++) {
                if (x != center || z != 3 || y != 1) placeBlockAtCurrentPosition(world, Blocks.air, 0, x, y, z, clip);
            }
            placeBlockAtCurrentPosition(world, MilitaryMaterials.CONCRETE, 0, x, 0, z, clip);
            for (int y = -1; y >= -8; y--) {
                Block below = getBlockAtCurrentPosition(world, x, y, z, clip);
                if (below.getMaterial().isSolid()) break;
                placeBlockAtCurrentPosition(world, Blocks.stonebrick, 0, x, y, z, clip);
            }
        }
        VillageOffice office = new VillageOffice(width, faction, (x, y, z, block, metadata) ->
                placeBlockAtCurrentPosition(world, block, metadata, x, y, z, clip));
        if (redesigned) office.build();
        else office.buildLegacy();

        int x = getXWithOffset(center, 3);
        int y = getYWithOffset(1);
        int z = getZWithOffset(center, 3);
        if (clip.isVecInside(x, y, z)) {
            CampObj camp = MilitaryRelations.camp(faction);
            if (!(world.getTileEntity(x, y, z) instanceof TileEntityFlag)) {
                world.setBlock(x, y, z, camp.campsBlock, 0, 2);
            }
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityFlag) {
                TileEntityFlag flag = (TileEntityFlag) tile;
                flag.designate("village");
                Occupation post = flag.occupation();
                if (post != null && post.tenure == 0 && MilitaryRelations.isFaction(faction) && !post.faction().equals(faction)) {
                    post.setFaction(faction);
                    hmggvcmob.world.WorldSavedData_Flag.get(world).markDirty();
                }
                if (post != null && post.entrance.length == 0) {
                    int gateX = getXWithOffset(center, 0), gateZ = getZWithOffset(center, 0);
                    post.entrance = new int[]{gateX, y, gateZ};
                    post.approach = gateX > x ? 1 : gateZ > z ? 2 : gateX < x ? 3 : 0;
                    post.footprint = width - 4;
                    Occupation.TraderLocation station = post.trader("main");
                    if (station != null && station.y == 0) {
                        station.position(new int[]{getXWithOffset(center + 2, center + 1), y,
                                getZWithOffset(center + 2, center + 1)});
                    }
                    hmggvcmob.world.WorldSavedData_Flag.get(world).markDirty();
                    MilitaryRoads.siteRegistered(world, post);
                }
            }
        }
        return true;
    }

    private static final class Handler implements VillagerRegistry.IVillageCreationHandler {
        @Override
        public StructureVillagePieces.PieceWeight getVillagePieceWeight(Random random, int villageSize) {
            return new StructureVillagePieces.PieceWeight(VillageOutpost.class, MilitaryConfig.villageWeight, 1);
        }

        @Override
        public Class<?> getComponentClass() { return VillageOutpost.class; }

        @Override
        public Object buildComponent(StructureVillagePieces.PieceWeight weight, StructureVillagePieces.Start start,
                                     List pieces, Random random, int x, int y, int z, int orientation, int type) {
            int size = MilitaryConfig.outpostWidth;
            StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, size, 9, size, orientation);
            if (!canVillageGoDeeper(box) || StructureComponent.findIntersecting(pieces, box) != null) return null;
            return new VillageOutpost(start, type, box, orientation, size, random);
        }
    }
}
