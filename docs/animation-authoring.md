# Optional named animations (Phase A)

Minecraft Forge 1.7.10 / Java 8. This adds a clip/controller layer over HMG's existing rigid gun parts. It does not change gun gameplay or replace OBJ/MQO meshes, VBOs, display lists, or Angelica compatibility.

## Setup

Put a UTF-8 JSON file inside your content pack:

```text
handmadeguns_Packs/MyPack/
  guns/mygun.txt
  animations/mygun.json
```

Before the gun's final `Unified_guns,...` registration line in its existing TXT file, add:

```text
Animations,animations/mygun.json
```

The directive is case-sensitive and uses the existing comma syntax (not `=`). Paths are relative to the pack root, also for the legacy `mods/handmadeguns/addgun/<Pack>` layout. Paths cannot escape the pack. Keep animation files outside `guns/` so the TXT scanner does not treat them as gun definitions. Animation parsing is client-only; servers need no animation resources.

No directive means the original animation path, unchanged. Missing files, invalid JSON, unsupported versions, or unknown parts disable the new definition for that gun and log a diagnostic; other guns continue loading. Remove the directive and reload to return to legacy playback.

## First moving bolt

The gun must already declare an HMG part named `bolt` using its existing part definitions. JSON does not create mesh groups or part hierarchy. Use the part's `partsname`, not the gun/item name. See the existing `AddParts`/`AddChildParts` definitions in your pack. Names are case-sensitive; multiple HMG instances with the same part name receive the same local track. The part's existing state visibility, attachment conditions, magazine conditions, pivot and parent remain in effect.

For a development model that actually contains separate groups named `bolt` and `magazine`, these existing TXT directives establish two ordinary parts visible in the normal HMG states:

```text
AddParts,bolt
SetAsNormalParts
AddParts,magazine
SetAsNormalParts
Animations,animations/mygun.json
```

Place them before gun registration. Do not duplicate parts that your gun already declares. Translation-only bolt/magazine motion needs no pivot adjustment; rotating around a hinge uses the part's existing HMG rotation-center definition.

```json
{
  "formatVersion": 1,
  "clips": {
    "fire": {
      "duration": 0.15,
      "transition": {"in": 0.02, "out": 0.05},
      "parts": {
        "bolt": [
          {"time": 0, "position": [0, 0, 0]},
          {"time": 0.04, "position": [0, 0, -0.4]},
          {"time": 0.15, "position": [0, 0, 0]}
        ]
      },
      "events": [{"time": 0.04, "event": "shell_eject"}]
    }
  }
}
```

`fire` is an additive layer: these values add to the underlying pose. If the same part already has legacy recoil motion, that motion also contributes. Author one source of recoil displacement, or provide a BASE `idle` track for that part to establish the intended underlying pose. A BASE track takes precedence over the legacy state transform for that named part. Untracked parts continue to use legacy state motion.

The [complete example](examples/animations/foundation.json) includes `idle`, `fire`, `reload`, and `inspect`, bolt and magazine tracks, fades, and presentation markers. It is an opt-in development example, not installed on any production gun. Copy it into a test pack with matching part names (or rename its tracks). Its displacement scale and 2.4-second reload are illustrative; choose values and durations appropriate to your gun.

## Format version 1

| Field | Meaning / default |
| --- | --- |
| `formatVersion` | Required integer `1`. |
| `clips` | Required object keyed by arbitrary, nonempty clip names. |
| clip `duration` | Required finite, non-negative seconds. |
| clip `loop` | `false` (default): finish and fade out; `true`: repeat; `"hold"`: stay on the final pose. Zero duration is valid for once/hold, invalid for repeat. |
| clip `parts` | Optional object: HMG part name to a nonempty keyframe array. |
| keyframe `time` | Required seconds within duration, strictly increasing in each track. |
| keyframe `position` | Three finite float values `[x,y,z]`; omitted means zero. |
| keyframe `rotation` | Three finite float values `[x,y,z]`; omitted means zero. |
| clip `events` | Optional array of `{ "time": seconds, "event": "name", "data": "optional text" }`. |
| clip `transition` | Optional `{ "in": seconds, "out": seconds }`; each defaults to `0.1`. Zero explicitly disables that fade. |
| clip `priority` | Integer, default `0`; higher/equal can replace an interruptible clip on the same layer. |
| clip `interruptible` | Boolean, default `true`; blocks ordinary replacement requests when false. Owner cancellation still works. |

Unknown fields are ignored, allowing author metadata and future optional extensions. Required fields and known optional fields must have valid types. JSON comments, trailing commas, duplicate object keys, NaN, infinity, and numeric strings are not accepted. Keyframe/event indices in diagnostics are zero-based. Simultaneous event times are valid and preserve array order.

## Coordinates and interpolation

