# TaCZ Asset Compatibility Audit

Audit date: 2026-09-14. This is a source/static audit of the TaCZ 1.7 source tree, the readable packs present in the inspected workspace, and HMG's common Blockbench/Bedrock runtime. It does not claim an in-game render test. No source TaCZ or add-on file was modified.

## Result in Brief

The readable TaCZ content uses one strong presentation convention: a cube-based Bedrock hierarchy with common view/hand marker bones, weapon-local action animations, and shared rifle/pistol default animations for idle, walking, and running. HMG now reads unchanged exported Bedrock geometry and numeric Bedrock animations into the same model/animation runtime used by `.bbmodel`. The generic movement layer supplies directional walk, ADS walk, and the authored sprint sequence without a pack script.

Across the official content, SX, ClassicRCCRP, The Continental, and WaT, 362 of 390 display definitions have a readable conventional presentation set. Geometry parsing succeeded for all 813 exported geometry files in those open trees, including ancillary models. Individual HMG gameplay definitions and visual runtime acceptance are still required before calling every weapon imported. The bundled validation pack starts that work with the official AK-47 and Glock 17.

Warzone is not part of either denominator. Its actual assets are stored in `recursion/taczpack.dat`; HMG explicitly rejects that packed/obfuscated payload and does not decode or inspect it.

## Audited Inputs and Method

- Official TaCZ: `TACZ1.7/src/main/resources/assets/tacz` and its default visual-state code.
- Readable installed packs: SX Gunpack, ClassicRCCRP 1.1.6 hotfix 2, The Continental 1.9, and WaT 0.4.0. Warzone was detected only to apply the required packed-format exclusion.
- HMG: the `.bbmodel` and exported-Bedrock parsers, common model renderer, animation controller/client, asset resolver, configuration directives, and `HMG/Reference files/cod4_ak.bbmodel`.

Display definitions were resolved to their selected model, animation, texture, and visual-script resource. Readable geometry bone names and readable animation names were counted. Each gun animation was unioned with its declared rifle/pistol/default animation before checking for ordinary idle, draw, fire, reload, and inspect presentation. This deliberately does not grade ballistics, attachment behavior, per-round reload semantics, fire selectors, under-barrels, or Lua-only gameplay.

## Distribution Inventory

| Content | Displays | Selected model refs | Animation refs | Texture refs | Visual script refs | Static ordinary-presentation result |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Official TaCZ | 54 | 54 | 54 | 54 | 31 | 53 complete; minigun has no standard reload clip |
| SX Gunpack | 65 | 64 | 59 | 64 | 31 | 56 complete; 9 exceptions below |
| ClassicRCCRP | 183 | 183 (182 resolve) | 141 | 182 resolve | 71 | 167 complete; 16 exceptions below |
| The Continental | 31 | 31 | 28 | 31 | 4 | 29 complete; 2 missing animation resources |
| WaT | 57 | 54 | 49 | 57 | 18 | 57 complete |
| **Readable target total** | **390** | **386 selected (385 resolve)** | **331** | **388 resolve** | **155** | **362 complete; 28 exceptions** |

Multiple displays may intentionally share a model or animation, so unique-resource counts need not equal display counts. “Complete” here means the readable source has the usual static idle, draw, fire, reload, and inspect family after its declared shared default is included. It is a compatibility estimate, not direct HMG load status.

## Common Rig and Texture Contract

All 54 selected official models contain `root`, `camera`, `constraint`, `idle_view`, `iron_view`, `thirdperson_hand`, `lefthand`, `righthand`, `lefthand_pos`, and `righthand_pos`. The same core placement set occurs in all 64 selected SX models, all 182 resolved ClassicRCCRP models, and all 54 selected WaT models. Continental has all three view/placement markers in 31/31 models and the hand/constraint set in 29/31.

Other widespread markers include `positioning`, `fixed`, `ground`, `refit_view`, `refit_scope_view`, `refit_muzzle_view`, `refit_extended_mag_view`, `muzzle_pos`, `muzzle_flash`, `scope_pos`, `laser_pos`, and `shell`. HMG generically consumes only the markers that define its existing presentation contract:

- `idle_view`, falling back to `camera`, for first-person hip placement.
- `iron_view` for first-person ADS placement.
- `thirdperson_hand` for the player-held third-person origin.
- `fixed` for HMG's 3D inventory and creative-tab preview origin.
- `lefthand_pos` and `righthand_pos` for first-person player arms when `ModelArm` is enabled.

