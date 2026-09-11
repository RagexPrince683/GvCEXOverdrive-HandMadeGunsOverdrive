# Optional named animations (Phase A)

Minecraft Forge 1.7.10 / Java 8. This adds a clip/controller layer over HMG's existing rigid gun parts. OBJ/MQO and Phase A JSON remain supported. Direct Blockbench importing creates parts and clips for the same runtime and uses the existing VBO/display-list mesh renderer. Gun gameplay is unchanged.

## Direct Blockbench projects

For a unified gun, place the native project inside the pack and add this before `Unified_guns,...`:

```text
BlockbenchModel,models/ak.bbmodel
```

The path is relative to the pack root, not `assets/handmadeguns/textures/model`. Both supported pack root layouts work. The directive enables model rendering; it takes precedence over `ObjModel`/`ObjTexture`. A later `CanObj,false` can disable rendering. Embedded animations take precedence over `Animations` JSON if both are present, with a diagnostic. Existing OBJ/MQO guns without this directive retain their behavior.

No `AddParts` declarations or exported meshes are required. Outliner groups become `HMGGunParts`; `partsname` is the authored name, and `animationId`/embedded tracks use the persistent bone UUID. Duplicate names do not merge embedded tracks. Root cubes get a synthetic part using their element UUID. HMG's normal action detection, per-instance playback isolation, cancellation, inspect key, presentation events and direct animation API are reused.

Supported input is `.bbmodel` format 4.x/5.x with `bedrock`, `bedrock_old`, or `free` model format: nested cube geometry, origins/rest rotations, inflated cubes, visibility/export flags, per-face UV rectangles including flipped/rotated UVs, and box UVs including mirroring. Version 5's separate `groups` array and older inline outliner groups are accepted. Null-textured faces are omitted. Embedded PNG data is preferred over stale author-machine texture paths; otherwise textures resolve relative to the project and must remain inside the pack. These formats use the project UV resolution. Texture layers use the saved composite image.

`BlockbenchTransform` owns the conversion: Blockbench pixels to the legacy HMG firearm baseline at 3/16 HMG unit per pixel, a 180-degree Z rotation (positions `[-x,-y,z]`, rotations `[-rx,-ry,rz]`), absolute origins converted to parent-local offsets, and ZYX Euler rotation. The threefold normalization accounts for HMG's legacy MQO `/100` model units; it is applied equally to geometry, bone pivots and animated translations. Rest and animated angles are added before rotation; geometry is local to its bone pivot. Translation, rotation and scale have separate clocks and sparse channels preserve the underlying layer. Scale blends around one and multiplies on the additive layer. Rotation angles are not wrapped. Linear, step, uniform Catmull–Rom and numeric Bezier handles are supported, including split incoming/outgoing values. Catmull–Rom follows the reference editor's neighbor selection and split-key parameterization. Bezier time is solved numerically rather than using the editor's 200-sample approximation. Non-numeric Molang is rejected; empty/whitespace channel expressions evaluate to zero.

Project parsing and PNG decoding occur during HMG pack loading, but embedded textures are uploaded lazily on the first item render. Forge 1.7.10 invokes mod pre-initialization before Minecraft constructs its texture manager, so model construction must remain free of texture-manager and OpenGL work.

Declared animation lengths remain authoritative: keys after the end remain available as interpolation control points, without extending action duration. `once`, `loop` and `hold` map to Phase A playback; a zero-duration loop becomes a constant hold pose. Sound/particle/timeline keys become `blockbench_sound`, `blockbench_particle`, or `blockbench_timeline` presentation events with their data-point JSON as payload. Effect keys beyond the declared duration are not dispatched. The importer does not execute scripts or open author-machine sound paths.

For imported rigs, `Gunparts_offsetScale` scales geometry, bone offsets and animated translations together, without multiplying the scale again at each hierarchy level. `ModelScala` remains the outer model scale. Legacy OBJ/MQO scaling is unchanged.

HMG's presentation frame is separate from imported model space. A single outer X half-turn maps the TaCZ model frame into the legacy HMG display basis at root traversal. Equipped first/third person, dropped items, inventory/GUI and nested gun traversal use that same boundary; child bones never apply it again. The `3/16` import units are unaffected. Equipped Blockbench models additionally use TaCZ positioning nodes when present: first person aligns `idle_view` and blends its rigid inverse transform toward `iron_view` for ADS, while player-held third person applies HMG's fixed 1.7 hand-origin bridge and then aligns `thirdperson_hand` to that origin. These paths bypass legacy `ModelEquipped`, `ModelHigh`, `SimpleADSOffset*`, `SprintingPoint`, `SprintingRotation` and configured third-person model placement; those settings remain active for OBJ/MQO guns. Blockbench guns remain at their authored idle presentation while sprinting. If `iron_view` is absent, ADS holds the idle view. `camera` is the idle fallback when `idle_view` is absent.

