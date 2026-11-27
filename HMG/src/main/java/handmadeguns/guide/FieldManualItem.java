package handmadeguns.guide;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class FieldManualItem extends Item {
    private final Runnable open;

    private FieldManualItem(String name, CreativeTabs tab, Runnable open) {
        this.open = open;
        setUnlocalizedName(name);
        setTextureName("minecraft:book_written");
        setCreativeTab(tab);
        setMaxStackSize(1);
    }

    public static Item register(String id, String name, CreativeTabs tab, Item ingredient, boolean enabled, Runnable open) {
        Item manual = new FieldManualItem(name, enabled ? tab : null, enabled ? open : null);
        GameRegistry.registerItem(manual, id);
        if (enabled) GameRegistry.addShapelessRecipe(new ItemStack(manual), Items.book, ingredient);
        return manual;
    }

    @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote && open != null) open.run();
        return stack;
    }
}
