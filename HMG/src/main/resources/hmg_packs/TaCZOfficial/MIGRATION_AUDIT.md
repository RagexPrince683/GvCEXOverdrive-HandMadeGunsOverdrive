# Official TaCZ migration audit

Latest static migration date: 2026-09-19. Minecraft was not launched.

## Migrated mappings

| HMG gameplay definition | TaCZ presentation source | Mapping | Presentation notes |
| --- | --- | --- | --- |
| AA-12 | `aa12` | exact | standard tactical/empty reload |
| AK-47 | `ak47` | exact | standard tactical/empty reload |
| Desert Eagle | `deagle` | exact | standard tactical/empty reload |
| Glock 17 | `glock_17` | exact | standard tactical/empty reload |
| G3 | `hk_g3` | exact | standard tactical/empty reload |
| G36K | `g36k` | exact | standard tactical/empty reload and HMG scope overlays |
| M16A1 | `m16a1` | exact | standard tactical/empty reload |
| M16A4 | `m16a4` | exact | standard tactical/empty reload |
| M1911 | `m1911` | exact | no hand-specific override |
| M249 | `m249` | exact | standard tactical/empty reload |
| M4A1 | `m4a1` | exact | standard tactical/empty reload |
| MP5 | `hk_mp5a5` | exact | standard tactical/empty reload |
| P90 | `p90` | exact | standard tactical/empty reload |
| QBZ-95 | `qbz_95` | exact | standard tactical/empty reload |
| RPG-7 | `rpg7` | exact | imported rocket bone follows HMG ammunition state |
| RPK | `rpk` | exact | standard tactical/empty reload |
| SCAR-H | `scar_h` | exact | standard tactical/empty reload |
| SCAR-L | `scar_l` | exact | standard tactical/empty reload |
| UMP45 | `ump45` | exact | standard tactical/empty reload |
| Uzi | `uzi` | exact | standard tactical/empty reload |
| M870 | `m870` | exact | staged intro -> per-shell insert -> end |
| M1014 | `m1014` | exact | staged intro -> per-shell insert -> end |
| Kar98k | `kar98` | exact | staged intro -> per-round insert -> end |
| M320 | `m320` | exact standalone launcher | marker-less reload uses declared authored whole-clip sound |
| HK416 | `hk416d` | directly compatible D variant | HMG HK416 gameplay remains unchanged |
| M14 | `mk14` | directly compatible Mk 14 EBR variant | HMG M14 gameplay remains unchanged |

Every definition is separately registered in the player-facing `HMG TaCZ Guns`
tab (internal key `HMG_Bedrock_TaCZ`). HMG retains
damage, projectile behavior, recoil, spread, fire rate, magazine/ammunition,
attachments, restrictions, reload gameplay, and firing sounds. Official TaCZ
supplies unchanged geometry, texture, local/shared animation, marker timing,
mechanical audio, hand/view nodes, and first/third-person presentation.

## Reload and audio compatibility

`AnimationEventSounds,true` routes namespaced Bedrock sound markers through HMG's
client reload-sound path only for the local first-person owner. Marker playback is
clocked by the imported clip; skipped-frame markers retain the existing animation
event ordering. `shoot` and `fire` clip markers are rejected. The pack contains
only the actually referenced non-fire OGG dependencies plus sound registrations.

M320's authored reload clips have no markers, so its definition uses the generic
`AnimationSound` clip-start fallback. This is the only content-specific audio
override. The upstream M1014 animation references `tacz:m1014/cloth_move_3`, but
the official source tree has no corresponding OGG; that one marker is deliberately
silent rather than replaced with an approximate sound.

M870 and M1014 keep their original HMG eight-item shell magazines and 25-tick
per-shell commit time. Kar98k previously used one five-round clip item, which
cannot expose HMG's per-round commit lifecycle. Its migrated variant instead uses
the already existing one-round `7.92mm bullet` item in five magazine slots and a
14-tick round commit. This changes only the migrated variant's reload carrier; its
HMG ballistics, capacity, firing sound, bolt timing, and reserve authority remain.

The staged bridge starts `reload_intro` or `reload_intro_empty`, plays one
`reload_loop` for each HMG-authorized insertion, holds the loop's final pose until
the existing interrupt window resolves, and plays `reload_end` on completion or
cancellation. Animations never grant ammunition.

## Dependencies and provenance

Geometry, model textures, weapon-local animations, shared rifle/pistol defaults,
referenced mechanical OGGs, item icons, and scope/ADS overlays are copied only as
required by these definitions. Official assets and authors are credited in
`CREDITS.txt`; the upstream README and GPL-3.0 license remain beside this audit.
The HMG icons/overlays remain byte-identical dependencies from GVCguns/Addfixing.

## Intentionally excluded

- Minigun: requires spin/operation/heat behavior rather than an ordinary HMG reload.
- AUG: official asset is 5.56x45 while HMG's `auga3` definition is the 9 mm conversion.
- M700: official asset is .30-06 while HMG R700 is .223.
- SPAS-12: HMG's available SPAS-15 is not the same weapon.
- SKS Tactical and other substantially customized variants: no direct HMG gameplay equivalent.
- Other official guns: no exact or directly compatible HMG definition was identified.
- Warzone: packed `taczpack.dat` content remains uninspected and unimported.
- SX and WaT: published CC BY-NC-ND terms prohibit adapted redistribution.
- The Continental: no exact/directly compatible HMG gameplay match was found.

ClassicRCCRP's six licensed exact matches are maintained in the separate
`TaCZClassicRCCRP` pack with their own audit, credits, and license notices.

## Crafting and cleanup

All 26 migrated items use `CopyRecipe` mappings to the exact legacy gameplay
counterpart. The copies are resolved after all recipe roots load, so their Gun
Smithing Table ingredients and category remain synchronized with the authoritative
legacy recipes without duplicating ingredient grids.

The earlier `TaCZCompatibility` AK-47/Glock 17 development pack was removed. Its
geometry, animations, and model textures were byte-identical to the production
copies already retained here; its duplicate gun registrations and redundant item
icons are no longer packaged.
