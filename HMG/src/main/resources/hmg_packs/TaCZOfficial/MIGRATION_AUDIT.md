# Official TaCZ migration audit

Static audit date: 2026-09-15. Minecraft was not launched.

## Migrated mappings

| HMG gun | TaCZ source gun | Source pack | Reload clip | Duration | HMG ticks |
| --- | --- | --- | --- | ---: | ---: |
| AA-12 | aa12 | Official TaCZ | reload_tactical | 3.2500 s | 65 |
| AK-47 | ak47 | Official TaCZ | reload_tactical | 2.4000 s | 48 |
| Desert Eagle | deagle | Official TaCZ | reload_tactical | 2.0333 s | 41 |
| Glock 17 | glock_17 | Official TaCZ | reload_tactical | 1.9333 s | 39 |
| G3 | hk_g3 | Official TaCZ | reload_tactical | 2.2500 s | 45 |
| G36K | g36k | Official TaCZ | reload_tactical | 2.6333 s | 53 |
| M16A1 | m16a1 | Official TaCZ | reload_tactical | 2.3333 s | 47 |
| M16A4 | m16a4 | Official TaCZ | reload_tactical | 2.3333 s | 47 |
| M1911 | m1911 | Official TaCZ | reload_tactical | 2.0667 s | 41 |
| M249 | m249 | Official TaCZ | reload_tactical | 5.9333 s | 119 |
| M4A1 | m4a1 | Official TaCZ | reload_tactical | 2.3667 s | 47 |
| MP5 | hk_mp5a5 | Official TaCZ | reload_tactical | 2.3000 s | 46 |
| P90 | p90 | Official TaCZ | reload_tactical | 2.7083 s | 54 |
| QBZ-95 | qbz_95 | Official TaCZ | reload_tactical | 2.4333 s | 49 |
| RPG-7 | rpg7 | Official TaCZ | reload_empty | 3.5333 s | 71 |
| RPK | rpk | Official TaCZ | reload_tactical | 3.0667 s | 61 |
| SCAR-H | scar_h | Official TaCZ | reload_tactical | 2.3333 s | 47 |
| SCAR-L | scar_l | Official TaCZ | reload_tactical | 2.4333 s | 49 |
| UMP45 | ump45 | Official TaCZ | reload_tactical | 2.5333 s | 51 |
| Uzi | uzi | Official TaCZ | reload_tactical | 2.1333 s | 43 |

Each definition starts from its existing HMG gun and retains gameplay/stat,
magazine, fire-sound, and relevant attachment directives. Legacy model transforms
and part animations are replaced by unchanged official Bedrock presentation assets.

## Intentionally deferred or skipped

- M870, M1014, and Kar98k: per-shell/per-round presentation needs sequencing beyond HMG's single reload lifecycle.
- M320: custom launcher/under-barrel behavior is outside the ordinary-gun scope.
- Minigun: no ordinary reload family and requires special operation.
- SKS Tactical and other substantially customized variants: not substituted for standard HMG guns.
- AK-74/AK-74M and other caliber/model variants: not treated as AK-47 matches.
- Warzone: packed `taczpack.dat` content was not decoded, inspected, or imported.
- SX, ClassicRCCRP, Continental, and WaT matches remain candidates for later batches.

## Known presentation gap

Authored animation durations are applied to HMG's reload timer. The unchanged
animations retain TaCZ sound markers, but HMG does not yet resolve those identifiers
to bundled TaCZ sound files. Variants retain the HMG reload sound as an explicit
temporary fallback; TaCZ reload/mechanical audio import remains incomplete.
