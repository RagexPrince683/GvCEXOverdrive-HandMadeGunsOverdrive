# Handmade Guns Overdrive

Handmade Guns Overdrive (HMG-Overdrive) is a maintained Minecraft Forge 1.7.10 firearms framework. It combines the established HMG/GvCEX gun-pack ecosystem with modernized loading, rendering, animation, multiplayer, and server-management behavior.

The mod is data-driven: guns, magazines, ammunition, attachments, models, sounds, recipes, and presentation behavior can be supplied by content packs without adding new Java code.

> **This project is part of [DEADHOUR](https://www.curseforge.com/minecraft/modpacks/deadhour).**

## Highlights

- **Ready-to-play official content.** The maintained HMG packs are bundled in the mod JAR. A separate asset download is not required.
- **External content packs.** Packs in `handmadeguns_Packs/` load after the bundled baseline and can add content or override compatible bundled definitions and resources.
- **Native Blockbench support.** Unified guns can load `.bbmodel` projects directly, including cubes, bones, pivots, hierarchy, embedded textures, and embedded animations. OBJ or MQO export is not required.
- **Modern animation runtime.** Named clips support layered BASE/ACTION/ADDITIVE playback, transitions, interruption, per-weapon ownership, and presentation events. Existing OBJ/MQO guns and legacy HMG motion remain supported.
- **TaCZ-style presentation.** Imported projects can use familiar action names, first-person and ADS view nodes, third-person hand placement, and authored hand locators.
- **Configurable firearm systems.** HMG provides pack-defined weapons, magazines, ammunition, attachments, projectiles, gun skins, placed guns, gun racks, crafting components, and the Gun Smithing Table.
- **Multiplayer and administration.** Server-owned reload and ammunition policy, synchronized weapon mobility, manual dropped-gun pickup, reload tools, global infinite ammo, and server-side balance controls support shared servers.
- **Legacy-mod compatibility.** Maintained integrations cover Combatives, Angelica/Celeritas, BackTools, Flan's armor presentation, Guide-API, and other common 1.7.10 rendering environments.

## Requirements

| Component | Requirement |
| --- | --- |
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614 |
| Java | Java 8 recommended |
| Installation side | Client and server |
| Mod ID | `HandmadeGuns` |
| External content packs | Optional; required only for additional or server-specific content |

### Client and Server Requirements

- Install Handmade Guns Overdrive on **both the client and server**. The mod registers gameplay items, blocks, entities, GUIs, sounds, networking, and content-pack resources.
- Release jars carry HMG's vecmath runtime fallback and attach it directly to HMG's existing Forge `LaunchClassLoader` during coremod initialization when `javax.vecmath` is absent. The availability check uses a class resource lookup to avoid LaunchWrapper's failed-class cache; client and dedicated-server startup do not depend on the thread context or system classloader. A separate vecmath jar is not required.
- Clients and servers should use matching HMG config and content packs. Mismatched gun, magazine, bullet, or attachment definitions can cause missing items, unusable weapons, or disconnects.
- Client-only render options still belong in the generated config, but gameplay-affecting options should be treated as server policy.

## Installation

1. Install Minecraft Forge 1.7.10-10.13.4.1614.
2. Place the HMG-Overdrive release JAR in the instance's `mods/` folder on both the client and server.
3. Launch once to generate `config/HandmadeGuns.cfg`.
4. Review the controls in the `HandmadeGuns` category; several legacy defaults overlap and the inspect key is unbound by default.
5. If the server uses additional content packs, place them in the instance-root `handmadeguns_Packs/` directory on both sides.

The JAR already contains the maintained official guns and assets. Do not install the private `handmadeguns_builtin/` cache as a pack or edit it; HMG regenerates that cache from the JAR for its legacy file-based loaders.

For a fuller walkthrough, see [Getting started](docs/getting-started.md).

## Content Packs

The recommended external layout is:

```text
handmadeguns_Packs/
  MyPack/
    guns/
    magazines/
    bullets/
    attachments/
    models/
    animations/
    textures/
      models/
      items/
      misc/
    sounds/
```

Asset resolution is pack-scoped. HMG rejects absolute paths, `..` traversal, canonical or symbolic-link escapes, and cross-pack searches.

A unified gun can use a normal OBJ/MQO model with optional named animation JSON:

```text
Model,my_rifle.obj
ModelTexture,my_rifle.png
IconTexture,my_rifle_icon.png
Animations,my_rifle.json
```

`IconTexture` (or the legacy `Texture` spelling) uses the supplied normal item sprite directly and
is the default inventory behavior. Packs that explicitly set `UseModelIcon,true` opt into one lazy
canonical model capture, persistently cached under `cache/hmg/icons` instead of redrawing the gun
model for every inventory, creative-tab, GUI, or NEI pass.

Or it can load a native Blockbench project, including its embedded model and animations:

```text
BlockbenchModel,my_rifle.bbmodel
```

The Blockbench file is used directly; it does not need to be exported to another model format or modified for HMG. The gun still uses an ordinary HMG TXT definition for gameplay values.

See [Content packs](docs/content-packs.md) and [Animation and Blockbench authoring](docs/animation-authoring.md) for supported layouts, directives, animation names, positioning nodes, and current importer limits.

## Documentation

Start with the [documentation index](docs/README.md), or open a guide directly:

| Guide | Purpose |
| --- | --- |
| [Getting started](docs/getting-started.md) | Installation, controls, and first-use workflow |
| [Content packs](docs/content-packs.md) | Pack roots, asset layout, definitions, recipes, skins, and overrides |
| [Animation and Blockbench authoring](docs/animation-authoring.md) | Native `.bbmodel` importing and optional animation JSON |
| [Configuration reference](docs/configuration-reference.md) | Generated config keys and advanced pack directives |
| [Server administration](docs/server-administration.md) | Deployment, balancing, reloads, and operations |
| [Command reference](docs/command-reference.md) | Player, development, and administrator commands |
| [Compatibility notes](docs/compatibility.md) | Combatives, Angelica/Celeritas, BackTools, and Flan's interactions |
| [Known limitations](docs/known-limitations.md) | Unsupported features, validation status, and remaining gaps |
| [Changelog](CHANGELOG.md) | Chronological implementation history |

## Compatibility Notes

- Install matching HMG builds on the client and server.
- Keep server-required external packs synchronized. Definition mismatches can produce missing or unusable content even when cosmetic model and animation parsing is client-only.
- Guide-API is optional and only provides the HMG Field Manual integration.
- Blockbench importing does not import TaCZ Lua state machines, gameplay code, ballistics, camera constraints, or complete attachment/ammunition logic.
- Compatibility and animation systems with outstanding in-game checks are listed in [Known limitations](docs/known-limitations.md).

## Building From Source

This repository uses legacy ForgeGradle conventions and requires a Java 8 toolchain.

```bash
./gradlew setupDecompWorkspace
./gradlew :HMG:build
```

The maintained module is `HMG/`. The repository also contains older companion and reference modules that are not covered by the main HMG documentation.

HMG release versions are stored in `HMG/version.properties`. Production packaging manages the build number automatically; edit only `mod_version` when intentionally preparing a new release.

## Links

- [GitHub repository](https://github.com/RagexPrince683/GvCEXOverdrive-HandMadeGunsOverdrive)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/hand-made-guns-overdrive)
- [Modrinth](https://modrinth.com/mod/handmade-guns-overdrive)
- [DEADHOUR modpack](https://www.curseforge.com/minecraft/modpacks/deadhour)
