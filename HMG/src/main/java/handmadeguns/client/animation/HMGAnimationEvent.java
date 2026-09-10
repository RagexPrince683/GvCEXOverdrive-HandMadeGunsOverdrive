package handmadeguns.client.animation;

import cpw.mods.fml.common.eventhandler.Event;
import handmadeguns.animation.AnimationEvent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

/** Client Forge-bus notification. Consumers must be presentation-only; no gun/world gameplay mutations. */
public final class HMGAnimationEvent extends Event {
    public final ItemStack stack;
    public final Entity owner;
    public final IItemRenderer.ItemRenderType context;
    public final String source, clip;
    public final AnimationEvent marker;
    public final long generation, cycle;

    HMGAnimationEvent(ItemStack stack, Entity owner, IItemRenderer.ItemRenderType context, String source,
                      String clip, AnimationEvent marker, long generation, long cycle) {
        this.stack = stack; this.owner = owner; this.context = context; this.source = source;
        this.clip = clip; this.marker = marker; this.generation = generation; this.cycle = cycle;
    }
}
