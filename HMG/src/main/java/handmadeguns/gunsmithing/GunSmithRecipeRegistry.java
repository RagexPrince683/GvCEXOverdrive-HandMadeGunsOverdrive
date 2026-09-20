package handmadeguns.gunsmithing;

import cpw.mods.fml.common.registry.GameRegistry;
import handmadeguns.HandmadeGunsCore;
import handmadeguns.items.HMGItemCustomMagazine;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraftforge.oredict.OreDictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Authoritative, common-side registry for every Gun Smithing Table recipe. */
public final class GunSmithRecipeRegistry {
    private static final List<GunSmithRecipe> RECIPES = new ArrayList<GunSmithRecipe>();
    private static final List<RecipeCopy> PENDING_COPIES = new ArrayList<RecipeCopy>();

    private static final class RecipeCopy {
        private final ItemStack output;
        private final ItemStack source;
        private final File file;

        private RecipeCopy(ItemStack output, ItemStack source, File file) {
            this.output = output.copy();
            this.source = source.copy();
            this.file = file;
        }
    }

    private GunSmithRecipeRegistry() {}

    public static synchronized void register(GunSmithRecipe recipe) {
        if (recipe != null) RECIPES.add(recipe);
    }

    public static void register(ItemStack output, GunTableIngredient[] ingredients,
                                GunSmithRecipeCategory category) {
        if (output != null) register(new GunSmithRecipe(output, ingredients, category));
    }

    public static void register(ItemStack output, GunSmithRecipeCategory category, ItemStack... inputs) {
        register(output, normalizeIngredients(inputs), category);
    }

    /** Converts a legacy three-row shape into the table's fixed row-major grid. */
    public static GunSmithRecipe createLegacyShapedRecipe(ItemStack output, String[] rows,
                                                           ItemStack[] symbols, File sourceFile) {
        return createLegacyShapedRecipe(output, rows, symbols, sourceFile, categoryForOutput(output));
    }

    /** Converts a legacy shape while preserving a caller's historical table category. */
    public static GunSmithRecipe createLegacyShapedRecipe(ItemStack output, String[] rows,
                                                           ItemStack[] symbols, File sourceFile,
                                                           GunSmithRecipeCategory category) {
        return createLegacyShapedRecipe(output, rows, normalizeLegacySymbols(symbols), sourceFile, category);
    }

    public static GunSmithRecipe createLegacyShapedRecipe(ItemStack output, String[] rows,
                                                           GunTableIngredient[] symbols, File sourceFile,
                                                           GunSmithRecipeCategory category) {
        if (output == null || rows == null || rows.length != 3) return null;
        GunTableIngredient[] ingredients = new GunTableIngredient[GunSmithRecipe.SLOT_COUNT];
        for (int row = 0; row < 3; row++) {
            String shapeRow = rows[row] == null ? "" : rows[row];
            for (int column = 0; column < shapeRow.length(); column++) {
                char symbol = shapeRow.charAt(column);
                if (symbol == ' ') continue;
                if (column >= 3 || symbol < 'a' || symbol > 'i'
                        || symbols == null || symbol - 'a' >= symbols.length
                        || symbols[symbol - 'a'] == null) {
                    HandmadeGunsCore.Debug("[GunSmith] Rejecting legacy recipe in %s: unresolved symbol '%s' at row %s, column %s",
                            sourceFile == null ? "unknown file" : sourceFile.getPath(), symbol, row + 1, column + 1);
                    return null;
                }
                ingredients[row * 3 + column] = symbols[symbol - 'a'];
            }
        }
        return new GunSmithRecipe(output, ingredients, category);
    }

    public static GunTableIngredient[] normalizeLegacySymbols(ItemStack[] symbols) {
        GunTableIngredient[] normalized = new GunTableIngredient[symbols == null ? 0 : symbols.length];
        for (int i = 0; i < normalized.length; i++)
            if (symbols[i] != null) normalized[i] = normalizeIngredient(symbols[i]);
        return normalized;
    }

