package hmggvcmob.military;

import hmggvcmob.block.GVCBlockFlag;
import hmggvcmob.military.SiteRoof.Shape;
import hmggvcmob.tile.TileEntityFlag;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.Arrays;
import java.util.Random;

final class MilitarySiteLayout {
    private enum Layout { CHECKPOINT, TOWER, BARRACKS, HEADQUARTERS, TRENCH, FARM, DEPOT, CONTRACT_YARD,
        MARKET, RELAY, LODGE, RIG, WAREHOUSE_RUIN, DERELICT_RIG, DISTRICT, VILLAGE_OFFICE, FORTRESS, SUPPLY_TOWN,
        FIGHTING_PITS, COVERED_NEST, TRENCH_LINE, BUNKER, RIDGE_POST, BARRIER, TUNNEL_HIDEOUT, TUNNEL_COMPOUND }
    private enum Palette { ADAPTIVE, STANDARDIZED, IMPROVISED, INDUSTRIAL, SALVAGED, CIVILIAN, SANDSTONE, ALPINE }

    final World world;
    final MilitarySites.Definition site;
    private final Layout layout;
    final int x, z, turn, r;
    private final int[][] heights, clearance;
    private final Palette palette;
    private final Block wall, frame, roof, walkway;
    private final boolean offshore;
    private final boolean dryRig;
    private final boolean alternate;
    final int floor;
    private MilitaryFieldworks works;
    private int[] cache;
    static final int MAX_WIDTH = 81;
    private static final int MAX_RISE = 18;

    static boolean supports(String layout, String palette, int width) {
        try {
            Palette.valueOf(palette.toUpperCase(java.util.Locale.ROOT));
            int minimum;
            switch (Layout.valueOf(layout)) {
                case DISTRICT: minimum = 37; break;
                case FORTRESS: minimum = 57; break;
                case SUPPLY_TOWN: minimum = 45; break;
                case TUNNEL_COMPOUND: minimum = 25; break;
                case TUNNEL_HIDEOUT: minimum = 19; break;
                case TRENCH_LINE: case BUNKER: minimum = 13; break;
                case HEADQUARTERS: case CONTRACT_YARD: case MARKET: minimum = 17; break;
                case BARRACKS: case LODGE: case RIG: case DERELICT_RIG: minimum = 15; break;
                default: minimum = 11;
            }
            return width >= minimum && width <= MAX_WIDTH && width % 2 == 1;
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return false;
        }
    }

    static MilitarySiteLayout survey(World world, MilitarySites.Definition site, int x, int z, int turn, String biome) {
        return survey(world, site, x, z, turn, biome, false);
    }

