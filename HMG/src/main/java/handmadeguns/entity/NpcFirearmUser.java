
package handmadeguns.entity;

import net.minecraft.item.ItemStack;

/** Optional server-side firearm proficiency for an NPC. */
public interface NpcFirearmUser {
    float dispersionMultiplier(ItemStack weapon);
    float recoilMultiplier(ItemStack weapon);
    float reloadTimeMultiplier(ItemStack weapon);
}
