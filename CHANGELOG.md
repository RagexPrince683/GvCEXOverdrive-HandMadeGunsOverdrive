# Changelog

## Fix HMG manual dropped-gun pickup (PR pending)

- Enable manual dropped-gun pickup by default while preserving explicit existing
  configuration values and gun-only automatic-pickup filtering.
- Route the pickup key and a consumed targeted right click through the existing
  server-authoritative pickup packet and validation path.
- Improve look-ray targeting for ground items and show a hotbar HUD prompt with
  the player's current pickup key assignment and the targeted gun name.

## Fix HMG recipe lifecycle and Ore Dictionary consistency (PR pending)

- Defer content-pack crafting, smelting, and Gun Smithing Table recipes until the
  initialization phase, after every mod has registered its pre-initialization ores.
- Build vanilla `ShapedOreRecipe` and Gun Smithing Table recipes from one canonical
  normalized ingredient map so both accept the same Ore Dictionary members while
  NBT-bearing and non-Ore-Dictionary stacks remain exact.
- Separate polymer and metal-block recipes from their pre-initialization item,
  block, and Ore Dictionary registration.

## Audit HMG firearm sound levels (PR pending)

- Audit all parser-recognized unified-gun registrations in every HMG TXT pack and add or update rounded peak impulse `GunSoundLV` data where credible firearm or close-caliber measurements apply.
- Keep every sound-level record parser-safe with both required numeric fields, preserve unused second values for restricted non-suppressor guns, and leave ambiguous identities, launch impulses, and non-fired weapons unresolved rather than inventing measurements.

## `/reloadsetonlyhelditem` (PR pending)

- Add a player-only HMG command that selectively invalidates and reloads the model
  resources and renderer for the currently held custom gun without running the
  global `/reloadSettings` pack scan.
- Add per-resource invalidation to the HMG gun, OBJ, and MQO model caches while
  preserving their existing global-clear behavior.

## Allow numbered attachment anchors on model branches (PR pending)

- Let multiple gun parts register the same numbered attachment slot so mutually
  exclusive animated model branches can each supply the installed attachment's
  live transform without changing per-render deduplication or fallback detection.

## Fix optic-aware HandmadeGuns ADS magnification (PR pending)

- Resolve the held or placed gun's sight from attachment slot 1 and select the
  matching base, red-dot, scope, or custom-sight `Zoom` magnification safely.
- Keep `HMGFovHandler` authoritative for firearm ADS FOV changes while preserving
  the legacy zoom event handler's HUD, overlay, sensitivity, and rendering work.

## Restrict attachments for the AWW2 gun pack (PR pending)

- Enable `attachRestriction` for every gun definition in the AWW2 pack so each
  weapon accepts only its explicitly allowed attachments.

## Fix attachment-pack legacy addNewRecipe shapes (PR pending)

- Fix the independent `HMGAddAttachment` legacy parser missed by PR #153 / commit
  `6bfd4fd5a90354b196f6ead74470323f22569b0e` so `ItemA` through `ItemI` remain
  symbol definitions while `Recipe1` through `Recipe3` determine physical grid slots.
- Register attachment and ammunition recipes in both vanilla shaped crafting and the
  Ammo category of the canonical Gun Smithing Table registry from the same shape.
- Resolve item-or-block symbols safely and reset all legacy recipe state after every
  registration attempt.

## Fix legacy addNewRecipe Gun Smithing integration (PR pending)

- Keep legacy shaped recipes in vanilla crafting while registering the same output
  and positional 3x3 shape in the canonical Gun Smithing Table registry used by NEI.
- Resolve legacy item-or-block ingredients into one safe stack representation, log
  unresolved symbols, and reset all shape and ingredient state after each recipe.
- Categorize custom-magazine ammunition on the Ammo tab and firearm/default outputs
  on the Guns tab.

## HMGO Armor-Piercing Bullet Penetration (PR pending)

- Allow armor-piercing projectiles to pass through intact collidable blocks whose
  Forge blast resistance is no greater than vanilla stone, while bounded repeated
  tracing still stops at stronger blocks and detects entities behind penetrations.
- Limit armor-piercing projectiles with damage 7 or lower to one confirmed block
  penetration and projectiles with damage above 7 to two confirmed penetrations.
- Apply 90% direct-hit damage until the projectile penetrates its first qualifying
  block, then use normal configured damage for the rest of that projectile's life.
- Apply 90% of configured gravity to armor-piercing projectiles without changing
  shared gun or magazine configuration.

## Add interactive attachment GUI firearm preview controls (PR pending)

