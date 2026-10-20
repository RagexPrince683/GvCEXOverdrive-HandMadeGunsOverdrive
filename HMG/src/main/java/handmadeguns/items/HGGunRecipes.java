package handmadeguns.items;

import cpw.mods.fml.common.registry.GameRegistry;
import handmadeguns.compat.HGMetalCompat;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class HGGunRecipes {

    public static void init() {

        // =====================================================
        // CORE MECHANISMS
        // =====================================================

        // firing pin — single output (scarcity maintained)
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.firingPin, 3),
                " S ",
                " I ",
                " S ",
                'S', HGMetalCompat.STEEL_INPUT,
                'I', "ingotIron"
        ));

        // spring set — energy storage parts, now outputs 12
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.springSet, 12),
                " S ",
                " S ",
                " S ",
                'S', HGMetalCompat.STEEL_INPUT
        ));

        // trigger assembly — fire control group, now outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.triggerAssembly, 2),
                " P ",
                "RIR",
                " S ",
                'P', HGGunItems.springSet,
                'I', "ingotIron",
                'S', HGMetalCompat.STEEL_INPUT,
                'R', Items.redstone
        ));

        // =====================================================
        // MANUFACTURING TIERS
        // =====================================================

        // machined parts — mid-tier gate, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.machinedParts, 2),
                " S ",
                "IRI",
                " S ",
                'S', HGMetalCompat.STEEL_INPUT,
                'I', "ingotIron",
                'R', Items.redstone
        ));

        // precision components — high-tier gate, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.precisionComponents, 2),
                " M ",
                "RAR",
                " M ",
                'M', HGGunItems.machinedParts,
                'R', Items.redstone,
                'A', "ingotAluminum"
        ));

        // =====================================================
        // RECEIVERS (MANUFACTURING TYPE)
        // =====================================================

        // stamped receiver — WW2 / SMG / AK-type, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.stampedReceiver, 2),
                "SSS",
                "STS",
                "SSS",
                'S', HGMetalCompat.STEEL_INPUT,
                'T', HGGunItems.triggerAssembly
        ));

        // milled receiver — modern / precision, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.milledReceiver, 2),
                "PMP",
                "MTM",
                " M ",
                'M', HGGunItems.machinedParts,
                'P', HGGunItems.precisionComponents,
                'T', HGGunItems.triggerAssembly
        ));

        // =====================================================
        // CORE OPERATING ASSEMBLIES
        // =====================================================

        // blowback bolt — SMG/simple, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.blowbackBoltAssembly, 2),
                "PSS",
                "SF ",
                " S ",
                'P', HGGunItems.springSet,
                'S', HGMetalCompat.STEEL_INPUT,
                'F', HGGunItems.firingPin
        ));

        // rotating bolt — rifle pattern, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.rotatingBoltAssembly, 2),
                "PXS",
                "MF ",
                " S ",
                'P', HGGunItems.springSet,
                'X', HGGunItems.precisionComponents,
                'M', HGGunItems.machinedParts,
                'S', HGMetalCompat.STEEL_INPUT,
                'F', HGGunItems.firingPin
        ));

        // heavy carrier — LMG / high-mass, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.heavyBoltCarrierAssembly, 2),
                "PMP",
                "MFM",
                " S ",
                'P', HGGunItems.springSet,
                'M', HGGunItems.machinedParts,
                'S', HGMetalCompat.STEEL_INPUT,
                'F', HGGunItems.firingPin
        ));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.boltActionAssembly, 2),
                " XP",
                "MFM",
                " S ",
                'P', HGGunItems.springSet,
                'X', HGGunItems.precisionComponents,
                'M', HGGunItems.machinedParts,
                'S', HGMetalCompat.STEEL_INPUT,
                'F', HGGunItems.firingPin
        ));

        // =====================================================
        // BARREL ASSEMBLIES
        // =====================================================

        // light barrel — pistol / SMG, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.lightBarrelKit, 2),
                " S ",
                " M ",
                " S ",
                'S', HGMetalCompat.STEEL_INPUT,
                'M', HGGunItems.machinedParts
        ));

        // rifle barrel — standard, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.rifleBarrelKit, 2),
                " M ",
                " S ",
                " M ",
                'S', HGMetalCompat.STEEL_INPUT,
                'M', HGGunItems.machinedParts
        ));

        // heavy barrel — DMR / LMG, outputs 2
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.heavyBarrelKit, 2),
                " P ",
                " M ",
                " S ",
                'P', HGGunItems.precisionComponents,
                'M', HGGunItems.machinedParts,
                'S', HGMetalCompat.STEEL_INPUT
        ));

        // =====================================================
        // AR PLATFORM MODULES
        // =====================================================

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.gasSystemModule, 2),
                " A ",
                "RMR",
                " A ",
                'A', "ingotAluminum",
                'M', HGGunItems.machinedParts,
                'R', Items.redstone
        ));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.arUpper, 2),
                "AAA",
                "AM ",
                "AAA",
                'A', "ingotAluminum",
                'M', HGGunItems.machinedParts
        ));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.arLower, 2),
                "A A",
                "ATA",
                "A A",
                'A', "ingotAluminum",
                'T', HGGunItems.triggerAssembly
        ));

        // =====================================================
        // FEED SYSTEMS
        // =====================================================

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.beltFeedModule, 2),
                "SMS",
                "M M",
                "SMS",
                'S', HGMetalCompat.STEEL_INPUT,
                'M', HGGunItems.machinedParts
        ));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.pumpLeverActionModule, 2),
                " S ",
                "M M",
                " P ",
                'S', HGMetalCompat.STEEL_INPUT,
                'M', HGGunItems.machinedParts,
                'P', HGGunItems.springSet
        ));

        // =====================================================
        // LAUNCHER SYSTEMS
        // =====================================================

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.launcherTube, 2),
                " S ",
                " S ",
                " S ",
                'S', HGMetalCompat.STEEL_INPUT
        ));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.launcherFiringAssembly, 2),
                "PMP",
                "MTM",
                " M ",
                'P', HGGunItems.springSet,
                'M', HGGunItems.machinedParts,
                'T', HGGunItems.triggerAssembly
        ));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.launcherGripFrame, 2),
                " M ",
                "MSM",
                " M ",
                'M', HGGunItems.machinedParts,
                'S', HGMetalCompat.STEEL_INPUT
        ));

        //=====================================================
        //HANDGUN SYSTEMS
        //=====================================================

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.handgunSlideAssembly, 2),
                "PSS",
                "MFM",
                " S ",
                'P', HGGunItems.springSet,
                'M', HGGunItems.machinedParts,
                'S', HGMetalCompat.STEEL_INPUT,
                'F', HGGunItems.firingPin
        ));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.revolverMechanismAssembly, 2),
                "PXP",
                "MFM",
                " S ",
                'P', HGGunItems.springSet,
                'X', HGGunItems.precisionComponents,
                'M', HGGunItems.machinedParts,
                'S', HGMetalCompat.STEEL_INPUT,
                'F', HGGunItems.firingPin
        ));

        // =====================================================
        // INTERFACES / MOUNTS
        // =====================================================

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.stockMount, 2),
                "S S",
                " M ",
                "S S",
                'S', HGMetalCompat.STEEL_INPUT,
                'M', HGGunItems.machinedParts
        ));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.opticMount, 2),
                " A ",
                "PMP",
                " A ",
                'A', "ingotAluminum",
                'M', HGGunItems.machinedParts,
                'P', HGGunItems.precisionComponents
        ));
        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(HGGunItems.woodGunFurniture),
                " P ",
                "P P",
                " P ",
                'P', "plankWood"
        ));
    }
}



