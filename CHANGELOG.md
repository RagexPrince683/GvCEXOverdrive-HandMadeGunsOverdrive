# Changelog

2026-09-09 22:12 — Add opt-in named gun animation foundation

- Added immutable animation definitions, strict versioned JSON loading, local rigid-part keyframes, named clips, BASE/ACTION/ADDITIVE playback, priorities, interruption, current-pose crossfades, reverse/loop/hold playback and presentation-only event markers. Definitions contain no Minecraft client or OpenGL dependencies; no gameplay action is driven by an animation event.
- Added `Animations,animations/name.json` relative to the content-pack root, contextual diagnostics with legacy fallback, cached resources and reload invalidation. The new evaluator feeds the existing HMG part transforms without replacing OBJ/MQO meshes, VBO/display-list caching, pivots, visibility rules or legacy timing.
- Isolated playback by weak stack/owner/render-context identity, including stable attachment-preview copies and nested under-gun paths. Added optional local inspect input (unbound by default) and a client event/API hook. Existing gun state requests visual fire/reload/cock clips; no ammunition, projectile, vehicle gameplay or networking logic was replaced.
- Added author documentation, an opt-in bolt/magazine example and a dependency-free non-GL regression suite. Java 8 `:HMG:compileJava` and `:HMG:animationTest` passed with 74 checks; standalone Java 8/Gson tests also passed. Initial offline dependency resolution failed; fetching the existing declared dependencies allowed the unchanged build to succeed.
- In-game first/third-person, NPC, GUI, nested/mounted/placed, reload/inspect/switch, resource reload and dedicated-server startup checks remain outstanding. Automatic visual requests observe existing NBT/render state and cannot reconstruct every action missed between observations; no synchronized shot timeline or automatic holster scheduling is claimed.

2026-09-10 02:26 — Add player infinite ammo and repair impact/armor pose validation

- Added a server-only Creative/admin ammunition predicate and `/hmg infiniteammo <player> <true|false>` (permission 2), persisted with Forge player data across respawn/relog. Unified gun reloads and per-shell commits supply ammunition without consuming reserves; custom-magazine filling and the legacy gun-sword reload share the policy. Loaded rounds, chamber/bolt logic, restrictions and disposable weapon consumption remain unchanged. Mark supplied magazines to prevent collectible ejection duplicates after disabling/transferring the gun.
- Replaced the generic hurt-event ray and miss-means-headshot fallback with per-impact HMG damage classification against the actual victim bounds/eye region. Retained the 1.75 multiplier and sound, removed victim-wide per-tick suppression, and sorted traced entity candidates by distance. The existing optional common-side Combatives bridge provides authoritative pose eye height; no client headshot flag or client-only class dependency was added.
- Included transient `set_up` in the existing render-scoped bow-use bridge. Flan's now receives the same aiming input before its final biped-to-TurboModel copy; Combatives crawl, lean, cleanup and rotation-order code is untouched.
- Java 8 (Corretto 1.8.0_502) offline `:HMG:compileJava :HMG:animationTest` succeeded; the existing animation suite passed 74 checks. Source inspection covered reload ownership, direct impact call sites, Forge respawn persistence and Flan's reference renderer. No new test infrastructure, packaging, server launch or in-game validation was performed. Survival/Creative/admin toggling, non-op rejection, death/relog, unusual reloads, head/shoulder/prone hits and combined Flan's/Combatives poses require in-game checks. Head regions remain an approximation for unusual anatomy.
2026-09-10 02:31 — Make infinite ammo a global admin toggle

- Replaced the per-player command with `/hmg infiniteammo` to toggle all players, or `/hmg infiniteammo <true|false>` to set it explicitly. Permission level 2 remains required. The shared policy reads the overworld `hmgInfiniteAmmo` gamerule across dimensions, including future logins; it defaults to false and persists through the normal world-save lifecycle. Old per-player flags are ignored so global disabling applies to every non-Creative player. Creative and existing reload/consumption behavior remain unchanged.
- Updated command help, completion and documentation. Java 8 offline `:HMG:compileJava` passed; source/diff checks passed. Multiplayer toggling, late joins, dimension travel and save/restart persistence still require in-game validation.

2026-09-10 03:02 — Reject weighted jumps before vanilla movement

- Replaced conflicting post-jump handlers with a common weight policy and pre-jump rejection. Removed velocity clamps, fall/airborne resets and downward correction teleports; active flight/riding bypass weight rejection. Heavy mobility <= 0.70 now rejects directly, light >= 0.95 permits vanilla jumps, and medium 3/6-tick landing delays remain without vertical clamps.
- Added optional reflection registration into Combatives' jump resolver and a standalone Forge ASM fallback with MCP/SRG targets, manifest and development loading configuration. Removed the obsolete HMGJumpHandlerClient. Crawl/geometry, horizontal mobility and ordinary movement packets are unchanged.
- Added server-to-client mobility snapshots on login/reload, connection-scoped immutable reception and separate logical-side timer state with lifecycle cleanup. Client prediction never reads local gun mobility; vanilla C03 trust and in-flight prediction differences remain, without corrective movement writes.
- Retained opt-in `-Dhmg.jumpTrace=true` reproduction diagnostics and documented ownership, thresholds, protocol compatibility and remaining tests. Proceeded from source evidence at user direction; no before/after runtime trace was captured.
- Java 8 offline `:HMG:compileJava` and Combatives `compileJava`/Mixin processing passed; cached Forge MCP/SRG transformed classes passed ASM structure/dataflow checks. No packaging, reobfuscation or runtime launch. Full creative-flight, landing, switching, crawl, dedicated/integrated and policy-sync regression matrix remains in-game work.

2026-09-10 03:21 — Use physical block properties for HMG blast cover

