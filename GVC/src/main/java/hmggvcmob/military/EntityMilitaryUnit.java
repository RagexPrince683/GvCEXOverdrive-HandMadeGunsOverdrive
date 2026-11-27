
package hmggvcmob.military;

import handmadeguns.entity.NpcFirearmUser;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import hmggvcmob.camp.CampObj;
import hmggvcmob.entity.friend.GVCEntityPMC;
import hmggvcmob.entity.util.PlatoonOBJ;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.UUID;

public class EntityMilitaryUnit extends GVCEntityPMC implements NpcFirearmUser {
    private static final int FACTION = 29;
    private static final UUID HUNGER = UUID.fromString("3474f71d-1f8f-478a-aba9-f28d24025dff");
    private UUID owner, post;
    private int tenure;
    private String presetId = "";
    private UnitPresets.Preset preset;
    private ItemStack stowedWeapon;
    private boolean holdingSecondary, shortSupply, checkRegistration = true;

    public EntityMilitaryUnit(World world) {
        super(world);
        canuseAlreadyPlacedGun = false;
        setCanPickUpLoot(false);
        for (int i = 0; i <= 4; i++) setEquipmentDropChance(i, 0);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(FACTION, "pmc");
    }

    public void configure(UnitPresets.Preset definition, UUID owner, Occupation post) {
        this.preset = definition;
        this.presetId = definition.id;
        this.owner = owner;
        this.post = post == null ? null : post.id;
        this.tenure = post == null ? 0 : post.tenure;
        dataWatcher.updateObject(FACTION, definition.faction);
        setCustomNameTag(definition.name);
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(definition.health);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(movespeed = definition.speed);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(definition.range * 1.25);
        setHealth(definition.health);
        definition.equipArmor(this);
        ItemStack primary = definition.primary(getRNG());
        stowedWeapon = definition.secondary(getRNG());
        ItemStack ammunition = UnitPresets.Preset.item(definition.ammunition);
        readyWeapon(primary, ammunition);
        readyWeapon(stowedWeapon, ammunition);
        setCurrentItemOrArmor(0, primary);
        if (primary == null && stowedWeapon != null) useSecondary(true);
    }

