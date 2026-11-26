package hmggvcmob.entity.util;

import handmadevehicle.entity.parts.Modes;

/** IDs 0 through 4 match the original GVC save data and status packets. */
public enum PlatoonOrder {
    Wait(0, Modes.Wait),
    Follow(1, Modes.Follow),
    Go(2, Modes.Go),
    Land(3, Modes.Land),
    Attack(4, Modes.Attack),
    Patrol(5, Modes.Go),
    Defend(6, Modes.Wait);

    private static final PlatoonOrder[] ORDERS = values();
    public final int id;
    public final Modes legacyMode;

    PlatoonOrder(int id, Modes legacyMode) {
        this.id = id;
        this.legacyMode = legacyMode;
    }

    public static PlatoonOrder fromId(int id) {
        for (PlatoonOrder order : ORDERS) {
            if (order.id == id) return order;
        }
        return null;
    }

    public static PlatoonOrder fromLegacy(Modes mode) {
        for (PlatoonOrder order : ORDERS) {
            if (order.legacyMode == mode) return order;
        }
        return Wait;
    }
}