- Updated `HMGExplosion.hmgBlocksExplosionDamage` to ignore non-movement-blocking materials, location-sensitive replaceable blocks, webs explicitly, and blocks without collision boxes. Preserved the existing leaves exclusion and removed the vanilla plant blacklist and opacity/render-type filter; stone and physically substantial transparent blocks such as glass, fences, doors and slabs qualify as cover.
- The checked-in implementation already skipped vanilla vegetation and webs, so source inspection did not establish the cause of the reported vegetation attenuation. The confirmed defect was using visual opacity to exclude physical cover. Changes remain local to HMG entity exposure; server ownership, damage/falloff formulas, block destruction and the existing quarter-block ray sampling are unchanged. Partial blocks still use the existing occupied-cell approximation rather than exact ray/shape intersection.
- Inspected cached Forge 1.7.10 block/material sources and the server-only projectile explosion call path. Java 8 offline `:HMG:compileJava` passed after using the existing Gradle cache with the required filesystem access. Source checks show grass/ferns/flowers/crops/saplings and webs are skipped at every sampled cell, including repeated layers, while stone/glass retain cover eligibility. No packaging, runtime launch or in-game damage measurements were performed; vegetation layers, stone/glass walls, partial blocks and dedicated-server behavior still need in-game validation.

2026-09-10 03:24 — Separate configured blast damage falloff from height

- Updated `HMGExplosion.doExplosionA` and `getExplosionDamage`: configured damage now requires vertical separation from entity position of at most the original radius `R` and uses horizontal `sqrt(dx*dx + dz*dz)` instead of full 3D distance. Preserved full damage through `R`, linear falloff to zero at `2R`, and terrain exposure multiplication. Removed the spherical/eye-direction damage gate for this path so valid cylinder targets and targets at the explosion's eye-based direction origin can receive damage.
- Preserved 3D knockback eligibility, direction, strength and player packet values, the GVC-used legacy unconfigured damage formula, server ownership, block destruction and the previous occlusion fix. Documented the cylindrical volume, abrupt vertical cutoff and differing damage/knockback reach in `docs/compatibility.md`.
- Java 8 offline `:HMG:compileJava` passed. Seventeen mathematical checks covered equal horizontal distance at heights 0/1/3/near the limit, directly above, below, outside vertical reach, horizontal boundaries, the cylinder beyond the old sphere and exposure multiplication. Source/diff inspection confirmed unchanged stone-cover checks and knockback formulas. No runtime launch, packaging or in-game damage measurements; actual height/terrain/stone-wall comparisons and dedicated-server validation remain outstanding.

2026-09-10 16:49 — Import native Blockbench gun models and animations

- Added opt-in BlockbenchModel,models/name.bbmodel for unified guns. Imports v4/v5 outliner bones with UUID identity and authored HMG part names, cube geometry/pivots/rest rotations, per-face/box UVs, embedded or pack-local PNG textures, and embedded clips without OBJ export, AddParts declarations or separate animation JSON.
- Reused Phase A playback, instance isolation, action triggers and presentation events. Added independent numeric channels, scale composition, split keys, step/Catmull-Rom/Bezier sampling and sparse-channel layering. Centralized Blockbench-to-HMG transforms; preserved authored durations and trailing interpolation control keys. Added TaCZ idle/fire/dry-reload aliases and first-person hand anchors. OBJ/MQO and Phase A JSON behavior remain supported; gameplay/network authority is unchanged.
- Integrated imported meshes with existing VBO/display-list rendering, skin overlays and explicit HMG model reload resource replacement. Documented pack syntax, the full AK audit, conventions and unsupported features in the existing authoring/content-pack guides.
- Reference AK parsed: 99 bones, 857 cubes, one camera helper, embedded 256x256 PNG with 128x128 UV resolution, 14 original clips plus four aliases. Java 8 compilation and all 74 existing Phase A checks passed. In-memory reference comparisons passed for 31,260 animation component samples (maximum error 1.14e-13) and 8,048 vertices/UVs across 2,012 faces (maximum position error 2.97e-8), including all 99 parent links. No new test artifacts, packaging, remapping or runtime launch.
- Visual fire/reload/inspect acceptance, arms, render passes, reload lifecycle and dedicated-server startup remain unverified in-game. Mesh/armature/billboard geometry, cube stretch/rescale, global/quaternion channels, plugin easing, nonnumeric Molang, multi-file rigs and bone bindings/reset are unsupported. TaCZ Lua/camera/constraint/view/attachment/ammunition logic and effect playback are not imported; effect keys are presentation markers only.

2026-09-10 17:00 — Add Blockbench AK example pack entry

- Added `AKM_Blockbench.txt`, cloning the existing AKM stats and attachments while using `models/cod4_ak.bbmodel` and its embedded animations. Its 52-tick reload timer matches the embedded 2.6-second tactical reload clip.
- Copied the reference AK Blockbench project into the GVCguns pack so content authors can load the example directly without an OBJ export or manual `AddParts` declarations. The empty-magazine 3.4-second clip remains selected by ammo state and may require a longer timer if a pack wants to match that branch exactly.

2026-09-10 17:12 — Enforce HMG pack ownership before gun importing

- Added ownership checks for immediate packs under the existing normal and legacy HMG roots, used by startup resource/definition/script/recipe enumeration and settings reload. Direct gun loading rejects external or redirected gun paths before parsing; Blockbench and animation JSON resolve their root through that same check. Flan directories cannot enter the gun importer via these entry points. Existing pack formats and OBJ/MQO resource lookup remain unchanged.
- Source and production-instance inspection found no recursive Blockbench discovery or Flan fallback. TaP-Escalation is registered from `Flan/TaP-Escalation` by Flan's Mod; the only production `BlockbenchModel` directive is the HMG `GVCguns/guns/AKM_Blockbench.txt`. The ownership gap was trust in caller-supplied pack/gun locations.
- Java 8 offline `:HMG:compileJava :HMG:animationTest` passed, including all 74 existing animation checks. Reviewed ownership/path and reload call sites; no new test infrastructure or runtime launch. Co-installed Flan/TaP-Escalation and HMG in-game loading still requires runtime validation.

2026-09-10 17:35 — Defer Blockbench texture upload until rendering

