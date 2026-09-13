# Compatibility Notes

This document records maintained cross-mod and rendering behavior. These integrations are optional unless a server or pack explicitly depends on the other mod.

## Angelica and Celeritas rendering

HMG parses models during Forge initialization but creates model display lists, VBOs, and Blockbench texture uploads lazily on the first normal render. This avoids issuing rendering or texture-manager work before Angelica has entered a managed render frame or before Minecraft has constructed its texture manager.

OBJ and MQO display-list compilation uses model-local Tessellators instead of the global renderer singleton. Inventory, NEI, held, and world-gun rendering therefore does not interrupt a Tessellator drawing session owned by Angelica/Celeritas or another renderer.

The VBO renderer scopes and restores the caller's client-array state, array-buffer binding, and matrix mode. This applies to normal HMG rendering as well as compatibility renderers such as NEI. The behavior does not require a hard Angelica dependency, and disabling `Render.enableVBOModelRendering` retains the legacy display-list fallback.

## Combatives camera and aim recoil

### Authoritative point of aim

For handheld player-fired projectiles and their lock-on ray, HMG optionally consumes Combatives' common-side `InteractionRay.authoritative` result. This makes projectile direction use the player's gameplay `rotationYaw`/`rotationPitch` and makes the spawn/lock origin use Combatives' synchronized effective eye-above-bounding-box-floor geometry. Crawl, swim, crouch, standing, transition, and gameplay-scale eye origins therefore follow the same accepted geometry on client and server.

The ray intentionally does not include Combatives' rendered camera shake, bob, lean, translation, or other presentation effects. HMG still applies its existing weapon elevation adjustment before resolving the ray, retains the half-block forward spawn, and applies the existing projectile spread afterward. ADS does not select a different gameplay ray; its existing spread multiplier remains unchanged.

This integration is reflected and cached on the common side. If Combatives is absent, its class or method shape is incompatible, or invocation fails, HMG uses its exact legacy `getEyeHeight()` and `GunsUtils.getLook()` behavior. Placed guns, connected turrets, vehicles, NPCs, and unrelated `GunsUtils.getLook()` callers are not changed.

HMG-Overdrive supports an optional two-layer first-person recoil integration with Combatives. When Combatives is installed, `Compatibility.enableCombativesRecoilIntegration` is enabled, and the Combatives camera API accepts the base shot impulse, HMG submits visual weapon recoil as named Combatives camera impulses under separate kick, punch, and sustained source IDs.

Accepted Combatives shots also use `Compatibility.enableCombativesAimRecoilIntegration` to apply actual client aim recoil. This aim controller mutates only the local first-person player's `rotationPitch` and `rotationYaw`, so the centered vanilla crosshair, ray traces, projectile direction, and normal client-to-server rotation sync naturally follow the displaced look direction. Combatives impulses remain visual render offsets; they do not by themselves change player aim.

The Combatives path includes:

- distinct first-shot pitch punch;
- smaller continuous horizontal yaw drift with directional continuity;
- persistent pitch climb and conservative yaw drift through the HMG aim-recoil controller;
- visible transient yaw recoil through Combatives yaw support;
- roll based on horizontal recoil direction;
- rearward camera translation when the Combatives camera API accepts translation impulses;
- short high-frequency punch impulses for fire-rate-sensitive camera pressure;
- burst reset after a short firing gap or weapon/world/player-state change.

If Combatives is absent, disabled, unavailable, or rejects the base kick impulse for a shot, HMG automatically uses the legacy smooth recoil path. `RenderTickSmoothing.addSmoothRecoil(...)` and `applySmoothRecoil(...)` are the legacy owner that mutates real player pitch/yaw, so the new aim controller is only invoked after Combatives accepts the visual shot. The legacy path is cleared only after the exact shot impulse is accepted, so both aim owners are not applied at the same time and rejected API submissions still preserve normal HMG recoil.


Verbose recoil diagnostics can be enabled with `Compatibility.enableCombativesRecoilDebug=true`. Diagnostics log each submitted source ID, rotation, translation, timing, frequency, decay type, priority, stacking mode, acceptance result, fallback decision, weapon-state reset, aim recoil shot additions, pending and accumulated pitch/yaw, per-tick application, delayed recovery, detected player mouse deltas, weapon key, and whether new aim or legacy ownership is active. Normal gameplay should leave this disabled.

Aim recoil applies most per-shot displacement over a few client ticks, waits for `combativesAimRecoilRecoveryDelayMs` after the last accepted shot, and then subtracts only the controller-owned recoil contribution. Mouse movement is preserved because recovery is contribution-based rather than locked to a pre-burst view angle; pulling down or steering into the recoil reduces the remaining recoverable contribution instead of causing a later snap back to an obsolete target.

## BackTools player-back rendering

HMG's optional BackTools bridge is entered only from `RenderPlayerEvent.Specials.Post`. It reads the remembered stack for that event's exact player, makes one private render copy, and opens a thread-local scope containing that player and that exact `ItemStack` object. The compatibility renderer must claim that scope before it performs renderer lookup or any HMG work; the claim is single-use and the bridge removes the scope in `finally`.