TaCZ hand functional renderers use the player-model frame, not a wrist-centered `ModelRenderer`. Vanilla 1.7.10 shoulders are at `[-5,2,0]` (right) and `[5,2,0]` (left); the arm boxes reach wrist centers `[-6,12,0]` and `[6,12,0]`. The local Z half-turn maps these into the authored locator frame, retaining the shoulder/pivot compensation. The shared presentation correction then rotates both the gun and those arms together. Do not add a second wrist translation or copy locator Euler angles into the arm: the hierarchy already supplies that orientation.

Authored animation names are retained for `AnimationClient.request`. Missing HMG names receive these aliases (an authored HMG name always wins):

| HMG action | Imported source | Playback |
| --- | --- | --- |
| `idle` | `static_idle` | Authored mode; zero-length pose holds |
| `fire` | `shoot` | Once per detected shot |
| `reload_empty` | `reload_dry` | Once |
| `reload` | `reload_tactical`, otherwise `reload_empty` | Once |

Names already matching `draw`, `reload_tactical`, `reload_empty`, `bolt`, `cock`, `inspect` and `inspect_empty` use the existing action hooks. No bolt/cock animation is invented when the project has none. Direct `shoot` playback keeps its authored loop; only the `fire` alias overrides it, matching TaCZ's shot-triggered `PLAY_ONCE_STOP` convention. HMG's existing additive fire layer remains in use. Authoritative reload timing still comes from the gun TXT/server; an accepted manual reload sends a presentation-only start event to its owning client, and the visual ACTION then runs to the imported clip's natural completion independently of gameplay completion.

With `ModelArm,true`, TaCZ `lefthand_pos`/`righthand_pos` placeholders drive Minecraft 1.7 player arms in first person under the complete imported bone transform. The locator frame follows TaCZ's local 180-degree Z arm rotation, and the arm mesh receives the same Blockbench-to-HMG size normalization as the gun. Authored locators take precedence over `ModelArmOffset*`/`ModelArmRotation*`; if either locator is absent or hidden, that hand falls back to the existing legacy arm settings. `ModelArm,false` suppresses imported locator arms. Legacy OBJ/MQO arm behavior is unchanged. The 1.7 arm mesh/skin layout differs from modern slim arms. Other TaCZ marker groups retain their names, UUIDs and transforms but do not automatically configure HMG attachments, camera recoil, shell/muzzle effects, ammunition visibility or extended-magazine selection. `static_auto`, `static_semi`, `switch_auto`, `switch_semi` and extended-inspect names are callable, but TaCZ Lua state machines are not imported. Camera/constraint animation never changes authoritative aiming or movement.

Explicit HMG model/settings reload rereads the project and replaces its parts, animation definition and GPU resources. Replacement releases the old imported model's textures and mesh buffers on the existing client reload path. F3+T retains the existing HMG external-model reload semantics; use the HMG reload command after editing the project. Parsing/importing is client-only; dedicated servers do not need the project file.

### Reference AK audit

The actual fixture is `HMG/Reference files/cod4_ak.bbmodel`: project name `cod4_ak`, identifier `ak47`, format 5.0 / Bedrock, per-face UV mode. The importer audit parsed 99 bones, 857 cubes and one camera element. There are no meshes. Its embedded `ak74.png` is 256×256 with a 128×128 project UV grid; fractional UV coordinates and omitted faces occur. Cubes and some groups have rest rotations; pivots are absolute project coordinates. The root pivot is `[0,8.2,6.5]`, the magazine branch `[0,6.9,1.5]`, and the bolt `[0.48863,17.54799,-0.0625]`.

The four root groups are `camera`, `root`, `views` and `positioning`. Under `root`, `lefthand_and_mag` contains the magazine/bullet variants and left hand, while `bone2` contains the spare magazine, `AKM` assembly and right hand. `AKM` contains stock, bolt, body, barrel, sight, trigger/safety and attachment-marker branches. Extended magazines and attachment adapters are hidden in the saved project. `views` contains idle/iron/refit markers; `positioning` contains fixed/ground/third-person markers. All 99 names, UUIDs, origins, rotations, visibility and face counts can be printed by the production audit entry point `handmadeguns.client.modelLoader.blockbench.BlockbenchProject <file.bbmodel>` on the development runtime classpath (no OpenGL context required).

