package handmadeguns.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import handmadeguns.HandmadeGunsCore;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import java.util.Locale;

/** Presentation classification; never changes legacy gun type or gameplay defaults. */
public enum WeaponClass {
    ASSAULT_RIFLE("Assault Rifles"),
    SUBMACHINE_GUN("Submachine Guns"), MACHINE_GUN("Machine Guns"), SHOTGUN("Shotguns"),
    SNIPER_RIFLE("Sniper Rifles"), BOLT_ACTION_RIFLE("Bolt-Action Rifles"),
    HANDGUN("Pistols"), LAUNCHER("Launchers"), SPECIAL("Other / Special Weapons"),
    VEHICLE_WEAPON("Vehicle Weapons"), SAMPLE_WEAPON("Sample Weapons");

    public final String label;
    private CreativeTabs tab;
    private Item icon;

    WeaponClass(String label) { this.label = label; }

    public static WeaponClass parse(String value) {
        String key = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        // External pack compatibility terminates at the broad, canonical categories.
        if ("CARBINE".equals(key) || "BATTLE_RIFLE".equals(key)) return ASSAULT_RIFLE;
        if ("MARKSMAN_RIFLE".equals(key) || "DMR".equals(key)) return SNIPER_RIFLE;
        return valueOf(key);
    }

    /** Conservative compatibility for external packs without the new metadata. */
    public static WeaponClass legacy(String registration) {
        if ("AR".equals(registration)) return ASSAULT_RIFLE;
        if ("LMG".equals(registration)) return MACHINE_GUN;
        if ("HG".equals(registration)) return HANDGUN;
        if ("SR".equals(registration) || "AMR".equals(registration)) return SNIPER_RIFLE;
        if ("SG".equals(registration) || "SGF".equals(registration)) return SHOTGUN;
        if ("RR".equals(registration) || "GL".equals(registration)) return LAUNCHER;
        return SPECIAL;
    }

    public CreativeTabs tab(Item item) {
        if (icon == null) icon = item;
        if (tab == null) tab = new CreativeTabs("HMG_" + name()) {
            @Override @SideOnly(Side.CLIENT) public Item getTabIconItem() {
                return icon == null ? HandmadeGunsCore.hmg_bullet : icon;
            }
            @Override @SideOnly(Side.CLIENT) public String getTranslatedTabLabel() { return label; }
        };
        return tab;
    }
}