- Rotate the attachment GUI firearm preview with a left-button drag that begins in
  the preview area, without interfering with attachment or player inventory slots.
- Zoom the firearm and its installed attachments with the mouse wheel while the
  pointer is over the preview, with safe per-GUI zoom and pitch limits.
- Render the live attachment preview from a copied stack so viewing the model does
  not modify the held firearm or its NBT.

## Display attachment names in firearm tooltips (PR pending)

- Resolve firearm attachment-whitelist identifiers to their localized, player-facing
  item names in the `Valid attachments` tooltip, with the configured identifier as a
  fallback when an attachment is unavailable.
- Document the distinction between stable `allowattach` identifiers and attachment
  display names for content-pack authors.

## Fix HMG attachment GUI slot validation (PR pending)

- Require every attachment to match the established slot type: sight, support,
  muzzle, under-barrel, or special ammunition.
- Apply gun attachment whitelists as an additional compatibility restriction
  instead of allowing a whitelisted item to bypass its slot type.
- Restrict shift-click attachment destinations to the five exposed GUI slots
  while preserving the existing 54-entry NBT-backed inventory and slot numbers.

## Add slot-aware ADS and gun-part attachment offsets (PR pending)

- Add `opticshift1` through `opticshift5` gun directives that combine live occupied-slot offsets with first-person ADS alignment and its transition without changing zeroing or elevation.
- Add `moveifattachpresent1` through `moveifattachpresent5` part directives that compose local movement with existing animation matrices, child hierarchies, and external attachment anchors.
- Validate both directive families as three finite values and document their direct mapping to the existing attachment GUI/NBT slots.

## Add close-range slug door breaching (PR pending)

- Add the per-ammunition `candoorbreach` option, disabled when omitted, and enable it for the bundled slug rounds.
- Allow explicitly configured projectiles fired within three blocks to open directly impacted wooden doors while preserving iron doors and normal bullet collision behavior.

## Fix external 3D attachment reload and render passes (PR pending)

- Rebuild existing external attachment item renderers, including numbered model and texture variants, after destructive model-cache invalidation during `/reloadSettings`.
- Match installed attachments to gun opaque/transparent alpha and depth pass semantics, isolate all changed GL state, and consistently use the selected numbered model and texture.

## Hide gun parts when attachment slots are occupied (PR pending)

- Add `removeifattachpresent1` through `removeifattachpresent5` gun-part directives, mapped directly to the existing attachment GUI/NBT slots.
- Hide marked part geometry and its child subtree from live installed-item state while preserving animated external 3D attachment anchors on the same part.
- Document reversible, OR-based visibility behavior for legacy and external 3D attachments.

## Support five independent external 3D attachment slots (PR pending)

- Add numbered model, texture, and gun-location keys for existing attachment inventory slots 1 through 5, retaining singular fallbacks and deterministic standalone rendering.
- Add independent `SetAttachmentAttach1` through `SetAttachmentAttach5` animated part anchors with first-anchor duplicate handling per slot.
- Keep one cached item renderer per attachment while selecting the installed model, texture, location, and animated anchor from the actual NBT/GUI slot number.

## Add animated gun-part anchors for 3D attachments

- Add the gun model-part directive `SetAttachmentAttach` for both `AddParts` and
  `AddChildParts`, making `attachmentlocation` local to the selected part's live
  animation matrix.
- Render model-based installed attachments once from the selected part, or once
  from gun-local space for packs without an anchor, rather than once per recursive
  child-part traversal.
- Keep the first configured anchor and warn about later duplicates without
  breaking affected content packs.

## Configurable 3D attachment inventory transforms

- Add per-attachment `InventoryScale` and three-axis `InventoryOffset` TXT
  controls for model-based inventory items, including finite-value validation
  and backwards-compatible defaults.
- Keep installed attachment placement, dropped-item rendering, legacy models,
  and gun rendering independent from these inventory-only settings.
- Document both comma and assignment syntax and configure the sample attachment.

## Fix one-use weapon consumption

- Fixed `isOneuse,true` weapons remaining as empty items after use.
- Restored immediate one-use weapon consumption.
- Preserved manual reload behavior for normal player guns.

## Correct ammunition ballistic override propagation

- Correct `CustomMagazine` `BlletSpread`/`BulletSpread` and `bulletFuse`
  overrides so the exact loaded round supplies the projectile's base spread and
  fuse before it is consumed, including FIFO per-shell `MultiMagazine` guns.
- Preserve normal ADS, diffusion, movement, and attachment spread modifiers
  without ratio or division behavior, and preserve explicit zero overrides.

