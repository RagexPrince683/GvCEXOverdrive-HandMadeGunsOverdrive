package hmggvcmob.military;

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
        MARKET, RELAY, LODGE, RIG, WAREHOUSE_RUIN, DERELICT_RIG, DISTRICT,
        FIGHTING_PITS, COVERED_NEST, TRENCH_LINE, BUNKER, RIDGE_POST, BARRIER, TUNNEL_HIDEOUT, TUNNEL_COMPOUND }
    private enum Palette { ADAPTIVE, STANDARDIZED, IMPROVISED, INDUSTRIAL, SALVAGED, CIVILIAN, SANDSTONE, ALPINE }

    final World world;
    final MilitarySites.Definition site;
    private final Layout layout;
    final int x, z, turn, r;
    private final int[][] heights;
    private final Block wall, frame, roof, walkway;
    private final boolean offshore;
    final int floor;
    private MilitaryFieldworks works;

    static boolean supports(String layout, String palette, int width) {
        try {
            Palette.valueOf(palette.toUpperCase(java.util.Locale.ROOT));
            int minimum;
            switch (Layout.valueOf(layout)) {
                case DISTRICT: minimum = 37; break;
                case TUNNEL_COMPOUND: minimum = 25; break;
                case TUNNEL_HIDEOUT: minimum = 19; break;
                case TRENCH_LINE: case BUNKER: minimum = 13; break;
                case HEADQUARTERS: case CONTRACT_YARD: case MARKET: minimum = 17; break;
                case BARRACKS: case LODGE: case RIG: case DERELICT_RIG: minimum = 15; break;
                default: minimum = 11;
            }
            return width >= minimum && width <= 47 && width % 2 == 1;
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return false;
        }
    }

    static MilitarySiteLayout survey(World world, MilitarySites.Definition site, int x, int z, int turn, String biome) {
        int r = site.width / 2;
        boolean offshore = "RIG".equals(site.layout) || "DERELICT_RIG".equals(site.layout);
        int[][] heights = new int[site.width][site.width];
        int[] samples = new int[site.width * site.width];
        int count = 0, water = -1;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int sx = x + dx, sz = z + dz;
            int top = world.getHeightValue(sx, sz) - 1;
            int y = top;
            while (y > 1 && vegetation(world.getBlock(sx, y, sz))) y--;
            Block surface = world.getBlock(sx, y, sz);
            if (offshore) {
                if (surface.getMaterial() != Material.water || !"ocean".equals(biome)) return null;
                if (water < 0) water = y;
                if (water != y) return null;
                while (y > water - 29 && world.getBlock(sx, y, sz).getMaterial() == Material.water) y--;
                if (water - y < 5 || water - y > 28 || world.getBlock(sx, y, sz).getMaterial() == Material.water) return null;
            } else {
                BiomeGenBase local = world.getWorldChunkManager().getBiomeGenAt(sx, sz);
                if (!natural(surface, local) || top - y > (site.fieldwork ? 36 : 18)
                        || y > world.getActualHeight() - 18) return null;
                for (int dy = 1; dy <= 3; dy++) {
                    Block below = world.getBlock(sx, y - dy, sz);
                    if (below.hasTileEntity(world.getBlockMetadata(sx, y - dy, sz))) return null;
                }
            }
            heights[dx + r][dz + r] = y;
            samples[count++] = y;
        }
        Arrays.sort(samples);
        int floor = offshore ? water + 4 : samples[samples.length / 2];
        if (!offshore && (floor - samples[0] > site.slope || samples[samples.length - 1] - floor > site.slope
                || Math.abs(heights[r + rotateX(0, -r, turn)][r + rotateZ(0, -r, turn)] - floor) > 2)) return null;
        MilitarySiteLayout plan = new MilitarySiteLayout(world, site, x, z, turn, r, heights, floor, offshore, biome);
        if (plan.layout.ordinal() >= Layout.FIGHTING_PITS.ordinal()) {
            plan.works = new MilitaryFieldworks(plan, biome);
            if (!plan.works.survey()) return null;
        }
        return plan;
    }

    private MilitarySiteLayout(World world, MilitarySites.Definition site, int x, int z, int turn, int r,
                               int[][] heights, int floor, boolean offshore, String biome) {
        this.world = world;
        this.site = site;
        this.x = x;
        this.z = z;
        this.turn = turn;
        this.r = r;
        this.heights = heights;
        this.floor = floor;
        this.offshore = offshore;
        layout = Layout.valueOf(site.layout);
        Palette palette = Palette.valueOf(site.palette.toUpperCase(java.util.Locale.ROOT));
        if (palette == Palette.ADAPTIVE) {
            palette = "desert".equals(biome) ? Palette.SANDSTONE : "snow".equals(biome) ? Palette.ALPINE : Palette.SALVAGED;
        }
        switch (palette) {
            case STANDARDIZED: wall = Blocks.stonebrick; frame = Blocks.double_stone_slab; roof = Blocks.stone_slab; walkway = Blocks.gravel; break;
            case IMPROVISED: wall = Blocks.planks; frame = Blocks.log; roof = Blocks.wooden_slab; walkway = Blocks.dirt; break;
            case INDUSTRIAL: wall = Blocks.hardened_clay; frame = Blocks.stonebrick; roof = Blocks.stone_slab; walkway = Blocks.stonebrick; break;
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
            if (!offshore) {
                int lip = Math.max(-1, Math.min(1, surface(0, -r)));
                post.entrance = point(0, lip + 1, -r);
                post.approach = turn;
            }
            switch (layout) {
            case CHECKPOINT: checkpoint(); break;
            case TOWER: tower(0, 1, 2, 6); break;
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
            }
        }
        post.footprint = r;
        put(0, anchorY() - floor, 0, post.campObj.campsBlock);
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

    private void ground() {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int ground = heights[r + rotateX(dx, dz, turn)][r + rotateZ(dx, dz, turn)] - floor;
            if (Math.abs(dx) == r || Math.abs(dz) == r) {
                int lip = Math.max(-1, Math.min(1, ground));
                if (Math.abs(dx) <= 1 && dz == -r) put(dx, lip, dz, walkway);
                else if (ground >= -1 && ground <= 1) continue;
                else {
                    for (int dy = ground; dy < lip; dy++) put(dx, dy, dz, frame);
                    put(dx, lip, dz, frame);
                }
                for (int dy = lip + 1; dy <= ground + 1; dy++) if (dy <= ground || vegetation(get(dx, dy, dz))) put(dx, dy, dz, Blocks.air);
                continue;
            }
            for (int dy = ground + 1; dy < 0; dy++) put(dx, dy, dz, frame);
            put(dx, 0, dz, walkway);
            for (int dy = 1; dy <= Math.max(9, ground + 1); dy++) put(dx, dy, dz, Blocks.air);
        }
    }

    private void deck() {
        fill(-r + 1, 0, -r + 1, r - 1, 0, r - 1, walkway);
        for (int dx : new int[]{-r + 3, r - 3}) for (int dz : new int[]{-r + 3, r - 3}) {
            int ground = heights[r + rotateX(dx, dz, turn)][r + rotateZ(dx, dz, turn)] - floor;
            fill(dx, ground + 1, dz, dx + 1, 0, dz + 1, frame);
        }
        fill(-2, -3, -r, 2, -3, -r + 1, Blocks.planks);
        fill(-2, -3, -r + 2, 2, 0, -r + 2, frame);
        ladder(0, -2, 0, -r + 1, 0);
        fence(Blocks.iron_bars);
    }

    private void checkpoint() {
        fence(Blocks.iron_bars);
        room(-r + 1, 0, -r + 1, -2, 4, -1, false);
        put(-r + 2, 1, -2, Blocks.crafting_table);
        for (int dx : new int[]{-2, 2}) fill(dx, 1, -r + 1, dx, 3, -r + 1, frame);
        fill(-2, 4, -r + 1, 2, 4, -r + 1, roof);
    }

    private void tower(int dx, int dz, int half, int height) {
        for (int px : new int[]{dx - half, dx + half}) for (int pz : new int[]{dz - half, dz + half}) {
            fill(px, 1, pz, px, height + 2, pz, frame);
        }
        fill(dx - half, height, dz - half, dx + half, height, dz + half, wall);
        fill(dx - half, height + 1, dz + half, dx + half, height + 1, dz + half, Blocks.fence);
        fill(dx - half, height + 3, dz - half, dx + half, height + 3, dz + half, roof);
        fill(dx + 1, 1, dz + 1, dx + 1, height, dz + 1, frame);
        ladder(dx + 1, 1, height, dz, 0);
    }

    private void barracks() {
        fence(Blocks.iron_bars);
        room(-r + 2, 0, -r + 2, -2, 5, r - 2, false);
        room(2, 0, 1, r - 2, 4, r - 2, true);
        for (int dz = -r + 4; dz < r - 2; dz += 3) bench(-r + 3, dz);
        put(3, 1, r - 3, Blocks.crafting_table);
    }

    private void headquarters() {
        fence(Blocks.iron_bars);
        room(-r + 2, 0, 3, r - 2, 6, r - 2, false);
        room(-r + 2, 0, -r + 2, -3, 5, 0, false);
        room(3, 0, -r + 2, r - 2, 5, 0, false);
        fill(-3, 1, r - 4, 3, 1, r - 4, Blocks.wooden_slab);
        put(-r + 4, 1, -3, Blocks.bookshelf);
        put(r - 4, 1, -3, Blocks.crafting_table);
        for (int dx : new int[]{-r + 1, r - 1}) lamp(dx, 1, 1);
    }

    private void trench() {
        for (int dx = -r + 1; dx <= r - 1; dx++) {
            put(dx, -2, 2, Blocks.planks);
            put(dx, -1, 2, Blocks.air);
            put(dx, 0, 2, Blocks.air);
            put(dx, 0, 3, wall);
            if (Math.abs(dx) % 4 <= 1) put(dx, 2, 2, roof);
        }
        put(-2, -1, 2, Blocks.planks);
        put(2, -1, 2, Blocks.planks);
        room(2, 0, -r + 1, r - 1, 3, -1, false);
    }

    private void farm() {
        room(-r + 2, 0, 1, -1, 5, r - 2, true);
        room(2, 0, 2, r - 2, 4, r - 2, false);
        for (int dx = 2; dx <= r - 2; dx++) {
            for (int dz = -r + 3; dz <= -2; dz++) {
                put(dx, 0, dz, dx == 3 ? Blocks.water : Blocks.farmland, dx == 3 ? 0 : 7);
                if (dx != 3) put(dx, 1, dz, Blocks.wheat, 4);
            }
        }
        put(-r + 3, 1, r - 3, Blocks.crafting_table);
        fill(2, 1, -r + 2, r - 2, 1, -r + 2, Blocks.fence);
    }

    private void depot() {
        room(-r + 2, 0, -1, r - 2, 6, r - 2, false);
        for (int dx : new int[]{-r + 3, r - 3}) {
            fill(dx, 1, 2, dx, 2, r - 3, Blocks.planks);
        }
        put(-3, 1, -r + 2, Blocks.crafting_table);
        fill(2, 0, -r + 2, r - 2, 0, -3, Blocks.double_stone_slab);
    }

    private void contractYard() {
        fence(Blocks.iron_bars);
        room(-r + 2, 0, 2, r - 2, 6, r - 2, false);
        room(-r + 2, 0, -r + 2, -3, 5, -1, false);
        fill(3, 0, -r + 2, r - 2, 0, -1, Blocks.double_stone_slab);
        for (int dz = -r + 3; dz <= -2; dz += 3) put(r - 3, 1, dz, Blocks.hay_block);
        fill(-2, 1, r - 3, 2, 1, r - 3, Blocks.wooden_slab);
        lamp(3, 1, 1);
    }

    private void market() {
        room(-r + 2, 0, 3, -2, 5, r - 2, true);
        room(2, 0, 3, r - 2, 5, r - 2, true);
        for (int dx : new int[]{-5, 5}) {
            fill(dx - 2, 1, -4, dx + 2, 1, -4, Blocks.wooden_slab);
            for (int post : new int[]{dx - 2, dx + 2}) fill(post, 1, -6, post, 3, -6, Blocks.fence);
            for (int line = -7; line <= -3; line++) {
                for (int px = dx - 2; px <= dx + 2; px++) put(px, 4, line, Blocks.wool, (px - dx) % 2 == 0 ? 0 : 9);
            }
        }
        lamp(0, 1, 2);
    }

    private void relay() {
        room(-r + 1, 0, 1, -1, 4, r - 1, false);
        fill(r - 3, 1, 2, r - 3, 8, 2, Blocks.iron_bars);
        fill(r - 4, 7, 2, r - 1, 7, 2, Blocks.iron_bars);
        put(r - 3, 9, 2, Blocks.glowstone);
        fill(2, 0, -3, r - 1, 0, -2, Blocks.stonebrick);
        put(3, 1, -3, Blocks.cauldron);
    }

    private void lodge() {
        room(-r + 2, 0, 2, r - 2, 6, r - 2, true);
        room(-r + 2, 0, -r + 2, -3, 4, -1, false);
        fill(r - 3, 1, r - 3, r - 3, 7, r - 3, Blocks.cobblestone);
        put(r - 4, 1, r - 3, Blocks.furnace, facing(0));
        for (int dx = -r + 3; dx < 1; dx += 2) bench(dx, r - 3);
    }

    private void rig(boolean ruined) {
        int inset = r - 2;
        room(-inset, 0, 2, 0, 5, inset, false);
        fill(4, 1, 2, 4, 8, 2, Blocks.iron_bars);
        fill(2, 7, 2, inset, 7, 2, Blocks.iron_bars);
        fill(inset, 4, 2, inset, 7, 2, Blocks.iron_bars);
        fill(2, 1, inset, inset, 2, inset, Blocks.hardened_clay);
        if (ruined) {
            put(-inset, 1, 3, Blocks.air);
            put(-inset, 2, 3, Blocks.air);
            put(3, 2, inset, Blocks.air);
            put(-inset + 2, 5, 4, Blocks.air);
        } else {
            put(4, 9, 2, Blocks.glowstone);
            put(-2, 1, inset - 1, Blocks.crafting_table);
        }
    }

    private void ruin(Random random) {
        room(-r + 2, 0, -r + 3, r - 2, 5, r - 2, false);
        for (int i = 0; i < r * 2; i++) {
            int px = -r + 2 + random.nextInt(r * 2 - 3), pz = -r + 3 + random.nextInt(r * 2 - 4);
            put(px, 5, pz, Blocks.air);
            if (Math.abs(px) > 2 || Math.abs(pz) > 1) put(px, 1, pz, Blocks.cobblestone);
        }
        fill(-r + 2, 1, -1, -r + 2, 3, 1, Blocks.air);
        fill(r - 2, 1, r - 3, r - 2, 2, r - 2, Blocks.air);
    }

    private void district() {
        int outer = r - 3, mid = r / 2;
        fence(Blocks.iron_bars);
        fill(-1, 0, -r + 1, 1, 0, r - 1, Blocks.stonebrick);
        fill(-r + 1, 0, -1, r - 1, 0, 1, Blocks.stonebrick);
        // Offices and warehouse flank the north gate. The two residential buildings face the cross street.
        room(-outer, 0, -outer, -4, 7, -mid - 1, false);
        room(4, 0, -outer, outer, 6, -mid - 1, false);
        room(-outer, 0, 3, -4, 5, mid, true);
        room(4, 0, 3, outer, 5, mid, true);
        room(-outer, 0, mid + 3, -4, 5, outer, false);
        fill(5, 0, mid + 3, outer, 0, outer, Blocks.double_stone_slab);
        for (int px = 6; px <= outer - 1; px += 3) put(px, 1, outer - 1, Blocks.hay_block);
        for (int z : new int[]{-mid, 2, mid + 2}) for (int x : new int[]{-3, 3}) lamp(x, 1, z);
        for (int x : new int[]{-outer + 1, 5}) {
            for (int dz = 5; dz < mid; dz += 2) bench(x, dz);
            put(x, 1, -mid - 2, Blocks.crafting_table);
        }
        fill(-7, 1, -2, -5, 1, -2, Blocks.wooden_slab);
        fill(5, 1, -2, 7, 1, -2, Blocks.wooden_slab);
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

    private void room(int x1, int base, int z1, int x2, int top, int z2, boolean pitched) {
        int door = (x1 + x2) / 2;
        fill(x1, base, z1, x2, base, z2, Blocks.planks);
        fill(x1, base + 1, z1, x1, top - 1, z2, wall);
        fill(x2, base + 1, z1, x2, top - 1, z2, wall);
        fill(x1, base + 1, z1, x2, top - 1, z1, wall);
        fill(x1, base + 1, z2, x2, top - 1, z2, wall);
        for (int px : new int[]{x1, x2}) for (int pz : new int[]{z1, z2}) fill(px, base + 1, pz, px, top, pz, frame);
        for (int px = x1 + 2; px <= x2 - 2; px += 3) {
            put(px, base + 2, z2, Blocks.glass_pane);
            if (Math.abs(px - door) > 1) put(px, base + 2, z1, Blocks.glass_pane);
        }
        for (int pz = z1 + 2; pz <= z2 - 2; pz += 3) {
            put(x1, base + 2, pz, Blocks.glass_pane);
            put(x2, base + 2, pz, Blocks.glass_pane);
        }
        fill(door, base + 1, z1, Math.min(door + 1, x2 - 1), base + 2, z1, Blocks.air);
        fill(x1, top, z1, x2, top, z2, roof);
        if (pitched) {
            int tiers = Math.min(3, (x2 - x1) / 2);
            for (int i = 1; i <= tiers; i++) fill(x1 + i, top + i, z1, x2 - i, top + i, z2, roof);
        }
        put(x1 + 1, top - 1, z1 + 1, Blocks.glowstone);
    }

    private void lamp(int px, int y, int pz) {
        fill(px, y, pz, px, y + 2, pz, Blocks.fence);
        put(px, y + 3, pz, Blocks.glowstone);
    }

    private void bench(int px, int pz) {
        put(px, 1, pz, Blocks.wool, 7);
        put(px + 1, 1, pz, Blocks.wooden_slab);
    }

    private void fence(Block material) {
        for (int i = -r + 1; i < r; i++) {
            if (Math.abs(i) > 1) put(i, 1, -r + 1, material);
            put(i, 1, r - 1, material);
            put(-r + 1, 1, i, material);
            put(r - 1, 1, i, material);
        }
    }

    private void ladder(int px, int bottom, int top, int pz, int facing) {
        for (int dy = bottom; dy <= top; dy++) put(px, dy, pz, Blocks.ladder, facing(facing));
    }

    private int facing(int direction) { return new int[]{2, 5, 3, 4}[(direction + turn) & 3]; }

    private void loot(Random random) {
        if (site.loot.length == 0) return;
        for (int[] spot : new int[][]{{-2, 2}, {2, 2}, {-2, -2}, {2, -2}}) {
            int px = spot[0], pz = spot[1];
            if (get(px, 1, pz) != Blocks.air || !get(px, 0, pz).getMaterial().isSolid() || traderAt(px, pz)) continue;
            stock(px, 1, pz, random);
            return;
        }
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

    static boolean natural(Block block, BiomeGenBase biome) {
        return block == Blocks.grass || block == Blocks.dirt || block == Blocks.stone || block == Blocks.sand
                || block == Blocks.gravel || block == Blocks.snow || block == Blocks.sandstone || block == biome.topBlock;
    }
}
