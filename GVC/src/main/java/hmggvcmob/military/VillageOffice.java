package hmggvcmob.military;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/** The same occupation office geometry is used by village pieces and structure previews. */
final class VillageOffice {
    interface Placer {
        void put(int x, int y, int z, Block block, int metadata);
    }

    private final Placer place;
    private final int end, middle;
    private final Block wall, trim, roof;
    private final int banner;
    private final boolean irregular;

    VillageOffice(int width, String faction, Placer place) {
        this.place = place;
        end = width - 1;
        middle = end / 2;
        irregular = MilitaryRoads.rough(faction);
        wall = irregular ? MilitaryMaterials.REVETMENT : MilitaryMaterials.CONCRETE;
        trim = irregular ? MilitaryMaterials.SANDBAGS : MilitaryMaterials.BLAST_WALL;
        roof = irregular ? MilitaryMaterials.CORRUGATED : MilitaryMaterials.STEEL_PLATE;
        banner = "allies".equals(faction) ? 13 : "pmc".equals(faction) ? 14 : 11;
    }

    void build() {
        int front = Math.max(4, middle - 2);
        int back = end - 1;
        int partition = Math.max(middle + 2, back - 3);
        fill(0, 0, 0, end, 0, end, Blocks.gravel);
        fill(middle - 1, 0, 0, middle + 1, 0, front - 1, MilitaryMaterials.TREADWAY);
        for (int x = 0; x <= end; x++) {
            if (Math.abs(x - middle) > 1) put(x, 1, 0, MilitaryMaterials.SANDBAGS);
        }
        for (int x : new int[]{1, end - 1}) {
            fill(x, 1, 0, x, 1, front - 1, trim);
            fill(x, 2, 1, x, 2, front - 1, irregular ? Blocks.fence : Blocks.iron_bars);
        }
        fill(middle - 2, 1, 0, middle - 2, 4, 1, trim);
        fill(middle + 2, 1, 0, middle + 2, 4, 1, trim);
        fill(middle - 2, 4, 0, middle + 2, 4, 1, roof);
        put(middle - 2, 3, 0, Blocks.wool, banner);
        put(middle + 2, 3, 0, Blocks.wool, banner);
        put(middle, 3, 1, Blocks.glowstone);

        fill(2, 0, front, end - 2, 0, back, MilitaryMaterials.TREADWAY);
        fill(2, 1, front, 2, 5, back, wall);
        fill(end - 2, 1, front, end - 2, 5, back, wall);
        fill(2, 1, back, end - 2, 5, back, wall);
        fill(2, 1, front, end - 2, 5, front, wall);
        for (int x = 2; x <= end - 2; x += 4) {
            fill(x, 1, front, x, 5, front, trim);
            fill(x, 1, back, x, 5, back, trim);
        }
        for (int x = 3; x < end - 2; x += 4) {
            fill(x, 2, front, Math.min(x + 1, end - 3), 3, front, Blocks.glass_pane);
            fill(x, 2, back, Math.min(x + 1, end - 3), 3, back, Blocks.glass_pane);
        }
        fill(middle - 1, 1, front, middle + 1, 2, front, Blocks.air);
        fill(middle - 1, 4, front, middle + 1, 4, front, Blocks.wool, banner);
        for (int x : new int[]{middle - 2, middle + 2}) {
            fill(x, 1, front - 1, x, 4, front - 1, trim);
        }
        fill(middle - 2, 4, front - 2, middle + 2, 4, front, roof);
        new SiteRoof(place::put, roof, trim, 0).build(2, 6, front, end - 2, back, 0, end,
                irregular ? SiteRoof.Shape.SHED : SiteRoof.Shape.MONITOR, irregular);
        if (partition < back - 1) {
            fill(3, 1, partition, end - 3, 4, partition, wall);
            fill(middle - 1, 1, partition, middle, 2, partition, Blocks.air);
            fill(middle + 1, 2, partition, end - 3, 2, partition, Blocks.glass_pane);
        }
        fill(3, 1, middle + 1, middle - 1, 1, middle + 1, MilitaryMaterials.REVETMENT);
        put(3, 1, middle + 2, Blocks.bookshelf);
        put(end - 3, 1, back - 1, Blocks.crafting_table);
        if (end >= 14) {
            put(end - 5, 1, back - 1, Blocks.bookshelf);
            put(end - 4, 1, back - 1, Blocks.furnace);
            put(4, 2, middle + 1, Blocks.carpet, banner);
            put(end - 3, 1, middle + 3, Blocks.wooden_slab);
            fill(3, 5, middle, end - 3, 5, middle, trim);
            put(4, 5, middle, Blocks.glowstone);
            put(end - 4, 5, middle, Blocks.glowstone);
        }
        put(middle - 1, 5, front + 1, Blocks.glowstone);
        put(middle + 1, 5, back - 1, Blocks.glowstone);
        if (front >= 6) {
            put(2, 1, 2, MilitaryMaterials.SANDBAGS);
            put(end - 2, 1, 2, MilitaryMaterials.REVETMENT);
        }
    }