- Fixed the production startup crash in `BlockbenchModel.texture`: Forge 1.7.10 calls mod pre-initialization before `Minecraft.renderEngine`/`TextureManager` is constructed, while HMG was creating `DynamicTexture` during gun TXT parsing. Model parsing and embedded PNG decoding remain CPU-only; the first actual item render now registers the texture and updates the renderer's primary texture reference.
- Production evidence confirms the crashing model is the HMG `GVCguns` AK fixture (identical SHA-256 to the repository copy), not TaP-Escalation. The crash report lists TaP because Flan loaded it as its own content pack. Texture disposal tolerates the same early lifecycle as a secondary safeguard.
- Java 8 offline `:HMG:compileJava :HMG:animationTest` passed, including all 74 existing animation checks. Source inspection confirmed Forge's texture manager is constructed after mod pre-initialization. The production client was inspected read-only and was not relaunched.

2026-09-10 17:56 — Correct Blockbench model-space transforms

- Replaced the incorrect Y-axis half-turn with the TaCZ-compatible Z-axis half-turn for imported geometry, pivots and animation channels. Blockbench absolute origins remain parent-relative at runtime, rest and animated rotations compose in ZYX order, and the entire correction stays inside `BlockbenchTransform` before HMG presentation transforms.
- Normalized Blockbench pixels at `3/16` HMG unit so the reference AK matches the legacy MQO firearm baseline with its inherited `ModelScala,0.5`; `ModelScala`, `InworldScale` and `Gunparts_offsetScale` remain content controls and OBJ/MQO units are unchanged.
- `ModelArm,true` now enables imported TaCZ hand locators, whose arm frame and mesh scale follow the corrected model coordinates. A missing or hidden locator falls back per hand to the existing `ModelArmOffset*`/`ModelArmRotation*` path; authored locators take precedence.
- Source comparison covered Blockbench parent-space evaluation and TaCZ geometry, animation and hand-render conversion. Java 8 offline `:HMG:compileJava :HMG:animationTest` passed, including all 74 existing animation checks. No runtime launch or packaging was performed; in-game fire/reload/inspect and arm alignment remain to be checked.

2026-09-10 22:08 — Separate Blockbench display basis and stabilize playback ownership

- Added one root-traversal X half-turn between TaCZ model space and HMG presentation, covering equipped, dropped, GUI and nested gun renders. Preserved the `3/16` units and all bone/keyframe conversions. The correction rotates locator arms with the gun; verified the local Z hand frame and both vanilla shoulder/wrist offsets against TaCZ and the AK placeholders instead of adding weapon-specific arm rotations. Legacy arm fallback remains outside the imported frame.
- Custom animation requests are idempotent unless the new explicit restart overload is used; automatic shots still retrigger. Transition snapshots now evaluate the current composed pose before layer mutation, including consecutive requests without an intervening sample. First-person ownership survives vanilla synchronized ItemStack replacement in the same item/slot session instead of restarting draw. Imported opaque/transparent passes share ADS progress.
- Extended the existing animation suite with repeated-state, back-to-back request and rapid-reversal checks. Java 8 offline `:HMG:compileJava :HMG:animationTest` passed with 82 checks, and `git diff --check` passed. No runtime launch or packaging was performed; dropped/equipped orientation, animated hand alignment and ADS transitions still require in-game verification.

2026-09-10 22:45 — Use TaCZ positioning nodes for equipped Blockbench guns

- First-person imported models now align the authored `idle_view` node and blend its inverse rigid transform to `iron_view` during ADS. This bypasses legacy HMG equipped, ADS and sprint placement for Blockbench models while preserving it for OBJ/MQO guns; reload returns to the authored idle view and continues through the shared Phase A action layer.
- Third-person player-held imports align `thirdperson_hand` at the existing 1.7 hand-context basis instead of applying legacy configured model rotation/translation. Dropped, GUI and nested presentation, the global display correction, model units and bone transforms are unchanged.
- The animation suite now parses the reference TaCZ AK and proves its tactical/dry reload mapping, 2.6-second duration, advancing playback and nonzero root pose. Java 8 compilation and all 89 checks passed. User runtime evidence confirms the imported reload pose is rendered in inventory; first-person/third-person placement, ADS, reload visibility and hands still require visual verification.

2026-09-10 23:14 — Consume live first-person gun state and retain the hand origin

- First-person animation playback now reads reload, cock, bolt and ammunition state from the live selected hotbar stack when Vanilla renders an equivalent cached `itemToRender`. Playback identity and the rendered stack remain unchanged, so synchronization cannot hide a reload pose or restart the equipped animation session.
- Blockbench third-person placement retains HMG's fixed 1.7 player-hand origin translation before model scaling and then applies the authored `thirdperson_hand` inverse. Configured legacy model offsets remain bypassed, and OBJ/MQO rendering is unchanged.
- Source inspection covered Vanilla 1.7 `ItemRenderer`, `RenderPlayer`, Forge's equipped-item helper and TaCZ's inverse positioning-node transform. Java 8 offline `:HMG:compileJava :HMG:animationTest` passed with all 89 checks. Runtime verification of first-person reload and third-person grip alignment remains pending.

2026-09-10 23:29 — Give imported reload clips first-person pose ownership

- Blockbench first-person ACTION playback now follows the same tick-stable reload state that already controls the renderer's ADS and sprint transitions. Imported guns traverse from the neutral HMG baseline during reload so legacy `GunState.Reload` transforms cannot take ownership from the Blockbench clip; OBJ/MQO reload presentation is unchanged.
- Source tracing confirmed the resulting ownership path. Java 8 offline `:HMG:compileJava :HMG:animationTest` passed with all 89 checks; first-person reload, hands and sprint placement still require in-game verification.

2026-09-11 04:31 — Add pack-scoped clean asset layout