## Fix per-shell ballistic coefficient initialization regression

- Resolve and reset each content pack's `damageCof` and `speedCof` before
  attachment parsing, so per-shell ammunition overrides and guns use the same
  owning-pack scale without leaking values between packs.
- Parse `damageCof` and `speedCof` independently, preventing damage settings
  from falling through into speed settings, and restore the same pack context
  when live gun settings are reloaded.

## Fix per-shell bullet stability precision

- Keep ammunition-defined bullet stability values as doubles from TXT parsing
  through the effective per-shot profile and spawned projectile, avoiding a
  lossy conversion from the native HMG ballistic type.

## Add per-shell CustomMagazine ballistic overrides

- `CustomMagazine` ammunition may now override projectile power, speed, gravity,
  spread, pellet count, type/model, explosion and block damage, stability, damage
  range, knockback, bounce, resistance, acceleration, fuse, and water resistance.
  Omitted keys continue to inherit the gun TXT value; existing `damagemodify` and
  `speedmodify` multipliers are applied after absolute power/speed overrides.
- Supported keys and aliases are `BulletPower`, `BulletSpeed`, `BulletGravity`,
  `BulletSpread`/`BlletSpread`, `ShotGun_Pellet`/`PerFireRound`, `BulletType`,
  `Explosion`/`Explosionlevel`, `BlockDestroy`/`Blockdestroy`, `BulletStability`,
  `damageRange`, `KnockBack`/`knockback`, `KnockBackY`/`knockbackY`,
  `CanBounce`, `BounceRate`/`bouncerate`, `BounceLimit`/`bouncelimit`,
  `Resistance`/`resistance`, `Acceleration`/`acceleration`,
  `AccelerationDelay`/`accelerationDelay`, `AccelerationFuse`/`accelerationFuse`,
  `Bulletmodel`/`BulletModelName`, `ResistanceInWater`, and `fuse`/`bulletFuse`.
- `MultiMagazine` guns with `PerShellReload,true` retain the actual shell stacks
  in their tube as a FIFO queue. Selecting a new shell affects future loading,
  not rounds already in the tube.

Example buckshot ammunition:

```text
Name,12 gauge buckshot
Stack,64
BulletRound,1
BulletPower,5
BulletSpeed,4
BlletSpread,0.12
ShotGun_Pellet,9
CustomMagazine,12g_buckshot
```

Example slug projectile count:

```text
Name,12 gauge slug
BulletRound,1
ShotGun_Pellet,1
CustomMagazine,12g_slug
```

## Add fast TXT-only gun settings reload command

- `/reloadsettings` continues to reload gun TXT settings and invalidate/reload
  changed 3D models for live model development.
- Add `/reloadsettingsnomodel` to reload gun TXT settings while retaining the
  currently loaded 3D models and their OBJ/MQO and GPU-resource caches.

## Fix startup models while preserving live `/reloadsettings` refreshes

- Keep Forge's immediate resource-listener registration callback from releasing
  gun and vehicle models that were loaded and assigned during pre-initialization.
- Reserve destructive OBJ/MQO cache and GPU-resource invalidation for the explicit
  HMG and HMV settings reload paths, which immediately reparse packs and replace
  renderer model references.
- Preserve VBO and legacy display-list cleanup for repeated live model refreshes.

## Restore live model reloads through `/reloadsettings`

- Invalidate the gun-maker, OBJ, and MQO model caches before re-reading pack
  settings so existing gun and vehicle render registrations receive freshly
  parsed model instances.
- Release obsolete OBJ VBOs and legacy OBJ/MQO OpenGL display lists on the client
  thread, keeping repeated developer reloads from leaking GPU resources.
- Reuse the existing settings reload and renderer replacement paths without
  recreating registered items or entities.

## Fix CustomNPC+ held HMG gun orientation

- Scope the Forge render-helper workaround in all three HMG gun renderers so an
  equipped gun omits the generic `BLOCK_3D` helper only while a CustomNPC+ NPC is
  being rendered, while retaining its custom `EQUIPPED` renderer and all other
  render-type support.
- Prevent CustomNPC+ from applying its generic 3D-item transform before HMG's own
  non-player gun transform, which previously combined two incompatible transform
  systems and made aimed guns point sideways.
- Restore the original helper behavior for players and every non-CustomNPC entity,
  while preserving first-person, dropped-item,
  inventory, attachment, skin, reload, animation, LittleMaid, and configured gun
  transforms.

## Fix gun Motion mouse sensitivity scaling

- Correct the previous sensitivity implementation, which incorrectly introduced a
  second `Weight` system, by deriving held-gun sensitivity from the existing
  `Motion` handling value instead.
