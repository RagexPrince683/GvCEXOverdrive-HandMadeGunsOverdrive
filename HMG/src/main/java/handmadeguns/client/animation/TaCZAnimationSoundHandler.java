package handmadeguns.client.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.IItemRenderer;

import static handmadeguns.HandmadeGunsCore.HMG_proxy;

/** Plays presentation-only TaCZ mechanical markers without changing HMG gun sound authority. */
public final class TaCZAnimationSoundHandler {
    @SubscribeEvent public void onAnimationEvent(HMGAnimationEvent event) {
        if (!"bedrock_sound".equals(event.marker.name)
                || isFireClip(event.clip)
                || event.context != IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON
                || event.owner == null || event.owner != Minecraft.getMinecraft().thePlayer
                || event.stack == null || !(event.stack.getItem() instanceof HMGItem_Unified_Guns)) return;
        HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)event.stack.getItem();
        if (!gun.gunInfo.animationEventSounds) return;
        String effect = effect(event.marker.data);
        if (effect == null) return;
        int separator = effect.indexOf(':');
        if (separator <= 0 || separator == effect.length() - 1) return;
        String namespace = effect.substring(0, separator);
        String path = effect.substring(separator + 1);
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-z0-9_./-]+")) return;
        HMG_proxy.playsoundatEntity_reload("handmadeguns:" + namespace + "/" + path,
                1.0F, 1.0F, event.owner, false);
    }

    private static boolean isFireClip(String clip) {
        return clip != null && clip.matches("(^|.*_)(shoot|fire)($|_.*)");
    }

    private static String effect(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            JsonElement parsed = new JsonParser().parse(data);
            if (!parsed.isJsonObject()) return null;
            JsonObject object = parsed.getAsJsonObject();
            JsonElement effect = object.get("effect");
            return effect != null && effect.isJsonPrimitive() ? effect.getAsString() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