The exported geometry does not encode its base visibility by itself. TaCZ applies that state in `BedrockGunModel`: an attachment-free gun hides `mount`, `sight_folded`, `mag_extended_1` through `mag_extended_3`, `additional_magazine`, `handguard_tactical`, non-scope attachment-position geometry, and every child of `attachment_adapter`. HMG applies this same generic base state during Bedrock import. For example, the AK's `rail2` is a child of `mount`, and the Glock's standard and extended magazines are mutually exclusive. Dynamic attachment selection is still owned by HMG rather than importing TaCZ's attachment controller.

The remaining markers retain their hierarchy and transforms as ordinary bones. They are not automatically wired to HMG muzzle effects, optics, shell spawning, or dynamic attachment selection because those are already owned by HMG definitions and gameplay. This avoids importing a second, competing state system.

Readable exported models are cube-based Bedrock geometry using nested bones, pivots/rest rotations, per-face or box UVs, mirror, frequent cube `inflate`, and authored texture dimensions. A broader loader pass parsed all 813 geometry files in the five readable trees with zero failures: official 264, SX 114, ClassicRCCRP 284, Continental 53, and WaT 98. That pass also found recurring negative cube dimensions and one explicitly unnamed bone; both are accepted because TaCZ's renderer accepts them. No readable target required `poly_mesh`, texture meshes, locator objects, or bone bindings. Textures are ordinary PNGs selected explicitly by the HMG gun definition.

## Follow-up face audit — 2026-09-15

The earlier UV/depth/reload corrections were not runtime-validated; user testing reported them incomplete. TaCZ's `BedrockCubePerFace` delegates JSON face selection to `FaceUVsItem.getFace`: Java EAST/WEST select exported west/east, and Java UP/DOWN select exported down/up. North/south keep their names. HMG previously skipped this lookup. It also changed UV-ring indices without changing the associated vertex, reversing the corner binding. Import now selects the correct face and reorders each position/UV pair together. Box expansion remains TaCZ's integer-dimension layout, including negative dimensions; mirror swaps X endpoints and winding only for box UVs. Per-face mirror remains ignored as in TaCZ. UV V increases from the image top and normalization uses declared geometry dimensions rather than decoded image dimensions.

Direct comparison uses the first three unrotated, X-symmetric stock cubes shared by native `ak47_geo.json` and the known-good `cod4_ak.bbmodel` (eight faces, 32 position/UV pairs). For the first stock cube, exported east uses `[18,0]..[25.5,1]`, while west uses `[18,1]..[25.5,2]`: these belong to different rendered sides, not one globally flipped atlas. All corresponding rendered stock corners now agree statically. The six-face synthetic cube separately proves each face's top-left/top-right/bottom-right/bottom-left assignment, negative UV extents, box mirror behavior, negative dimensions, mesh winding and a declared 64x32 UV grid with a 128x64 image. This is focused static evidence, not a full visual comparison of every AK surface; no source textures or models were edited.

## Common Animation Contract

The action vocabulary is highly regular: `static_idle`, `draw`, `put_away`, `shoot`, `reload_empty`/`reload_dry`, `reload_tactical`, `inspect`, and `inspect_empty`. The existing HMG aliases cover `static_idle` to `idle`, `shoot` to `fire`, `reload_dry` to `reload_empty`, and `reload_tactical` to `reload`. Magazine, bolt, slide, trigger, safety, hand, and ammunition branches are simply animated bones; they do not require special rendering code.

All 390 readable-target displays declare a shared default animation family. The breakdown is:

| Content | Rifle default | Pistol default | Other shared run default |
| --- | ---: | ---: | ---: |
| Official | 40 | 14 | 0 |
| SX | 61 | 4 | 0 |
| ClassicRCCRP | 142 | 19 | 22 `ccrp:rifle_tac_rush_default` |
| Continental | 6 | 25 | 0 |
| WaT | 40 | 17 | 0 |

The official `rifle_default.animation.json` and `pistol_default.animation.json` animate only `root`, `camera`, and `constraint`. Both define `idle`, `run_start`, `run`, `run_hold`, `run_end`, `walk_aiming`, `walk_forward`, `walk_sideway`, and `walk_backward`; the pistol default also defines `walk_aiming_2`. The weapon-local files usually omit locomotion entirely. This is why external missing-clip merge is preferable to copying movement clips into every weapon project.

TaCZ's default Lua selects walking direction/ADS and sequences `run_start` → `run` or airborne `run_hold` → `run_end`. It may phase-lock cycles to accumulated walking distance. HMG now reproduces the state and clip selection on its client animation clock, but does not execute the Lua or distance-lock the cycle.