    void buildLegacy() {
        int frontage = middle;
        fill(middle - 1, 0, 0, middle + 1, 0, end - 1, MilitaryMaterials.TREADWAY);
        for (int i = 0; i <= end; i++) {
            put(0, 1, i, trim);
            put(end, 1, i, trim);
            if (i > 0 && i < frontage) {
                put(0, 2, i, Blocks.iron_bars);
                put(end, 2, i, Blocks.iron_bars);
            }
            if (Math.abs(i - middle) > 1) put(i, 1, 0, MilitaryMaterials.SANDBAGS);
        }
        for (int x : new int[]{middle - 2, middle + 2}) {
            fill(x, 1, 0, x, 4, 1, trim);
            put(x, 3, 0, Blocks.wool, banner);
        }
        fill(middle - 2, 4, 0, middle + 2, 4, 1, roof);
        put(middle - 1, 3, 1, Blocks.glowstone);
        put(middle + 1, 3, 1, Blocks.glowstone);
        fill(1, 0, frontage, end - 1, 0, end, MilitaryMaterials.TREADWAY);
        fill(1, 1, frontage, 1, 5, end, wall);
        fill(end - 1, 1, frontage, end - 1, 5, end, wall);
        fill(1, 1, end, end - 1, 5, end, wall);
        fill(1, 1, frontage, end - 1, 5, frontage, wall);
        fill(2, 2, frontage, middle - 2, 3, frontage, Blocks.glass_pane);
        fill(middle + 2, 2, frontage, end - 2, 3, frontage, Blocks.glass_pane);
        fill(middle - 1, 1, frontage, middle + 1, 2, frontage, Blocks.air);
        fill(2, 2, end, end - 2, 3, end, Blocks.glass_pane);
        fill(0, 5, frontage - 1, end, 5, frontage, roof);
        fill(1, 6, frontage, end - 1, 6, end, roof);
        fill(2, 6, frontage, end - 2, 6, frontage, trim);
        fill(middle - 1, 4, frontage, middle + 1, 4, frontage, Blocks.wool, banner);
        put(middle, 4, frontage + 1, Blocks.glowstone);
        fill(2, 1, end - 2, middle - 2, 1, end - 2, MilitaryMaterials.REVETMENT);
        put(2, 1, end - 1, Blocks.bookshelf);
        put(end - 2, 1, end - 1, Blocks.crafting_table);
        put(end - 2, 1, frontage + 2, Blocks.iron_bars);
        put(end - 3, 1, frontage + 2, MilitaryMaterials.STEEL_PLATE);
        put(1, 1, 2, MilitaryMaterials.SANDBAGS);
        put(end - 1, 1, 3, MilitaryMaterials.STEEL_PLATE);
        put(end - 2, 1, 3, MilitaryMaterials.STEEL_PLATE);
    }

    private void put(int x, int y, int z, Block block) { put(x, y, z, block, 0); }
    private void put(int x, int y, int z, Block block, int metadata) { place.put(x, y, z, block, metadata); }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        fill(x1, y1, z1, x2, y2, z2, block, 0);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block, int metadata) {
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) {
            put(x, y, z, block, metadata);
        }
    }
}