- Added one deterministic content-pack resolver for definitions, models, textures, Blockbench projects, animations, sounds, and attachment-related resources. Recommended `models/`, `textures/`, `animations/`, `sounds/`, and plural `attachments/` paths resolve before known legacy locations, while absolute paths, parent traversal, canonical escapes, and cross-pack searching are rejected.
- Added concise gun directives such as `Model,akm.mqo`, `Texture,akm.png`, `BlockbenchModel,cod4_ak.bbmodel`, and `Animations,akm.json`. Existing `CanObj`/`ObjModel`/`ObjTexture`, singular `attachment/`, convenience resource folders, and Minecraft-style asset trees remain supported as compatibility fallbacks.
- Blockbench first-person guns no longer consume legacy `SprintingPoint` or `SprintingRotation`; OBJ/MQO sprint presentation is unchanged. Updated the checked-in Blockbench AK example and content-pack documentation to show the clean logical directories.
- Java 8 offline compilation passed. All 89 existing animation checks and 20 focused resolver checks passed, covering clean and legacy resolution, clean-name precedence, definition merging, staging, path escape rejection, and neighboring-pack isolation. In-game validation remains required for both clean and legacy packs.

2026-09-11 12:00 — Migrate repository HMG content packs to clean asset folders

- Moved authored models (`.mqo`, `.mqoz`, `.obj`, `.bbmodel`) to each pack's `models/`, model/source textures to `textures/`, item icons to `textures/items/`, sight overlays to `textures/misc/`, OGG files to `sounds/`, and legacy `attachment/` definitions to `attachments/`. Pack boundaries were preserved; `HMG/src/main/resources/assets/` was not changed.
- Updated migrated TXT references to clean `items/` and `misc/` paths and corrected on-disk case. Verified cross-pack dependencies were copied only into packs that already referenced them, because clean resolution is intentionally pack-local. The existing `assets/handmadeguns/sounds.json` files in Addfixing/aww2pack remain as Minecraft resource-domain metadata; their OGG sources are now clean `sounds/` files.
- Added `repositoryPackAssetTest`, which rejects authored files left under legacy pack asset directories, deprecated path references, path escapes, case mismatches, and missing migrated references. It records a small allowlist of pre-existing missing logical icon/scope references rather than inventing replacement assets.
- Java 8 offline `:HMG:compileJava` passed; the 89-check animation suite, 26-check resolver suite, and 2,566-check repository-pack validation passed. `git diff --check` passed. No packaging or in-game validation is claimed here; the validator reports only the documented pre-existing missing logical icon/scope references.

2026-09-11 05:35 — Separate HMG model, item, and misc textures

- Reclassified repository pack textures into `textures/models/`, `textures/items/`, and `textures/misc/`; no content-pack texture remains at `textures/` root. `HMG/src/main/resources/assets/` remains untouched.
- Replaced the generic resolver texture type with explicit model, item, and misc categories. `ObjTexture`, `ModelTexture`, `SkinTexture`, and `3dmodeltex` now resolve only model textures; `Texture` resolves only item icons; `ScopeTexture` resolves only misc textures. Startup staging mirrors each category to its corresponding Minecraft resource location without cross-category copies.
- Added STG44 regression coverage proving `aww2pack` resolves `stg44.png` independently from `textures/models/` and `textures/items/`, while `itemTextureName()` still rejects a model-texture path. Legacy resource-folder lookup remains supported for third-party packs.
- Java 8 offline `:HMG:compileJava` passed; the animation suite passed 89 checks, the resolver suite passed 32 checks, and repository-pack validation passed 2,584 checks (with only its existing 36 allowlisted unresolved references). `git diff --check` passed. No packaging or in-game validation is claimed here.

2026-09-11 05:46 — Bridge accepted reloads to imported ACTION clips

- Added a server-authorized reload presentation event for the owning client. Accepted manual reloads capture empty versus tactical from the pre-mutation ammunition state, identify the selected slot/item and event, and queue exactly one imported ACTION request with the existing `reload` fallback. Rejected requests send nothing; mismatched client weapons ignore delayed events.
- Removed imported reload startup from `IsReloading` edge inference. The event-owned presentation snapshot now drives both imported animation ownership and renderer reload branching; live `IsReloading` validates/cancels it after a short synchronization grace. Guns without an imported reload clip continue through legacy reload motion.
- Moved the reload request handler's player, inventory, gun NBT and reload mutations onto a server-tick queue because Forge 1.7.10 SimpleImpl invokes handlers on its network thread. Gameplay reload timing, ammunition and magazine authority remain server-owned; the new client packet mutates presentation state only.
- Java 8 offline `:HMG:compileJava :HMG:animationTest` passed; the suite reported 107 checks including the reload bridge cases. `git diff --check` passed. No packaging, runtime launch or in-game validation was performed. First-person empty/tactical playback, aborts, rapid switching and dedicated-server behavior remain to be tested in game.

2026-09-11 06:02 — Let accepted imported reload actions finish naturally

- Removed `IsReloading`-loss and synchronization-timeout cancellation from accepted reload presentation. Gameplay completion cannot be distinguished from an abort by that boolean, so it no longer stops or releases the imported ACTION clip.
- Imported reload pose ownership now follows the accepted pending/playing ACTION until the controller reaches the clip's natural end. Completion is settled on the next animation-clock snapshot so opaque/transparent render passes cannot disagree at the ending frame. Selected-slot/item mismatch still invalidates the action immediately; existing entry removal continues to cover unequip, weapon switch and world change.
- Reworked reload bridge coverage for tactical/empty exactly-once startup, true/false NBT transitions without restart or cancellation, natural duration and ownership release, explicit invalidation, slot/item mismatch and legacy fallback. Java 8 offline `:HMG:compileJava :HMG:animationTest` passed with 109 checks; `git diff --check` passed. No packaging or runtime launch was performed. The corrected full-length animation still requires in-game confirmation; gameplay/visual magazine-event alignment remains a separate concern.

2026-09-11 06:18 — Bundle vecmath for dedicated-server runtime

