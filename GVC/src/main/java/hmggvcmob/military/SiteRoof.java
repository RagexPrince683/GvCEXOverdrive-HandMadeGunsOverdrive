package hmggvcmob.military;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/** Roof geometry in building-local coordinates. The caller owns world rotation. */
final class SiteRoof {
    enum Shape { FLAT, PARAPET, GABLE, HIP, SHED, BARREL, MONITOR }

    interface Placer {
        void put(int x, int y, int z, Block block, int metadata);
    }

    private final Placer place;
    private final Block cover, frame;
    private final Block stairs, slab;
    private final int turn;

    SiteRoof(Placer place, Block cover, Block frame, int turn) {
        this.place = place;
        this.frame = frame;
        this.turn = turn;
        boolean timber = cover == Blocks.wooden_slab;
        boolean sandstone = cover == Blocks.stone_slab;
        this.cover = timber ? Blocks.planks : sandstone ? Blocks.sandstone : cover;
        stairs = timber ? Blocks.spruce_stairs : sandstone ? Blocks.sandstone_stairs : null;
        slab = timber || sandstone ? cover : null;
    }

    static int stairMetadata(int direction, int turn) {
        // The stair's high side points north, east, south or west in local coordinates.
        switch ((direction + turn) & 3) {
            case 0: return 3;
            case 1: return 0;
            case 2: return 2;
            default: return 1;
        }
    }

    void build(int x1, int y, int z1, int x2, int z2, int min, int max, Shape shape, boolean acrossZ) {
        if (shape == Shape.FLAT) {
            for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) full(x, y, z);
            return;
        }
        int left = Math.max(min, x1 - 1), right = Math.min(max, x2 + 1);
        int front = Math.max(min, z1 - 1), back = Math.min(max, z2 + 1);
        if (shape == Shape.MONITOR && right - left >= 6 && back - front >= 5) {
            monitor(left, y, front, right, back, acrossZ);
            return;
        }
        int span = acrossZ ? back - front : right - left;
        for (int x = left; x <= right; x++) for (int z = front; z <= back; z++) {
            if (shape == Shape.PARAPET || shape == Shape.MONITOR) {
                full(x, y, z);
                if (x == left || x == right || z == front || z == back) place.put(x, y + 1, z, frame, 0);
                continue;
            }
            int offset = acrossZ ? z - front : x - left;
            int end = acrossZ ? Math.min(x - left, right - x) : Math.min(z - front, back - z);
            int level = rise(shape, offset, span, end);
            int inward = offset <= span / 2 || shape == Shape.SHED ? 1 : -1;
            int next = offset + inward;
            boolean step = next >= 0 && next <= span && rise(shape, next, span, end) == level + 1;
            boolean endWall = x >= x1 && x <= x2 && z >= z1 && z <= z2
                    && (x == x1 || x == x2 || z == z1 || z == z2);
            int support = level;
            if (offset > 0) support = Math.min(support, rise(shape, offset - 1, span, end));
            if (offset < span) support = Math.min(support, rise(shape, offset + 1, span, end));
            if (shape == Shape.HIP && end > 0) support = Math.min(support, rise(shape, offset, span, end - 1));
            for (int i = endWall ? 0 : support; i < level; i++) full(x, y + i, z);
            if (stairs != null && step && !endWall && shape != Shape.HIP) {
                int direction = acrossZ ? inward == 1 ? 2 : 0 : inward == 1 ? 1 : 3;
                place.put(x, y + level, z, stairs, stairMetadata(direction, turn));
            } else if (slab != null && shape == Shape.HIP && level == 0 && !endWall) {
                // Upper slabs share the underside of the eave with adjacent full courses.
                // Variant 1 is spruce in the wooden slab and sandstone in the stone slab.
                place.put(x, y, z, slab, 1 | 8);
            } else {
                full(x, y + level, z);
            }
        }
    }

    private static int rise(Shape shape, int offset, int span, int end) {
        int side = Math.min(offset, span - offset);
        switch (shape) {
            case HIP: return Math.min(4, Math.min(side, end) / 2);
            case SHED: return offset * Math.min(4, span / 2) / Math.max(span, 1);
            case BARREL:
                double normalized = (offset * 2.0 - span) / Math.max(span, 1);
                int arch = (int) Math.round(Math.min(5, (span + 1) / 3.0) * Math.sqrt(1 - normalized * normalized));
                // A vertical shoulder can step two courses near the eaves.
                return Math.min(arch, side + 1);
            default: return Math.min(5, side / 2);
        }
    }

    private void monitor(int left, int y, int front, int right, int back, boolean acrossZ) {
        int middle = acrossZ ? (front + back) / 2 : (left + right) / 2;
        for (int x = left; x <= right; x++) for (int z = front; z <= back; z++) {
            int offset = Math.abs((acrossZ ? z : x) - middle);
            int end = acrossZ ? Math.min(x - left, right - x) : Math.min(z - front, back - z);
            if (offset >= 2 || end <= 1) full(x, y, z);
            if (offset <= 2 && end == 1) place.put(x, y + 1, z, frame, 0);
            if (offset == 2 && end > 1) place.put(x, y + 1, z, Blocks.glass, 0);
            if (offset <= 2 && end >= 1) full(x, y + 2, z);
        }
    }

    private void full(int x, int y, int z) {
        place.put(x, y, z, cover, slab == Blocks.wooden_slab ? 1 : 0);
    }
}