- **Time:** seconds of client simulation time, derived from completed client ticks plus Minecraft render partial ticks at 20 ticks/second. No 60-FPS assumption. Pausing an integrated game pauses the animation clock. Server/world simulation stalls are not replaced with wall-clock animation time.
- **Position:** existing HMG part-local model units, multiplied by the existing `gunPartsScale`. There is no automatic pixels-to-blocks or Blockbench conversion. `+X/+Y/+Z` are the same mesh-local axes used by legacy HMG motion, transformed by the parent and the existing model/world/first-person matrices. Mirrored model setup can change their apparent screen direction.
- **Rotation:** degrees, linear Euler components, without shortest-arc wrapping. For example 350 to 10 passes through 180. Author 350 to 370 for a positive 20-degree turn.
- **Transform order:** existing default-offset transform first; then animated translation; then translation to the existing rotation center; Y, X, Z rotation calls; then inverse center translation. In OpenGL notation the animated matrix is `T(position * scale) T(pivot * scale) Ry Rx Rz T(-pivot * scale)`. Matrix multiplication applies the rightmost operation to vertices first. Attachment-presence translations and existing selector/elevation transforms remain in their original locations.
- **Vector-axis parts:** legacy `rotateTypeIsVector` remains supported: `rotation[0]` is the angle about the existing configured axis; components 1 and 2 are unused. JSON does not redefine pivots or axes.
- **Sampling:** deterministic componentwise linear interpolation; hold the first key before its timestamp and the last key afterward. No spline overshoot, easing, quaternion, scale, or visibility channels in version 1.
- **Hierarchy:** child animation remains local to its existing parent; parent movement also moves children and anchored attachments. JSON cannot reveal a part hidden by legacy state/attachment conditions.

Legacy TXT time scales, recoil frames, cock/reload tick units, angle wrapping choices, render flags and motion interval boundaries are not reinterpreted. `LegacyMotionAdapter` calls the existing evaluators, copies their shared scratch output immediately, and contributes transforms to the same pose mixer. Guns without JSON bypass that adapter entirely.

## Playback and transitions

Each instance has BASE, ACTION and ADDITIVE layers. BASE and ACTION replace the channels of named tracks; ADDITIVE adds six transform components to the result. Absent tracks inherit the lower layer or the current legacy transform. These are rigid-part offsets, not skeletal animation masks.

The mixer uses one final-pose crossfade envelope. A new request captures the current composite pose, including any unfinished fade. It blends toward the incoming layers over `transition.in`; normal completion or explicit stop fades toward the remaining layers/legacy pose over `transition.out`. The incoming clip clock runs during its fade. Successive requests in one frame capture the same last evaluated pose. This preserves an interrupted inspect pose rather than jumping through idle. Changing only the legacy baseline with no active transition retains legacy timing; the framework does not globally smooth old TXT states.

The controller supports named play, restart, stop, looping/hold, reverse playback, activity/progress queries and same-layer priorities. Arbitrary names are supported. A rejected replacement leaves the current clip and its event cursor intact. Reverse progress measures elapsed traversal (0 to 1), while sampling runs from duration toward zero.

Automatic presentation requests observe existing gun state:

| Trigger | Request |
| --- | --- |
| First observation of an instance/context | BASE `idle`; first-person ACTION `draw` if present. |
| Observed recoil start/reset or ammo decrease while recoiling | ADDITIVE `fire`; cancels ACTION `inspect`/`inspect_empty`. |
| Existing `IsReloading` begins | ACTION `reload_empty` if ammo is zero and that clip exists, otherwise `reload_tactical` for a nonempty gun when available, otherwise `reload`. Cancels the previous action. |
| Existing `IsReloading` ends | Stop the matching reload clip if still active. |
| Existing `CockingTime` becomes positive outside reload | ACTION `cock`, otherwise `bolt`; cancels the previous action. |
| Inspect key | ACTION `inspect_empty` when empty and defined, otherwise `inspect`. Blocked during existing reload/cocking and an observed shot. |

The inspect key is **unbound by default**, configurable under HandmadeGuns in Controls. It affects the local first-person presentation only. Automatic gun-state cancellation deliberately outranks authored interruption locks: a cosmetic clip cannot hold up legal firing or force an obsolete reload pose. Clip priority applies to ordinary requests on the same layer; ADDITIVE can coexist with ACTION. Missing automatic clip names simply issue no playback request. Any remaining BASE tracks still apply; omit those tracks if you want the corresponding legacy action transform to remain visible.

`reload_empty` and `reload_tactical` select visuals from existing ammo state only. They do not introduce a chamber or new reload semantics. Match clip duration to existing gun timing; the controller does not stretch reload gameplay to fit an asset. A once clip can finish visually before gameplay ends.

`holster`, `bolt`, `cock`, `inspect_empty`, and arbitrary custom clips can also be requested through the client API. Automatic holster-before-unequip scheduling is not added: vanilla can stop rendering the stack immediately on a switch.

## Presentation events and extension API