- Added a Java 8/Forge 1.7.10-compatible Shadow packaging step that carries `javax.vecmath:vecmath:1.5.2` inside HMG at `META-INF/libraries/vecmath-1.5.2.jar`. The existing coremod bootstrap checks for an already available `javax.vecmath.Vector3d` and extracts/adds the bundled fallback only when needed, so companion GVC/WW2/Linker APIs remain binary-compatible and an existing provider is not replaced.
- Disabled the thin jar as the release artifact and registered the embedded archive with ForgeGradle reobfuscation; the existing coremod manifest markers are preserved. Development compilation still uses the normal Maven dependency.
- Java 8 `:HMG:compileJava`, `:HMG:shadowJar --offline`, and `:HMG:reobf --offline` passed. The inspected reobfuscated jar contained the embedded vecmath jar and no top-level duplicate `javax/vecmath` classes. No fresh dedicated-server launch was performed; server startup with the produced jar remains the final runtime check.

2026-09-11 13:20 — Bundle official HMG content and initialize Creative guns

- Added the maintained HMG packs to the mod resources and materialized their read-only JAR source into a private runtime cache solely for the existing file-based parser/resource pipeline. Bundled packs load before `handmadeguns_Packs` and the legacy root; external definitions and resources consequently remain the final override layer, while their existing reload workflow stays filesystem-only.
- Compatible external magazine and attachment definitions now update the already registered bundled item instead of failing a duplicate registration. Incompatible identifier collisions are reported and leave the existing item intact.
- Creative-tab unified guns now begin with normal full loaded-magazine NBT. Infinite-ammo reloads continue to use virtual supplied magazines, which are never returned or spawned during removal; fired rounds still decrement normally and reload state/animation timing is unchanged. Survival acquisition and reserve consumption are unchanged.
- Java 8 offline compilation, `shadowJar`, and reobfuscation passed; the remapped release archive contains all six pack trees (1,687 `hmg_packs/` entries). Existing resolver and repository-pack checks passed (32 and 2,584 checks respectively; 36 documented pre-existing unresolved logical references). Standalone runtime installation, resource precedence/live reload, Creative reload inventory behavior, and survival consumption still require in-game validation.

2026-09-11 14:05 — Fix packaged bundled-pack discovery

- Stopped converting opaque Forge `jar:` code-source URIs to `File`. Bundled-pack discovery now handles `file:` class/resource directories directly and opens both ordinary JAR code sources and Forge/LaunchWrapper `jar:` URLs through `JarURLConnection`, without parsing archive URL strings.
- Added regression coverage for a development directory, a `file:` JAR location, and the opaque `jar:file:...!/` form that crashed during packaged pre-initialization. Pack materialization, external precedence, registration, and reload ordering are unchanged.
- Java 8 offline compilation and all 38 resolver/materialization checks passed. The final shadow/reobfuscated `4.0.0.3` archive contains the corrected `JarURLConnection` path and all 1,687 bundled-pack entries. Actual Forge client pre-initialization was left for user runtime testing.

2026-09-11 23:06 — Avoid poisoning LaunchWrapper's vecmath class cache

- Replaced the speculative `Vector3d` class load with a resource availability check. LaunchWrapper 1.12 records failed loads in `invalidClasses` and does not clear them when a URL is added, which prevented the dedicated-server fallback from loading after extraction.
- Attach the existing bundled jar through the defining `LaunchClassLoader`'s public `addURL`, then verify class visibility through that same loader. Existing providers and repeated initialization reuse the available runtime; extraction checks the nested jar and reports missing, corrupt or inaccessible runtime failures. No context/system loader, child loader, client classes or packaging changes are introduced.
- Java 8 offline `:HMG:compileJava` and `git diff --check` passed. Both `:HMG:runClient` and `:HMG:runServer` were attempted but failed before HMG injection because CodeChicken could not select its mappings directory. Packaged dedicated-server startup without external vecmath and normal client startup remain unverified; the development server classpath includes external vecmath and cannot establish bundled-only behavior.

2026-09-13 19:46 — Add generic TaCZ locomotion compatibility

- Added an additive movement layer between imported static pose and action/fire playback. Equipped Blockbench guns now select shared idle, directional walk, ADS-walk, run entrance/loop/airborne-hold/exit clips from player movement without importing TaCZ Lua, gameplay, attachment, or weapon-specific state machines.
- Added unchanged numeric Bedrock `.animation.json` fallback loading. Embedded `.bbmodel` actions win by name while a shared rifle/pistol default can supply missing locomotion; duration, transform channels, split keys, linear/step/Catmull–Rom interpolation, and sound/particle markers are retained. Invalid external fallback no longer discards valid embedded clips.
- Audited official TaCZ and the installed SX, ClassicRCCRP, Warzone, Continental, and WaT packs. The common cube/marker/action/default-animation contract is documented with per-pack source completeness and exceptions. Installed content exposes exported Bedrock/glTF geometry rather than native `.bbmodel`, and Warzone hides its payload in `taczpack.dat`; neither model format boundary was bypassed or misreported as working.
- Added parser, layer-order, locomotion-transition, alias, duration, event, and Molang-rejection checks. Java 8 offline `:HMG:compileJava :HMG:animationTest` passed with all 130 checks, and `git diff --check` passed. No runtime launch or packaging was performed; first/third-person movement, action transitions, hands, UVs, and installed-content authoring still require in-game validation.

2026-09-14 00:40 — Import native TaCZ Bedrock geometry