The bridge follows BackTools' exact stack-equality guard. If the normalized held stack is the remembered stack, neither renderer draws it on the back. Holding a different HMG weapon does not suppress the remembered weapon: after a switch from gun A to gun B, the exact remembered A stack remains eligible for that player's scoped back render.

Forge item render types do not identify render ownership. In particular, `ENTITY` can be requested by GUI compatibility renderers as well as world renderers. NEI, normal inventory rendering, dropped items, and unrelated held-item renders therefore cannot qualify for BackTools behavior merely because their stack is an HMG gun or their renderer is `HMGRenderItemGun_U_NEW`. A different stack of the same item type also fails the identity check.

Verbose scope diagnostics use the normal `Logging.enableDebugLogging` option. They report the source event, whether a scope exists, its player and stack identity, the incoming identity, render type, and acceptance result. These diagnostics are disabled in normal production configuration.

The BackTools boundary and OBJ VBO state isolation address separate problems. Limiting BackTools prevents unrelated item rendering from causing back-item work, while `HMGVboMeshGroup` still preserves the caller's client-array state, array-buffer binding, and matrix mode for every legitimate HMG render, including NEI and inventory icons.

### GTNH-NEI item-grid trace

The bundled GTNH-NEI reference source confirms that `ItemsGridSlot` passes its stored stack directly to `GuiContainerManager.drawItem`. The ordinary path calls `RenderItem.renderItemAndEffectIntoGUI`, whose Forge custom-renderer dispatch is an `INVENTORY` render, not a BackTools player render. NEI does not clone the stack in this grid-to-`RenderItem` path. Collapsible item groups can draw both a background item and the foreground item, but amount text, damage bars, and effects are phases of the same draw rather than repeated BackTools calls.

`safeItemRenderContext` wraps third-party item rendering with matrix/attribute and Tessellator recovery, catches a broken item renderer so the rest of the panel can continue, and substitutes a fire icon only for the failed grid draw. It does not publish a general NEI thread-local ownership marker. None is needed here: an NEI render has no HMG-owned BackTools scope, whereas the bridge's exact player-and-stack scope positively identifies the legitimate call without loading or naming any NEI class.

The previously changing compatibility `stackIdentity` values came from the old bridge copying the remembered BackTools stack once per `RenderPlayerEvent.Specials.Post`; they were not identities of stacks cloned by the NEI grid. NEI could make the timing and model churn conspicuous by rendering many HMG icons, but its grid path cannot itself fire a player-specials event. The former compatibility entry was broad in a different dimension: every player-specials post event performed a remembered-stack candidate check, and acceptance was based on a remembered HMG item plus renderer/type checks rather than an unforgeable render owner. The scoped bridge now makes the only renderer invocation itself and requires object identity with its private selected-stack token.

## HMG temporary firing pose and Flan's armor

HMG's `triggerHeldGun` sets `set_up` for the temporary hip-fire aiming pose. The player renderer already reads that state into `aimedBow`; the render-scoped item-use bridge now includes the same state as well as normal ADS. Previously that bridge included only `Key_ADS`.

Flan's `ModelCustomArmour.render` resets its own `aimedBow` from bow-use state, calls `ModelBiped.setRotationAngles`, then copies those resolved biped parts into TurboModel parts. Supplying the same aiming state before this calculation lets vanilla and custom armor follow the same final pose calculation. No gun rotation constants or cached arm transforms are introduced. The bridge restores real item-use state after equipment rendering.

Combatives' existing crawl-before-aim, lean-after-aim, leg/lean cleanup, optional late mixin and TurboModel rotation-order correction are unchanged. This fixes the divergent pose input; it does not introduce a separate snapshot/copy system for the main player model. Normal ADS, transient hip-fire, crawl, lean, release and player switching still require visual checks with Flan's installed.

## Server-validated HMG headshots

Direct HMG bullet damage carries classification from its actual entity impact, not a second ray reconstructed from projectile orientation. Entity candidates are traced on the same block-clipped segment and ordered by distance before penetration handling. Only that direct impact source receives the existing 1.75 multiplier and headshot sound; melee, unrelated mods and explosions do not inherit HMG's bonus. There is no client-supplied headshot flag. Each damage attempt is independent, without a victim-wide per-tick suppression flag.

The head region is bounded by the struck entity's actual AABB and eye height, allowing HMG's existing 0.1 collision envelope but no downward expansion into shoulders. The existing common-side Combatives aim bridge supplies its authoritative pose/scale eye height when available. This is an eye-based region within HMG's collision geometry, not skeletal hitboxes: unusual non-humanoid models and prone visual geometry require in-game verification. It does not enlarge HMG's collision box to match a cosmetic lean or rendered head outside that box.
## Weight-based jump rejection and Combatives

HMG now evaluates weapon weight at `EntityLivingBase.jump()` entry. The existing
`gunInfo.motion <= 0.70` heavy cutoff rejects the jump outright; it no longer
clamps a first jump and arms a later cancellation. Light weapons (`motion >= 0.95`)
allow vanilla jumping immediately, including after switching away from a heavy
weapon. Medium weapons retain the existing 3/6-tick landing delay, but an allowed
jump keeps vanilla vertical velocity. The former 0.2/0.35 velocity clamps and
post-jump correction paths are removed. Horizontal gun mobility is unchanged.