    private void readyWeapon(ItemStack weapon, ItemStack ammunition) {
        if (weapon == null) return;
        HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns) weapon.getItem();
        gun.prepareNpcWeapon(weapon, worldObj, ammunition == null ? null : ammunition.getItem());
    }

    public UUID owner() {
        return owner;
    }

    public String faction() {
        return dataWatcher.getWatchableObjectString(FACTION);
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void configurePreview(String faction, ItemStack weapon) {
        dataWatcher.updateObject(FACTION, faction);
        setCurrentItemOrArmor(0, weapon);
    }

    public UnitPresets.Preset preset() {
        return preset;
    }

    boolean isReinforcement() { return post != null; }

    @Override
    public boolean canBeCommandedBy(EntityPlayer player) {
        return player.getUniqueID().equals(owner);
    }

    @Override
    public boolean canAutoEnlist() {
        return false;
    }

    @Override
    public int commandRank() {
        return preset == null ? 0 : preset.rank;
    }

    @Override
    public int commandSize() {
        return preset == null || preset.rank == 0 ? super.commandSize() : preset.capacity + 1;
    }

    @Override
    public CampObj getCampObj() {
        return MilitaryRelations.camp(faction());
    }

    @Override
    public boolean isThisAttackAbleCamp(CampObj camp) {
        return MilitaryRelations.factionRelation(faction(), MilitaryRelations.faction(camp)) < 0;
    }

    @Override
    public boolean isThisFriendCamp(CampObj camp) {
        return MilitaryRelations.factionRelation(faction(), MilitaryRelations.faction(camp)) > 0;
    }

    @Override
    public boolean is_this_entity_friend(Entity other) {
        if (other instanceof EntityPlayer || MilitaryRelations.faction(other) != null) return !MilitaryRelations.hostile(this, other);
        return MilitaryRelations.factionRelation(faction(), "union") < 0 ? false : super.is_this_entity_friend(other);
    }

    public boolean mayEngage(EntityLivingBase target) {
        Occupation home = post == null ? null : hmggvcmob.world.WorldSavedData_Flag.get(worldObj).byId(post);
        return home == null || home.contains(target.posX, target.posZ);
    }

    public void setSupplyPenalty(boolean penalty, boolean hungry) {
        shortSupply = penalty;
        net.minecraft.entity.ai.attributes.IAttributeInstance speed = getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        AttributeModifier applied = speed.getModifier(HUNGER);
        if (hungry && applied == null) speed.applyModifier(new AttributeModifier(HUNGER, "Military rations", -.18, 2).setSaved(false));
        else if (!hungry && applied != null) speed.removeModifier(applied);
    }

    public boolean hasSupplyPenalty() {
        return shortSupply;
    }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + (preset == null ? 0 : preset.armor));
    }

    @Override
    protected void damageEntity(DamageSource source, float amount) {
        super.damageEntity(source, preset == null || source.isDamageAbsolute() ? amount : amount * (1F - preset.resistance));
    }

    @Override
    public float dispersionMultiplier(ItemStack weapon) {
        return (preset == null ? 1 : preset.dispersion) * (shortSupply ? 1.35F : 1);
    }

    @Override
    public float recoilMultiplier(ItemStack weapon) {
        return preset == null ? 1 : preset.recoil;
    }

    @Override
    public float reloadTimeMultiplier(ItemStack weapon) {
        return (preset == null ? 1 : preset.reload) * (shortSupply ? 1.2F : 1);
    }

    @Override
    public boolean interact(EntityPlayer player) {
        if (!canBeCommandedBy(player)) return false;
        return super.interact(player);
    }

    @Override
    public void onLivingUpdate() {
        if (!worldObj.isRemote) {
            if (checkRegistration) {
                checkRegistration = false;
                if (owner != null && !MilitaryEconomy.get().attach(this)) {
                    setDead();
                    return;
                }
            }
            if ((ticksExisted + getEntityId()) % 20 == 0) {
                if (owner != null) MilitaryEconomy.get().updateAssignment(this);
                if (post != null) {
                    Occupation home = hmggvcmob.world.WorldSavedData_Flag.get(worldObj).byId(post);
                    if (home == null || home.tenure != tenure) {
                        setDead();
                        return;
                    }
                }
                selectWeapon();
            }
        }
        super.onLivingUpdate();
    }

    private void selectWeapon() {
        if (stowedWeapon == null || getHeldItem() == null || preset == null) return;
        EntityLivingBase target = getAttackTarget();
        boolean secondary = target != null && "anti_armor".equals(preset.behavior)
                && (getDistanceSqToEntity(target) < 15 * 15 || target.width < 1.2F && target.height < 2.6F);
        if (secondary != holdingSecondary) useSecondary(secondary);
    }

    private void useSecondary(boolean selected) {
        ItemStack replacement = stowedWeapon;
        stowedWeapon = getHeldItem();
        setCurrentItemOrArmor(0, replacement);
        holdingSecondary = selected;
    }

    @Override
    public void setDead() {
        if (!worldObj.isRemote && owner != null) MilitaryEconomy.get().lost(this);
        super.setDead();
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        owner = MilitaryEconomy.parseUuid(tag.getString("MilitaryOwner"));
        post = MilitaryEconomy.parseUuid(tag.getString("MilitaryPost"));
        tenure = tag.getInteger("MilitaryTenure");
        presetId = tag.getString("MilitaryPreset");
        preset = UnitPresets.get(presetId);
        String faction = tag.getString("MilitaryFaction");
        if (MilitaryRelations.validId(faction)) dataWatcher.updateObject(FACTION, faction);
        if (preset != null) movespeed = preset.speed;
        holdingSecondary = tag.getBoolean("SecondaryActive");
        if (tag.hasKey("StowedWeapon")) stowedWeapon = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("StowedWeapon"));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (owner != null) tag.setString("MilitaryOwner", owner.toString());
        if (post != null) tag.setString("MilitaryPost", post.toString());
        tag.setInteger("MilitaryTenure", tenure);
        tag.setString("MilitaryPreset", presetId);
        tag.setString("MilitaryFaction", faction());
        tag.setBoolean("SecondaryActive", holdingSecondary);
        if (stowedWeapon != null) tag.setTag("StowedWeapon", stowedWeapon.writeToNBT(new NBTTagCompound()));
    }
}
