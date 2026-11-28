
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

    public VillageOutpost() {}

    private VillageOutpost(StructureVillagePieces.Start start, int type, StructureBoundingBox box,
                           int orientation, int width, Random random) {
        super(start, type);
        this.boundingBox = box;
        this.coordBaseMode = orientation;
        this.width = width;
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
    }

    @Override
    protected void func_143011_b(NBTTagCompound tag) {
        super.func_143011_b(tag);
        ground = tag.getInteger("GVCGround");
        width = tag.getInteger("GVCWidth");
        faction = tag.getString("GVCFaction");
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox clip) {
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
        Block masonry = Blocks.cobblestone;
        fillWithBlocks(world, clip, 0, 1, 0, end, 6, end, Blocks.air, Blocks.air, false);
        fillWithBlocks(world, clip, 0, 0, 0, end, 0, end, masonry, masonry, false);
        for (int x = 0; x <= end; x++) {
            for (int z = 0; z <= end; z++) {
                for (int y = -1; y >= -8; y--) {
                    Block below = getBlockAtCurrentPosition(world, x, y, z, clip);
                    if (below.getMaterial().isSolid()) break;
                    placeBlockAtCurrentPosition(world, masonry, 0, x, y, z, clip);
                }
            }
        }

        for (int i = 0; i <= end; i++) {
            wall(world, clip, 0, i);
            wall(world, clip, end, i);
            wall(world, clip, i, end);
            if (Math.abs(i - center) > 1) wall(world, clip, i, 0);
        }
        fillWithBlocks(world, clip, center - 1, 0, 0, center + 1, 0, end - 5, Blocks.gravel, Blocks.gravel, false);
        fillWithBlocks(world, clip, center - 2, 3, 0, center + 2, 3, 1, Blocks.planks, Blocks.planks, false);
        placeBlockAtCurrentPosition(world, Blocks.torch, 5, center - 2, 2, 1, clip);
        placeBlockAtCurrentPosition(world, Blocks.torch, 5, center + 2, 2, 1, clip);

        int barracks = end - 4;
        fillWithBlocks(world, clip, 1, 0, barracks, end - 1, 0, end - 1, Blocks.planks, Blocks.planks, false);
        fillWithBlocks(world, clip, 1, 1, barracks, end - 1, 3, end - 1, masonry, Blocks.air, false);
        fillWithBlocks(world, clip, 1, 4, barracks, end - 1, 4, end - 1, Blocks.planks, Blocks.planks, false);
        for (int x = center - 1; x <= center + 1; x++) {
            placeBlockAtCurrentPosition(world, Blocks.air, 0, x, 1, barracks, clip);
            placeBlockAtCurrentPosition(world, Blocks.air, 0, x, 2, barracks, clip);
        }
        for (int x = 2; x <= end - 2; x += 2) {
            placeBlockAtCurrentPosition(world, Blocks.glass_pane, 0, x, 2, end - 1, clip);
            placeBlockAtCurrentPosition(world, Blocks.wool, 7, x, 1, end - 2, clip);
        }
        placeBlockAtCurrentPosition(world, Blocks.crafting_table, 0, 2, 1, barracks + 1, clip);
        placeBlockAtCurrentPosition(world, Blocks.torch, 5, center, 1, end - 2, clip);

        fillWithBlocks(world, clip, 1, 1, 1, 2, 3, 2, masonry, masonry, false);
        for (int y = 1; y <= 4; y++) placeBlockAtCurrentPosition(world, Blocks.ladder, getMetadataWithOffset(Blocks.ladder, 3), 2, y, 3, clip);
        fillWithBlocks(world, clip, 1, 4, 1, 2, 4, 2, Blocks.planks, Blocks.planks, false);
        placeBlockAtCurrentPosition(world, Blocks.fence, 0, 1, 5, 1, clip);
        placeBlockAtCurrentPosition(world, Blocks.fence, 0, 2, 5, 1, clip);
        placeBlockAtCurrentPosition(world, Blocks.torch, 5, 1, 5, 2, clip);

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
                    hmggvcmob.world.WorldSavedData_Flag.get(world).markDirty();
                    MilitaryRoads.siteRegistered(world, post);
                }
            }
        }
        return true;
    }

    private void wall(World world, StructureBoundingBox clip, int x, int z) {
        placeBlockAtCurrentPosition(world, Blocks.cobblestone, 0, x, 1, z, clip);
        placeBlockAtCurrentPosition(world, Blocks.cobblestone_wall, 0, x, 2, z, clip);
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
            StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, size, 7, size, orientation);
            if (!canVillageGoDeeper(box) || StructureComponent.findIntersecting(pieces, box) != null) return null;
            return new VillageOutpost(start, type, box, orientation, size, random);
        }
    }
}