- Added `BedrockModel` as a second source format for the existing HMG imported-model runtime. Unchanged TaCZ geometry 1.12.0/1.21.0 now supplies nested cube bones, pivots/rest rotations, per-face and box UVs, mirror, inflate, positive or negative dimensions, texture dimensions, marker bones, and the same authored hand/view positioning used by `.bbmodel`; audited advanced Bedrock mesh/locator/binding features remain rejected.
- Made `Animations` an ordered local-to-shared source list. Embedded `.bbmodel` clips and weapon-local Bedrock actions retain precedence, shared rifle/pistol files fill only missing names, and optional tracks absent from a selected exported model are ignored with a diagnostic while remaining tracks stay strict.
- Added byte-identical official AK-47 and Glock 17 presentation assets plus ordinary HMG gun definitions as the initial validation set. CPU audits resolved 94 bones/761 cubes with 19 local plus 22 shared AK clips, and 57 bones/264 cubes with 14 local plus 16 shared Glock clips; all ten copied model/animation/texture/icon assets match their source SHA-256.
- Explicitly rejected packs containing root or `recursion/taczpack.dat` before resource, tab, coefficient, or definition loading. Warzone is excluded from compatibility counts and planning; HMG does not decode or inspect its packed/obfuscated payload.
- The production loader parsed all 813 exported geometry files across official TaCZ, SX, ClassicRCCRP, Continental, and WaT with zero failures. Java 8 offline compilation, all 144 animation/import checks, all 41 resolver checks, and 2,584 repository-pack checks passed (36 documented pre-existing unresolved references). No client runtime validation or packaging was performed; visual first/third-person, ADS, movement, hands, UV, and renderer acceptance remains required.

2026-09-14 18:27 — Restore legacy reload startup and Bedrock base presentation

- Fixed the accepted-reload client handoff for original HMG guns. The server-authorized packet previously returned when a renderer had no imported animation definition, so native Bedrock actions started while legacy OBJ/MQO renderers could miss the client `IsReloading`/`RloadTime` state their authored reload motions consume. Matching legacy held stacks now reconcile only that presentation timer; server gameplay and ammunition authority remain unchanged.
- Imported TaCZ Bedrock geometry now starts in TaCZ's ordinary attachment-free visibility state. Conditional mount/sight, alternate-magazine, tactical-handguard, attachment-position and adapter-child geometry is hidden generically; the AK mount owns its rail subtree and the Glock shows only `mag_standard`. No complete TaCZ attachment controller or gun-specific asset workaround was added.
- Native Bedrock 3D inventory previews now use the authored `fixed` origin. Third-person `thirdperson_hand` translations use TaCZ's default gun-context factor while HMG `ModelScala`/`InworldScale` continue to control rendered size. First-person positioning, locomotion/action selection, skins, native `.bbmodel`, legacy model/texture rendering, and packed TaCZ rejection paths are unchanged.
- Java 8 offline compilation passed. The animation/import suite passed 295 checks, the pack resolver passed 48 checks, and repository asset validation passed 2,584 checks with its 36 documented pre-existing unresolved references. No Minecraft launch or in-game acceptance was performed; legacy reload playback, Bedrock GUI centering, conditional geometry, AK surface presentation, and third-person grip alignment require manual validation.

2026-09-14 21:43 — Correct Bedrock item contexts and UV binding

- Corrected Bedrock cube UV ownership after coordinate conversion. Vertex Y-order compensation already produced TaCZ's physical corner order, but UVs were indexed from the reversed ring and then the shared mesh builder reversed both arrays again. Box UV, per-face UV, mirrored boxes, negative dimensions and declared texture resolution now retain TaCZ's UV-to-vertex association without changing geometry or source assets.
- Bedrock GUI previews retain the authored `fixed` translation for the confirmed slot centering while leaving orientation to HMG's existing inventory transform. TaCZ `fixed` is a modern item-frame convention, and applying its rotation after HMG's GUI angle caused the imported preview mismatch. The authored generic fixed-context scale of 1.2 is now also applied at the aligned anchor; first-person, third-person, dropped-item and `.bbmodel` scales are unchanged.
- Bedrock third-person positioning keeps TaCZ's uniform 0.6 locator factor and the corrected vertical placement, while converting locator depth to Forge 1.7's equipped-hand convention so the weapon moves toward the held side instead of behind the arm. The existing `.bbmodel` positioning path is unchanged.
- Java 8 offline compilation passed. The animation/import suite passed 308 checks, including focused mirrored, per-face, negative-dimension and negative-extent UV mappings; the pack resolver passed 48 checks, and repository asset validation passed 2,584 checks with its 36 documented pre-existing unresolved references. No Minecraft launch was performed; GUI angle/scale, AK surface presentation and third-person depth require manual in-game acceptance.

2026-09-15 04:40 — Correct reload clock consumption and rebuild Bedrock face and hand mapping

- User runtime results supersede prior unvalidated UV, third-person depth and legacy reload claims. Holding reload was rewinding the client `RloadTime` on every request, including repeats rejected by the server; those resets are removed. First-person legacy rendering now reads the live matching held stack's reload flag and timer together. Original tick interpolation, final-frame snap, part-motion traversal and server reload/ammunition authority remain intact. Cached/live stack divergence is a source-supported failure path, not a claim that it explains every reported missing animation.
- Native Bedrock GUI/creative rendering moves eight GUI pixels right before rotation/scaling, preserving the confirmed scale and standard HMG inventory angle. The offset does not affect first/third person or dropped items.
- Corrected the missing TaCZ per-face east/west and up/down lookup and restored paired vertex/UV corner ordering. All six face orientations, box/per-face mirror semantics, negative sizes/extents and declared UV normalization have static coverage; 32 corresponding stock corners agree with the known-good `.bbmodel` AK. Source textures/models, `.bbmodel` import, attachment visibility, skins, texture isolation and Warzone rejection are unchanged.
- Replaced the native Bedrock third-person depth-sign patch and legacy mesh-origin offsets with the inverse of the inspected RenderPlayer/Forge/HMG item transforms, an arm-relative palm anchor and the full authored locator inverse. Locator and geometry share units and outer HMG scale; TaCZ's translation-only 0.6 factor is removed. Existing `.bbmodel` hand placement and first-person TaCZ placement/action/movement paths are retained.
- Java 8 offline compilation passed. Animation/import tests passed 613 checks, resolver tests 48 and repository asset tests 2,584 (36 pre-existing unresolved references). Tests include actual legacy NBT/progress/motion consumers, cube face/winding checks and nested rotated locator cancellation, with the existing AKS74U +Z barrel basis as an HMG comparison. No Minecraft launch or packaging was performed. Manual testing remains required for legacy reload playback (tap/hold, empty/tactical, completion/interruption/switch), GUI position at unchanged scale, all AK surfaces, third-person grip/depth/down-arm placement under player poses, and the preserved first-person/skin/attachment behavior.
2026-09-15 12:00 — Begin production HMG-to-TaCZ weapon migration