| Embedded name | Declared seconds | Authored loop |
| --- | ---: | --- |
| `static_idle` | 0 | loop |
| `reload_dry` | 3.4 | once |
| `reload_tactical` | 2.6 | once |
| `static_auto`, `static_semi` | 0.0417 | loop |
| `draw` | 0.85 | once |
| `put_away` | 0.5667 | hold |
| `switch_auto`, `switch_semi` | 0.65 | once |
| `inspect` | 7.7 | once |
| `inspect_xmag` | 7.8 | once |
| `inspect_empty` | 8.15 | once |
| `inspect_empty_xmag` | 8.3 | once |
| `shoot` | 0.7667 | loop |

The AK uses position/rotation/scale channels, linear/Catmull–Rom interpolation, 79 split keys, and sound markers. Seventeen individual components are newline-only zero expressions. Some cubic control keys extend past the declared duration. There are no Bezier/easing, global-rotation or quaternion-interpolation channels in this fixture. TaCZ's reference `default_state_machine.lua`, `ak47_state_machine.lua`, `BedrockAnimatedModel`, `BedrockGunModel` and hand functional renderers establish the action and marker conventions; the AK's `reload_dry` additionally needs the alias above.

### Current limits

Mesh/armature/billboard geometry, cube rescale/stretch, bone bindings/reset, multi-file rigs, global/quaternion interpolation, plugin easing, and non-numeric Molang/timing expressions are rejected with diagnostics. Animation controllers/Lua, particle playback, external sound playback, animated textures/PBR and TaCZ animated-camera/constraint/attachment/ammunition logic are not implemented. Static `idle_view`, `iron_view`, `camera` and `thirdperson_hand` positioning paths are supported. The animation `override` flag follows Phase A layer precedence rather than resetting a Blockbench preview animation stack. Unsupported geometry/transform input fails the project import instead of silently producing a partial weapon.

Compilation and CPU audits do not prove visual acceptance. First-person fire/reload/inspect comparisons, UV orientation, player arms, hidden magazine variants, inventory/third-person views, resource reload, VBO/display-list/Angelica rendering and dedicated-server startup still require runtime validation.

Validation performed on the reference AK: Java 8 compilation and the existing 74 Phase A checks passed. An in-memory numerical comparison of 31,260 component samples across 154 channels against the reference keyframe sampler agreed within 1.14e-13; all 14 declared durations were preserved. Independent ZYX matrix calculations checked all 99 parent links and 8,048 vertices/UVs across 2,012 imported textured faces, with maximum position error 2.97e-8 HMG units. These checks produced no additional repository test infrastructure.

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

Presentation triggers use the following request paths:

| Trigger | Request |
| --- | --- |
| First observation of an instance/context | BASE `idle`; first-person ACTION `draw` if present. |
| Observed recoil start/reset or ammo decrease while recoiling | ADDITIVE `fire`; cancels ACTION `inspect`/`inspect_empty`. |
| Server accepts a manual reload | Send the owning client one slot/item/event-identified presentation event. ACTION `reload_empty` is selected from the accepted-start ammo snapshot when empty, or `reload_tactical` when nonempty, with `reload` as the existing fallback. Cancels the previous action. |
| Imported reload ACTION completes | Release imported pose ownership naturally. `IsReloading=false` does not stop an accepted clip because it cannot distinguish gameplay completion from an abort. |
| Existing `CockingTime` becomes positive outside reload | ACTION `cock`, otherwise `bolt`; cancels the previous action. |
| Inspect key | ACTION `inspect_empty` when empty and defined, otherwise `inspect`. Blocked during existing reload/cocking and an observed shot. |

The inspect key is **unbound by default**, configurable under HandmadeGuns in Controls. It affects the local first-person presentation only. Automatic gun-state cancellation deliberately outranks authored interruption locks: a cosmetic clip cannot hold up legal firing or force an obsolete reload pose. Clip priority applies to ordinary requests on the same layer; ADDITIVE can coexist with ACTION. Missing automatic clip names simply issue no playback request. Any remaining BASE tracks still apply; omit those tracks if you want the corresponding legacy action transform to remain visible.

`reload_empty` and `reload_tactical` select visuals once from the server ammunition state at reload acceptance, before magazine removal can change it. The event does not introduce a chamber or new reload semantics. Match clip duration to existing gun timing; the controller does not stretch reload gameplay to fit an asset. A once clip can finish visually before gameplay ends. If no imported reload clip or fallback exists, the renderer retains the legacy reload state motion.

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

