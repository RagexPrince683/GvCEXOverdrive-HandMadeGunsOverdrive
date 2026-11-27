
package hmggvcmob.military;

import hmggvcmob.GVCMobPlus;
import hmggvcmob.camp.CampObj;
import hmggvcmob.entity.IflagBattler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class MilitaryRelations {
    private static final List<String> FACTIONS = java.util.Collections.unmodifiableList(Arrays.asList("union", "allies", "pmc"));
    private static final int[][] DEFAULT_RELATIONS = {{100, -50, 30}, {-50, 100, -50}, {30, -50, 100}};
    private static final int[][] relations = new int[3][3];

    private MilitaryRelations() {}

    static void load(String[] entries) {
        for (int i = 0; i < relations.length; i++) System.arraycopy(DEFAULT_RELATIONS[i], 0, relations[i], 0, relations.length);
        for (String entry : entries) {
            String[] fields = entry.trim().split("[=,]");
            if (fields.length != 3) continue;
            int first = FACTIONS.indexOf(fields[0].trim());
            int second = FACTIONS.indexOf(fields[1].trim());
            if (first < 0 || second < 0 || first == second) continue;
            try {
                relations[first][second] = relations[second][first] = clamp(Integer.parseInt(fields[2].trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    public static List<String> factions() {
        return FACTIONS;
    }

    static boolean isFaction(String faction) {
        return FACTIONS.contains(faction);
    }

    public static String faction(CampObj camp) {
        if (camp == GVCMobPlus.guerrillas) return "allies";
        if (camp == GVCMobPlus.soldiers) return "union";
        return "pmc";
    }

    public static CampObj camp(String faction) {
        if ("allies".equals(faction)) return GVCMobPlus.guerrillas;
        if ("union".equals(faction)) return GVCMobPlus.soldiers;
        return GVCMobPlus.forPlayer;
    }

    public static String faction(Entity entity) {
        if (entity instanceof IflagBattler) return faction(((IflagBattler) entity).getCampObj());
        if (entity instanceof EntityMilitaryTrader) return ((EntityMilitaryTrader)entity).faction();
        return null;
    }

    public static int factionRelation(String first, String second) {
        int left = FACTIONS.indexOf(first);
        int right = FACTIONS.indexOf(second);
        return left < 0 || right < 0 ? 0 : relations[left][right];
    }

    public static int reputation(UUID player, String faction) {
        if (player == null || !isFaction(faction)) throw new IllegalArgumentException("Player and known faction required");
        return MilitaryEconomy.get().account(player).relations.getOrDefault(faction, defaultReputation(faction));
    }

    public static void setReputation(UUID player, String faction, int score) {
        if (player == null || !isFaction(faction)) throw new IllegalArgumentException("Player and known faction required");
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player);
        account.relations.put(faction, clamp(score));
        economy.changed(account);
    }

    public static int playerRelation(EntityPlayer player, String faction) {
        return player.worldObj.isRemote ? defaultReputation(faction) : reputation(player.getUniqueID(), faction);
    }

    public static boolean hostile(Entity source, Entity other) {
        String faction = faction(source);
        if (faction == null || other == null) return false;
        UUID owner = source instanceof EntityMilitaryUnit ? ((EntityMilitaryUnit)source).owner() : null;
        if (other instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)other;
            return !player.getUniqueID().equals(owner) && playerRelation(player, faction) < 0;
        }
        String opposing = faction(other);
        if (opposing == null) return false;
        UUID otherOwner = other instanceof EntityMilitaryUnit ? ((EntityMilitaryUnit)other).owner() : null;
        if (owner != null && owner.equals(otherOwner)) return false;
        if (faction.equals(opposing)) return false;
        if (owner != null && otherOwner == null && !source.worldObj.isRemote) return reputation(owner, opposing) < 0;
        if (owner == null && otherOwner != null && !source.worldObj.isRemote) return reputation(otherOwner, faction) < 0;
        return factionRelation(faction, opposing) < 0;
    }

    static int clamp(int score) {
        return Math.max(-100, Math.min(100, score));
    }

    private static int defaultReputation(String faction) {
        return "allies".equals(faction) ? -50 : 10;
    }
}
