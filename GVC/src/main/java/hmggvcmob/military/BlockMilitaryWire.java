package hmggvcmob.military;

import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;

public final class BlockMilitaryWire extends BlockPane {
    private static int renderId = 18;

    BlockMilitaryWire() {
        super("gvcmob:barbed_wire", "gvcmob:barbed_wire_edge", Material.iron, true);
    }

    public static void setRenderId(int id) { renderId = id; }

    @Override
    public int getRenderType() { return renderId; }
}