    /** Builds a ShapedOreRecipe argument list from the canonical normalized symbols. */
    public static Object[] createForgeRecipeArguments(String[] rows, GunTableIngredient[] symbols) {
        ArrayList<Object> recipe = new ArrayList<Object>();
        for (int row = 0; row < 3; row++) recipe.add(rows[row]);
        if (symbols != null) for (int i = 0; i < symbols.length; i++) {
            if (symbols[i] != null) {
                recipe.add(Character.valueOf((char) ('a' + i)));
                GunTableIngredient ingredient = symbols[i];
                recipe.add(ingredient instanceof OreDictionaryIngredient
                        ? ((OreDictionaryIngredient) ingredient).getOreName()
                        : ((ExactStackIngredient) ingredient).getExactStack());
            }
        }
        return recipe.toArray(new Object[recipe.size()]);
    }

    public static GunSmithRecipeCategory categoryForOutput(ItemStack output) {
        Item item = output == null ? null : output.getItem();
        if (item instanceof HMGItemCustomMagazine) return GunSmithRecipeCategory.AMMO;
        return GunSmithRecipeCategory.GUNS;
    }

    /** Compatibility adapter for old pack-facing callers; gun is the historical default. */
    @Deprecated
    public static void register(ItemStack output, ItemStack... inputs) {
        register(output, GunSmithRecipeCategory.GUNS, inputs);
    }

    /** Compatibility adapter for old pack-facing callers; gun is the historical default. */
    @Deprecated
    public static void register(ItemStack output, GunTableIngredient[] ingredients) {
        register(output, ingredients, GunSmithRecipeCategory.GUNS);
    }

    /** Compatibility adapter for the former parallel exact/ore arrays. */
    @Deprecated
    public static void register(ItemStack output, ItemStack[] inputs, String[] oreInputs) {
        int length = Math.max(inputs == null ? 0 : inputs.length, oreInputs == null ? 0 : oreInputs.length);
        GunTableIngredient[] ingredients = new GunTableIngredient[Math.min(length, GunSmithRecipe.SLOT_COUNT)];
        for (int i = 0; i < ingredients.length; i++) {
            ItemStack input = inputs != null && i < inputs.length ? inputs[i] : null;
            String ore = oreInputs != null && i < oreInputs.length ? oreInputs[i] : null;
            if (ore != null && !ore.trim().isEmpty()) {
                ingredients[i] = createExplicitOreIngredient(ore.trim(), input == null ? 1 : Math.max(1, input.stackSize));
            } else if (input != null) {
                ingredients[i] = normalizeIngredient(input);
            }
        }
        register(output, ingredients, GunSmithRecipeCategory.GUNS);
    }

    public static synchronized List<GunSmithRecipe> getAll() {
        return Collections.unmodifiableList(new ArrayList<GunSmithRecipe>(RECIPES));
    }

    public static synchronized List<GunSmithRecipe> getRecipes(GunSmithRecipeCategory category) {
        List<GunSmithRecipe> found = new ArrayList<GunSmithRecipe>();
        for (GunSmithRecipe recipe : RECIPES) if (recipe.getCategory() == category) found.add(recipe);
        return Collections.unmodifiableList(found);
    }

    public static List<GunSmithRecipe> getGunRecipes() { return getRecipes(GunSmithRecipeCategory.GUNS); }
    public static List<GunSmithRecipe> getAmmoRecipes() { return getRecipes(GunSmithRecipeCategory.AMMO); }

    public static synchronized List<GunSmithRecipe> findByOutput(ItemStack output) {
        List<GunSmithRecipe> found = new ArrayList<GunSmithRecipe>();
        if (output != null) for (GunSmithRecipe recipe : RECIPES) {
            if (matchesRecipeOutput(recipe.getOutput(), output)) found.add(recipe);
        }
        return Collections.unmodifiableList(found);
    }