- Scale the bundled packs' small Motion differences with a monotonic, bounded curve
  while preserving normal sensitivity at `Motion,1.0` and safe handling of invalid
  values.
- Keep existing gun definitions compatible without content-pack migration and avoid
  registering the render-tick handler on both event buses.

## Fix Gun Smithing Table NEI runtime-state lookup

- Gun Smithing Table NEI output lookup no longer compares mutable gun runtime
  NBT, so tooltip initialization, loaded magazines, fire modes, and attachments
  do not hide a gun's base recipe.
- Pressing `R` on guns and stateful ammunition now consistently resolves the
  correct table recipe, while real metadata variants and recipe-defined output
  NBT remain distinct and every matching recipe remains visible.
- Isolate each NEI recipe and usage query to prevent an unsuccessful lookup from
  retaining an unrelated prior result; registration continues to use the proven
  MCHO `API.registerRecipeHandler` and `API.registerUsageHandler` pattern.

## Refactor HMG Ore Dictionary and NEI support

- Follow MCHO's Forge 1.7.10 recipe approach by automatically normalizing ordinary
  Gun Smithing Table item inputs to their first valid Ore Dictionary entry while
  preserving counts, explicit `ore:` forms, wildcard metadata, and tagged stacks.
- Add `exact:modid:item[:meta[:count]]` for packs that intentionally require a
  specific registered item even when Forge associates it with an ore entry.
- Register the client-only Gun Smithing Table recipe and usage handler through
  NEI's supported `API` methods, with cycling ore alternatives sourced from the
  same canonical table recipes used by GUI and server crafting.
- Keep Gun Smithing Table-only recipes outside vanilla crafting; NEI visibility
  reads directly from the table registry and requires no vanilla recipe copy.

## Fix Gun Smithing Table recipe discovery in NEI

- Replaced the previous Gun Smithing Table NEI entry point because its class name
  did not satisfy NEI 1.7.10 plugin discovery and it therefore never registered
  the recipe and usage handler.
- Gun Smithing Table recipes now have direct, live NEI recipe and usage support,
  including exact/NBT stacks, Ore Dictionary alternatives, stack amounts, and
  every distinct gun and ammunition recipe.
- Gun Smithing Table-only content-pack recipes no longer need or receive duplicate
  vanilla crafting registrations merely to be visible in NEI.

## Gun Smithing Table recipe system refactor and NEI integration fix

- Replace the split gun/ammunition lists and global crafting-recipe scan with one
  canonical, category-aware Gun Smithing Table recipe model and immutable queries.
- Make the GUI, server crafting transactions, pack loaders, and NEI recipe/usage
  lookups consume the same exact-stack and Ore Dictionary ingredient definitions.
- Keep every distinct duplicate-output recipe and expose both Guns and Ammo through
  NEI's dedicated, localized `Gun Smithing Table` category without fake crafting
  recipes or name-based ammunition heuristics.

## Tune gun weight mouse sensitivity diagnostics

- Retuned gun-weight mouse sensitivity for mass-like content-pack values, including
  a subtle effect for the default weight and a lower safety clamp for heavy weapons.
- Added disabled-by-default, held-gun-change diagnostics and a separate debug-only
  forced `0.10` multiplier for verifying the render-tick input hook without log spam.
- Track whether a gun definition explicitly supplied `Weight` so diagnostics identify
  guns silently using the parser default.

## Fix Gun Smithing Table NEI integration

- Mirror both Gun Smithing Table pages in the custom NEI category: registered gun
  recipes and the combined ammo recipes, including compatible recipes discovered
  from Minecraft's `CraftingManager`.
- Match crafting-result and ingredient-usage lookups against the complete table
  recipe population while preserving Ore Dictionary alternative cycling.
- Suppress only exact cross-list recipe duplicates, retaining distinct recipes
  that happen to produce the same item.
## Add model-based attachment rendering

- Add the optional comma-based `attach3dmodel,{model}` attachment key while
  retaining the legacy icon and embedded gun-part paths when it is absent.
- Add `attachmentlocation,x,y,z[,rotation]` as a shared local gun-model anchor
  for every installed model attachment, with safe per-file numeric diagnostics.
- Reuse cached HMG pack models, existing model textures, item renderer plumbing,
  and the gun's existing `Items` attachment NBT slots in inventory, first-person,
  third-person, entity, and placed-gun rendering paths.

## Fix NEI 1.0.5.120 Gun Smithing compatibility

