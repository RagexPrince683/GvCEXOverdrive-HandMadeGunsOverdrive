package handmadeguns.compat;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.registry.GameRegistry;
import handmadeguns.gunsmithing.GunSmithRecipeRegistry;
import handmadeguns.gunsmithing.GunTableIngredient;
import handmadeguns.items.HGBaseItems;
import handmadeguns.items.HGMetalBlocks;
import handmadeguns.items.HGMetalItems;
import handmadeguns.items.HGMetalRecipes;
import handmadeguns.world.HGWorldGenConfig;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Container;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

/** Real Forge registries/recipes with small registry fixtures; does not launch or emulate the other mods. */
public final class HMGMaterialCompatibilityTests {
    private static int checks;
    private static LoadController controller;
    private static final Map<String, cpw.mods.fml.common.ModContainer> loadedMods =
            new HashMap<String, cpw.mods.fml.common.ModContainer>();

    public static void main(String[] args) throws Exception {
        if (!(HMGMaterialCompatibilityTests.class.getClassLoader() instanceof LaunchClassLoader)) {
            LaunchClassLoader loader = new LaunchClassLoader(
                    ((URLClassLoader) HMGMaterialCompatibilityTests.class.getClassLoader()).getURLs());
            Launch.classLoader = loader;
            Thread.currentThread().setContextClassLoader(loader);
            loader.loadClass(HMGMaterialCompatibilityTests.class.getName()).getMethod("main", String[].class)
                    .invoke(null, (Object) args);
            return;
        }
        Field logSide = cpw.mods.fml.relauncher.FMLRelaunchLog.class.getDeclaredField("side");
        logSide.setAccessible(true);
        logSide.set(null, cpw.mods.fml.relauncher.Side.SERVER);
        Loader.injectData("7", "10", "99", "99", "1.7.10", "9.05", new File("."), Collections.emptyList());
        Bootstrap.func_151354_b();
        controller = new LoadController(Loader.instance());
        setField(Loader.instance(), "modController", controller);
        setField(Loader.instance(), "namedMods", loadedMods);
        activate("HandmadeGuns");
        HGBaseItems.init();
        HGMetalItems.init();
        HGMetalBlocks.init();
        HGMetalCompat.initializeRecipeInputs();
        HGWorldGenConfig.init();
        check(HGWorldGenConfig.generateCopper && HGWorldGenConfig.generateAluminum,
                "Own ores must not disable standalone generation");
        check(!contains("ingotAnyPlastic", HGBaseItems.polymer), "Synthetic polymer exported as plastic");
        check(!contains("ingotSteel", HGMetalItems.steelIngot), "Synthetic steel exported as industrial steel");
        check(contains("ingotCopper", HGMetalItems.copperIngot), "Copper compatibility lost");
        check(contains("ingotAluminum", HGMetalItems.aluminumIngot), "Aluminum compatibility lost");
        if (args.length > 0 && args[0].equals("standalone")) {
            HGMetalRecipes.init();
            HGMetalBlocks.registerRecipes();
            check(FurnaceRecipes.smelting().getSmeltingResult(new ItemStack(HGMetalItems.ironInfusedCoal))
                    .getItem() == HGMetalItems.steelIngot, "Standalone steel fallback missing");
            check(FurnaceRecipes.smelting().getSmeltingResult(new ItemStack(HGMetalBlocks.aluminumOre))
                    .getItem() == HGMetalItems.aluminumIngot, "Standalone aluminum smelting missing");
            System.out.println("HMG standalone material checks: " + checks + " passed");
            return;
        }

        GunTableIngredient steel = GunSmithRecipeRegistry.normalizeIngredient(new ItemStack(HGMetalItems.steelIngot, 3));
        GunTableIngredient polymer = GunSmithRecipeRegistry.normalizeIngredient(new ItemStack(HGBaseItems.polymer, 2));
        check(steel.matches(new ItemStack(HGMetalItems.steelIngot)), "Saved fallback steel rejected");
        check(polymer.matches(new ItemStack(HGBaseItems.polymer)), "Saved furniture polymer rejected");
        check(steel.getRequiredAmount() == 3 && polymer.getRequiredAmount() == 2, "Ingredient quantities changed");
        // Capture Forge's list before provider registration: late additions must reach both crafting paths.
        ShapedOreRecipe captured = new ShapedOreRecipe(new ItemStack(HGMetalItems.copperIngot),
                "SP", 'S', HGMetalCompat.STEEL_INPUT, 'P', HGMetalCompat.POLYMER_INPUT);

        activate("hbm");
        loadedMods.put("hbm", Loader.instance().activeModContainer());
        Item ordinarySteel = item("item.ingot_steel");
        Item polyethylene = item("item.ingot_polymer");
        Item bakelite = item("item.ingot_bakelite");
        Item polycarbonate = item("item.ingot_pc");
        Item weaponSteel = item("item.ingot_weaponsteel");
        Item stainlessSteel = item("item.ingot_stainless");
        Item rubber = item("item.ingot_rubber");
        OreDictionary.registerOre("ingotSteel", ordinarySteel);
        OreDictionary.registerOre("ingotWeaponSteel", weaponSteel);
        OreDictionary.registerOre("ingotStainlessSteel", stainlessSteel);
        OreDictionary.registerOre("ingotAnyPlastic", polyethylene);
        OreDictionary.registerOre("ingotAnyPlastic", bakelite);
        // An accidental generic alias must not promote a known HBM grade into the private steel input.
        OreDictionary.registerOre("ingotSteel", weaponSteel);
        HGMetalCompat.initializeRecipeInputs();
        int steelInputs = OreDictionary.getOres(HGMetalCompat.STEEL_INPUT).size();
        HGMetalCompat.initializeRecipeInputs();
        check(OreDictionary.getOres(HGMetalCompat.STEEL_INPUT).size() == steelInputs, "Duplicate input registration");
        check(steel.matches(new ItemStack(ordinarySteel)), "RTM ordinary steel cannot craft HMG");
        check(!steel.matches(new ItemStack(weaponSteel)) && !steel.matches(new ItemStack(stainlessSteel)),
                "Steel grades were merged");
        check(polymer.matches(new ItemStack(polyethylene)) && polymer.matches(new ItemStack(bakelite))
                && polymer.matches(new ItemStack(polycarbonate)), "Explicit furniture alternatives lost");
        check(!polymer.matches(new ItemStack(rubber)), "Rubber incorrectly accepted as furniture polymer");
        check(!polymer.matches(new ItemStack(polyethylene, 1, 1)), "Provider metadata was broadened");
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            public boolean canInteractWith(EntityPlayer player) { return true; }
        }, 3, 3);
        grid.setInventorySlotContents(0, new ItemStack(ordinarySteel));
        grid.setInventorySlotContents(1, new ItemStack(polyethylene));
        check(captured.matches(grid, null), "Captured Forge recipe missed late provider inputs");
        grid.setInventorySlotContents(0, new ItemStack(weaponSteel));
        check(!captured.matches(grid, null), "Forge recipe accepted a different steel grade");
        check(!contains("ingotAnyPlastic", HGBaseItems.polymer) && !contains("ingotSteel", HGMetalItems.steelIngot),
                "Private bridge leaked back into provider commodities");

        activate("mcheli");
        Item vehicleSteel = item("item.ingot_steel");
        Item armorPlate = item("lap");
        OreDictionary.registerOre("ingotSteel", vehicleSteel);
        HGMetalCompat.initializeRecipeInputs();
        check(!steel.matches(new ItemStack(vehicleSteel)) && !steel.matches(new ItemStack(armorPlate)),
                "Vehicle synthetics became bulk steel");

        activate("HandmadeGuns");
        int before = CraftingManager.getInstance().getRecipeList().size();
        HGMetalRecipes.init();
        // Provider fixtures are installed: no fallback or conversion production may be registered.
        check(CraftingManager.getInstance().getRecipeList().size() == before, "Metal conversion/fallback registered");
        check(FurnaceRecipes.smelting().getSmeltingResult(new ItemStack(HGMetalItems.ironInfusedCoal)) == null,
                "External steel can be bypassed through stored infused coal");
        HGMetalBlocks.registerRecipes();
        check(FurnaceRecipes.smelting().getSmeltingResult(new ItemStack(HGMetalBlocks.aluminumOre)) == null,
                "HMG ore smelting bypasses RTM aluminum processing");
        check(FurnaceRecipes.smelting().getSmeltingResult(new ItemStack(HGMetalBlocks.copperOre))
                .getItem() == HGMetalItems.copperIngot, "Legitimate copper smelting lost");
        check(GameRegistry.findItem("HandmadeGuns", "polymer") == HGBaseItems.polymer
                && GameRegistry.findItem("HandmadeGuns", "steelIngot") == HGMetalItems.steelIngot,
                "Persistent item identities changed");
        activate(null);
        System.out.println("HMG material compatibility: " + checks + " checks passed");
    }

    private static void activate(String id) throws Exception {
        if (id == null) { setField(controller, "activeContainer", null); return; }
        ModMetadata metadata = new ModMetadata();
        metadata.modId = id;
        metadata.name = id;
        setField(controller, "activeContainer", new DummyModContainer(metadata));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Item item(String name) {
        Item item = new Item();
        GameRegistry.registerItem(item, name);
        return item;
    }

    private static boolean contains(String key, Item item) {
        for (ItemStack stack : OreDictionary.getOres(key)) if (stack.getItem() == item) return true;
        return false;
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
}
