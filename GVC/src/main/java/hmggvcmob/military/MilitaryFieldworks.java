package hmggvcmob.military;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Random;

/** Block plans for small field positions and the two walk-in underground sites. */
final class MilitaryFieldworks {
    private final MilitarySiteLayout plan;
    private final boolean improvised, abandoned, underground, compound;
    private final Block facing, retaining;
    private int deck;

    MilitaryFieldworks(MilitarySiteLayout plan, String biome) {
        this.plan = plan;
        String palette = plan.site.palette;
        improvised = "improvised".equals(palette) || "salvaged".equals(palette);
        abandoned = plan.site.classification == Occupation.Classification.ABANDONED;
        compound = "TUNNEL_COMPOUND".equals(plan.site.layout);
        underground = compound || "TUNNEL_HIDEOUT".equals(plan.site.layout);
        facing = improvised ? Blocks.log : "industrial".equals(palette) ? MilitaryMaterials.CONCRETE : MilitaryMaterials.SANDBAGS;
        retaining = improvised ? MilitaryMaterials.REVETMENT : "sandstone".equals(palette) || "desert".equals(biome)
                && "adaptive".equals(palette) ? Blocks.sandstone : MilitaryMaterials.CONCRETE;
    }

    boolean survey() {
        if (!underground) {
            deck = "BUNKER".equals(plan.site.layout) || "RIDGE_POST".equals(plan.site.layout) ? 0 : -1;
            return plan.floor + deck > 0;
        }
        int depth = compound ? 10 : 8;
        int start = plan.surface(0, -plan.r);
        deck = start - depth;
        if (plan.floor + deck < 8) return false;
        int wing = compound ? 9 : 5;
        int back = compound ? 6 : 4;
        for (int x = -wing - 1; x <= wing + 1; x++) for (int z = -3; z <= back + 1; z++) {
            if (plan.surface(x, z) <= deck + 4) return false;
            for (int y = deck - 1; y <= deck + 4; y++) {
                Block block = plan.get(x, y, z);
                if (block != Blocks.air && !earth(block)) return false;
            }
        }
        return passableRamp(0, -plan.r, -2) && (!compound || passableRamp(7, plan.r, 2));
    }

    private boolean passableRamp(int x, int startZ, int end) {
        int surface = plan.surface(x, startZ);
        if (surface < deck + 5 || surface > deck + 11) return false;
        int direction = Integer.compare(end, startZ);
        int distance = Math.abs(end - startZ);
        if (surface - deck > distance + 1) return false;
        for (int i = 0; i <= distance; i++) for (int side = -1; side <= 1; side++) {
            int z = startZ + i * direction;
            int y = Math.max(deck, surface - i - 1);
            if (Math.abs(plan.surface(x + side, z) - y) > 16) return false;
            for (int level = y - 1; level <= y + 3; level++) {
                Block block = plan.get(x + side, level, z);
                if (!earth(block) && !MilitarySiteLayout.vegetation(block)) return false;
            }
        }
        return true;
    }

    private static boolean earth(Block block) {
        return block == Blocks.stone || block == Blocks.dirt || block == Blocks.grass || block == Blocks.sand
                || block == Blocks.gravel || block == Blocks.sandstone || MilitarySiteLayout.ore(block)
                || block == Blocks.hardened_clay || block == Blocks.stained_hardened_clay || block == Blocks.clay;
    }

    int anchor() { return deck + 1; }