- Register Gun Smithing recipe and usage handlers through NEI 1.0.5.120's
  `GuiCraftingRecipe.craftinghandlers` and `GuiUsageRecipe.usagehandlers` lists.
- Remove recipe catalyst registration because NEI 1.0.5.120 does not provide the
  newer catalyst APIs; recipe and ingredient lookup remain available through NEI.

## Add crafting-based gun skin removal

- Add a dynamic shapeless recipe that removes an applied gun skin when its gun is
  crafted with the same stable-ID skin item or with one dye.
- Accept vanilla dyes and modded dyes using standard Forge `dye*` Ore Dictionary
  entries while rejecting different skins and unrelated extra ingredients.
- Copy the complete gun stack and remove only the root `GunSkin` NBT value so all
  ammunition, attachment, damage, naming, and firing state remains intact.

## Fix Gun Smithing Table crafting and add NEI recipe support

- Deliver Gun Smithing Table outputs immediately from the server, drop only an
  insertion remainder, and explicitly synchronize the player and open containers.
- Serialize each inventory transaction so rapid requests cannot consume the same
  ingredients twice, and reject requests unless the smithing container is open.
- Add optional NEI integration with a dedicated Gun Smithing Table category,
  table catalyst, distinct duplicate-output recipes, and cycling ore alternatives.
- Compile against NEI's development API without packaging it as an HMG runtime
  dependency, and document recipe discovery for content-pack authors.

## Gun weight mouse sensitivity

- Reduce first-person mouse sensitivity while a unified HMG gun is held, using the
  gun definition's existing `Weight` value and a clamped weight curve.
- Combine gun-weight sensitivity with the existing ADS zoom adjustment for the same
  render camera update, then restore the user's exact configured sensitivity.
- Leave GUI input, unarmed/non-HMG input, placed guns, and vehicle cameras unchanged.

## Fix M16A4 skinned transparent reticle group selection

- Associate the M16A4 red-dot reticle marker with its actual `mat4`,
  `mat4reticle`, and `mat4reticlePlate` MQO object family instead of the
  nonexistent `mat41` family.
- Make model-group discovery color- and depth-write-free so initialization can
  never turn a transparent reticle plate into an invisible depth occluder.
- Preserve the existing isolated gun-skin housing overlay and depth-tested,
  original-texture reticle pass.

## Fix HMG transparent sight reticle rendering

- Isolate the reticle and stencil-plate pass from gun-skin OpenGL state, explicitly
  restoring normal alpha blending and `GL_LEQUAL` depth testing for both skinned and
  unskinned guns.
- Keep reticle-plate generation stencil-only by disabling depth writes, then render
  the reticle against both that stencil and the existing opaque scene depth so gun
  bodies and optic housings occlude it without making transparent lenses opaque.
- Restore the renderer's incoming pass after rendering reticle children instead of
  leaving the shared pass selector set to the transparent pass.

## Fix HMG sight housing gun skin rendering

- Apply universal gun skins to gun-owned iron-sight, red-dot, and scope housing parts
  even when their model definitions mark them as attachment parts, while continuing
  to exclude unrelated attachments, bullets, lights, lasers, and reticle/plate parts.
- Mask the overlay against the opaque base render with an exact depth comparison, so
  transparent and semi-transparent lens pixels remain on their original render path.
- Remove polygon offset from the overlay pass because the repeated geometry now uses
  the opaque pass's matching depth without writing new depth.

## Fix universal HMG gun skins

- Replace target-pair recipe generation with one dynamic recipe accepting exactly one
  `HMGItem_Unified_Guns` stack and one valid skin in either grid order.
- Preserve the complete input gun stack while setting or replacing only its `GunSkin`
  NBT value, allowing loaded, damaged, attached, named, and previously skinned guns.
- Remove the gun-name map, target matching, target warnings, and gun-specific AK47 and
  PKM development skins; legacy `SkinTarget` lines are parsed and deliberately ignored.
- Resolve overlays solely from the skin ID on each rendered gun stack, keeping skins
  instance-local and universal across gun models while excluding attachment parts.
- Update the content-pack documentation and universal `ar_sample_skin` definition.

## Fix HMG gun skin crafting with concrete recipes

- Track every final unified gun object by its pack-declared `GunName`, including
  reused registered guns, and generate one exact-object crafting recipe for each
  valid gun/skin target pair during Forge initialization.
- Preserve the input gun stack and all existing state while applying `GunSkin`, and
  report recipe totals plus every unresolved target once during startup.
- Document that skin crafting is resolved after pack loading without crafting-time
  Forge registry identifier lookups.
