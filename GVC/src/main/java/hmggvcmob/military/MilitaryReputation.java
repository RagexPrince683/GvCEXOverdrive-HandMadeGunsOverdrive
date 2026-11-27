package hmggvcmob.military;

import handmadeguns.entity.PlacedGunEntity;
import handmadevehicle.entity.EntityVehicle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

final class MilitaryReputation {
    private static final int MAX_PARTICIPANTS = 8;
    private static final Map<Entity, Combat> COMBAT = new WeakHashMap<>();
    private static final Set<UUID> PENDING = new LinkedHashSet<>();
    private static final Map<String, Double> VALUES = new HashMap<>();

    private MilitaryReputation() {}

    static void configure() {
        VALUES.clear();
        for (String entry : MilitaryConfig.classificationValues) {
            int separator = entry.indexOf('=');
            if (separator <= 0) continue;
            try {
                double value = Double.parseDouble(entry.substring(separator + 1).trim());
                if (value >= 0 && value <= 10) VALUES.put(entry.substring(0, separator).trim(), value);
            } catch (NumberFormatException ignored) {
                // An invalid override leaves the classification at its base value.
            }
        }
    }

    static void hurt(EntityLivingBase victim, DamageSource damage) {
        if (!MilitaryRelations.isFaction(MilitaryRelations.faction(victim))) return;
        Entity attacker = damage.getEntity();
        UUID player = responsible(damage);
        if (attacker == null && player == null) return;
        Combat combat = COMBAT.computeIfAbsent(victim, ignored -> new Combat());
        long now = victim.worldObj.getTotalWorldTime();
        if (attacker instanceof EntityLivingBase && attacker != victim && MilitaryConfig.retaliationTicks > 0
                && !(MilitaryRelations.owner(victim) != null && MilitaryRelations.owner(victim).equals(
                        attacker instanceof EntityPlayer ? attacker.getUniqueID() : MilitaryRelations.owner(attacker)))) {
            combat.attacker = attacker.getUniqueID();
            combat.retaliateUntil = now + MilitaryConfig.retaliationTicks;
        }
        if (player != null && (MilitaryConfig.attributeIndirect || MilitaryConfig.shareReputation)) {
            combat.participants.remove(player);
            combat.participants.put(player, now);
            if (combat.participants.size() > MAX_PARTICIPANTS) {
                Iterator<UUID> entries = combat.participants.keySet().iterator();
                entries.next();
                entries.remove();
            }
        }
    }

    static boolean retaliating(Entity victim, Entity attacker) {
        if (victim.worldObj.isRemote) return false;
        Combat combat = COMBAT.get(victim);
        return combat != null && attacker.getUniqueID().equals(combat.attacker)
                && victim.worldObj.getTotalWorldTime() < combat.retaliateUntil;
    }

    static void death(EntityLivingBase victim, DamageSource damage) {
        String defeated = MilitaryRelations.faction(victim);
        if (!MilitaryRelations.isFaction(defeated)) return;
        Combat combat = COMBAT.computeIfAbsent(victim, ignored -> new Combat());
        if (combat.credited) return;
        combat.credited = true;
        UUID killer = responsible(damage);
        long cutoff = victim.worldObj.getTotalWorldTime() - MilitaryConfig.assistTicks;
        if (killer == null && MilitaryConfig.attributeIndirect && damage.getEntity() == null) {
            for (Map.Entry<UUID, Long> participant : combat.participants.entrySet()) {
                if (participant.getValue() >= cutoff) killer = participant.getKey();
            }
        }
        if (killer == null) return;
        apply(killer, victim, defeated, 1);
        if (MilitaryConfig.shareReputation) {
            for (Map.Entry<UUID, Long> participant : combat.participants.entrySet()) {
                if (!killer.equals(participant.getKey()) && participant.getValue() >= cutoff) {
                    apply(participant.getKey(), victim, defeated, .5);
                }
            }
        }
    }

    private static UUID responsible(DamageSource damage) {
        Entity source = damage.getEntity();
        if (source instanceof EntityVehicle) {
            if (!MilitaryConfig.attributeVehicle) return null;
            Entity[] seats = ((EntityVehicle) source).getBaseLogic().getRiddenEntityList();
            source = seats.length == 0 ? null : seats[0];
        } else if (source instanceof PlacedGunEntity) {
            if (!MilitaryConfig.attributeVehicle) return null;
            source = source.riddenByEntity;
        }
        if (source instanceof EntityPlayer) {
            boolean mounted = source.ridingEntity instanceof PlacedGunEntity || source.ridingEntity instanceof EntityVehicle
                    || source.ridingEntity instanceof handmadevehicle.entity.EntityDummy_rider;
            return (mounted ? MilitaryConfig.attributeVehicle : MilitaryConfig.attributePlayer) ? source.getUniqueID() : null;
        }
        if (!(source instanceof EntityMilitaryUnit)) return null;
        EntityMilitaryUnit unit = (EntityMilitaryUnit) source;
        if (unit.ridingEntity != null && !MilitaryConfig.attributeVehicle) return null;
        boolean squad = unit.getPlatoon() != null && (unit.getPlatoon().managedOwner() != null || unit.getPlatoon().size() > 1);
        return (squad ? MilitaryConfig.attributeSquad : MilitaryConfig.attributeHired) ? unit.owner() : null;
    }