`HMGAnimationEvent` is posted on the **client Forge event bus**, with stack, owner (when an entity exists), render context, source, clip, marker name/data, generation and loop cycle. Consumers must filter context: a preview/other-player instance is distinct from first person. No default consumer spawns casings, sounds, particles, muzzle flashes or changes visibility in this foundation. Subscribe to supply such presentation behavior. This avoids duplicating HMG's existing effects.

Events do not load ammunition, fire the weapon, change inventory, or mutate authoritative world state. Names such as `mag_in`, `sound`, `muzzle`, or `custom` are labels, not built-in gameplay commands.

Forward playback delivers markers in `(previousTime, currentTime]`, including every marker skipped over by a slow frame. The initial endpoint is emitted once when playback first advances (including a zero-second advance). Repeat delivers the end marker followed by the next cycle's zero marker; these are distinct author markers. Reverse playback uses descending timestamps and stable order for simultaneous markers. Restart creates a new generation. Stopping/interruption discards future markers from the old cursor; a fading snapshot never emits old events. Multiple render passes at the same clock value do not re-emit markers. Ordering between independent layers is BASE, ACTION, ADDITIVE, not a global timestamp merge.

Client extensions can queue a named clip with:

```java
AnimationClient.request(stack, owner, IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON,
        "inspect", AnimationController.Layer.ACTION);
```

This returns whether a request was queued, not whether its later priority check succeeds. It is consumed on the next eligible root render, and rechecked against current gun reload/cock/shot state. Use the common `AnimationController` API for direct playback operations in a separately owned presentation context. Both APIs must be called on their owning thread; the HMG client integration runs on the client thread.

## Ownership, reload and limitations

Definitions and keyframes are shared immutable data. Playback is a weak stack-keyed client registry, further distinguished by owner, render context and under-gun slot path. No elapsed time, event cursor or controller is stored in a model or item definition. GUI preview copies use the real stack plus GUI identity; nested under-guns use the parent stack/path because their stacks are reconstructed from NBT. Mounted render calls supply their owner identity. Scopes restore their predecessor in `finally`, so nested rendering does not replace the outer animation scope.

The clock advances once at render-tick START; root evaluation is idempotent at that clock value. Parts, transparent passes, skin passes and repeated GUI drawing only reuse the sampled pose. Local held-stack replacement/switch, world change, resource reload and expired unseen entries reset playback. A newly observed/culled gun starts from its currently observed state; this is not a persistent or network-synchronized animation timeline.

JSON is cached by canonical path, including cached failures. Existing explicit model/pack reload workflows invalidate animation resources. A general resource reload also refreshes these CPU-only assets without deleting any GL objects. Edited/missing assets are retried on reload, not once per frame. Replaced definitions cause fresh instance state; this intentionally cancels old events/transitions. Logs identify file, clip, part, keyframe/event and field where available. A bad referenced part disables the file for that gun rather than partially applying an uncertain animation.

The runtime representation (`AnimationDefinition`, `AnimationClip`, `AnimationTrack`, `AnimationKeyframe`, `AnimationEvent`, `AnimationPose`) is independent of JSON. Future Blockbench/Bedrock import can produce those objects. **This is not a full Blockbench/Bedrock/glTF importer.** No third-person full-body system, model replacement, gameplay recoil, magazine/chamber redesign, ballistics, optics or networking redesign is included.

Automatic fire requests rely on already observable render/NBT state. Several shots between observations, or an entire reload/cock between renders, cannot be reconstructed reliably. Event exactly-once semantics apply to each started playback, not to a network guarantee of one clip per server shot. Phase B can add presentation shot/action identifiers after auditing existing networking; this pass does not invent them.

## Validation

Run `gradlew :HMG:animationTest` (also included by `:HMG:test`) using the repository's Java 8 setup. The suite exercises strict parsing, bad numbers/parts/clips, evaluation boundaries, interrupted fades, priority, marker order, loop/restart/stop, zero duration, two-instance isolation and 30/60/120/240-FPS multi-pass clocks. It also parses the example asset.

The non-GL suite can also be compiled with Java 8 using only `handmadeguns/animation/*.java`, `AnimationTests.java`, and Minecraft's existing Gson 2.2.4 jar; supply the example file as its argument. This separates runtime math/parser verification from a Forge dependency-resolution failure.

Implementation validation on 2026-09-09: Java 8 `:HMG:compileJava` and `:HMG:animationTest` passed; the suite reported 74 checks. The first offline build could not resolve three existing dependencies; the unchanged online build fetched them and succeeded. The standalone Java 8/Gson run also passed. No dependency or toolchain versions were changed. The common animation classes compile and execute without Minecraft client or OpenGL classes on the classpath; this checks their dependency boundary, not dedicated-server startup.

In-game verification is still required: old guns without JSON; first/third person; NPC; inventory and attachment preview; nested under-guns; mounted/placed guns; ADS/fire/reload/cock; rapid switches; inspect interrupted by fire/reload; 30/240 FPS and stalls; F3+T/model reload; dedicated-server startup. Source inspection or compilation alone does not establish those results.