## Compatibility by Content Set

### Native reference and official TaCZ

`HMG/Reference files/cod4_ak.bbmodel` remains a native-project proof: 99 bones, 857 cubes, embedded PNG, common view/hand markers, magazine/bolt/attachment branches, and 14 embedded action clips parse into the shared HMG part and animation runtime.

The bundled `TaCZCompatibility` validation pack instead uses unchanged exported official assets. Its AK-47 parses as 94 bones/761 cubes; all 19 local clips and all 22 rifle-default clips validate. Its Glock 17 parses as 57 bones/264 cubes; all 14 local and 16 pistol-default clips validate, with optional absent `bullet2` and `bullet_in_mag2` tracks ignored. The local files precede the defaults, so normal weapon actions retain ownership while the defaults supply missing movement. These are CPU/static validations pending visual in-game acceptance.

For the official source, these 53 displays have the normal source presentation set when their original `.bbmodel` rig and declared default are available:

`aa12`, `ai_awp`, `ak47`, `aug`, `b93r`, `cz75`, `db_long`, `db_short`, `deagle`, `deagle_golden`, `fn_evolys`, `fn_fal`, `g36k`, `glock_17`, `hk_g3`, `hk_mk23`, `hk_mp5a5`, `hk416d`, `kar98`, `lonetrail`, `m1014`, `m107`, `m16a1`, `m16a4`, `m1911`, `m249`, `m320`, `m4a1`, `m700`, `m870`, `m95`, `m9a4`, `mk14`, `p320`, `p90`, `qbz_191`, `qbz_95`, `rhino357`, `rpg7`, `rpk`, `scar_h`, `scar_l`, `sks_tactical`, `spas_12`, `spr15hb`, `springfield1873`, `taurus500`, `taurus943`, `timeless50`, `type_81`, `ump45`, `uzi`, and `vector45`.

`minigun` lacks the ordinary reload family and depends on special behavior, so it is not counted as a complete conventional gun. Its static model, placement, idle/run, draw, fire, and inspect material can still use the generic presentation path; minigun-specific operation is out of scope.

### SX Gunpack

All displays except the following 9 have a complete readable ordinary-presentation set:

- `97s_display`: no standard fire clip.
- `m1917zh_display`, `p77_display`, `p771_display`, `p772_display`, `type54_display`: referenced animation resource is absent from the extracted pack.
- `r1895_display`, `r1895t_display`: no standard reload family.
- `toz_display`: no standard inspect family.

The other 56 are presentation-ready at source level through exported Bedrock geometry, subject to an HMG gun definition and runtime acceptance.

### ClassicRCCRP

All displays except the following 16 have a complete readable ordinary-presentation set:

- `ak103`, `ak74`, `ak74m`, `km_ak74m`, `rpk_203`, `rpk74m`, and `zenit_ak104`: no standard inspect family.
- `crow_and_egret`: no standard fire clip.
- `m1887_long`: no standard reload family.
- `fn_fal_display`: references absent `classicr:gun/fn_fal_geo` model and `classicr:gun/uv/fn_fal` texture. The similarly named tactical assets were not substituted.
- `gp25_ak103`, `gp25_ak74`, `gp25_ak74m`, `gp25_km_ak74m`, `gp25_rpk_203`, and `gp25_rpk74m`: no standard inspect family. Their base-rifle presentation can still use the generic path; GP-25 firing, ammunition, and switching are deliberately excluded.

The other 167 are presentation-ready at source level through exported Bedrock geometry, subject to an HMG gun definition and runtime acceptance.

### Warzone — excluded packed format

Warzone is excluded from compatibility counts and import planning. Its referenced model, animation, texture, and script payload is stored behind `recursion/taczpack.dat`. HMG neither decodes nor executes this private container. Encountering a root or `recursion/` `taczpack.dat` rejects that pack with `Unsupported packed/obfuscated TaCZ payload: taczpack.dat; HMG imports only directly readable assets`. The check is format-based, not a blacklist of ordinary TaCZ conventions or another pack's name.

### The Continental

29 of 31 displays have a complete readable ordinary-presentation set. `g34_copperhead_display` and `ttig34_display` reference animation resources absent from the extracted pack. The 29 others are presentation-ready at source level through exported Bedrock geometry, subject to an HMG gun definition and runtime acceptance.

### WaT

All 57 displays have a complete readable ordinary-presentation set after their declared default is included. They are presentation-ready at source level through exported Bedrock geometry, subject to HMG gun definitions and runtime acceptance.