Ordinary requests for the already-active clip are idempotent. The overload with a final `boolean restart` explicitly retriggers a discrete action; automatic shots retain that behavior. A transition captures the evaluated immutable pose before changing layers, including back-to-back requests without a render in between. ADS is still a presentation transition rather than an automatically requested Phase A clip. Imported models use that progress to blend `idle_view` to `iron_view`; both Blockbench render passes consume the same value.

## Ownership, reload and limitations

Definitions and keyframes are shared immutable data. Playback is a weak stack-keyed client registry, further distinguished by owner, render context and under-gun slot path. No elapsed time, event cursor or controller is stored in a model or item definition. GUI preview copies use the real stack plus GUI identity; nested under-guns use the parent stack/path because their stacks are reconstructed from NBT. Mounted render calls supply their owner identity. Scopes restore their predecessor in `finally`, so nested rendering does not replace the outer animation scope.

The clock advances once at render-tick START; root evaluation is idempotent at that clock value. Parts, transparent passes, skin passes and repeated GUI drawing only reuse the sampled pose. First-person playback survives synchronized stack-object replacement for the same item in the same selected hotbar slot, so an NBT update does not restart draw. Vanilla renders a cached `itemToRender`; reload, cock, bolt and ammunition state are read from the live selected stack when that cached object represents the same gun. The accepted reload event starts and owns the imported ACTION clip; that same presentation snapshot controls the renderer's reload branch until the controller reports natural ACTION completion. `IsReloading` neither starts, restarts nor cancels that imported action; it remains the legacy reload source when no imported action owns the pose. Slot/item switches, unequipping, world change, resource reload and expired unseen entries reset playback. Replacing a gun with an identical item in the same slot without an observed empty slot is treated as the same equipped session. A newly observed/culled gun starts from its currently observed state; this is not a persistent or globally synchronized animation timeline.

JSON is cached by canonical path, including cached failures. Existing explicit model/pack reload workflows invalidate animation resources. A general resource reload also refreshes these CPU-only assets without deleting any GL objects. Edited/missing assets are retried on reload, not once per frame. Replaced definitions cause fresh instance state; this intentionally cancels old events/transitions. Logs identify file, clip, part, keyframe/event and field where available. A bad referenced part disables the file for that gun rather than partially applying an uncertain animation.

The runtime representation (`AnimationDefinition`, `AnimationClip`, `AnimationTrack`, `AnimationKeyframe`, `AnimationEvent`, `AnimationPose`) is independent of JSON. The Blockbench importer produces those same objects. **This is not a full Blockbench/Bedrock/glTF importer.** No third-person full-body system, model replacement, gameplay recoil, magazine/chamber redesign, ballistics, optics or networking redesign is included.

Automatic fire and cock requests still rely on already observable render/NBT state. Several shots between observations cannot be reconstructed reliably. Reload is the exception: accepted manual reload commands carry a presentation event identifier and captured variant to the owning client. Event exactly-once semantics apply to each started playback, not to a network guarantee of one clip per server shot.

## Validation

Run `gradlew :HMG:animationTest` (also included by `:HMG:test`) using the repository's Java 8 setup. The suite exercises strict parsing, bad numbers/parts/clips, evaluation boundaries, interrupted fades, priority, marker order, loop/restart/stop, zero duration, two-instance isolation and 30/60/120/240-FPS multi-pass clocks. It parses the example asset and the reference TaCZ AK, and covers accepted tactical/empty reload events, duplicate suppression, rejection, weapon mismatch, NBT-independent natural completion, explicit invalidation, renderer ownership and legacy fallback.

The non-GL suite can also be compiled with Java 8 using only `handmadeguns/animation/*.java`, `AnimationTests.java`, and Minecraft's existing Gson 2.2.4 jar; supply the example file as its argument. This separates runtime math/parser verification from a Forge dependency-resolution failure.

Implementation validation on 2026-09-09: Java 8 `:HMG:compileJava` and `:HMG:animationTest` passed; the suite reported 74 checks. The first offline build could not resolve three existing dependencies; the unchanged online build fetched them and succeeded. The standalone Java 8/Gson run also passed. No dependency or toolchain versions were changed. The common animation classes compile and execute without Minecraft client or OpenGL classes on the classpath; this checks their dependency boundary, not dedicated-server startup.

In-game verification is still required: old guns without JSON; first/third person; NPC; inventory and attachment preview; nested under-guns; mounted/placed guns; ADS/fire/reload/cock; rapid switches; inspect interrupted by fire/reload; 30/240 FPS and stalls; F3+T/model reload; dedicated-server startup. Source inspection or compilation alone does not establish those results.