- Added 20 separately registered official-TaCZ Bedrock variants for exact conventional HMG matches: AA-12, AK-47, Desert Eagle, Glock 17, G3, G36K, M16A1, M16A4, M1911, M249, M4A1, MP5, P90, QBZ-95, RPG-7, RPK, SCAR-H, SCAR-L, UMP45, and Uzi. Existing HMG gameplay, magazines, relevant attachments, and fire sounds remain authoritative; legacy model presentation is replaced by unchanged official geometry, textures, local actions, shared movement animations, placement markers, and hand locators.
- Reload timers follow each selected TaCZ tactical (or RPG-7 empty) clip rounded to the nearest 1.7.10 tick. Per-shell M870/M1014/Kar98k, M320, minigun, questionable variant substitutions, addon-specific behavior, and packed Warzone content remain excluded. TaCZ reload/mechanical sound-marker playback is not yet wired, so the original HMG reload sound remains as a documented temporary fallback.
- Added generic `HMG Bedrock / TaCZ` and `HMG Blockbench` comparison tabs, a mapping/timing/skip audit, official contributor attribution, and retained upstream README/GPL text. Updated `mcmod.info` with concise TaCZ ecosystem attribution and a no-endorsement statement.
- Static asset and definition validation remains required. Minecraft was not launched; first/third-person placement, ADS, hands, visibility, reload timing, and audio synchronization require manual in-game testing.

2026-09-16 07:34 — Stabilize migrated TaCZ weapon presentation

- Kept imported `static_idle` aliases alive when an upstream export omitted its loop flag, restoring the authored post-draw hand pose. Added an isolated 0.12-second locomotion crossfade so walk/run changes cannot capture or replay an action pose.
- Native Bedrock guns now leave HMG's lowered sprint presentation on a fire request and use a four-tick, owning-gun-tick recovery gate on both logical sides; a tapped trigger is retained once through recovery and cancelled by reload. Legacy OBJ/MQO, `.bbmodel`, turret and non-held behavior is unchanged.
- Bound TaCZ's standard ammunition bones, plus bones authored fully hidden by `static_bolt_caught`, to HMG ammunition state. This covers the RPG-7 rocket and preserves its authored reload scale timing without a weapon-specific exception. Corrected only the RPG-7 imported inventory scale; equipped and world scales are unchanged. M1911 received no hand-specific workaround.
- Made the standalone migrated pack self-contained with the 21 referenced legacy HMG item/scope images, including both G36 overlays, and corrected the pre-existing TaCZCompatibility Glock icon spelling exposed by validating both development and distributed pack roots.
- Final Java 8 offline compilation and validation passed: 794 animation checks, 48 resolver checks, and 5,447 asset checks with 72 duplicated pre-existing unresolved references across the two checked pack roots. Minecraft was not launched; visual hand pose, sprint-to-fire timing, RPG reload visibility, movement blending, GUI scale, scopes, and M1911 regression still require in-game acceptance.

2026-09-19 20:23 — Complete TaCZ mechanical audio and per-shell migration

- Added an opt-in client bridge for namespaced Bedrock sound markers. Authored magazine, bolt, slide, pump, shell and other mechanical cues now follow animation time for the owning first-person gun, while fire-clip markers are rejected and HMG firing sounds/gameplay remain authoritative. Marker-less clips can declare a reusable clip-start fallback; M320 is the sole migrated override using it.
- Forced the automatic imported equip/draw request to one shot. Some TaCZ assets declare draw as loop/hold and rely on their Lua state machine to stop it; HMG now returns generically to base idle without importing that state machine.
- Layered staged imported presentation over HMG's existing per-shell gameplay: server-accepted intro, one held insertion clip per HMG-authorized round cycle, the existing firing-interrupt boundary, and a presentation-only finish/cancel clip. M870, M1014 and Kar98k now preserve committed rounds and never grant ammunition from animation events; legacy per-shell guns remain on their unchanged path unless explicitly opted in.
- Added official TaCZ variants for M870, M1014, Kar98k and standalone M320, plus directly compatible HK416D and Mk 14 presentations. Added exact ClassicRCCRP variants for AK-74, AK-74M, AKS-74U, HK416, M110 and MG36 in a separately attributed CC BY 4.0 pack. Assets are dependency-selected; one absent official M1014 marker and one absent ClassicRCCRP extended-magazine inspect marker remain intentionally silent.
- Kar98k's migrated variant uses HMG's existing single-round 7.92 mm item across five magazine slots so the legacy per-round commit lifecycle is real rather than a cosmetic full-clip reload. SX and WaT remain excluded by their published no-derivatives licenses; Continental has no direct gameplay match, and Warzone remains excluded by its packed format.
- Java 8 offline compilation passed. The animation/import suite passed 4,642 checks, the pack resolver passed 48 checks, and repository asset validation passed 5,619 checks with its 72 documented pre-existing unresolved references. Minecraft was not launched; empty/tactical audio timing, staged reload continuation/interruption, final-pose exit, Kar98 reserve consumption, M320 fallback audio, and all newly migrated presentation assets require manual in-game testing.

2026-09-19 21:04 — Finish migrated-gun packaging cleanup