- Add development pack skin definitions for the real `AK47` and newer-pack `PKM`
  declarations so concrete recipe registration can be exercised beyond `AR_sample`.

## Fix HMG gun skin crafting identity matching

- Match skin targets against each gun item's actual Forge unique identifier instead
  of reverse-looking-up the target, with bare HMG `GunName` targets preferred and
  legacy fully-qualified targets retained.
- Trim and reject empty target fields, validate skin targets once after pack loading,
  and document the canonical content-pack identifier format.
- Update the Addfixing sample to target `AR_sample` by its gun declaration identifier.

## Fix the HMG gun skin pipeline

- Corrected the Addfixing sample so its inventory icon, exact `HandmadeGuns:AR_sample`
  registry target, and model texture all resolve through existing pack resources.
- Fixed the sample's root failure: `SkinTexture` named an overlay PNG that was never
  included in Addfixing, so the loader disabled the overlay before rendering.
- Moved missing-resource fallback to a cached client render-time check instead of
  storing client resource state on shared skin metadata.
- Standardized compatibility checks on exact Forge registry identifiers and kept
  texture-path construction common/server safe.
- Reset the legacy overlay pass color to white while retaining isolated blend, depth,
  and polygon-offset state.
- Resolve each exact `SkinTarget` to its registered gun `Item` for crafting and
  rendering compatibility, so damage and per-stack gun state never affect matching.
- Restore the OpenGL current-color state after each transparent overlay pass.

## Data-driven gun skins

- Added ordinary content-pack gun skin items with stable identifiers, cached overlay resources, and per-skin compatible gun registry identifiers.
- Added one generic shapeless skin application recipe that copies the gun and changes only its `GunSkin` NBT value.
- Added transparent base-model overlay rendering to the legacy and current unified gun renderers, with isolated fixed-function OpenGL state.
- Documented the skin pack keys, texture layout, crafting behavior, and an Addfixing attachment template.

## HMG semantic version and automatic build numbers

- Added `HMG/version.properties` with manually managed `mod_version` and Gradle-managed `build_number` values.
- Updated the HMG Gradle build to compute `mod_version.build_number`, expose `modVersion`, `buildNumber`, and `fullVersion`, and apply the computed version to generated jars.
- Added production-build-only build number increments with rollback on failed builds so failed packaging attempts do not consume version numbers.
- Documented the release workflow so future releases only require editing `mod_version`.

## HMG → Combatives aim recoil ownership

- Added a dedicated client-side HMG aim-recoil controller for accepted Combatives shots so sustained fire now changes actual local pitch/yaw instead of relying only on short-lived visual camera impulses.
- Added contribution-based recoil recovery, burst accumulation caps, deterministic horizontal drift, mouse counter-recoil accounting, and safe reset behavior for invalid players, death, and dimension changes.
- Tuned Combatives visual recoil to remain a punch/roll/translation layer with reduced sustained pitch so persistent aim climb is owned by the HMG aim controller.
- Documented the actual aim recoil, visual recoil, delayed recovery, diagnostics, configuration, and legacy fallback split.

## HMG Combatives Camera Recoil Integration

- Added optional Combatives camera recoil integration for HMG first-person weapon fire.
- Routed compatible recoil through Combatives camera impulses with dynamic first-shot punch, sustained-fire pressure, horizontal drift continuity, and burst reset/recovery behavior.
- Preserved HMG legacy recoil as the fallback when Combatives is absent, disabled, or its camera API is unavailable.
- Added `Compatibility.enableCombativesRecoilIntegration` with a default of `true`.
- Corrected Combatives recoil ownership so legacy recoil is only suppressed after a shot impulse is accepted, strengthened the independent base pitch kick for single shots and shotguns, split kick/punch/sustained source IDs, and documented full yaw recoil support.
- Added `Compatibility.enableCombativesRecoilDebug` diagnostics for impulse submission, fallback, and weapon-state reset investigation.

## Gate spam logging behind debug config

- Added `Logging.enableDebugLogging` for verbose HMG startup/content-pack diagnostics.
- Gated content-pack resource confirmations, script confirmations, recipe success logs, and registration timing summaries behind debug logging while preserving errors, warnings, and concise registration-complete summaries by default.
- Updated configuration and server administration documentation for the quieter default logging behavior.

## Gun Smithing Table ore dictionary recipes

