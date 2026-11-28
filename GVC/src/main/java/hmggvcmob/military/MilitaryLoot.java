package hmggvcmob.military;

import handmadeguns.items.guns.HMGItem_Unified_Guns;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import java.util.Random;
import java.util.UUID;

final class MilitaryLoot {
    private MilitaryLoot() {}

    static void drop(LivingDropsEvent event, EntityMilitaryUnit unit) {
        event.drops.clear();
        UUID player = MilitaryReputation.responsible(event.source);
        if (player == null || unit.owner() != null || !MilitaryRelations.hostileToPlayer(player, unit.faction())) return;
        WorldSavedData_Flag data = WorldSavedData_Flag.get(unit.worldObj);
        Occupation post = unit.homeSite() == null ? null : data.byId(unit.homeSite());
        long now = unit.worldObj.getTotalWorldTime();
        if (post != null && now < post.nextSpoils) return;
        UnitPresets.Preset preset = unit.preset();
        int tier = preset == null ? 1 : preset.tier;
        Random random = unit.getRNG();
        ItemStack held = unit.getHeldItem();
        if (held != null && held.getItem() instanceof HMGItem_Unified_Guns) {
            HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)held.getItem();
            if (MilitaryConfig.weaponLoot > 0 && random.nextInt(100) < MilitaryConfig.weaponLoot) add(event, unit, held.copy());
            if (random.nextInt(100) < MilitaryConfig.ammunitionLoot) {
                Item ammunition = gun.getcurrentMagazine(held.copy());
                if (ammunition != null) {
                    ItemStack stack = new ItemStack(ammunition);
                    stack.stackSize = Math.min(stack.getMaxStackSize(), 1 + random.nextInt(1 + tier));
                    add(event, unit, stack);
                }
            }
        }
        MilitaryRelations.Faction faction = MilitaryRelations.definition(unit.faction());
        if (faction != null && faction.loot.length > 0 && random.nextInt(100) < MilitaryConfig.supplyLoot) {
            add(event, unit, UnitPresets.Preset.item(faction.loot[random.nextInt(faction.loot.length)]));
        }
        if (MilitaryConfig.hostileCoinDrop > 0 && random.nextInt(100) < Math.min(85, 45 + tier * 10)) {
            add(event, unit, new ItemStack(MilitarySupplies.coinItem(0), 1 + random.nextInt(MilitaryConfig.hostileCoinDrop)));
        }
        if (post != null && !event.drops.isEmpty()) {
            post.nextSpoils = now + MilitaryConfig.reinforcementLootCooldown;
            data.markDirty();
        }
    }

    private static void add(LivingDropsEvent event, EntityMilitaryUnit unit, ItemStack stack) {
        if (stack == null) return;
        EntityItem drop = new EntityItem(unit.worldObj, unit.posX, unit.posY + .25, unit.posZ, stack);
        drop.delayBeforeCanPickup = 10;
        event.drops.add(drop);
    }
}