    /**
     * Compares the identity of an item produced by a table recipe, rather than the
     * mutable state of the particular stack being inspected by NEI.
     */
    public static boolean matchesRecipeOutput(ItemStack recipeOutput, ItemStack queried) {
        if (recipeOutput == null || queried == null || recipeOutput.getItem() != queried.getItem()) return false;

        Item item = recipeOutput.getItem();
        // Guns use both damage and NBT for live ammunition, firing, and attachment state.
        if (item instanceof HMGItem_Unified_Guns) return true;
        // Custom magazines use item damage as their remaining-round count.
        if (item instanceof HMGItemCustomMagazine) return true;

        int recipeMetadata = recipeOutput.getItemDamage();
        int queriedMetadata = queried.getItemDamage();
        if (recipeMetadata != OreDictionary.WILDCARD_VALUE
                && queriedMetadata != OreDictionary.WILDCARD_VALUE
                && recipeMetadata != queriedMetadata) return false;

        // An output tag is recipe-defined data.  An untagged output, by contrast,
        // must accept irrelevant runtime tags added to the queried stack.
        return !recipeOutput.hasTagCompound() || ItemStack.areItemStackTagsEqual(recipeOutput, queried);
    }

    public static synchronized List<GunSmithRecipe> findByIngredient(ItemStack stack) {
        List<GunSmithRecipe> found = new ArrayList<GunSmithRecipe>();
        if (stack != null) for (GunSmithRecipe recipe : RECIPES) {
            for (GunTableIngredient ingredient : recipe.getIngredients()) {
                if (ingredient != null && ingredient.matches(stack)) { found.add(recipe); break; }
            }
        }
        return Collections.unmodifiableList(found);
    }

    public static void registerFromFile(File recipeFile) {
        try {
            parseAndRegisterAddRecipeFile(recipeFile);
        } catch (IOException e) {
            HandmadeGunsCore.Debug("[GunSmith] Failed to parse %s: %s", recipeFile.getName(), e.getMessage());
        }
    }

    /** Resolves pack-level CopyRecipe directives after every recipe root has loaded. */
    public static synchronized void resolvePendingCopies() {
        for (RecipeCopy copy : PENDING_COPIES) {
            List<GunSmithRecipe> sources = findByOutput(copy.source);
            if (sources.isEmpty()) {
                HandmadeGunsCore.Debug("[GunSmith] Cannot copy recipe in %s: source %s has no registered recipe",
                        copy.file.getPath(), copy.source);
                continue;
            }
            if (!findByOutput(copy.output).isEmpty()) {
                HandmadeGunsCore.Debug("[GunSmith] Skipping duplicate copied recipe output %s from %s",
                        copy.output, copy.file.getPath());
                continue;
            }
            for (GunSmithRecipe source : sources) {
                register(new GunSmithRecipe(copy.output, source.getIngredients(), source.getCategory()));
            }
        }
        PENDING_COPIES.clear();
    }

    private static GunTableIngredient[] normalizeIngredients(ItemStack[] inputs) {
        GunTableIngredient[] ingredients = new GunTableIngredient[GunSmithRecipe.SLOT_COUNT];
        if (inputs != null) for (int i = 0; i < Math.min(inputs.length, ingredients.length); i++) {
            if (inputs[i] != null) ingredients[i] = normalizeIngredient(inputs[i]);
        }
        return ingredients;
    }

    /**
     * MCHO-style normalization for every ordinary Gun Smithing Table stack input.
     * The first Forge ore ID is authoritative, matching Forge's deterministic
     * registration order. Tagged stacks remain exact so NBT requirements are not lost.
     */
    public static GunTableIngredient normalizeIngredient(ItemStack stack) {
        if (stack == null) return null;
        if (!stack.hasTagCompound()) {
            int[] oreIds = OreDictionary.getOreIDs(stack);
            if (oreIds != null) {
                for (int i = 0; i < oreIds.length; i++) {
                    String oreName = OreDictionary.getOreName(oreIds[i]);
                    if (isValidOreName(oreName)) {
                        if (oreIds.length > 1) {
                            HandmadeGunsCore.Debug("[GunSmith] %s has %s Ore Dictionary IDs; selecting first valid entry %s",
                                    stack, oreIds.length, oreName);
                        }
                        return new OreDictionaryIngredient(oreName, Math.max(1, stack.stackSize));
                    }
                }
            }
        }
        return new ExactStackIngredient(stack);
    }

    private static GunTableIngredient createExplicitOreIngredient(String oreName, int amount) {
        return isValidOreName(oreName) && amount > 0 ? new OreDictionaryIngredient(oreName, amount) : null;
    }

    private static boolean isValidOreName(String oreName) {
        if (oreName == null || oreName.trim().isEmpty()) return false;
        String expected = oreName.trim();
        for (String registered : OreDictionary.getOreNames()) {
            if (expected.equals(registered)) return true;
        }
        return false;
    }

    private static void parseAndRegisterAddRecipeFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            String line;
            GunTableIngredient[] ingredients = new GunTableIngredient[GunSmithRecipe.SLOT_COUNT];
            boolean reading = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.toLowerCase().startsWith("copyrecipe,")) {
                    String[] parts = line.split(",", 3);
                    ItemStack output = parts.length == 3 ? parseStack(parts[1]) : null;
                    ItemStack source = parts.length == 3 ? parseStack(parts[2]) : null;
                    if (output != null && source != null) {
                        synchronized (GunSmithRecipeRegistry.class) {
                            PENDING_COPIES.add(new RecipeCopy(output, source, file));
                        }
                    } else {
                        HandmadeGunsCore.Debug("[GunSmith] Rejecting CopyRecipe in %s: expected resolvable output and source",
                                file.getPath());
                    }
                } else if (line.equalsIgnoreCase("AddRecipe")) {
                    ingredients = new GunTableIngredient[GunSmithRecipe.SLOT_COUNT];
                    reading = true;
                } else if (reading && line.toLowerCase().startsWith("slot")) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) try {
                        int slot = Integer.parseInt(parts[0].trim().substring(4)) - 1;
                        if (slot >= 0 && slot < ingredients.length) ingredients[slot] = parseIngredient(parts[1], file, slot);
                    } catch (NumberFormatException ignored) { }
                } else if (reading && line.toLowerCase().startsWith("craftitem")) {
                    String[] parts = line.split(",", 2);
                    ItemStack output = parts.length == 2 ? parseStack(parts[1]) : null;
                    if (output != null) register(output, ingredients, GunSmithRecipeCategory.GUNS);
                    reading = false;
                }
            }
        } finally { reader.close(); }
    }

    private static GunTableIngredient parseIngredient(String value, File file, int slot) {
        String text = value.trim();
        String lower = text.toLowerCase();
        int prefix = lower.startsWith("ore:") ? 4 : lower.startsWith("oredict:") ? 8
                : lower.startsWith("oredictionary:") ? 14 : 0;
        if (prefix > 0) {
            String spec = text.substring(prefix).trim();
            int amount = 1;
            int separator = spec.lastIndexOf(':');
            if (separator >= 0) try {
                amount = Integer.parseInt(spec.substring(separator + 1).trim());
                spec = spec.substring(0, separator).trim();
            } catch (NumberFormatException e) {
                reject(file, slot, "invalid Ore Dictionary count"); return null;
            }
            GunTableIngredient explicit = createExplicitOreIngredient(spec, amount);
            if (explicit != null) return explicit;
            reject(file, slot, "invalid Ore Dictionary ingredient"); return null;
        }
        boolean exact = lower.startsWith("exact:");
        if (exact) text = text.substring(6).trim();
        ItemStack stack = parseStack(text);
        if (stack == null) reject(file, slot, "could not resolve " + text);
        return stack == null ? null : exact ? new ExactStackIngredient(stack) : normalizeIngredient(stack);
    }

    private static ItemStack parseStack(String value) {
        String text = value.trim().replace(',', ':');
        while (text.endsWith(":")) text = text.substring(0, text.length() - 1);
        String[] parts = text.split(":");
        if (parts.length < 2 || parts.length > 4) return null;
        int meta = 0, count = 1;
        try {
            if (parts.length > 2) meta = Integer.parseInt(parts[2].trim());
            if (parts.length > 3) count = Integer.parseInt(parts[3].trim());
        } catch (NumberFormatException e) { return null; }
        String modId = parts[0].trim();
        String name = parts[1].trim();
        Item item = GameRegistry.findItem(modId, name);
        if (item == null) {
            Block block = GameRegistry.findBlock(modId, name);
            if (block != null) item = Item.getItemFromBlock(block);
        }
        return item == null || count <= 0 ? null : new ItemStack(item, count, meta);
    }

    private static void reject(File file, int slot, String reason) {
        HandmadeGunsCore.Debug("[GunSmith] Rejecting %s Slot%d: %s", file.getName(), slot + 1, reason);
    }
}