    static MilitarySiteLayout survey(World world, MilitarySites.Definition site, int x, int z, int turn, String biome,
                                     boolean preview) {
        int r = site.width / 2;
        boolean offshore = "RIG".equals(site.layout) || "DERELICT_RIG".equals(site.layout);
        boolean dryRig = offshore && preview && !"ocean".equals(biome);
        boolean sea = offshore && !dryRig;
        boolean precinct = "FORTRESS".equals(site.layout) || "SUPPLY_TOWN".equals(site.layout) || "DISTRICT".equals(site.layout);
        int[][] heights = new int[site.width][site.width];
        int[][] clearance = sea ? null : new int[site.width][site.width];
        int[] samples = new int[site.width * site.width];
        int count = 0, water = -1, pockets = 0, highestPocket = -1;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int sx = x + dx, sz = z + dz;
            int top = surfaceTop(world, sx, sz);
            int y = top;
            while (y > 1 && vegetation(world.getBlock(sx, y, sz))) y--;
            Block surface = world.getBlock(sx, y, sz);
            if (sea) {
                if (surface.getMaterial() != Material.water || !"ocean".equals(biome)) return null;
                if (water < 0) water = y;
                if (water != y) return null;
                while (y > water - 29 && world.getBlock(sx, y, sz).getMaterial() == Material.water) y--;
                if (water - y < 5 || water - y > 28 || world.getBlock(sx, y, sz).getMaterial() == Material.water) return null;
            } else {
                BiomeGenBase local = world.getWorldChunkManager().getBiomeGenAt(sx, sz);
                if (!site.fieldwork && !dryRig && surface.getMaterial() == Material.water) {
                    if (++pockets > samples.length / (precinct ? 25 : 16)
                            || dx == rotateX(0, -r, turn) && dz == rotateZ(0, -r, turn)
                            || "SUPPLY_TOWN".equals(site.layout) && dx == rotateX(0, r, turn) && dz == rotateZ(0, r, turn)) return null;
                    highestPocket = Math.max(highestPocket, y);
                    int bottom = y - 4;
                    do {
                        surface = world.getBlock(sx, --y, sz);
                    } while (y > bottom && surface.getMaterial() == Material.water);
                }
                if (!(natural(surface, local) || precinct && ore(surface)) || top - y > (site.fieldwork ? 36 : 18)
                        || y >= world.getActualHeight() - MAX_RISE - (dryRig ? 4 : 0)) return null;
                for (int dy = 1; dy <= 3; dy++) {
                    Block below = world.getBlock(sx, y - dy, sz);
                    if (below.hasTileEntity(world.getBlockMetadata(sx, y - dy, sz))) return null;
                }
                if (dryRig) {
                    if (y < 17) return null;
                    for (int dy = 1; dy <= 12; dy++) {
                        Block below = world.getBlock(sx, y - dy, sz);
                        if (!natural(below, local) && !ore(below)) return null;
                    }
                }
            }
            heights[dx + r][dz + r] = y;
            if (!sea) clearance[dx + r][dz + r] = top;
            samples[count++] = y;
        }
        Arrays.sort(samples);
        int grade = samples[samples.length / 2];
        int floor = sea ? water + 4 : dryRig ? grade + 4 : grade;
        if (!sea && (highestPocket > grade || grade - samples[0] > site.slope
                || samples[samples.length - 1] - grade > site.slope)) return null;
        if (dryRig && samples[0] != samples[samples.length - 1]) return null;
        MilitarySiteLayout plan = new MilitarySiteLayout(world, site, x, z, turn, r, heights, clearance,
                floor, offshore, dryRig, biome);
        if (plan.layout.ordinal() >= Layout.FIGHTING_PITS.ordinal()) {
            plan.works = new MilitaryFieldworks(plan, biome);
            if (!plan.works.survey()) return null;
        } else if (!offshore && (!plan.accessibleEdge(-r) || plan.layout == Layout.SUPPLY_TOWN && !plan.accessibleEdge(r))) {
            return null;
        }
        return plan;
    }

    private MilitarySiteLayout(World world, MilitarySites.Definition site, int x, int z, int turn, int r,
                               int[][] heights, int[][] clearance, int floor, boolean offshore, boolean dryRig, String biome) {
        this.world = world;
        this.site = site;
        this.x = x;
        this.z = z;
        this.turn = turn;
        this.r = r;
        this.heights = heights;
        this.clearance = clearance;
        this.floor = floor;
        this.offshore = offshore;
        this.dryRig = dryRig;
        alternate = MilitarySites.random(world, x, z, 0x6E69A562B77CD1C3L).nextBoolean();
        layout = Layout.valueOf(site.layout);
        Palette configured = Palette.valueOf(site.palette.toUpperCase(java.util.Locale.ROOT));
        palette = configured != Palette.ADAPTIVE ? configured
                : "desert".equals(biome) ? Palette.SANDSTONE : "snow".equals(biome) ? Palette.ALPINE : Palette.SALVAGED;
        switch (palette) {
            case STANDARDIZED: wall = MilitaryMaterials.CONCRETE; frame = MilitaryMaterials.BLAST_WALL; roof = MilitaryMaterials.STEEL_PLATE; walkway = MilitaryMaterials.TREADWAY; break;
            case IMPROVISED: wall = MilitaryMaterials.REVETMENT; frame = Blocks.log; roof = MilitaryMaterials.CORRUGATED; walkway = Blocks.dirt; break;
            case INDUSTRIAL: wall = MilitaryMaterials.CORRUGATED; frame = MilitaryMaterials.BLAST_WALL; roof = MilitaryMaterials.STEEL_PLATE; walkway = MilitaryMaterials.TREADWAY; break;
            case CIVILIAN: wall = Blocks.brick_block; frame = Blocks.log; roof = Blocks.wooden_slab; walkway = Blocks.gravel; break;
            case SANDSTONE: wall = Blocks.sandstone; frame = Blocks.sandstone; roof = Blocks.stone_slab; walkway = Blocks.sandstone; break;
            case ALPINE: wall = Blocks.log; frame = Blocks.cobblestone; roof = Blocks.wooden_slab; walkway = Blocks.cobblestone; break;
            default: wall = Blocks.mossy_cobblestone; frame = Blocks.log; roof = Blocks.wooden_slab; walkway = Blocks.gravel;
        }
    }

    void build(Occupation post, Random random) {
        if (works != null) {
            works.build(post, random);
        } else {
            if (offshore) deck();
            else ground();
            if (!offshore && layout != Layout.VILLAGE_OFFICE) path(-1, -r + 1, 1, r - 2);
            if (!offshore && post != null) {
                int lip = Math.max(-1, Math.min(1, surface(0, -r)));
                post.entrance = point(0, lip + 1, -r);
                post.approach = turn;
                if (layout == Layout.SUPPLY_TOWN) {
                    lip = Math.max(-1, Math.min(1, surface(0, r)));
                    post.rearEntrance = point(0, lip + 1, r);
                }
            }
            switch (layout) {
            case CHECKPOINT: checkpoint(); break;
            case TOWER: lookout(); break;
            case BARRACKS: barracks(); break;
            case HEADQUARTERS: headquarters(); break;
            case TRENCH: trench(); break;
            case FARM: farm(); break;
            case DEPOT: depot(); break;
            case CONTRACT_YARD: contractYard(); break;
            case MARKET: market(); break;
            case RELAY: relay(); break;
            case LODGE: lodge(); break;
            case RIG:
            case DERELICT_RIG: rig(layout == Layout.DERELICT_RIG); break;
            case WAREHOUSE_RUIN: ruin(random); break;
            case DISTRICT: district(); break;
            case VILLAGE_OFFICE:
                new VillageOffice(site.width, site.faction, (px, py, pz, block, meta) ->
                        put(px - r, py, pz - r, block, meta)).build();
                break;
            case FORTRESS: fortress(); break;
            case SUPPLY_TOWN: supplyTown(); break;
            }
        }
        if (post == null) return;
        post.footprint = r;
        put(0, anchorY() - floor, 0, post.campObj.campsBlock,
                post.hasBeaconService() ? 0 : GVCBlockFlag.SITE_MARKER);
        TileEntity flag = world.getTileEntity(x, anchorY(), z);
        if (flag instanceof TileEntityFlag) ((TileEntityFlag)flag).designate(site.id);
        if (works != null) return;
        for (MilitarySites.Trader entry : site.traders) {
            // Stations are immovable. Authored positions must have a walkable floor and clear headroom.
            if (get(entry.x, 0, entry.z).getMaterial().isSolid()
                    && get(entry.x, 1, entry.z) == Blocks.air && get(entry.x, 2, entry.z) == Blocks.air) {
                Occupation.TraderLocation location = new Occupation.TraderLocation(entry.key, entry.role);
                location.position(new int[]{x + rotateX(entry.x, entry.z, turn), floor + 1, z + rotateZ(entry.x, entry.z, turn)});
                location.neutral = entry.neutral;
                post.addTrader(location);
            }
        }
        loot(random);
        spawnScavengers();
    }

    int anchorY() { return floor + (works == null ? 1 : works.anchor()); }

    int surface(int px, int pz) {
        return heights[r + rotateX(px, pz, turn)][r + rotateZ(px, pz, turn)] - floor;
    }

    int[] point(int px, int py, int pz) {
        return new int[]{x + rotateX(px, pz, turn), floor + py, z + rotateZ(px, pz, turn)};
    }

    private boolean accessibleEdge(int edge) {
        for (int px = -1; px <= 1; px++) {
            if (Math.abs(surface(px, edge)) > 2) return false;
        }
        return true;
    }

    private void ground() {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int sx = r + rotateX(dx, dz, turn), sz = r + rotateZ(dx, dz, turn);
            int ground = heights[sx][sz] - floor;
            int top = clearance[sx][sz] - floor;
            if (Math.abs(dx) == r || Math.abs(dz) == r) {
                int lip = Math.max(-1, Math.min(1, ground));
                if (Math.abs(dx) <= 1 && (dz == -r || dz == r && layout == Layout.SUPPLY_TOWN)) {
                    for (int dy = ground + 1; dy < lip; dy++) put(dx, dy, dz, frame);
                    put(dx, lip, dz, walkway);
                }
                else if (ground >= -1 && ground <= 1) continue;
                else {
                    for (int dy = ground; dy < lip; dy++) put(dx, dy, dz, frame);
                    put(dx, lip, dz, frame);
                }
                for (int dy = lip + 1; dy <= top; dy++) put(dx, dy, dz, Blocks.air);
                continue;
            }
            Block landscape = get(dx, ground, dz);
            for (int dy = ground + 1; dy < 0; dy++) put(dx, dy, dz, frame);
            put(dx, 0, dz, ore(landscape) ? Blocks.stone : landscape);
            for (int dy = 1; dy <= top; dy++) put(dx, dy, dz, Blocks.air);
        }
    }

    private void deck() {
        if (dryRig) {
            fill(-r, -16, -r, r, -16, r, Blocks.stone);
            fill(-r, -15, -r, r, -4, r, Blocks.water);
        }
        fill(-r + 1, 0, -r + 1, r - 1, 0, r - 1, MilitaryMaterials.STEEL_PLATE);
        for (int dx : new int[]{-r + 3, r - 3}) for (int dz : new int[]{-r + 3, r - 3}) {
            int ground = dryRig ? -16 : surface(dx, dz);
            fill(dx, ground + 1, dz, dx + 1, 0, dz + 1, frame);
        }
        for (int side : new int[]{-r + 3, r - 3}) {
            fill(-r + 3, -1, side, r - 2, -1, side + 1, MilitaryMaterials.STEEL_PLATE);
            fill(side, -1, -r + 3, side + 1, -1, r - 2, MilitaryMaterials.STEEL_PLATE);
        }
        fill(-2, -3, -r, 2, -3, -r + 1, Blocks.planks);
        fill(-2, -3, -r + 2, 2, 0, -r + 2, frame);
        ladder(0, -2, 0, -r + 1, 0);
        fence(Blocks.iron_bars);
    }

    private void checkpoint() {
        int front = -r + 2, left = -r + 1;
        Block barrier = palette == Palette.SANDSTONE ? Blocks.sandstone : MilitaryMaterials.SANDBAGS;
        path(-2, -r + 1, 2, r - 1);
        int back = palette == Palette.INDUSTRIAL || alternate ? 2 : 0;
        int height = palette == Palette.STANDARDIZED ? 5 : 4;
        Shape roof = palette == Palette.INDUSTRIAL ? Shape.SHED : palette == Palette.SALVAGED ? Shape.GABLE
                : palette == Palette.SANDSTONE ? Shape.PARAPET : Shape.HIP;
        room(left, 0, front, -2, height, back, roof);
        aperture(-2, 0, back - 2, 0, 1);
        office(left + 1, -3, 0, back - 1);
        cache(-3, 1, front + 1);
        for (int side : new int[]{-1, 1}) {
            int edge = side * (r - 1);
            fill(edge, 1, 2, edge, 1, r - 1, barrier);
            fill(side * 2, 1, front, side * 2, 4, front, frame);
            put(side * 2, 3, front + 1, Blocks.glowstone);
        }
        if (palette == Palette.SALVAGED) {
            canopy(3, r - 1, -1, 2, 3, MilitaryMaterials.CAMOUFLAGE);
        } else {
            fill(-2, 4, front, 2, 4, front + 1, roofBlock());
        }
        fill(3, 1, front, r - 1, 1, front, barrier);
        furnishing(r - 1, 1, 0, Blocks.cauldron);
        if (palette == Palette.SANDSTONE) {
            canopy(3, r - 1, front + 1, 1, 4, Blocks.sandstone);
        } else if (palette == Palette.SALVAGED) {
            rubble(3, r - 1, 1, r - 2, 3);
        }
        lamp(r - 2, 1, r - 2);
    }

    private void lookout() {
        boolean ruined = site.classification == Occupation.Classification.ABANDONED;
        int half = r >= 6 && palette != Palette.IMPROVISED ? 3 : 2;
        int height = palette == Palette.INDUSTRIAL ? 9 : palette == Palette.IMPROVISED ? 6 : 7 + (alternate ? 1 : 0);
        tower(0, 1, half, height, ruined);
        if (ruined) {
            rubble(-half - 1, half + 2, -2, 5, 7);
        } else {
            int left = -r + 1;
            fill(left, 1, -3, -2, 1, -3, MilitaryMaterials.SANDBAGS);
            furnishing(left, 1, -2, Blocks.crafting_table);
            cache(left + 1, 1, -2);
        }
    }

    private void tower(int dx, int dz, int half, int height, boolean ruined) {
        for (int px : new int[]{dx - half, dx + half}) for (int pz : new int[]{dz - half, dz + half}) {
            fill(px, 1, pz, px, height + 3, pz, frame);
            fill(px, 1, pz, px, 2, pz, MilitaryMaterials.BLAST_WALL);
        }
        fill(dx - half, height - 1, dz - half, dx + half, height - 1, dz - half, frame);
        fill(dx - half, height - 1, dz + half, dx + half, height - 1, dz + half, frame);
        fill(dx - half, height, dz - half, dx + half, height, dz + half, MilitaryMaterials.TREADWAY);
        perimeter(dx - half, height + 1, dz - half, dx + half, dz + half, wall);
        if (palette == Palette.STANDARDIZED || palette == Palette.INDUSTRIAL) {
            for (int y = height + 2; y <= height + 3; y++) {
                perimeter(dx - half, y, dz - half, dx + half, dz + half, Blocks.glass_pane);
                for (int px : new int[]{dx - half, dx + half}) for (int pz : new int[]{dz - half, dz + half}) {
                    put(px, y, pz, frame);
                }
            }
        }
        roof(dx - half, height + 4, dz - half, dx + half, dz + half,
                palette == Palette.ALPINE || palette == Palette.SALVAGED ? Shape.GABLE
                        : palette == Palette.IMPROVISED ? Shape.SHED : Shape.HIP);
        fill(dx + 1, 1, dz + 1, dx + 1, height - 1, dz + 1, frame);
        ladder(dx + 1, 1, height, dz, 0);
        if (ruined) {
            fill(dx - half, height + 1, dz, dx - half, height + 1, dz + 1, Blocks.air);
            fill(dx - half - 1, height + 4, dz + half - 1, dx - half + 1, height + 6, dz + half + 1, Blocks.air);
        } else {
            put(dx - half, height + 3, dz + half - 1, Blocks.glowstone);
            office(dx - half + 1, dx - half + 1, height, dz - half + 1);
        }
    }

    private void barracks() {
        fence(Blocks.iron_bars);
        int edge = r - 2;
        room(-edge, 0, alternate ? -edge + 2 : -edge, -2, alternate ? 4 : 5, edge,
                alternate ? Shape.BARREL : Shape.GABLE);
        aperture(-2, 0, 0, 0, 2);
        bunks(-edge + 1, -edge + (alternate ? 4 : 2), edge - 1);
        if (edge >= 8) bunks(-5, 4, edge - 1);
        if (edge >= 9) {
            for (int z : new int[]{-1, 5}) fill(-edge + 1, 1, z, -edge + 2, 2, z, frame);
        }
        int mess = edge >= 6 ? 2 : 0;
        room(2, 0, mess, edge, 5, edge, alternate ? Shape.HIP : Shape.SHED);
        dining(3, edge - 1, mess + 2, edge - 1);
        path(-2, -2, edge - 1, mess);
        if (edge >= 9) {
            room(4, 0, -edge + 1, edge, 4, -2, Shape.PARAPET);
            aperture(4, 0, -4, 0, 1);
            clinic(5, edge - 1, -edge + 3, -5);
            if (edge >= 10) washroom(edge - 3, 0, -5, edge - 1, -3);
            path(1, -5, 4, -3);
        }
        cache(3, 1, edge - 1);
    }

    private void headquarters() {
        fence(Blocks.iron_bars);
        int edge = r - 2;
        int hall = alternate ? 6 : 7;
        room(-edge, 0, 3, edge, hall, edge, Shape.PARAPET);
        if (alternate && edge >= 10) {
            room(-edge + 2, hall, 4, 2, hall + 5, edge, Shape.HIP);
            stairs(-edge + 3, -edge + 4, 5, 0, hall, frame);
            commandRoom(-edge + 2, hall, 5, 1, edge - 3);
            fill(3, hall, 3, edge, hall, edge, roofBlock());
        }
        room(-edge, 0, -edge, -3, 5, 0, Shape.SHED);
        room(3, 0, -edge + 1, edge, 5, -2, alternate ? Shape.HIP : Shape.MONITOR);
        aperture(-5, 0, 0, 2, 0);
        aperture(4, 0, -2, 2, 0);
        canopy(-3, 3, 1, 3, 4, roof);
        office(-edge + 1, -7, 0, -edge + 2);
        if (edge >= 10) {
            screen(5, 0, 6, edge - 1, 8);
            office(6, edge - 1, 0, edge - 1);
            if (!alternate) {
                screen(-5, 0, 6, edge - 1, 8);
                office(-edge + 1, -6, 0, edge - 1);
            }
            commandRoom(-4, 0, 4, 4, edge - 1);
        } else {
            commandRoom(-edge + 1, 0, 3, edge - 1, edge - 1);
        }
        clinic(4, edge - 1, -edge + 3, -5);
        path(-edge + 1, 1, edge - 1, 2);
        cache(edge - 1, 1, -3);
    }

    private void trench() {
        fill(-r + 1, -2, 2, r - 1, -2, 3, Blocks.planks);
        fill(-r + 1, -1, 2, r - 1, 0, 3, Blocks.air);
        fill(-r + 1, -1, 4, r - 1, 1, 4, MilitaryMaterials.REVETMENT);
        fill(-r + 1, 1, 4, -2, 1, 4, MilitaryMaterials.SANDBAGS);
        fill(2, 1, 4, r - 1, 1, 4, MilitaryMaterials.SANDBAGS);
        fill(-1, -1, 3, 1, -1, 3, Blocks.planks);
        fill(-1, 0, 4, 1, 1, 4, Blocks.air);
        path(-1, 1, 1, 1);
        canopy(-r + 1, -2, 1, 4, 2, MilitaryMaterials.CAMOUFLAGE);
        room(2, 0, -r + 1, r - 1, 4, -1, Shape.SHED);
        aperture(3, 0, -1, 1, 0);
        utility(r - 2, 0, -r + 2);
        cache(r - 2, 1, -2);
    }

    private void farm() {
        int edge = r - 2;
        boolean ruined = site.classification == Occupation.Classification.ABANDONED;
        int front = alternate ? 1 : 0;
        room(-edge, 0, front, -2, 5, edge, palette == Palette.CIVILIAN && alternate ? Shape.HIP : Shape.GABLE);
        aperture(-2, 0, 1, 0, 1);
        room(2, 0, 2, edge, 4, edge, palette == Palette.IMPROVISED ? Shape.SHED : Shape.GABLE);
        aperture(3, 0, 2, 2, 0);
        path(-edge + 1, -1, edge - 1, -1);
        for (int dx = 2; dx <= edge; dx++) for (int dz = -edge; dz <= -2; dz++) {
            if (ruined) {
                if (Math.floorMod(dx + dz, 4) == 0) put(dx, 1, dz, Blocks.tallgrass, 1);
            } else {
                put(dx, 0, dz, dx == 3 ? Blocks.water : Blocks.farmland, dx == 3 ? 0 : 7);
                if (dx != 3) put(dx, 1, dz, dx % 3 == 0 ? Blocks.carrots : Blocks.wheat, 5);
            }
        }
        fill(2, 1, -edge - 1, edge, 1, -edge - 1, Blocks.fence);
        if (ruined) {
            fill(-edge - 1, 5, edge - 1, -edge + 2, 8, edge + 1, Blocks.air);
            fill(2, 1, edge - 2, 2, 3, edge - 1, Blocks.air);
            rubble(3, edge, 3, edge, 6);
        } else {
            bunks(-edge + 1, front + 2, edge - 1);
            kitchen(-edge + 3, -3, edge - 1);
            shelving(edge - 1, 1, 3, edge - 1);
            if (edge >= 7) {
                canopy(-edge, -5, -3, front, 3, roof);
                furnishing(-edge + 1, 1, -2, Blocks.hay_block);
            }
        }
        cache(3, 1, edge - 1);
    }

    private void depot() {
        int edge = r - 2;
        boolean camp = palette == Palette.IMPROVISED;
        int left = camp ? -edge + 1 : -edge;
        int right = camp ? Math.max(1, edge - 3) : edge;
        int front = alternate && edge >= 7 ? 1 : -1;
        Shape cover = camp ? Shape.BARREL : palette == Palette.CIVILIAN ? Shape.GABLE
                : alternate ? Shape.BARREL : Shape.MONITOR;
        room(left, 0, front, right, camp ? 4 : 6, edge, cover);
        aperture(-2, 0, front, Math.min(right - 1, 2) + 2, 0);
        canopy(-3, Math.min(right, 3), front - 2, front, 4, roof);
        path(-edge, -edge, edge, front - 1);
        shelving(left + 1, 1, front + 2, edge - 1);
        shelving(right - 1, 1, front + 2, edge - 1);
        if (camp && edge >= 6) {
            canopy(edge - 2, edge, front + 2, edge, 4, MilitaryMaterials.CAMOUFLAGE);
            utility(edge - 1, 0, edge - 1);
        } else if (edge >= 7) {
            office(left + 1, -4, 0, -edge + 2);
            rack(right - 3, front + 3, edge - 1);
            if (right - left >= 15) rack(left + 3, front + 3, edge - 1);
            fill(-2, 0, -edge + 1, 2, 0, front - 1, MilitaryMaterials.TREADWAY);
        }
        cache(left + 2, 1, edge - 2);
    }

    private void contractYard() {
        fence(Blocks.iron_bars);
        int edge = r - 2;
        boolean salvage = palette == Palette.SALVAGED;
        int front = salvage || alternate ? 3 : 2;
        room(-edge, 0, front, edge, salvage ? 5 : 7, edge, salvage ? Shape.SHED : Shape.MONITOR);
        bay(-edge + 2, -3, front);
        bay(1, Math.min(5, edge - 2), front);
        room(-edge, 0, -edge, -3, 5, -1, salvage ? Shape.GABLE : Shape.SHED);
        aperture(-5, 0, -1, 2, 0);
        office(-edge + 1, -4, 0, -edge + 1);
        path(-edge + 1, 0, edge - 1, 1);
        path(3, -edge, edge, -1);
        canopy(3, edge, -edge, -4, 4, MilitaryMaterials.CORRUGATED);
        shelves(4, edge - 1, -edge + 1, -5);
        workbench(-edge + 1, edge - 1, edge - 1);
        if (edge >= 8) {
            partition(edge - 4, 0, front + 1, edge - 1, 4, front + 2);
            rack(edge - 1, front + 2, edge - 2);
            maintenance(-edge + 2, -3, front, edge - 2);
            maintenance(1, Math.min(5, edge - 2), front, edge - 2);
        }
        if (salvage) {
            cargo(edge - 3, -edge + 2, edge - 1, -edge + 4, true);
            rubble(4, edge, -3, 0, 3);
            fill(-edge, 3, edge - 3, -edge, 4, edge - 2, MilitaryMaterials.CORRUGATED);
        }
        cache(edge - 1, 1, edge - 2);
        lamp(3, 1, -2);
    }

    private void market() {
        int edge = r - 2;
        boolean desert = palette == Palette.SANDSTONE;
        room(-edge, 0, 3, -2, 5, edge, desert ? Shape.PARAPET : Shape.HIP);
        room(2, 0, alternate ? 4 : 3, edge, 5, edge, desert ? Shape.PARAPET : Shape.SHED);
        path(-edge, -7, edge, 2);
        stall(-5, -5, 0, desert ? 1 : 9);
        stall(5, -5, 0, desert ? 12 : 14);
        shelves(-edge + 1, -3, edge - 1, edge - 1);
        dining(3, edge - 1, 6, edge - 1);
        if (desert) {
            fill(-2, 0, -5, 2, 0, -3, Blocks.sandstone);
            fill(-1, 0, -4, 1, 0, -4, Blocks.water);
            perimeter(-2, 1, -5, 2, -3, Blocks.sandstone);
            canopy(-2, 2, -5, -3, 4, Blocks.sandstone);
        } else {
            furnishing(-edge + 1, 1, -1, Blocks.hay_block);
            furnishing(edge - 1, 1, -1, Blocks.flower_pot);
        }
        lamp(-edge, 1, 1);
        lamp(edge, 1, 1);
        cache(3, 1, edge - 2);
    }

    private void relay() {
        int edge = r - 1, mast = r - 3;
        room(-edge, 0, 1, -2, 5, edge, palette == Palette.ALPINE ? Shape.GABLE
                : palette == Palette.SANDSTONE ? Shape.PARAPET : Shape.SHED);
        aperture(-2, 0, 2, 0, 1);
        office(-edge + 1, -3, 0, edge - 1);
        utility(-3, 0, edge - 2);
        path(0, -2, edge, 3);
        fill(mast - 1, 0, 1, mast + 1, 0, 3, MilitaryMaterials.BLAST_WALL);
        fill(mast, 1, 2, mast, 10, 2, Blocks.iron_bars);
        fill(mast - 1, 1, 2, mast + 1, 3, 2, frame);
        fill(mast - 2, 7, 2, mast + 2, 7, 2, Blocks.iron_bars);
        fill(mast, 8, 0, mast, 8, 4, Blocks.iron_bars);
        put(mast, 11, 2, Blocks.glowstone);
        furnishing(edge - 1, 1, -2, Blocks.cauldron);
        cache(-edge + 1, 1, edge - 2);
    }

    private void lodge() {
        int edge = r - 2;
        room(-edge, 0, 2, edge, 6, edge, Shape.GABLE);
        room(-edge, 0, -edge, -3, 4, -1, alternate ? Shape.GABLE : Shape.SHED);
        canopy(-3, 3, 0, 2, 4, Blocks.planks);
        fill(edge - 1, 1, edge - 1, edge - 1, 10, edge - 1, Blocks.cobblestone);
        put(edge - 2, 1, edge - 1, Blocks.furnace, facing(0));
        dining(-2, edge - 2, 5, edge - 2);
        bunks(-edge + 1, 4, edge - 1);
        if (edge >= 8) screen(-edge + 4, 0, 6, edge - 1, 7);
        shelving(edge - 1, 1, 4, edge - 3);
        shelves(-edge + 1, -4, -edge + 1, -2);
        cache(-edge + 2, 1, -3);
    }

    private void rig(boolean ruined) {
        int inset = r - 2;
        path(-1, -r + 1, 1, r - 2);
        room(-inset, 0, 2, -2, 5, inset, Shape.MONITOR);
        aperture(-4, 0, 2, 1, 0);
        room(-inset, 0, -inset + 1, -6, 4, -3, Shape.SHED);
        aperture(-6, 0, -5, 0, 1);
        commandRoom(-inset + 1, 0, 2, -3, inset - 1);
        workbench(-inset + 1, -7, -4);
        if (inset >= 10) {
            room(3, 0, -inset, inset, 4, -5, Shape.SHED);
            aperture(3, 0, -7, 0, 1);
            if (ruined) shelving(inset - 1, 1, -inset + 2, -6);
            else clinic(4, inset - 1, -inset + 2, -7);
        }
        cache(-inset + 1, 1, inset - 2);
        int mast = inset - 1, craneHeight = inset > 12 ? 11 : 8;
        fill(mast - 1, 0, 1, mast + 1, 0, 5, MilitaryMaterials.BLAST_WALL);
        for (int rail = 2; rail <= 3; rail++) {
            fill(mast, 1, rail, mast, craneHeight + 2, rail, MilitaryMaterials.STEEL_PLATE);
            fill(mast - 2, 1, rail, mast - 2, craneHeight - 2, rail, MilitaryMaterials.CORRUGATED);
            fill(2, craneHeight, rail, mast, craneHeight, rail, MilitaryMaterials.STEEL_PLATE);
            for (int px = 2; px < mast; px++) put(px, craneHeight + 1, rail, Blocks.iron_bars);
        }
        fill(mast - 2, craneHeight - 2, 2, mast, craneHeight - 2, 3, MilitaryMaterials.STEEL_PLATE);
        fill(2, craneHeight - 3, 2, 2, craneHeight - 1, 2, Blocks.iron_bars);
        put(2, craneHeight - 4, 2, MilitaryMaterials.STEEL_PLATE);
        path(3, -inset, inset, -inset + 3);
        for (int px = 3; px <= inset - 2; px += 5) cargo(px, inset - 3, px + 2, inset, ruined);
        if (inset >= 10) {
            int utility = inset - 4;
            for (int dz : new int[]{-inset + 5, -inset + 10}) {
                if (dz < -4 || dz >= 0) continue;
                fill(utility, 1, dz, utility + 3, 1, dz + 2, MilitaryMaterials.BLAST_WALL);
                fill(utility, 2, dz, utility + 3, 3, dz + 2, Blocks.hardened_clay);
                fill(utility + 1, 4, dz, utility + 2, 4, dz + 2, MilitaryMaterials.STEEL_PLATE);
            }
        }
        if (ruined) {
            fill(-inset, 1, 3, -inset, 2, 4, Blocks.air);
            fill(-inset + 1, 5, 3, -inset + 3, 7, 5, Blocks.air);
            fill(r - 2, 0, -4, r - 1, 1, -2, Blocks.air);
            rubble(3, inset - 2, 5, inset - 4, 7);
        } else {
            put(mast, craneHeight + 2, 2, Blocks.glowstone);
            lamp(3, 1, -inset + 1);
        }
    }

    private void ruin(Random random) {
        int edge = r - 2;
        room(-edge, 0, -edge + 1, edge, 5, edge, Shape.MONITOR);
        int breach = -edge + 2 + random.nextInt(3);
        fill(-edge - 1, 5, breach, 0, 7, breach + 3, Blocks.air);
        fill(-edge, 1, breach, -edge, 5, breach + 3, Blocks.air);
        fill(edge - 2, 4, edge - 3, edge + 1, 6, edge + 1, Blocks.air);
        aperture(-1, 0, -edge + 1, 3, 0);
        fill(edge - 2, 2, -edge + 1, edge - 1, 3, -edge + 1, Blocks.air);
        shelving(edge - 1, 1, -2, edge - 1);
        rubble(-edge + 1, -2, breach, breach + 4, 4);
        rubble(2, edge - 1, 2, edge - 1, 6);
        rubble(edge - 3, edge + 1, -edge - 1, -edge, 3);
        cache(edge - 2, 1, -2);
    }

    private void district() {
        int outer = r - 3, mid = r / 2;
        fence(palette == Palette.CIVILIAN ? Blocks.fence : Blocks.iron_bars);
        fill(-1, 0, -r + 1, 1, 0, r - 1, Blocks.stonebrick);
        fill(-r + 1, 0, -1, r - 1, 0, 1, Blocks.stonebrick);
        path(-outer, -mid, outer, -mid + 1);
        path(-outer, mid + 2, outer, mid + 2);
        room(-outer, 0, -outer, -4, 7, -mid - 1, palette == Palette.CIVILIAN ? Shape.GABLE : Shape.PARAPET);
        aperture((-outer - 4) / 2, 0, -mid - 1, 1, 0);
        commandRoom(-outer + 1, 0, -outer, -5, -mid - 2);
        room(4, 0, -outer + 1, outer, 6, -mid - 1, Shape.MONITOR);
        aperture((outer + 4) / 2 - 1, 0, -mid - 1, 3, 0);
        shelves(5, outer - 1, -outer + 3, -mid - 3);
        room(-outer, 0, 3, -4, 5, mid, Shape.GABLE);
        room(4, 0, 3, outer - 2, 5, mid, Shape.SHED);
        aperture(-9, 0, 3, 2, 0);
        aperture(7, 0, 3, 3, 0);
        bunks(-outer + 1, 4, mid - 1);
        serving(5, outer - 3, mid - 1);
        room(-outer, 0, mid + 3, -4, 5, outer, Shape.SHED);
        clinic(-outer + 1, -5, mid + 5, outer - 1);
        fill(5, 0, mid + 3, outer, 0, outer, Blocks.stonebrick);
        canopy(6, outer - 1, outer - 4, outer, 4, roof);
        for (int px = 8; px <= outer - 3; px += 4) furnishing(px, 1, outer - 1, Blocks.hay_block);
        for (int z : new int[]{-mid, 2, mid + 2}) for (int x : new int[]{-3, 3}) lamp(x, 1, z);
        cache(5, 1, -mid - 2);
    }

    private void fortress() {
        int wallEdge = r - 2, precinct = r - 5;
        boolean irregular = "improvised".equals(site.palette);
        Block curtain = irregular ? MilitaryMaterials.REVETMENT : MilitaryMaterials.BLAST_WALL;
        Block lane = irregular ? Blocks.gravel : MilitaryMaterials.TREADWAY;
        fill(-3, 0, -r + 1, 3, 0, r - 4, lane);
        fill(-precinct, 0, -1, precinct, 0, 3, lane);
        for (int side : new int[]{-1, 1}) {
            int x = wallEdge * side;
            fill(x, 1, -wallEdge, x, 4, wallEdge, curtain);
            fill(x, 5, -wallEdge + 1, x, 5, wallEdge - 1, MilitaryMaterials.SANDBAGS);
            fill(x, 6, -wallEdge + 7, x, 6, wallEdge - 1, MilitaryMaterials.BARBED_WIRE);
            int left = side < 0 ? -wallEdge : 4, right = side < 0 ? -4 : wallEdge;
            fill(left, 1, -wallEdge, right, 4, -wallEdge, curtain);
            fill(left, 5, -wallEdge, right, 5, -wallEdge, MilitaryMaterials.SANDBAGS);
            fill(side < 0 ? -wallEdge + 7 : left, 6, -wallEdge,
                    side > 0 ? wallEdge - 7 : right, 6, -wallEdge, MilitaryMaterials.BARBED_WIRE);
            tower(side * (wallEdge - 3), -wallEdge + 4, 3, 7, false);
            lamp(side * 6, 1, -wallEdge + 1);
        }
        for (int z = -wallEdge + 10; z < wallEdge - 2; z += 7) {
            fill(-wallEdge, 1, z, -wallEdge + 1, 3, z, curtain);
            fill(wallEdge - 1, 1, z, wallEdge, 3, z, curtain);
        }
        fill(-wallEdge, 1, wallEdge, wallEdge, 4, wallEdge, curtain);
        canopy(-4, 4, -wallEdge - 1, -wallEdge + 2, 5, roof);
        perimeter(-4, 6, -wallEdge - 1, 4, -wallEdge + 2, MilitaryMaterials.SANDBAGS);
        for (int side : new int[]{-1, 1}) {
            fill(side * 4, 1, -wallEdge, side * 4, 5, -wallEdge + 1, curtain);
            put(side * 4, 4, -wallEdge - 1, Blocks.wool, accent());
        }

        int commandBack = precinct - 2;
        room(-10, 0, 8, 10, 6, commandBack, Shape.FLAT);
        room(-10, 6, 8, 10, 12, commandBack, irregular ? Shape.SHED
                : palette == Palette.INDUSTRIAL ? Shape.MONITOR : Shape.HIP);
        aperture(-2, 0, 8, 4, 0);
        fill(-1, 7, 8, 1, 8, 8, Blocks.glass_pane);
        canopy(-4, 4, 6, 8, 5, roof);
        fill(-10, 0, 6, 10, 0, 7, lane);
        fill(-3, 0, 4, 3, 0, 7, lane);
        stairs(-7, -5, 12, 0, 6, curtain);
        fortressCommand(commandBack, curtain);
        lamp(13, 1, 7);

        room(-precinct + 1, 0, -precinct + 8, -7, 6, -5, irregular ? Shape.GABLE : Shape.BARREL);
        aperture(-7, 0, -9, 0, 2);
        bunks(-precinct + 2, -precinct + 10, -6);
        room(7, 0, -precinct + 8, precinct - 1, 7, -4, Shape.MONITOR);
        aperture(10, 0, -4, 4, 0);
        for (int x = 10; x < precinct - 2; x += 6) cargo(x, -11, x + 2, -8, false);
        room(-precinct + 1, 0, 7, -14, 6, commandBack, Shape.SHED);
        aperture(-14, 0, 10, 0, 1);
        dining(-precinct + 2, -15, 14, commandBack - 2);
        shelves(-precinct + 2, -16, 8, 9);
        room(14, 0, 7, precinct - 1, 6, commandBack, Shape.MONITOR);
        aperture(14, 0, 10, 0, 2);
        workbench(15, precinct - 2, commandBack - 2);
        path(-precinct + 2, 4, -13, 6);
        path(14, 4, precinct - 2, 6);
        path(-precinct + 2, -4, -6, -2);
        path(8, -3, precinct - 2, -2);
        for (int side : new int[]{-1, 1}) {
            for (int z = -17; z <= 7; z += 8) lamp(side * 5, 1, z);
            for (int z = 15; z < commandBack; z += 8) lamp(side * 12, 1, z);
        }
        if (irregular) {
            canopy(-precinct + 2, -8, 0, 3, 4, MilitaryMaterials.CAMOUFLAGE);
        } else if ("industrial".equals(site.palette)) {
            fill(-9, 8, 8, 9, 9, 8, Blocks.glass_pane);
            fill(16, 1, 7, 19, 3, 7, Blocks.air);
            fill(21, 1, 7, 24, 3, 7, Blocks.air);
            fill(16, 0, 4, 24, 0, 6, lane);
        } else {
            for (int x : new int[]{-4, 4}) fill(x, 1, 8, x, 11, 8, curtain);
            fill(-3, 7, 7, 3, 7, 7, MilitaryMaterials.STEEL_PLATE);
            fill(-2, 5, 8, 2, 5, 8, Blocks.wool);
        }
    }

    private void fortressCommand(int back, Block partition) {
        int briefing = back - 3;
        for (int z = 9; z < back; z++) {
            put(-1, 0, z, MilitaryMaterials.TREADWAY);
            put(0, 0, z, MilitaryMaterials.TREADWAY);
            if (z > 9) {
                put(-1, 1, z, Blocks.carpet, accent());
                put(0, 1, z, Blocks.carpet, accent());
            }
        }
        fill(1, 1, 15, 10, 3, 15, partition);
        aperture(3, 0, 15, 2, 0);
        aperture(7, 0, 15, 1, 0);
        fill(1, 3, 15, 10, 3, 15, Blocks.glass_pane);
        serving(6, 9, 10);
        shelving(9, 1, 16, back - 2);
        workbench(2, 5, back - 1);
        furnishing(7, 1, back - 1, Blocks.anvil);
        clinic(7, 9, 12, 14);
        screen(6, 0, 11, 14, 12);
        cache(2, 1, back - 2);

        fill(-4, 6, 10, -3, 6, back - 1, MilitaryMaterials.TREADWAY);
        fill(-4, 7, 10, -3, 7, back - 1, Blocks.carpet);
        fill(0, 7, briefing - 1, 5, 7, briefing, MilitaryMaterials.REVETMENT);
        fill(1, 8, briefing - 1, 4, 8, briefing, Blocks.carpet);
        for (int x = 0; x <= 5; x += 2) {
            furnishing(x, 7, briefing - 2, Blocks.wooden_slab);
            furnishing(x, 7, briefing + 1, Blocks.wooden_slab);
        }
        shelving(9, 7, 10, back - 1);
        for (int z = 10; z < briefing - 3; z += 4) {
            shelving(2, 7, z, z + 1);
            furnishing(4, 7, z, Blocks.crafting_table);
            furnishing(5, 7, z, Blocks.wooden_slab);
        }
        for (int x = -9; x < 9; x += 3) furnishing(x, 7, back - 1, Blocks.bookshelf);
        screen(6, 6, 12, briefing, Math.max(12, briefing - 3));
        office(7, 9, 6, 11);
        if (back >= 23) office(-8, -6, 6, back - 3);
    }

    private void supplyTown() {
        int limit = r - 4, far = limit - 1;
        Block lane = "improvised".equals(site.palette) ? Blocks.gravel : MilitaryMaterials.TREADWAY;
        fill(-2, 0, -r + 1, 2, 0, r - 1, lane);
        fill(-limit, 0, -2, limit, 0, 2, lane);
        room(-limit, 0, -limit, -5, 6, -6, palette == Palette.CIVILIAN ? Shape.GABLE : Shape.PARAPET);
        room(5, 0, -limit + 2, limit, 7, -5, Shape.MONITOR);
        room(-limit, 0, 6, -6, 6, far, Shape.GABLE);
        room(5, 0, 7, limit - 2, 5, far, Shape.SHED);
        aperture(-5, 0, -13, 0, 2);
        aperture(5, 0, -13, 0, 2);
        aperture(-6, 0, 9, 0, 2);
        aperture(5, 0, 9, 0, 2);
        int westDoor = (-limit - 5) / 2, eastDoor = (5 + limit) / 2;
        aperture(westDoor, 0, -6, 1, 0);
        aperture(eastDoor - 1, 0, -5, 3, 0);
        serving(-limit + 1, -9, -8);
        shelving(-limit + 1, 1, -limit + 2, -9);
        shelves(7, limit - 1, -limit + 4, -limit + 6);
        cargo(limit - 5, -9, limit - 2, -7, false);
        bunks(-limit + 1, 8, far - 1);
        workbench(6, limit - 3, far - 2);
        canopy(-13, -6, 3, 6, 4, palette == Palette.CIVILIAN ? Blocks.wool : roof);
        canopy(5, 13, 3, 6, 4, palette == Palette.CIVILIAN ? Blocks.wool : roof);
        path(-limit, 3, -4, 5);
        path(4, 3, limit, 6);
        path(-4, -13, -3, -1);
        path(3, -13, 4, -1);
        for (int p : new int[]{-limit + 2, -4, 4, limit - 2}) lamp(p, 1, 4);
        serving(-12, -8, 5);
        for (int p = 8; p <= limit - 3; p += 4) {
            furnishing(p, 1, 6, palette == Palette.CIVILIAN ? Blocks.hay_block : MilitaryMaterials.REVETMENT);
        }
        cache(7, 1, far - 3);
        for (int side : new int[]{-1, 1}) {
            int x = side * (r - 2);
            fill(x, 1, -r + 3, x, 1, -3, MilitaryMaterials.SANDBAGS);
            fill(x, 1, 4, x, 1, r - 4, MilitaryMaterials.REVETMENT);
            fill(side < 0 ? -r + 2 : 3, 1, -r + 2, side < 0 ? -3 : r - 2, 1, -r + 2, MilitaryMaterials.SANDBAGS);
        }
    }

    private void spawnScavengers() {
        if (site.scavengers == 0 || world.difficultySetting == net.minecraft.world.EnumDifficulty.PEACEFUL) return;
        int count = 0;
        for (int[] spot : new int[][]{{-3, 3}, {3, 3}, {-3, -3}, {3, -3}}) {
            if (count >= site.scavengers) break;
            int px = spot[0], pz = spot[1];
            if (!get(px, 0, pz).getMaterial().isSolid() || get(px, 1, pz) != Blocks.air || get(px, 2, pz) != Blocks.air) continue;
            net.minecraft.entity.monster.EntityZombie scavenger = new net.minecraft.entity.monster.EntityZombie(world);
            scavenger.setLocationAndAngles(x + rotateX(px, pz, turn) + .5, floor + 1, z + rotateZ(px, pz, turn) + .5, 0, 0);
            if (world.spawnEntityInWorld(scavenger)) count++;
        }
    }

    private void room(int x1, int base, int z1, int x2, int top, int z2, Shape shape) {
        int door = (x1 + x2) / 2;
        Block flooring = palette == Palette.STANDARDIZED || palette == Palette.INDUSTRIAL || offshore
                ? MilitaryMaterials.TREADWAY : palette == Palette.SANDSTONE ? Blocks.sandstone : Blocks.planks;
        fill(x1, base, z1, x2, base, z2, flooring);
        for (int y = base + 1; y < top; y++) {
            perimeter(x1, y, z1, x2, z2, y == base + 1 ? frame : wall);
        }
        int bay = palette == Palette.STANDARDIZED ? 4 : palette == Palette.INDUSTRIAL ? 6 : 5;
        for (int px = x1; px <= x2; px += bay) {
            fill(px, base + 1, z1, px, top - 1, z1, frame);
            fill(px, base + 1, z2, px, top - 1, z2, frame);
        }
        for (int px : new int[]{x1, x2}) for (int pz : new int[]{z1, z2}) {
            fill(px, base + 1, pz, px, top - 1, pz, frame);
        }
        Block glazing = palette == Palette.IMPROVISED && site.classification == Occupation.Classification.INFLUENCE
                ? Blocks.iron_bars : Blocks.glass_pane;
        int windowTop = Math.min(top - 2, base + (palette == Palette.SANDSTONE ? 2 : 3));
        for (int px = x1 + 2; px < x2 - 1; px += bay) {
            fill(px, base + 2, z2, px + 1, windowTop, z2, glazing);
            if (px + 1 < door - 1 || px > door + 2) fill(px, base + 2, z1, px + 1, windowTop, z1, glazing);
        }
        for (int pz = z1 + 2; pz < z2 - 1; pz += bay) for (int px : new int[]{x1, x2}) {
            fill(px, base + 2, pz, px, windowTop, pz + 1, glazing);
        }
        if (palette == Palette.STANDARDIZED || palette == Palette.INDUSTRIAL) {
            perimeter(x1, top - 1, z1, x2, z2, MilitaryMaterials.STEEL_PLATE);
        }
        aperture(door, base, z1, Math.min(1, x2 - 1 - door), 0);
        if (top - base >= 5) {
            fill(door - 1, top - 1, z1, Math.min(door + 2, x2 - 1), top - 1, z1, frame);
            put(door, top - 2, z1, Blocks.stained_hardened_clay, accent());
        }
        roof(x1, top, z1, x2, z2, shape);
        for (int pz = z1 + 2; pz <= z2 - 1; pz += 7) {
            int span = x2 - x1;
            if (span >= 10 && top - base >= 5) {
                fill(x1 + 1, top - 1, pz, x2 - 1, top - 1, pz, frame);
                put(x1 + span / 3, top - 1, pz, Blocks.glowstone);
                put(x2 - span / 3, top - 1, pz, Blocks.glowstone);
            } else {
                put(x1 + 1, top - 1, pz, Blocks.glowstone);
                if (span >= 7) put(x2 - 1, top - 1, pz, Blocks.glowstone);
            }
        }
    }

    private void roof(int x1, int y, int z1, int x2, int z2, Shape shape) {
        boolean acrossZ = shape != Shape.BARREL && x2 - x1 > z2 - z1;
        new SiteRoof(this::put, shape == Shape.BARREL ? MilitaryMaterials.CORRUGATED : roof, frame, turn)
                .build(x1, y, z1, x2, z2, -r + 1, r - 1, shape, acrossZ);
    }

    private Block roofBlock() {
        if (roof == Blocks.wooden_slab) return Blocks.planks;
        if (roof == Blocks.stone_slab) return Blocks.sandstone;
        return roof;
    }

    private int accent() {
        switch (palette) {
            case STANDARDIZED: return 11;
            case INDUSTRIAL: return 14;
            case IMPROVISED: return 13;
            case CIVILIAN: return 9;
            case SANDSTONE: return 1;
            default: return 7;
        }
    }

    private void aperture(int px, int base, int pz, int width, int depth) {
        fill(px, base + 1, pz, px + width, base + 2, pz + depth, Blocks.air);
    }

    private void bay(int left, int right, int front) {
        fill(left, 1, front, right, 3, front, Blocks.air);
        fill(left, 4, front, right, 4, front, frame);
        fill(left, 0, front - 2, right, 0, front, MilitaryMaterials.TREADWAY);
    }

    private void stairs(int left, int right, int front, int base, int rise, Block support) {
        for (int step = 0; step < rise; step++) {
            int z = front + step;
            fill(left, base + 1, z, right, base + step, z, support);
            for (int x = left; x <= right; x++) {
                put(x, base + step + 1, z, Blocks.stone_brick_stairs, SiteRoof.stairMetadata(2, turn));
            }
            // Navigating mobs need the next standing height to clear the ceiling one tread early.
            fill(left, base + step + 2, z, right, base + step + 4, z, Blocks.air);
        }
        fill(left, base + rise, front + rise, right, base + rise, front + rise + 1, support);
        for (int z = front + Math.max(0, rise - 4); z < front + rise; z++) {
            furnishing(left - 1, base + rise + 1, z, Blocks.iron_bars);
            furnishing(right + 1, base + rise + 1, z, Blocks.iron_bars);
        }
    }

    private void partition(int x, int base, int front, int back, int height, int door) {
        fill(x, base + 1, front, x, base + height, back, wall);
        aperture(x, base, door, 0, 1);
    }

    private void screen(int x, int base, int front, int back, int door) {
        for (int z = front; z <= back; z++) {
            if (z == door || z == door + 1) continue;
            furnishing(x, base + 1, z, frame);
            furnishing(x, base + 2, z, Blocks.glass_pane);
        }
    }

    private void maintenance(int left, int right, int front, int back) {
        if (right - left < 2 || back <= front + 3) return;
        for (int z = front + 2; z <= back; z++) {
            int color = (z - front) % 3 == 0 ? 7 : 4;
            put(left, 0, z, Blocks.stained_hardened_clay, color);
            put(right, 0, z, Blocks.stained_hardened_clay, color);
        }
        furnishing(left, 1, back, Blocks.anvil);
        furnishing(right, 1, back, Blocks.crafting_table);
    }

    private void path(int x1, int z1, int x2, int z2) {
        fill(x1, 0, z1, x2, 0, z2, walkway == Blocks.dirt ? Blocks.gravel : walkway);
    }

    private void perimeter(int x1, int y, int z1, int x2, int z2, Block block) {
        fill(x1, y, z1, x2, y, z1, block);
        fill(x1, y, z2, x2, y, z2, block);
        fill(x1, y, z1 + 1, x1, y, z2 - 1, block);
        fill(x2, y, z1 + 1, x2, y, z2 - 1, block);
    }

    private void canopy(int x1, int x2, int z1, int z2, int y, Block cover) {
        for (int px : new int[]{x1, x2}) for (int pz : new int[]{z1, z2}) {
            for (int py = 1; py < y; py++) furnishing(px, py, pz, frame);
        }
        fill(x1, y, z1, x2, y, z2, cover == roof ? roofBlock() : cover);
    }

    private void stall(int px, int pz, int base, int color) {
        for (int col : new int[]{px - 2, px + 2}) for (int row : new int[]{pz - 2, pz + 1}) {
            fill(col, base + 1, row, col, base + 3, row, Blocks.fence);
        }
        for (int col = px - 2; col <= px + 2; col++) for (int row = pz - 2; row <= pz + 1; row++) {
            put(col, base + 4, row, Blocks.wool, (col - px) % 2 == 0 ? color : 0);
        }
        serving(px - 1, px + 1, pz - 1);
        furnishing(px + 1, base + 1, pz, Blocks.hay_block);
    }

    private void furnishing(int px, int py, int pz, Block block) {
        furnishing(px, py, pz, block, 0);
    }

    private void furnishing(int px, int py, int pz, Block block, int meta) {
        if ((px != 0 || pz != 0 || py > 2) && (py > 2 || !traderAt(px, pz)) && get(px, py, pz) == Blocks.air) {
            put(px, py, pz, block, meta);
        }
    }

    private void bunks(int px, int z1, int z2) {
        for (int pz = z1; pz <= z2; pz += 3) {
            bed(px, pz, 0);
            if (pz + 1 <= z2) furnishing(px, 1, pz + 1, Blocks.bookshelf);
        }
    }

    private void office(int left, int right, int base, int back) {
        for (int x = left; x <= right; x += 3) {
            furnishing(x, base + 1, back, Blocks.bookshelf);
            if (x + 1 <= right) furnishing(x + 1, base + 1, back, Blocks.wooden_slab);
        }
        if (right - left >= 3) {
            furnishing(right, base + 1, back - 2, Blocks.stone_slab);
            furnishing(right - 1, base + 1, back - 2, Blocks.wooden_slab);
        }
    }

    private void kitchen(int left, int right, int back) {
        for (int x = left; x <= right; x++) {
            int station = (x - left) % 4;
            furnishing(x, 1, back, station == 0 ? Blocks.furnace : station == 2 ? Blocks.cauldron
                    : station == 3 ? Blocks.crafting_table : Blocks.stone_slab);
        }
    }

    private void dining(int left, int right, int front, int back) {
        kitchen(left, right, back);
        if (right - left < 4 || back - front < 4) return;
        for (int z = front + 1; z < back - 2; z += 4) {
            for (int x = left + 1; x <= right - 2; x++) furnishing(x, 1, z, MilitaryMaterials.REVETMENT);
            for (int x = left + 1; x <= right - 2; x += 2) {
                furnishing(x, 1, z + 1, Blocks.wooden_slab);
                furnishing(x, 1, z - 1, Blocks.wooden_slab);
            }
        }
    }

    private void clinic(int left, int right, int front, int back) {
        if (back < front || right < left) return;
        for (int z = front; z <= back; z += 3) {
            if (right - left >= 2) {
                bed(left, z, 0);
                furnishing(left, 1, z + 1, Blocks.stone_slab);
            }
            furnishing(right, 1, z, Blocks.quartz_block);
            if (z < back) furnishing(right, 1, z + 1, Blocks.bookshelf);
        }
        utility(right, 0, back);
        furnishing(right, 3, back, Blocks.glowstone);
    }

    private void utility(int x, int base, int z) {
        furnishing(x, base + 1, z, Blocks.cauldron);
        furnishing(x, base + 1, z + 1, Blocks.quartz_stairs, SiteRoof.stairMetadata(3, turn));
    }

    private void washroom(int left, int base, int front, int right, int back) {
        screen(left, base, front, back, front + 1);
        fill(left, base, front, right, base, back, Blocks.quartz_block);
        utility(right, base, front + 1);
    }

    private void rack(int x, int front, int back) {
        if (back - front < 2) return;
        for (int z = front; z <= back; z++) {
            int offset = z - front;
            if (offset % 4 == 0 || z == back) {
                for (int y = 1; y <= 3; y++) furnishing(x, y, z, MilitaryMaterials.STEEL_PLATE);
            } else {
                furnishing(x, 1, z, offset % 2 == 0 ? Blocks.log : Blocks.hardened_clay);
                furnishing(x, 2, z, Blocks.wooden_slab, 1 | 8);
                if (offset % 3 == 0) furnishing(x, 3, z, MilitaryMaterials.REVETMENT);
            }
        }
    }

    void bed(int px, int pz, int base) {
        if ((px == 0 || px + 1 == 0) && pz == 0 || traderAt(px, pz) || traderAt(px + 1, pz)
                || !get(px, base, pz).getMaterial().isSolid() || !get(px + 1, base, pz).getMaterial().isSolid()
                || get(px, base + 1, pz) != Blocks.air || get(px + 1, base + 1, pz) != Blocks.air) return;
        int direction = (3 + turn) & 3;
        put(px, base + 1, pz, Blocks.bed, direction);
        put(px + 1, base + 1, pz, Blocks.bed, direction | 8);
    }

    private void serving(int x1, int x2, int pz) {
        for (int px = x1; px <= x2; px++) {
            furnishing(px, 1, pz, (px - x1) % 4 == 0 ? Blocks.crafting_table : Blocks.wooden_slab);
        }
    }

    private void commandRoom(int left, int base, int front, int right, int back) {
        for (int px = left; px <= right; px += 3) furnishing(px, base + 1, back, Blocks.bookshelf);
        int mid = (left + right) / 2;
        int table = Math.min(back - 2, front + (back - front) / 2);
        if (table <= front + 1) return;
        for (int px = mid - 1; px <= mid + 1; px++) furnishing(px, base + 1, table, MilitaryMaterials.REVETMENT);
        furnishing(mid, base + 2, table, Blocks.carpet);
        for (int px : new int[]{mid - 1, mid + 1}) furnishing(px, base + 1, table + 1, Blocks.wooden_slab);
    }

    private void workbench(int x1, int x2, int pz) {
        for (int px = x1; px <= x2; px += 3) {
            furnishing(px, 1, pz, Blocks.crafting_table);
            if (px + 1 <= x2) furnishing(px + 1, 1, pz, Blocks.furnace);
        }
    }

    private void shelves(int x1, int x2, int z1, int z2) {
        for (int px = x1; px <= x2; px += 3) shelving(px, 1, z1, z2);
    }

    private void shelving(int px, int y, int z1, int z2) {
        for (int pz = z1; pz <= z2; pz++) {
            furnishing(px, y, pz, (pz - z1) % 3 == 1 ? Blocks.planks : Blocks.bookshelf);
            if ((pz - z1) % 2 == 0) furnishing(px, y + 1, pz, Blocks.wooden_slab);
        }
    }

    private void cargo(int x1, int z1, int x2, int z2, boolean ruined) {
        fill(x1, 1, z1, x2, 2, z2, MilitaryMaterials.CORRUGATED);
        fill(x1, 3, z1, x2, 3, z2, MilitaryMaterials.STEEL_PLATE);
        for (int py = 1; py <= 2; py++) {
            put(x1, py, z1, MilitaryMaterials.REVETMENT);
            put(x2, py, z1, MilitaryMaterials.REVETMENT);
        }
        if (ruined) fill(x1 + 1, 1, z1, x1 + 1, 2, z1 + 1, Blocks.air);
    }

    private void rubble(int x1, int x2, int z1, int z2, int spacing) {
        for (int px = x1; px <= x2; px++) for (int pz = z1; pz <= z2; pz++) {
            if (Math.floorMod(px * 7 + pz * 3, spacing) == 0) {
                furnishing(px, 1, pz, MilitaryMaterials.BATTLE_RUBBLE);
            }
        }
    }

    private void cache(int px, int py, int pz) { cache = new int[]{px, py, pz}; }

    private void lamp(int px, int y, int pz) {
        if (traderAt(px, pz)) return;
        boolean steel = palette == Palette.STANDARDIZED || palette == Palette.INDUSTRIAL;
        put(px, y, pz, steel ? frame : Blocks.fence);
        fill(px, y + 1, pz, px, y + 2, pz, steel ? Blocks.iron_bars : Blocks.fence);
        put(px, y + 3, pz, Blocks.glowstone);
    }

    private void fence(Block material) {
        for (int i = -r + 1; i < r; i++) {
            if (Math.abs(i) > 1) put(i, 1, -r + 1, material);
            put(i, 1, r - 1, material);
            put(-r + 1, 1, i, material);
            put(r - 1, 1, i, material);
        }
    }

    private void ladder(int px, int bottom, int top, int pz, int direction) {
        for (int dy = bottom; dy <= top; dy++) put(px, dy, pz, Blocks.ladder, facing(direction));
    }

    private int facing(int direction) { return new int[]{2, 5, 3, 4}[(direction + turn) & 3]; }

    private void loot(Random random) {
        if (site.loot.length == 0) return;
        if (cache != null && availableCache(cache[0], cache[1], cache[2])) {
            stock(cache[0], cache[1], cache[2], random);
            return;
        }
        for (int[] spot : new int[][]{{-2, 2}, {2, 2}, {-2, -2}, {2, -2}, {0, -2}, {-3, 0}, {3, 0}}) {
            if (availableCache(spot[0], 1, spot[1])) {
                stock(spot[0], 1, spot[1], random);
                return;
            }
        }
    }

    private boolean availableCache(int px, int py, int pz) {
        return !traderAt(px, pz) && get(px, py - 1, pz).getMaterial().isSolid()
                && get(px, py, pz) == Blocks.air && get(px, py + 1, pz) == Blocks.air;
    }

    void stock(int px, int py, int pz, Random random) {
        if (site.loot.length == 0) return;
        put(px, py, pz, Blocks.chest, facing(0));
        TileEntity tile = world.getTileEntity(x + rotateX(px, pz, turn), floor + py, z + rotateZ(px, pz, turn));
        if (!(tile instanceof TileEntityChest)) return;
        TileEntityChest chest = (TileEntityChest)tile;
        int slot = random.nextInt(9);
        for (String id : site.loot) {
            ItemStack stack = UnitPresets.Preset.item(id);
            if (stack == null || slot >= chest.getSizeInventory()) continue;
            stack.stackSize = Math.min(stack.getMaxStackSize(), 1 + random.nextInt(3));
            chest.setInventorySlotContents(slot, stack);
            slot += 4;
        }
    }

    private boolean traderAt(int px, int pz) {
        for (MilitarySites.Trader trader : site.traders) if (trader.x == px && trader.z == pz) return true;
        return false;
    }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (int px = x1; px <= x2; px++) for (int py = y1; py <= y2; py++) for (int pz = z1; pz <= z2; pz++) put(px, py, pz, block);
    }

    void put(int px, int py, int pz, Block block) { put(px, py, pz, block, 0); }

    void put(int px, int py, int pz, Block block, int meta) {
        world.setBlock(x + rotateX(px, pz, turn), floor + py, z + rotateZ(px, pz, turn), block, meta, 2);
    }

    Block get(int px, int py, int pz) {
        return world.getBlock(x + rotateX(px, pz, turn), floor + py, z + rotateZ(px, pz, turn));
    }

    private static int rotateX(int x, int z, int turn) { return turn == 1 ? -z : turn == 2 ? -x : turn == 3 ? z : x; }
    private static int rotateZ(int x, int z, int turn) { return turn == 1 ? x : turn == 2 ? -z : turn == 3 ? -x : z; }

    static boolean vegetation(Block block) {
        Material material = block.getMaterial();
        return block == Blocks.air || block == Blocks.log || block == Blocks.log2 || block == Blocks.snow_layer
                || block == Blocks.cactus || material == Material.leaves || material == Material.plants || material == Material.vine;
    }

    static int surfaceTop(World world, int x, int z) {
        int y = Math.min(world.getActualHeight() - 1, world.getHeightValue(x, z));
        while (y > 0 && world.isAirBlock(x, y, z)) y--;
        return y;
    }

    static boolean natural(Block block, BiomeGenBase biome) {
        return block == Blocks.grass || block == Blocks.dirt || block == Blocks.stone || block == Blocks.sand
                || block == Blocks.gravel || block == Blocks.snow || block == Blocks.sandstone || block == biome.topBlock;
    }

    static boolean ore(Block block) {
        return block instanceof net.minecraft.block.BlockOre || block instanceof net.minecraft.block.BlockRedstoneOre;
    }
}