With the updated Combatives installed, HMG registers a common Java predicate
through the optional `JumpRestrictions.register(String, Predicate<EntityPlayer>)`
API using reflection once during initialization. Combatives evaluates it from its
existing jump HEAD injection alongside crawl rejection. Crawl-key release and
standing-clearance checks still run even if weight also rejects the jump. HMG's
fallback hook delegates ownership only after registration succeeds. An absent or
older Combatives without this API uses the HMG fallback; there is no hard mod
or Mixin dependency added to HMG.

HMG alone uses a Forge coremod entry branch targeting MCP `jump()V` / SRG
`func_70664_aZ()V` after deobfuscation. The jar manifest loads the coremod; existing
Gradle `runClient`/`runServer` tasks receive its development loading property.
Neither hook changes position, bounding boxes, onGround, fallDistance,
isAirBorne, flight flags, velocity or movement packets. Active creative flight
and riding bypass HMG's jump policy and clear its pending landing delay. Ordinary
falling/landing continues through vanilla movement. Vanilla's `EntityPlayer.jump`
wrapper still performs its existing statistics/exhaustion bookkeeping, as it did
with Combatives' original base-class cancellation.

The logical server reads its gun definitions. On login and after server pack
settings reload, HMG sends all registered gun mobility values to clients. Client
jump prediction reads only this snapshot, never client pack values. Until the
snapshot arrives, held HMG guns reject ground jumps; creative flight remains
available. Snapshots are connection-scoped and immutable; network handlers only
publish data and never mutate entities/worlds. Medium landing-delay prediction
uses the same algorithm on distinct client/server player instances. The server's
timer is authoritative, but prediction may differ while movement or policy
updates are in flight. No correction teleport is sent to reconcile those cases.
Use matching updated HMG builds on both sides for the new appended packet type.

This is jump-action rejection, not new anti-cheat movement validation: vanilla
1.7.10 still accepts C03 position reports separately after calling server jump.
A modified client ignoring prediction is not prevented from reporting movement
by this hook alone. HMG does not rewrite that packet path or Combatives geometry.

The old implementation registered two independent handlers on both physical
sides. `HMGJumpHandlerClient` lacked a logical-side check, used shared static UUID
maps in integrated play, and cleared motionY/isAirBorne/fallDistance from Forge's
post-vanilla jump event. It allowed jumps during its cooldown. The server END
tick handler instead blocked upward motion during cooldown, reset fallDistance,
sent velocity changes, and called `setPositionAndUpdate(posY - 0.01)`. Neither
excluded flight. This is confirmed source evidence for competing movement
corrections; the precise reported desync sequence has not been captured in-game.

For short reproduction traces, add `-Dhmg.jumpTrace=true` to both client and
server JVM arguments. HMG logs logical side, player/world tick, position, motionY,
onGround, isAirBorne, fallDistance, flying, held weapon/slot, synchronized mobility,
thresholds, timers, jump entry/owner/rejection, and successful vanilla jump events.
Enable Combatives `verboseMovementDebug` for crawl cancellation and C03/travel
traces. Remove the diagnostic JVM argument after collecting logs.

Java 8 offline HMG/Combatives compilation and Combatives Mixin annotation
processing passed. HMG's transformed cached Forge MCP and SRG classes passed ASM
structure/dataflow checks with one conditional entry return each. No packaging,
reobfuscation, runtime launch or in-game validation was performed. Verify HMG
alone and together with Combatives: light/heavy/medium weapons, repeated/sprinting
jumps, slabs/stairs, ledges, holding jump on landing, heavy/light switches,
creative flight/landing, crawl clearance, reconnect/respawn/dimension changes,
server reload and mismatched client pack values. Dedicated and integrated server
runs are both required, including real coremod/Mixin transformer ordering.

## HMG configured explosion damage

For projectile explosions with configured damage and radius `R`, damage eligibility
requires `abs(entity.posY - explosionY) <= R`. Within that vertical reach, damage
uses horizontal distance `h = sqrt(dx*dx + dz*dz)`: full configured damage through
`h <= R`, linear falloff between `R` and `2R`, and zero at or beyond `2R`.
Vertical separation uses the entity position, not eye height. This is a cylindrical
damage volume with an abrupt vertical cutoff; equal horizontal distance gives equal
distance falloff at every eligible height.

Damage is still multiplied by HMG terrain exposure, so terrain and target bounding
box visibility can produce different damage at different heights. Vegetation and
webs do not count as cover; stone and physical transparent blocks do. Exposure
retains its existing coarse occupied-cell sampling for partial blocks.

Knockback retains its original 3D distance, spherical reach and eye-based direction,
including player packet values. Consequently damage and knockback reach differ.
The legacy constructor without configured damage (used by GVC's guerrilla bomber)
retains its previous spherical damage formula. Explosion damage remains server-owned;
block destruction is unchanged. These rules have compilation and mathematical
validation; in-game height, cover and dedicated-server checks remain outstanding.