- Reworked Gun Smithing Table recipe ingredients to store exact-stack and ore-dictionary requirements explicitly instead of converting ore entries into a preview stack.
- Added deterministic shared inventory allocation for Gun Smithing Table GUI checks, gun crafting, and ammunition crafting so mixed-mod ore equivalents are validated and consumed consistently server-side.
- Added debug-only diagnostics and a development verification helper for exact, ore dictionary, mixed-stack, wildcard/metadata, late-registration, failed-validation, and no-double-count allocation scenarios.
- Documented ore dictionary pack recipe syntax for content authors, including optional required amounts such as `ore:ingotCopper:5`.

## Unreleased

### Documentation

- Refocused root and extended documentation on the actively maintained `HMG/` module, Handmade Guns Overdrive.
- Replaced GVC-centric guidance with HMG installation, dependency, compatibility, configuration, command, server, and content-pack documentation.
- Documented active `HandmadeGuns.cfg` keys and defaults from `HandmadeGunsCore`.
- Documented HMG commands `/reloadSettings` and `/hmgmanual` with permission levels and practical usage notes.
- Added a content-pack guide for `handmadeguns_Packs`, legacy pack paths, supported pack folders, resource handling, and `additionalSettings.txt`.
- Recorded HMG-specific undocumented systems discovered during the pass and remaining documentation gaps.

## BackTools HMG 3D back rendering

- Added a client-side BackTools compatibility bridge that reuses HMG Overdrive gun item renderers for guns shown on a player back.
- Preserved BackTools legacy back rendering for vanilla and non-HMG items, and safely falls back when a gun renderer is missing or fails.
- Added throttled diagnostic logging for custom back-render decisions and failures.

## BackTools HMG back-render stability follow-up

- Fixed HMG BackTools back rendering to copy and render only BackTools' remembered stack, skipping the back render when it matches the currently held stack.
- Removed render-time mutation of BackTools' stored item map to avoid current-weapon substitution and flicker.
- Adjusted the global back-mounted HMG gun pose so models lie flatter against the player back, and disabled culling only inside the isolated custom render path.

## BackTools HMG held-gun suppression and roll fix

- Suppressed HMG back rendering whenever the player is currently holding any HMG gun, preventing held weapons from appearing on the back.
- Restored the scoped BackTools legacy-icon suppression while HMG custom back rendering is active so only the 3D model appears after switching away from HMG guns.
- Added a final back-gun roll adjustment so the grip/bottom points downward while preserving the back-plane alignment and diagonal barrel pose.

## BackTools HMG back-gun transform correction

- Restored the working back-plane alignment for HMG back guns and moved the grip/bottom roll before the diagonal direction rotation so the model stays flat against the player's back.

## (5f21dfc Update Combatives camera recoil integration)

- Audited HMG recoil against the current Combatives camera API and documented its capabilities, validation, render semantics, continuous-effect tradeoffs, and future firearm integration opportunities.
- Added cached API-version and capability discovery with conservative degradation for missing or older optional camera surfaces.
- Fixed exact-shot recoil ownership so an active Combatives installation can no longer erase legacy recoil queued after a rejected submission.
- Added capability-gated yaw, roll, translation, and FOV construction plus burst cleanup on trigger release and reload, while retaining existing death, dimension, and weapon-switch cleanup.

## Fix HMG skin rendering across item views

- Rendered universal gun-skin overlays during HMG's guaranteed opaque model pass so they appear once in first person, third person, dropped-item, and model-inventory rendering without relying on Forge to provide item render pass 1.
- Preserved per-stack skin selection and the exclusions for attachment and bullet model parts; legacy scripted guns remain supported when their scripts use the renderer's `renderpartofmodel` helper.
- Updated repository ignore rules so `HMG/eclipse/handmadeguns_Packs` content-pack sources are tracked while other HMG Eclipse runtime files remain ignored.

## Add gun skin usage tooltips

- Add a localized tooltip to every gun skin item explaining that skins are applied
  by crafting them with a compatible gun.
- Show universal unified-HMG-gun compatibility for valid skins and safely identify
  invalid skin data.

## Fix model-based attachment rendering (PR pending)

- Fixed comma and assignment parsing for `attach3dmodel`, `3dmodeltex`, and
  `attachmentlocation` without changing legacy key parsing.
- Added cached extension resolution, dedicated model textures, order-independent
  attachment renderer finalization, and isolated installed/inventory GL state.
- Centered and scaled model attachment inventory icons and added dropped-item
  model rendering while retaining legacy attachment visuals and behavior.

## Fix 3D attachment inventory icon regression (PR pending)

- Restored the fixed attachment inventory centering correction removed by the
  configurable-transform change while keeping pack-authored offsets independent
  of the configurable scale multiplier.
- Corrected the `Attachment Test` fixture to use the visible `InventoryScale,1.0`
  baseline and documented multiplier and offset semantics.
