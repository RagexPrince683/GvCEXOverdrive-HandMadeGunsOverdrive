# Handmade Guns Overdrive

Handmade Guns Overdrive (HMG-Overdrive) is a maintained Minecraft Forge 1.7.10 firearms framework. It combines the established HMG/GvCEX gun-pack ecosystem with modernized loading, rendering, animation, multiplayer, and server-management behavior.

The mod is data-driven: guns, magazines, ammunition, attachments, models, sounds, recipes, and presentation behavior can be supplied by content packs without adding new Java code.

## Highlights

- **Ready-to-play official content.** The maintained HMG packs are bundled in the mod JAR. A separate asset download is not required.
- **External content packs.** Packs in `handmadeguns_Packs/` load after the bundled baseline and can add content or override compatible bundled definitions and resources.
- **Native Blockbench support.** Unified guns can load `.bbmodel` projects directly, including cubes, bones, pivots, hierarchy, embedded textures, and embedded animations. OBJ or MQO export is not required.
- **Modern animation runtime.** Named clips support layered BASE/ACTION/ADDITIVE playback, transitions, interruption, per-weapon ownership, and presentation events. Existing OBJ/MQO guns and legacy HMG motion remain supported.
- **TaCZ-style presentation.** Imported projects can use familiar action names, first-person and ADS view nodes, third-person hand placement, and authored hand locators.
- **Configurable firearm systems.** HMG provides pack-defined weapons, magazines, ammunition, attachments, projectiles, gun skins, placed guns, gun racks, crafting components, and the Gun Smithing Table.
- **Multiplayer and administration.** Server-owned reload and ammunition policy, synchronized weapon mobility, manual dropped-gun pickup, reload tools, global infinite ammo, and server-side balance controls support shared servers.
- **Legacy-mod compatibility.** Maintained integrations cover Combatives, Angelica/Celeritas, BackTools, Flan's armor presentation, Guide-API, and other common 1.7.10 rendering environments.

## Installation

HMG runs independently on Minecraft 1.7.10 with Forge 10.13.4.1614 and Java 8. Install the same build and gameplay content packs on the client and server. Source builds include the maintained HMG packs and a vecmath fallback. Guide-API is optional and enables the in-game Field Manual. See [Getting started](getting-started.md) and [Building from source](building.md).

## Documentation

Start with the [documentation index](README.md), or open a guide directly:

| Guide | Purpose |
| --- | --- |
| [Getting started](getting-started.md) | Installation, controls, and first-use workflow |
| [Content packs](content-packs.md) | Pack roots, asset layout, definitions, recipes, skins, and overrides |
| [Animation and Blockbench authoring](animation-authoring.md) | Native `.bbmodel` importing and optional animation JSON |
| [Configuration reference](configuration-reference.md) | Generated config keys and advanced pack directives |
| [Server administration](server-administration.md) | Deployment, balancing, reloads, and operations |
| [Command reference](command-reference.md) | Player, development, and administrator commands |
| [Compatibility notes](compatibility.md) | Combatives, Angelica/Celeritas, BackTools, and Flan's interactions |
| [Known limitations](known-limitations.md) | Unsupported features, validation status, and remaining gaps |
| [Changelog](../CHANGELOG.md) | Chronological implementation history |

## Compatibility Notes

- Install matching HMG builds on the client and server.
- Keep server-required external packs synchronized. Definition mismatches can produce missing or unusable content even when cosmetic model and animation parsing is client-only.
- Guide-API is optional and only provides the HMG Field Manual integration.
- Blockbench importing does not import TaCZ Lua state machines, gameplay code, ballistics, camera constraints, or complete attachment/ammunition logic.
- Compatibility and animation systems with outstanding in-game checks are listed in [Known limitations](known-limitations.md).