    private static void apply(UUID player, EntityLivingBase victim, String faction, double participation) {
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player);
        UnitPresets.Preset preset = victim instanceof EntityMilitaryUnit ? ((EntityMilitaryUnit) victim).preset() : null;
        double importance = preset == null ? victim instanceof EntityMilitaryTrader ? 1.5 : 1
                : (1 + (preset.tier - 1) * .25 + preset.rank * .35) * VALUES.getOrDefault(preset.classification, 1.0);
        int baseLoss = MilitaryConfig.reputationLoss;
        if (faction.equals(account.allegiance)) baseLoss += MilitaryConfig.ownFactionPenalty;
        adjust(account, faction, -points(baseLoss * importance * participation * MilitaryRelations.definition(faction).lossMultiplier));
        if (MilitaryConfig.reputationPerKill == 0 || MilitaryRelations.opponents(faction).isEmpty()
                || !MilitaryConfig.rewardOwned && MilitaryRelations.owner(victim) != null
                || !MilitaryConfig.rewardTraders && victim instanceof EntityMilitaryTrader
                || !MilitaryConfig.rewardReinforcements && victim instanceof EntityMilitaryUnit
                    && ((EntityMilitaryUnit) victim).isReinforcement()) return;
        long now = MilitaryEconomy.now();
        if (now - account.reputationWindow >= MilitaryConfig.reputationWindow || now < account.reputationWindow) {
            account.reputationWindow = now;
            account.creditedKills = 0;
        }
        if (MilitaryConfig.reputationKillLimit > 0 && account.creditedKills >= MilitaryConfig.reputationKillLimit) return;
        account.creditedKills++;
        economy.markDirty();
        for (String opponent : MilitaryRelations.opponents(faction)) {
            if (opponent.equals(account.allegiance) && player.equals(MilitaryRelations.owner(victim))) continue;
            adjust(account, opponent, points(MilitaryConfig.reputationPerKill * importance * participation
                    * MilitaryRelations.definition(opponent).gainMultiplier));
        }
    }

    private static int points(double value) {
        return value <= 0 ? 0 : Math.max(1, (int) Math.round(value));
    }

    static void adjust(MilitaryEconomy.Account account, String faction, int delta) {
        if (delta == 0) return;
        int old = MilitaryRelations.reputation(account.owner, faction);
        int score = MilitaryRelations.clamp(old + delta);
        if (score == old) return;
        MilitaryRelations.setReputation(account.owner, faction, score);
        if (MilitaryConfig.reputationNotices) {
            account.notices.merge(faction, score - old, Integer::sum);
            PENDING.add(account.owner);
        }
    }

    static void sendNotices(long now) {
        if (PENDING.isEmpty()) return;
        Iterator<UUID> pending = PENDING.iterator();
        while (pending.hasNext()) {
            UUID id = pending.next();
            MilitaryEconomy.Account account = MilitaryEconomy.get().findAccount(id);
            if (account == null) {
                pending.remove();
                continue;
            }
            if (now < account.nextNotice) continue;
            for (Object entry : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                EntityPlayerMP player = (EntityPlayerMP) entry;
                if (!id.equals(player.getUniqueID())) continue;
                for (Map.Entry<String, Integer> notice : account.notices.entrySet()) {
                    int delta = notice.getValue();
                    if (delta == 0) continue;
                    String faction = notice.getKey();
                    MilitaryRelations.Faction definition = MilitaryRelations.definition(faction);
                    int current = MilitaryRelations.reputation(id, faction);
                    int previous = current - delta;
                    player.addChatComponentMessage(new ChatComponentTranslation(delta > 0 ? "gvc.faction.gain" : "gvc.faction.loss",
                            definition.name, Math.abs(delta), current));
                    int currentTier = MilitaryRelations.recruitmentTier(faction, current);
                    if (MilitaryRelations.recruitmentTier(faction, previous) < currentTier) {
                        player.addChatComponentMessage(new ChatComponentTranslation("gvc.faction.unlock",
                                definition.name, currentTier));
                    }
                    int threshold = Math.max(definition.allegianceRequired, definition.hostileBelow);
                    if (definition.pledging && previous < threshold && current >= threshold) {
                        player.addChatComponentMessage(new ChatComponentTranslation("gvc.faction.pledge_available", definition.name));
                    }
                }
                break;
            }
            account.notices.clear();
            account.nextNotice = now + MilitaryConfig.noticeTicks;
            pending.remove();
        }
    }

    static void stop() {
        COMBAT.clear();
        PENDING.clear();
    }

    private static final class Combat {
        UUID attacker;
        long retaliateUntil;
        boolean credited;
        final Map<UUID, Long> participants = new LinkedHashMap<>();
    }
}
