# GVC legacy firearm registries

The `GVCGuns` entries below already held save-compatibility placeholders. Their registry IDs and icons remain available to load old worlds and inventories. They are hidden from creative and search results. There is no normal recipe, camp drop, or NPC loadout that issues those placeholders. The old recipe that produced the `M2F` placeholder was removed.

| Legacy `GVCGuns` item IDs | Replacement in `HandmadeGuns` |
| --- | --- |
| `AK74`, `RPK74`, `AKS74U`, `M1911`, `SVD`, `PKM` | Same individual registry names |
| `M16A4`, `M4A1`, `G17`, `G18C`, `M110`, `M240B` | Same individual registry names |
| `G36`, `MG36`, `RPG7`, `M134` | Same individual registry names |
| `M10` | `MAC-10` |
| `M870` | `Remington Model 870` |
| `M249` | `FN M249 (Minimi)` |
| `M9` | `Beretta M9` |
| `R700` | `Remington Model 700` |
| `M82A3` | `Barrett M82` |
| `SMAW` | `Mk153 SMAW` |
| `G36C` | `G36K`, following the prior GVC mapping |
| `M2F` | `M2 Flame Thrower` |

The conversion is a one-item shapeless recipe, available wherever the replacement is registered as an HMG unified gun. It creates the HMG weapon with default weapon state. Existing player and chest stacks keep their IDs and can be converted as needed. Infantry equipped with a supported placeholder replace it with the corresponding HMG weapon when they tick on the server. An NPC's custom weapon name is preserved. Unknown replacements stay readable under their original ID.

The former `AK74 PSO-1`, `PKM PSO-1`, `M16A4 ACOG`, `M240B ACOG`, `M16A4 M320`, and `G36 AG36` variants, `UZI`, `CBJ-MS`, `MP7`, `P226`, `FIM92`, and `MBTLAW` appear in legacy assets or disabled source. They do not have active firearm registrations in this GVC build. Historical localization and assets can remain for compatibility with resource packs.

## Retained equipment and references

| Owner | Retained IDs and purpose |
| --- | --- |
| `GVCGuns` | `GunBox` dispenses HMG firearms. `target` and `TargetEgg` provide a practice target. `SentryEgg` and `SentryAAGEgg` configure stationary sentries. |
| `GVCGuns` | `IED`, `IED_AT`, `C4` and `C4cn` retain demolition and mine gameplay. `Camp`, `Contna`, `box`, `boxEgg`, `cm`, and `HealthPack` retain equipment, supplies, and utility behavior. |
| `GVCGuns` | `prahelmet`, `prachestp`, `praleggings`, and `praboots` retain armor recipes and equipment. |
| `GVCMob` | `defsetter`, `arenaUnit`, `MobSpawnerEgg`, `MobSpawner2Egg`, and `FlagEgg` retain their registry entries. Infantry eggs keep their existing IDs and issue HMG loadouts for armed variants. |
| `GVCMob` | `requestSupport_arty` continues to request artillery. `requestSupport_Heli`, `requestsupport_fighter`, `requestsupport_attacker`, and the vehicle eggs remain present for saves with vehicle support deferred. |
| `HandmadeGuns` | `bullet_hmg`, `bullet_hg_hmg`, `bullet_lmg_hmg`, `bullet_shell_hmg`, `bullet_rr_hmg`, `5.45mm magazine`, `5.45mm long magazine`, `5.56mm magazine`, `7.62mm magazine`, `7.62mm clip`, and `12.7mm Magazine` remain HMG-owned ammo and magazine references. |
| `HandmadeGuns` | `Prastic` remains the armor crafting material. `missile`, `Type38`, and `PTRK2041` remain HMG-owned references. |