- Added deferred `CopyRecipe` mappings so all 32 TaCZ/ClassicRCCRP variants and the Blockbench AKM inherit the exact Gun Smithing Table ingredients and category of their HMG gameplay counterpart without duplicated recipe grids.
- Increased the imported RPG-7 inventory-only scale from 0.5 to 0.75 and normalized M870/M1014 from the inherited legacy 3.0 scale to 0.75. Equipped, world, first-person, third-person, ADS, and gameplay values are unchanged.
- Cleaned player-facing tab and item names, gave ClassicRCCRP its own tab, and moved the pre-existing Blockbench AKM definition/model into a self-contained `HMGBlockbench` pack while retaining shared legacy assets in GVCguns.
- Removed the obsolete `TaCZCompatibility` development pack after confirming its AK-47/Glock 17 geometry, animation, and model-texture files were byte-identical to production `TaCZOfficial` assets. Production registrations remain the only variants.
- Java 8 offline compilation passed. The animation/import suite passed 4,642 checks, the pack resolver passed 48 checks, repository asset validation passed 5,599 checks with its 72 documented pre-existing unresolved references, and all 33 recipe-copy mappings resolved statically to registered targets and legacy recipe outputs. Minecraft was not launched; inventory sizing, tab placement/labels, item names, and all inherited Gun Smithing Table recipes require manual in-game confirmation.

2026-09-19 21:20 — Hold zero-duration imported locomotion poses

- Fixed the ClassicRCCRP AKS-74U crash when equipped. Its authored `static_idle` is a zero-duration held pose, but the generic locomotion request overrode that with a repeating loop and `AnimationPlayback` correctly rejected an impossible zero-length loop. The controller now preserves the imported hold semantics for that exact loop override while leaving timed locomotion and action playback unchanged.
- Added regression coverage for requesting and advancing a generic locomotion loop over a zero-duration Bedrock idle alias. Minecraft was not launched; equipping the AKS-74U remains the manual runtime check.

2026-09-20 09:24 — Add independent HMG technology progression

- Added optional `TechYear` and half-step `TechTier` gun metadata with one configurable year resolver, compatibility-safe unrestricted behavior for unclassified packs, tooltips, and held-item inspection.
- Added per-world persistent HMG progression, permission-level-2 commands, immediate client synchronization, configurable operator/creative bypass, and independent enable/disable state.
- Enforced the server-owned tier decision in firing, trigger packets, Gun Smithing Table transactions, and ordinary crafting completion without deleting existing restricted items. Classified the maintained TaCZ migration sets and Blockbench AKM by represented variant year.
- Java 8 offline compilation passed. No Minecraft launch or in-game validation was performed; persistence, live command sync, each bypass mode, blocked crafting/use, and tooltips still require multiplayer runtime testing.

2026-09-20 10:08 — Complete bundled HMG technology-year metadata

- Audited all 242 bundled `guns/` configuration files against the actual gun-construction directives:
  228 loadable firearms, launchers, grenades, and vehicle weapons participate in progression, while
  14 melee, recipe, kit, attachment, and template-only files are intentionally outside it.
- Added explicit `TechYear` metadata to the 195 previously unclassified definitions, covering the
  legacy, WWII, GVC, HMG, newer GVC, vehicle-weapon, Blockbench, Bedrock/TaCZ, and ClassicRCCRP
  content paths. All 228 loadable definitions now contain exactly one year/tier declaration.
- Used represented-variant dates for duplicated families and later configurations. Generic sample
  definitions and vaguely named conversions use documented approximate years rather than remaining
  unrestricted. Static completeness validation passed; in-game tier display and gating remain to be tested.

2026-09-20 10:31 — Harden bundled-pack materialization on Windows

- Stopped the initialization phase from materializing the bundled pack cache a second time after the cache had already been registered with Minecraft's resource system. Materialization failures now have their own initialization boundary instead of being mislabeled as recipe-loading failures.
- Reuse byte-equivalent generated files instead of replacing them: archive entries use their stored size and CRC, while development-directory sources use a size/content comparison. Changed files are written to closed sibling temporary files and then moved into place, with atomic replacement where supported and a bounded Windows sharing/access-failure retry on only the final move.
- Added pack, source-entry, destination, and operation context to materialization failures while preserving the filesystem exception as the cause. The generated-cache path remains confined to `handmadeguns_builtin/`; external and user-authored pack roots are unchanged.
- Audited bundled-pack readers. No HMG code was found reading the generated `sounds.json`, so the reported lock owner remains unproven; PrismLauncher, Minecraft resource loading, and external Windows software cannot be distinguished from the exception alone. Fixed deterministic closure for the actual leaked/exception-prone HMG magazine, definition, settings, sound-writer, resource, and script streams found during the audit.
- Java 8 offline compilation passed. The pack-resolver suite covers unchanged directory/JAR reuse, changed-file replacement, and the absence of a leftover temporary file after successful replacement; Minecraft was not launched, and a real transient external Windows lock still requires runtime validation.

2026-09-20 14:58 — Add persistent model-derived HMG inventory icons

- Preserved model-derived icons as the default for model-backed guns. `IconTexture`/legacy `Texture`
  and `UseModelIcon,false` remain explicit authored-sprite overrides rather than becoming the global
  default. Inventory, hotbar, creative, GUI, and NEI share one generated icon; equipped, dropped,
  placed, skin, and attachment rendering remains live and unchanged.
- Added an independent schema-versioned cache under `cache/hmg/icons/`: shipped prebakes resolve first,
  disk PNGs second, and a deduplicated capture queue third. Content hashes cover the definition,
  model, texture, external imported textures, animation sources, transforms, content ID, renderer,
  canonical appearance, and schema rather than relying on size/mtime metadata.
- Capture uses an attachment-free, unskinned stack and deterministic time-zero idle/static-idle pose,
  skips arbitrary render scripts, supports the renderer's opaque/transparent passes, reuses one
  framebuffer/read buffer, uploads immediately, and submits atomic PNG saves to a bounded writer.
  Pending and failed states use the authored sprite without an expensive live-render fallback.
- Added resource-reload cleanup, changed-source targeted model refresh, aggregate diagnostics, and a
  same-generator developer export mode. Offline `:HMG:compileJava` passed, and user runtime testing
  confirmed the expected generated-icon behavior; broader resource-reload/prebake coverage remains.
