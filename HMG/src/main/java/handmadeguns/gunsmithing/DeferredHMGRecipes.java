package handmadeguns.gunsmithing;

import cpw.mods.fml.common.registry.GameRegistry;
import handmadeguns.compat.HGMetalCompat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Recipe definitions collected while content-pack items are created in preInit. */
public final class DeferredHMGRecipes {
    private static final List<PendingRecipe> PENDING = new ArrayList<PendingRecipe>();
    private static boolean registered;

    private DeferredHMGRecipes() {}

    public static synchronized void queueContentPackRecipe(ItemStack output, String[] rows,
            ItemStack[] symbols, File source, GunSmithRecipeCategory category) {
        ensureOpen();
        PENDING.add(new PendingContentPackRecipe(output, rows, symbols, source, category));
    }

    public static synchronized void queueLegacyRecipe(ItemStack output, String[] rows, Item[] symbols) {
        ensureOpen();
        PENDING.add(new PendingLegacyRecipe(output, rows, symbols));
    }

    public static synchronized void queueSmelting(Item input, ItemStack output, float experience) {
        ensureOpen();
        PENDING.add(new PendingSmeltingRecipe(input, output, experience));
    }

    public static synchronized void registerAll() {
        if (registered) return;
        for (PendingRecipe recipe : PENDING) recipe.register();
        PENDING.clear();
        registered = true;
    }

    private static void ensureOpen() {
        if (registered) throw new IllegalStateException("HMG recipe registration has already completed");
    }

    private interface PendingRecipe { void register(); }

    private static final class PendingContentPackRecipe implements PendingRecipe {
        private final ItemStack output;
        private final String[] rows;
        private final ItemStack[] symbols;
        private final File source;
        private final GunSmithRecipeCategory category;

        private PendingContentPackRecipe(ItemStack output, String[] rows, ItemStack[] symbols,
                File source, GunSmithRecipeCategory category) {
            this.output = output.copy();
            this.rows = rows.clone();
            this.symbols = new ItemStack[symbols.length];
            for (int i = 0; i < symbols.length; i++)
                this.symbols[i] = symbols[i] == null ? null : symbols[i].copy();
            this.source = source == null ? null : new File(source.getPath());
            this.category = category;
        }

        public void register() {
            GunTableIngredient[] normalized = GunSmithRecipeRegistry.normalizeLegacySymbols(symbols);
            GunSmithRecipe tableRecipe = GunSmithRecipeRegistry.createLegacyShapedRecipe(
                    output, rows, normalized, source, category);
            if (tableRecipe == null) return;
            GameRegistry.addRecipe(new ShapedOreRecipe(output,
                    GunSmithRecipeRegistry.createForgeRecipeArguments(rows, normalized)));
            GunSmithRecipeRegistry.register(tableRecipe);
        }
    }

    private static final class PendingLegacyRecipe implements PendingRecipe {
        private final ItemStack output;
        private final String[] rows;
        private final Item[] symbols;
        private PendingLegacyRecipe(ItemStack output, String[] rows, Item[] symbols) {
            this.output = output.copy(); this.rows = rows.clone(); this.symbols = symbols.clone();
        }
        public void register() {
            ArrayList<Object> args = new ArrayList<Object>();
            for (String row : rows) args.add(row);
            for (int i = 0; i < symbols.length; i++) if (symbols[i] != null) {
                args.add(Character.valueOf((char) ('a' + i)));
                String materialInput = HGMetalCompat.recipeInput(new ItemStack(symbols[i]));
                args.add(materialInput == null ? symbols[i] : materialInput);
            }
            GameRegistry.addRecipe(new ShapedOreRecipe(output, args.toArray(new Object[args.size()])));
        }
    }

    private static final class PendingSmeltingRecipe implements PendingRecipe {
        private final Item input; private final ItemStack output; private final float experience;
        private PendingSmeltingRecipe(Item input, ItemStack output, float experience) {
            this.input = input; this.output = output.copy(); this.experience = experience;
        }
        public void register() { GameRegistry.addSmelting(input, output, experience); }
    }

}