- Guaranteed balanced inventory matrix state and reset the render color to
  opaque white while the attachment texture remains bound.
## Hot-reload HMG inventory model transforms (PR pending)

- Made 3D attachment inventory renderers read scale and offset from the live
  registered item instead of retaining startup snapshots.
- Extended settings-only reloads to update existing attachment items without
  re-registering items, recipes, models, or renderers.
- Added gun `InventoryOffset,x,y,z` parsing and inventory-only rendering with
  scale-independent displacement, including the M26 grenade pack setting.

## 54be881 Prevent Angelica startup VBO crash

- Stop using `renderAll()` as a model-initialization hook during HMG pre-initialization.
- Keep OBJ VBO and legacy display-list creation lazy until the model's first normal
  render, after the client has entered a managed render frame.

## Fix HMG-O model Tessellator ownership (4696756)

- Compile OBJ and MQO model geometry with locally owned Tessellators so inventory,
  NEI, equipped, and world item rendering cannot interrupt the global drawing
  session used by Angelica/Celeritas or another renderer.
- Balance every model Tessellator drawing lifecycle with `draw()` in a `finally`
  block while retaining the existing display-list output and Forge 1.7.10 fallback.

## (193f051 Preserve renderer state around HMG OBJ VBO draws)

- Fixed the OBJ VBO renderer replacing its caller's array-buffer binding with zero and disabling the caller's vertex, normal, and texture-coordinate arrays after every gun-model group.
- Preserve complete client-array state and restore the prior array-buffer binding and matrix mode through structured cleanup, including when model drawing fails.
- Keep VBO upload equally isolated, preventing lazy compilation during NEI/inventory rendering from detaching a renderer-owned buffer.

## (4be9060 Scope BackTools HMG rendering to player events)

- Added a single-use, thread-local BackTools render scope containing the exact `RenderPlayerEvent` player and exact private `ItemStack` selected for the back render.
- Require that scope before BackItemRenderCompat performs renderer lookup or HMG rendering, so NEI, inventory, dropped-item, and unrelated item-renderer calls cannot qualify by item class or `ENTITY` render type.
- Guard expanded source, owner, scoped-stack, incoming-stack, render-type, and acceptance diagnostics behind the existing debug-logging configuration.
- Re-audited the separate OBJ VBO fix: legitimate HMG renders continue to preserve caller-owned client-array, array-buffer, and matrix state.

## (8978b05 Fix HandmadeGuns BackTools render ownership)

- Preserve BackTools' exact remembered-versus-held stack equality behavior so switching from HMG gun A to gun B still renders remembered gun A on the player's back instead of suppressing every HMG-to-HMG switch.
- Restrict candidate diagnostics to the compatibility package and describe their exact selected-stack equality result.
- Document the GTNH-NEI grid path, its `INVENTORY` renderer dispatch and render-state recovery, and why changing compatibility stack identities came from per-player private back-stack copies rather than NEI clones.

2026-08-29 00:00 — Align HMG player point of aim with Combatives

- Added a cached, common-side optional bridge to Combatives' authoritative interaction ray, with exact legacy fallback when the mod or compatible API is unavailable.
- Player-fired handheld projectiles now use synchronized effective eye geometry and gameplay body yaw/pitch while preserving elevation offsets, spread, and the existing forward spawn distance.
- Removed the legacy post-construction head-yaw override only for projectiles that used the authoritative ray, and aligned handheld player lock-on direction and block-ray origin without changing placed guns, turrets, vehicles, or NPC firing.
- Source-level audit and call-site searches were performed; build and in-game validation remain outstanding per the task's no-build instruction.

2026-09-09 02:59 — Bound third-person ADS item-use compatibility to rendering

- LivingEventHooks now captures and restores the prior item-use stack/count around player rendering, retaining the existing Flan's bow-style ADS signal through armour and held-item rendering. Reusable scopes support nested renders, and the Pre hook runs at LOWEST priority to respect earlier cancellation.
- Previously the synthetic item-use state escaped into Minecraft.runTick, suppressing normal right-click firing before C08 packet creation. This HMG defect does not require Combatives; the swapped-key PacketTriggerHeld path bypasses that gate. No aim, spread, recoil, server packet or projectile behavior was changed.
- Source-traced normal and swapped input through trigger handling, readiness checks and authoritative projectile construction. Java 8 :HMG:compileJava was attempted but dependency resolution failed offline for vecmath, InventoryTweaks and OpenComputers before compilation. No packaged or in-game success is claimed; both key layouts and integrated/dedicated servers still need testing.
