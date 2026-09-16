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

## Follow-up presentation and dependency audit

Static follow-up date: 2026-09-16. The standalone `TaCZOfficial` pack now carries
the complete set of legacy HMG assets referenced by these definitions: 13 item
icons from `GVCguns`/`Addfixing` and eight scope overlays from `Addfixing`, copied
unchanged under the same typed paths. This includes `scope_g36.png` and
`scope_g36_night.png`; G36K ADS did not require a TaCZ-specific scope image or a
copy of an entire source pack. Repository validation now checks both the development
pack tree and the distributed `src/main/resources/hmg_packs` tree, so a migrated
definition cannot silently rely on a sibling pack's copy of an icon or overlay.

The imported `static_idle` alias is held when TaCZ omitted an explicit loop flag,
preserving the authored hand locators after draw. Imported locomotion crossfades
over 0.12 seconds, and held native Bedrock guns leave the lowered sprint pose before
the owning gun tick releases a queued trigger after four recovery ticks. Ammunition
bones follow HMG's round count using TaCZ's standard names plus bones authored at
zero scale throughout `static_bolt_caught`; this recognizes the RPG-7 `rocket` bone
without a weapon-specific rule and preserves its authored reload scaling.

RPG-7 uses `InventoryScale,0.5` only for its imported GUI preview. Gameplay scale,
first person, third person, dropped items, and other migrated guns are unchanged.
No M1911 hand-specific behavior was added; it remains a regression boundary for the
generic locator and ammunition-bone rules.

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
