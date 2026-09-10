# Known Limitations, Undocumented Discoveries, and Gaps

## Undocumented HMG Features and Systems Discovered

This HMG-focused documentation pass found these systems that were not described by the old root README:

- `HandmadeGuns` is the maintained mod id for HandMadeGunsOverdrive.
- HMG loads content packs from `handmadeguns_Packs` and the legacy `mods/handmadeguns/addgun` path.
- HMG includes a Gun Smithing Table, crafting materials, metal ores/blocks, gun parts, gun racks, placed guns, projectile entities, optional Guide-API manual integration, and manual dropped-gun pickup.
- HMG registers legacy reload/manual commands with permission level `0`, and `/hmg infiniteammo` with permission level `2`; see the command reference.
- HMG auto-generates copper and aluminum only when external ore dictionary entries are not present.
- HMG has configurable rendering, ADS/key behavior, cartridges, block destruction, knockback, threaded hit checks, LMM compatibility, GuideBook, and manual pickup options.

## Documentation Gaps That Could Not Be Completed

These gaps remain because the source lacks enough user-facing comments or because the legacy parsers are broad:

- The full grammar for gun, magazine, bullet, attachment, recipe, tab, and script files needs a dedicated parser-by-parser reference.
- Some key behavior is legacy and overlaps by default; exact in-game results should be validated with a live client and representative guns.
- `MAXGUNSINV`, `cfg_ADS_Sneaking`, and `cfg_AvoidHit` are documented as active keys, but their complete user-facing semantics require deeper gameplay testing.
- `/reloadSettings` reloads gun definitions and models, but the safe boundaries for hot-reloading every pack feature are not fully documented in source.
- No maintained screenshots were found in the repository.
- The repository includes other modules, but this documentation intentionally focuses on `HMG/` because it is the actively maintained module.

## Legacy Limitations

- HMG targets Minecraft Forge 1.7.10 and legacy ForgeGradle 1.2.
- Java 8 is recommended.
- Config changes generally require a restart.
- World-generation changes affect newly generated chunks, not existing chunks.
- Client/server content-pack mismatches can produce missing items, broken recipes, unusable weapons, or connection issues.
