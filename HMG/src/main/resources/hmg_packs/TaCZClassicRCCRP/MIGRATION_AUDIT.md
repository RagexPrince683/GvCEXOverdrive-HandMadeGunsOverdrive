# ClassicRCCRP migration audit

Static migration date: 2026-09-19. Minecraft was not launched.

| New HMG variant | ClassicRCCRP source | HMG gameplay definition | Geometry / texture / animation / audio source |
| --- | --- | --- | --- |
| ClassicRCCRP AK-74 | `ccrp:ak74` | `GVCguns/guns/AK74.txt` | ClassicRCCRP 1.1.6 hotfix 2 |
| ClassicRCCRP AK-74M | `ccrp:ak74m` | `GVCguns/guns/AK74m.txt` | ClassicRCCRP 1.1.6 hotfix 2 |
| ClassicRCCRP AKS-74U | `ccrp:aks74u` | `GVCguns/guns/AKS74U.txt` | ClassicRCCRP 1.1.6 hotfix 2 |
| ClassicRCCRP HK416 | `ccrp:hk416` | `GVCguns/guns/hk416.txt` | ClassicRCCRP 1.1.6 hotfix 2 |
| ClassicRCCRP M110 | `ccrp:m110` (`ccrp:sr25_data`) | `GVCguns/guns/M110.txt` | ClassicRCCRP 1.1.6 hotfix 2 |
| ClassicRCCRP MG36 | `ccrp:mg36` | `GVCguns/guns/MG36.txt` | ClassicRCCRP 1.1.6 hotfix 2 |

All six are separate items in the player-facing `HMG ClassicRCCRP Guns` tab.
Their HMG gameplay directives
are retained; only legacy model transforms/part motions are replaced. The copied
audio set is dependency-derived from non-fire animation markers. Firing clips are
blocked by the generic handler, so all firing audio remains HMG's.

The M110 display intentionally points at ClassicRCCRP's shared `sr25` data and
animation while retaining HMG's M110 gameplay. One optional extended-magazine
inspect marker, `ccrp:sr25/sr25_inspect_xmag_magslide`, has no matching OGG in the
source archive and is left silent rather than substituted. HMG does not select
the source pack's extended-magazine-specific inspect clip automatically.

Each migrated item uses a `CopyRecipe` mapping to its exact HMG gameplay
counterpart. Recipe copies resolve after every pack recipe has loaded and therefore
retain the legacy Gun Smithing Table ingredients and category.

The apparent ClassicRCCRP AUG A3 match is excluded: its source data is 5.56x45,
while HMG's `auga3.txt` is the 9 mm conversion. Other ClassicRCCRP weapons lack an
exact or directly compatible HMG definition and were not forced into approximate
mappings. Source GP-25/state-machine variants remain excluded because HMG has no
equivalent imported switching/presentation contract.