    void build(Occupation post, Random random) {
        if (underground) {
            tunnel(post, random);
            return;
        }
        int r = plan.r;
        int rear = r;
        int reach = Math.min(r - 1, 10);
        switch (plan.site.layout) {
            case "FIGHTING_PITS":
                lane(-1, -1, 1, rear - 1, deck);
                lane(-4, -2, -1, 0, deck);
                lane(1, 0, 4, 2, deck);
                line(-4, -1, -3, false);
                line(1, 4, -1, false);
                bank(-4, -2, facing, 2);
                bank(4, 0, facing, 2);
                brace(-4, -3, -2);
                brace(4, -1, 2);
                plan.put(-3, deck + 1, -2, Blocks.crafting_table);
                break;
            case "COVERED_NEST":
                lane(-3, -2, 2, 1, deck);
                lane(0, 1, 2, 3, deck);
                line(-3, 2, -3, false);
                shelter(-3, 2, -2, 0, improvised);
                bank(-3, 1, facing, 2);
                plan.put(-2, deck + 1, -1, Blocks.crafting_table);
                break;
            case "TRENCH_LINE":
                lane(-reach, -1, 0, 0, deck);
                lane(-1, -2, reach, -1, deck);
                lane(-1, 0, 1, rear - 1, deck);
                lane(-reach, -3, -reach + 2, -1, deck);
                lane(reach - 2, -4, reach, -2, deck);
                line(-reach + 3, -2, -2, abandoned);
                line(1, reach - 3, -3, abandoned);
                line(-reach, -reach + 2, -4, false);
                line(reach - 2, reach, -5, false);
                shelter(-4, -2, -1, 0, improvised);
                if (!abandoned) {
                    shelter(2, 4, -2, -1, improvised);
                    plan.put(-reach + 1, deck + 1, -3, Blocks.crafting_table);
                } else {
                    plan.put(-reach + 1, deck + 1, -3, MilitaryMaterials.BATTLE_RUBBLE);
                    plan.put(reach - 1, deck + 1, -4, MilitaryMaterials.BATTLE_RUBBLE);
                }
                break;
            case "BUNKER":
                lane(-3, -2, 3, 2, deck);
                lane(-1, 2, 1, rear - 1, deck);
                for (int x = -3; x <= 3; x++) {
                    bank(x, -3, retaining, 2);
                    if (Math.abs(x) > 1) bank(x, 2, retaining, 2);
                }
                for (int z = -2; z <= 2; z++) {
                    bank(-3, z, retaining, 2);
                    bank(3, z, retaining, 2);
                }
                plan.fill(-4, deck + 3, -4, 4, deck + 3, 3, retaining);
                plan.fill(-3, deck + 4, -4, 3, deck + 4, -4, facing);
                for (int x : new int[]{-2, 0, 2}) plan.put(x, deck + 2, -3, Blocks.iron_bars);
                plan.put(-2, deck + 1, -1, abandoned ? MilitaryMaterials.BATTLE_RUBBLE : Blocks.crafting_table);
                plan.put(2, deck + 2, 1, abandoned ? Blocks.air : Blocks.glowstone);
                if (abandoned) {
                    plan.fill(3, deck + 2, -3, 4, deck + 3, -1, Blocks.air);
                    plan.put(2, deck + 1, -1, MilitaryMaterials.BATTLE_RUBBLE);
                }
                break;
            case "RIDGE_POST":
                lane(-2, -2, 2, 2, deck);
                lane(-1, 2, 1, rear - 1, deck);
                line(-2, 2, -3, false);
                shelter(-2, 2, -2, 1, improvised);
                bank(2, 2, facing, 2);
                plan.put(-1, deck + 1, -2, Blocks.furnace);
                plan.put(2, deck + 2, 0, Blocks.glowstone);
                break;
            case "BARRIER":
                lane(-2, -r, 2, r, deck);
                lane(-4, -2, -2, 1, deck);
                line(-4, -2, -3, false);
                bank(-2, 1, MilitaryMaterials.CONCRETE, 2);
                bank(2, -2, MilitaryMaterials.CONCRETE, 2);
                shelter(-4, -2, -2, 0, improvised);
                plan.put(-3, deck + 1, -2, Blocks.crafting_table);
                plan.put(2, deck + 3, -2, Blocks.glowstone);
                for (int z : new int[]{-r + 1, r - 1}) {
                    plan.put(-2, deck + 1, z, MilitaryMaterials.SANDBAGS);
                    plan.put(2, deck + 1, z, MilitaryMaterials.SANDBAGS);
                }
                break;
            default:
                throw new IllegalStateException(plan.site.layout);
        }
        if (!"BARRIER".equals(plan.site.layout)) {
            int approachY = plan.surface(0, rear);
            for (int z = rear; z >= 1; z--) {
                int y = Math.max(deck, approachY - (rear - z));
                if (z == rear && approachY < deck) y = deck - 1;
                lane(-1, z, 1, z, y);
            }
        }
        if (improvised) {
            for (int[] p : new int[][]{{-4, 2}, {3, -2}, {-3, -4}, {4, 3}}) {
                int top = plan.surface(p[0], p[1]);
                if (plan.get(p[0], top + 1, p[1]) == Blocks.air && !(p[0] == 3 && "BARRIER".equals(plan.site.layout))) {
                    if (plan.get(p[0], top, p[1]).getMaterial().isSolid()) plan.put(p[0], top + 1, p[1], Blocks.leaves, 4);
                }
            }
        }
        int guardX = "BARRIER".equals(plan.site.layout) ? -3 : -1;
        if (post == null) return;
        post.guardPoint = plan.point(guardX, anchor(), -1);
        post.patrolPoint = plan.point(1, anchor(), 1);
        if ("BARRIER".equals(plan.site.layout) || "RIDGE_POST".equals(plan.site.layout)) {
            int y = "BARRIER".equals(plan.site.layout) ? deck : Math.max(deck - 1, plan.surface(0, rear));
            post.entrance = plan.point(0, y + 1, rear);
            post.approach = (plan.turn + 2) & 3;
        }
        int cacheX = "TRENCH_LINE".equals(plan.site.layout) ? -3 : -2;
        int cacheZ = "FIGHTING_PITS".equals(plan.site.layout) ? -2 : "TRENCH_LINE".equals(plan.site.layout) ? 0 : 0;
        if (plan.get(cacheX, deck, cacheZ).getMaterial().isSolid()
                && plan.get(cacheX, anchor(), cacheZ) == Blocks.air && plan.get(cacheX, anchor() + 1, cacheZ) == Blocks.air) {
            plan.stock(cacheX, anchor(), cacheZ, random);
        }
    }

