package handmadeguns.client.animation;

import handmadeguns.animation.AnimationPose;
import handmadeguns.client.render.GunState;
import handmadeguns.client.render.HMGGunParts;
import handmadeguns.client.render.HMGGunParts_Motion_PosAndRotation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compatibility evaluator: retains HMG's state precedence, units, interval and null semantics. */
public final class LegacyMotionAdapter {
    private LegacyMotionAdapter() { }

    public static AnimationPose sample(List<HMGGunParts> parts, Set<String> targets, GunState[] states, float time) {
        Map<String, AnimationPose.Transform> result = new LinkedHashMap<String, AnimationPose.Transform>();
        collect(parts, targets, states, time, result);
        return new AnimationPose(result);
    }

    private static void collect(List<HMGGunParts> parts, Set<String> targets, GunState[] states, float time,
                                Map<String, AnimationPose.Transform> result) {
        for (HMGGunParts part : parts) {
            GunState selected = null;
            for (GunState state : states) if (enabled(part, state)) { selected = state; break; }
            if (selected != null && targets.contains(part.partsname)) {
                HMGGunParts_Motion_PosAndRotation value = sample(part, selected, time);
                // Legacy interpolation returns a GLOBAL scratch object. Copy before any child/other sample.
                if (value != null) result.put(part.partsname, new AnimationPose.Transform(
                        value.posX, value.posY, value.posZ, value.rotationX, value.rotationY, value.rotationZ));
            }
            // HMG propagates the selected parent state, not the original fallback list, to children.
            if (selected != null) collect(part.childs, targets, new GunState[]{selected}, time, result);
            if (selected != null && part.reticleChild != null)
                collect(part.reticleChild, targets, new GunState[]{selected}, time, result);
        }
    }

    private static boolean enabled(HMGGunParts part, GunState state) {
        switch (state) {
            case Default: return part.rendering_Def;
            case ADS: return part.rendering_Ads;
            case Recoil: return part.rendering_Recoil;
            case Cock: return part.rendering_Cock;
            case Reload: return part.rendering_Reload;
            default: return false;
        }
    }

    private static HMGGunParts_Motion_PosAndRotation sample(HMGGunParts part, GunState state, float time) {
        switch (state) {
            case Default: return part.getRenderinf_None();
            case ADS: return part.getRenderinfOfADS();
            case Recoil: return part.hasMotionRecoil ? part.getRecoilmotion(time) : part.getRenderinfOfRecoil();
            case Cock: return part.hasMotionCock ? part.getcockmotion(time) : part.getRenderinfOfCock();
            case Reload: return part.hasMotionReload ? part.getReloadmotion(time) : part.getRenderinfOfReload();
            default: return null;
        }
    }

    public static HMGGunParts_Motion_PosAndRotation apply(AnimationPose pose, String part,
                                                         HMGGunParts_Motion_PosAndRotation legacy) {
        AnimationPose.Transform value = pose.parts.get(part);
        if (value == null || legacy == null) return legacy;
        HMGGunParts_Motion_PosAndRotation result = new HMGGunParts_Motion_PosAndRotation(
                value.x, value.y, value.z, value.rx, value.ry, value.rz);
        result.renderOnOff = legacy.renderOnOff;
        result.rotateVec = legacy.rotateVec;
        return result;
    }
}
