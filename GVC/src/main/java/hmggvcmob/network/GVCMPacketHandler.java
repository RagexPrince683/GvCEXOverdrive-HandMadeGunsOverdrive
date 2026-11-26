package hmggvcmob.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class GVCMPacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("GVCMR");

    public static void init() {
        INSTANCE.registerMessage(GVCXMMessageGKdata.class, GVCXMPacketSyncGKdata.class, 1, Side.CLIENT);
        // ID 2 was the flag broadcast. Chunk watchers now receive vanilla tile updates.
        // IDs 3 and 4 are reserved for the deferred MGAX55 control protocol.
        INSTANCE.registerMessage(GVCPacket_PlatoonInfoSync.class, GVCPacket_PlatoonInfoSync.class, 5, Side.CLIENT);
    }
}