    private void lane(int x1, int z1, int x2, int z2, int floor) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            int top = plan.surface(x, z);
            for (int y = top + 1; y < floor; y++) plan.put(x, y, z, retaining);
            plan.put(x, floor, z, improvised ? Blocks.planks : Blocks.cobblestone);
            for (int y = floor + 1; y <= Math.max(top + 1, floor + 2); y++) plan.put(x, y, z, Blocks.air);
        }
    }

    private void line(int x1, int x2, int z, boolean breached) {
        for (int x = x1; x <= x2; x++) {
            if (!breached || Math.floorMod(x, 4) != 1) bank(x, z, facing, Math.floorMod(x + z, 5) == 0 ? 2 : 1);
        }
    }

    private void bank(int x, int z, Block block, int height) {
        int top = plan.surface(x, z);
        for (int y = Math.min(top + 1, deck); y <= deck + height; y++) plan.put(x, y, z, block);
        for (int y = deck + height + 1; y <= Math.max(top + 1, deck + 3); y++) plan.put(x, y, z, Blocks.air);
    }

    private void shelter(int from, int to, int z1, int z2, boolean concealed) {
        for (int side : new int[]{from, to}) for (int end : new int[]{z1, z2}) bank(side, end, retaining, 2);
        for (int x = from; x <= to; x++) for (int z = z1; z <= z2; z++) {
            plan.put(x, deck + 3, z, concealed ? MilitaryMaterials.CAMOUFLAGE : MilitaryMaterials.CORRUGATED);
            if (concealed && Math.floorMod(x * 2 + z, 4) == 0) plan.put(x, deck + 4, z, Blocks.leaves, 4);
        }
    }

    private void brace(int x, int from, int to) {
        for (int z = from; z <= to; z++) bank(x, z, retaining, 1);
    }

    private void tunnel(Occupation post, Random random) {
        chamber(-2, -2, 2, compound ? 6 : 3, retaining);
        chamber(-5, -1, -2, 3, Blocks.planks);
        plan.fill(-2, anchor(), 0, -2, anchor() + 1, 1, Blocks.air);
        if (post != null) plan.stock(-4, anchor(), 2, random);
        plan.put(-4, anchor(), -1, Blocks.crafting_table);
        plan.put(-5, anchor(), 2, Blocks.bookshelf);
        plan.bed(-4, 1, deck);
        if (compound) {
            chamber(2, -2, 6, 2, Blocks.planks);
            chamber(2, 3, 9, 6, Blocks.planks);
            chamber(-9, 3, -2, 6, Blocks.planks);
            plan.fill(2, anchor(), -1, 2, anchor() + 1, 0, Blocks.air);
            plan.fill(2, anchor(), 4, 2, anchor() + 1, 5, Blocks.air);
            plan.fill(-2, anchor(), 4, -2, anchor() + 1, 5, Blocks.air);
            if (post != null) plan.stock(4, anchor(), 5, random);
            plan.put(5, anchor(), -1, Blocks.anvil);
            plan.put(8, anchor(), 6, Blocks.crafting_table);
            plan.bed(-8, 4, deck);
            plan.bed(-8, 5, deck);
            plan.put(-5, anchor(), 5, Blocks.bookshelf);
            plan.put(-4, anchor(), 5, Blocks.crafting_table);
            ramp(7, plan.r, 2);
            plan.fill(5, anchor(), 2, 8, anchor() + 1, 2, Blocks.air);
        }
        ramp(0, -plan.r, -2);
        if (post == null) return;
        post.underground = true;
        post.guardPoint = plan.point(-3, anchor(), 1);
        post.patrolPoint = plan.point(1, anchor(), compound ? 4 : 2);
    }

    private void chamber(int x1, int z1, int x2, int z2, Block floorBlock) {
        plan.fill(x1, deck, z1, x2, deck, z2, floorBlock);
        plan.fill(x1 + 1, anchor(), z1 + 1, x2 - 1, anchor() + 2, z2 - 1, Blocks.air);
        plan.fill(x1, anchor(), z1, x1, anchor() + 2, z2, retaining);
        plan.fill(x2, anchor(), z1, x2, anchor() + 2, z2, retaining);
        plan.fill(x1 + 1, anchor(), z1, x2 - 1, anchor() + 2, z1, retaining);
        plan.fill(x1 + 1, anchor(), z2, x2 - 1, anchor() + 2, z2, retaining);
        plan.fill(x1, anchor() + 3, z1, x2, anchor() + 3, z2, retaining);
        plan.put((x1 + x2) / 2, anchor() + 2, z2, Blocks.glowstone);
        for (int z = z1 + 2; z < z2; z += 4) {
            plan.fill(x1 + 1, anchor() + 2, z, x2 - 1, anchor() + 2, z, MilitaryMaterials.REVETMENT);
        }
    }

    private void ramp(int x, int boundary, int end) {
        int start = plan.surface(x, boundary);
        int direction = Integer.compare(end, boundary);
        for (int i = 0; i <= Math.abs(end - boundary); i++) {
            int z = boundary + i * direction;
            int y = Math.max(deck, start - i - 1);
            plan.fill(x - 1, y, z, x + 1, y, z, Blocks.planks);
            plan.fill(x - 1, y + 1, z, x + 1, y + 3, z, Blocks.air);
            for (int px = x - 1; px <= x + 1; px++) {
                Block above = plan.get(px, y + 4, z);
                if (above == Blocks.sand || above == Blocks.gravel) plan.put(px, y + 4, z, Blocks.log);
            }
            if (i % 4 == 2) plan.put(x - 2, y + 2, z, Blocks.glowstone);
        }
        // Side foliage marks an entrance at close range while leaving the three walkable columns clear.
        for (int side : new int[]{x - 2, x + 2}) {
            int y = plan.surface(side, boundary) + 1;
            plan.put(side, y, boundary, Blocks.leaves, 4);
        }
    }
}