## Generic Improvements Added

- A distinct additive `MOVEMENT` layer ordered after static base pose and before overriding actions/additive fire.
- Generic locomotion selection for standing idle, directional walk, ADS walk, sprint entrance, sprint loop, airborne hold, and sprint exit.
- TaCZ `run_*` names plus conservative `sprint*`, `walking*`, and ADS-walk aliases.
- An unchanged Bedrock `.animation.json` reader for numeric transform channels, declared/inferred duration, loop/hold, split keys, linear/step/Catmull–Rom interpolation, and sound/particle presentation markers.
- A native exported-Bedrock geometry reader for versions 1.12.0/1.21.0, nested bones, rest transforms, box/per-face UVs, mirror, inflate, positive/negative dimensions, texture dimensions, and marker bones, producing the existing HMG runtime representation.
- Ordered missing-clip merge: a `.bbmodel` project's embedded actions or a first weapon-local animation file wins, while later shared defaults supply omitted movement names.
- Optional absent Bedrock animation tracks are ignored with a diagnostic; remaining tracks stay strict.
- `BedrockModel` and explicit `ModelTexture` gun directives, plus initial ordinary HMG definitions for the unchanged official AK-47 and Glock 17 assets.
- Native Bedrock base-state visibility; authored `fixed` GUI centering and fixed-context scale under HMG's existing inventory angle; and TaCZ's default third-person locator factor with Forge 1.7 hand-depth conversion. Native `.bbmodel` presentation remains unchanged.
- TaCZ-compatible Bedrock UV-to-vertex binding for box/per-face cubes, mirror, inflate, negative dimensions and declared texture resolution. Geometry and texture assets remain unchanged.
- A format-level rejection for root or `recursion/taczpack.dat` payloads.
- Failure isolation: a bad external fallback logs a diagnostic but no longer discards valid embedded project animations.
- Opt-in playback of authored, namespaced Bedrock sound markers for the owning
  first-person gun. Fire-clip markers are excluded so HMG firing sounds remain
  authoritative; marker-less clips can declare one pack-local fallback sound.
- An opt-in staged per-shell presentation bridge. HMG's existing gameplay commits
  one round at a time and controls interruption, while imported clips provide an
  intro, one held insertion per commit cycle, and an optional finish action.

## Deliberate Exclusions and Remaining Limits

The following are not necessary for ordinary weapon presentation and were deliberately not imported:

- TaCZ Lua/state-machine execution, timers, and queries.
- Under-barrel/GP-25 switching, selection, ammunition, and networking. The official
  M320 is migrated only as HMG's existing standalone launcher.
- Pack-specific fire selectors, inspect variants, tactical-rush logic, or custom run scripts.
- Dynamic attachment/refit selection, non-default extended-magazine state, bullet visibility, optics, laser, muzzle, shell, and camera-constraint logic.
- Ballistics, recoil authority, movement rules, stamina, inventory, reload gameplay, or server authority changes.
- Decoding, unpacking, reverse-engineering, or importing Warzone's private `taczpack.dat` payload.

Still unsupported are glTF, Bedrock `poly_mesh`, texture meshes, locator objects, bone bindings, multiple-geometry files, versions outside the audited 1.12.0/1.21.0 pair, nonnumeric Molang, global/quaternion interpolation, and script-driven animation expressions. `walk_aiming_2` is retained and directly callable but is not guessed as a universal state because only the pistol default exposes it and the condition belongs to TaCZ's script. Per-shell/per-round sequencing is not inferred globally: a gun definition must opt into the established `reload_intro[_empty]` -> `reload_loop` -> `reload_end` contract and supply intro timing while HMG retains round authority.

Migration follow-up on 2026-09-19 added the official M870, M1014, Kar98k, M320,
HK416D, and Mk 14 variants, plus six exact ClassicRCCRP counterparts. SX and WaT
were not migrated because their published CC BY-NC-ND licenses prohibit adapted
redistribution. Continental is CC BY 4.0 but supplied no exact/directly compatible
HMG gameplay match in this pass. Warzone remains excluded by its packed format.

Manual runtime validation has confirmed representative AK-47/Glock 17 first-person rendering, actions, locomotion transitions, skin compatibility, base visibility filtering and GUI centering. Remaining acceptance includes the corrected GUI angle/scale, AK UV presentation, third-person forward placement, legacy reload playback, dropped views and optional renderer paths in Minecraft 1.7.10. After that gate, additional source-complete weapons can receive ordinary HMG definitions in batches; exceptions above should not be silently promoted.
