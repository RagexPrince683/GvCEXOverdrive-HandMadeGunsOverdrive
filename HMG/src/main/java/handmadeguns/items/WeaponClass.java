package handmadeguns.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import handmadeguns.HandmadeGunsCore;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import java.util.Locale;

/** Presentation classification; never changes legacy gun type or gameplay defaults. */
public enum WeaponClass {
    ASSAULT_RIFLE("Assault Rifles"), BATTLE_RIFLE("Battle Rifles"), CARBINE("Carbines"),
    SUBMACHINE_GUN("Submachine Guns"), MACHINE_GUN("Machine Guns"), SHOTGUN("Shotguns"),
    SNIPER_RIFLE("Sniper Rifles"), BOLT_ACTION_RIFLE("Bolt-action Rifles"), MARKSMAN_RIFLE("Marksman Rifles"),
    HANDGUN("Handguns"), LAUNCHER("Launchers"), SPECIAL("Other / Special Weapons");

    public final String label;
    private CreativeTabs tab;
    private Item icon;

    WeaponClass(String label) { this.label = label; }

    public static WeaponClass parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'));
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
