# Handmade Guns Overdrive

Handmade Guns Overdrive (`HandmadeGuns`) is a Minecraft Forge 1.7.10 firearms mod focused on configurable, pack-driven guns for survival, creative testing, and modpack/server balancing. It is the actively maintained module in this repository.

The mod adds a firearms framework rather than a single fixed weapon list: gun packs can define guns, magazines, bullets, attachments, sounds, models, recipes, and scripts. The current codebase also includes HMG crafting materials, ores, a Gun Smithing Table, gun racks, placed guns, bullet entities, optional Guide-API manual support, and server/client configuration for rendering, ADS behavior, explosions, cartridges, and manual dropped-gun pickup.

### Angelica compatibility

HMG finishes parsing models during Forge initialization, but creates model display
lists and VBOs lazily on their first normal render. This avoids issuing model draws
before Angelica 2.1.29 has entered a managed render frame while preserving both the
runtime VBO path and the legacy display-list fallback.

OBJ and MQO display-list compilation uses model-local Tessellators rather than the
global renderer singleton. This keeps inventory, NEI, held, and world gun rendering
from interrupting a Tessellator drawing session owned by Angelica/Celeritas or any
other renderer, and requires no Angelica-specific dependency.

## Who Is This For?

- **Players** who want modular firearms, ammunition, attachments, and gun-pack content in Minecraft 1.7.10.
- **Modpack authors** who need a configurable gun framework with external content-pack loading.
- **Server owners** who need global balance controls for recoil behavior, block damage, pickup rules, render options, and pack recipes.
- **Content creators** who want to ship guns, magazines, bullets, sounds, textures, models, and recipes without hardcoding every item in Java.

## Major Features

- **Pack-driven firearms** loaded from `handmadeguns_Packs` and the legacy `mods/handmadeguns/addgun` path.
- **Gun Smithing Table** and HMG crafting components for pack and in-game progression.
- **Ammunition/projectile framework** covering standard bullets, rockets, grenades, torpedoes, AP, frag, TE, AT, HE, flame, cartridges, lasers, and placed guns.
- **Attachment and magazine systems** with pack-defined compatibility.
- **Client controls** for reload, ADS, attachment GUI, magazine switching, gun fixing, zeroing, seeker controls, selector cycling, and optional manual pickup.
- **Optional Guide-API integration** for an in-game HMG Field Manual, plus `/hmgmanual` status help.
- **Ore/material support** for copper, aluminum, steel, polymer, and gun-part crafting materials.
- **Server balance controls** for block destruction, cartridge ejection, ADS behavior, knockback, threaded hit checks, LMM friendly fire, and manual pickup.

## Requirements and Compatibility

| Requirement | Value |
| --- | --- |
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614 for Minecraft 1.7.10 |
| Java | Java 8 recommended |
| Mod id | `HandmadeGuns` |
| Display name | `HandMadeGunsOverdrive` |
| Hard dependencies declared by HMG | None in `mcmod.info` |
| Optional integration | Guide-API for the HMG Field Manual; LittleMaidMobX-related config hooks are present |
| Project pages recorded in metadata | GitHub repository and CurseForge project links |

### Client and Server Requirements

- Install Handmade Guns Overdrive on **both the client and server**. The mod registers gameplay items, blocks, entities, GUIs, sounds, networking, and content-pack resources.
- Release jars carry HMG's vecmath runtime fallback and attach it directly to HMG's existing Forge `LaunchClassLoader` during coremod initialization when `javax.vecmath` is absent. The availability check uses a class resource lookup to avoid LaunchWrapper's failed-class cache; client and dedicated-server startup do not depend on the thread context or system classloader. A separate vecmath jar is not required.
- Clients and servers should use matching HMG config and content packs. Mismatched gun, magazine, bullet, or attachment definitions can cause missing items, unusable weapons, or disconnects.
- Client-only render options still belong in the generated config, but gameplay-affecting options should be treated as server policy.

## Installation

### Players

1. Install Minecraft 1.7.10.
2. Install Forge 10.13.4.1614.
3. Place the Handmade Guns Overdrive jar in `.minecraft/mods`.
4. Add any required HMG content packs to `.minecraft/handmadeguns_Packs` or the instance root's `handmadeguns_Packs` folder.
5. Start the game once to generate `config/HandmadeGuns.cfg`.
6. Review key bindings under the `HandmadeGuns` category.

HMG's third-person ADS armour compatibility temporarily exposes bow-style item use
only during player rendering and restores the previous item-use stack/count after
armour and held items finish. This prevents the visual ADS signal from suppressing
normal right-click firing on subsequent client ticks. With Combatives, use a version
that resolves crawl/swim animation before the biped ADS arm pose; Flan's armour then
copies those resolved arms. Neither compatibility path requires Flan's to be installed.

### Dedicated Servers

1. Install a Forge 1.7.10 server.
2. Place the Handmade Guns Overdrive jar in `mods/`.
3. Add the same content packs used by clients to the server `handmadeguns_Packs` folder.
4. Start the server once, stop it, and edit `config/HandmadeGuns.cfg`.
5. Restart the server and confirm clients have the same HMG jar and pack files.

### Building From Source

This repository uses legacy ForgeGradle 1.2 conventions for Minecraft 1.7.10:

```bash
./gradlew setupDecompWorkspace
./gradlew build
```

HMG releases are versioned from `HMG/version.properties`. Edit only `mod_version=x.y.z` when preparing a new release; production packaging tasks such as `build`, `shadowJar`, and `reobf` automatically reserve the next `build_number` and produce jars with the computed `mod_version.build_number` value. Development tasks such as `runClient`, `runServer`, `test`, and workspace/IDE setup do not increment the build number. If a production build fails, Gradle restores the previous build number so failed builds do not consume release iterations.

If your environment uses a different ForgeGradle 1.2 setup task, use the equivalent workspace-generation task for your IDE.

## Configuration Overview

The main HMG config is generated as `config/HandmadeGuns.cfg` from the `HandmadeGuns` mod id.

Important categories include:

- `Gun` — inventory limits, muzzle flash, ADS behavior, conflict-key handling, block destruction, hit checks, and knockback.
- `Render` — zoom rendering, FOV, player rendering, stencil use, flash effects, and VBO model rendering.
- `Cartridge` — ejected cartridge rendering/lifetime behavior.
- `ManualGunPickup` — default dropped-gun pickup using the `Pickup HMG Gun` key or a targeted right click.
- `GuideBook` — optional Guide-API manual registration.
- `LMM` — LittleMaidMobX-related friendly-fire/render compatibility settings.

See the complete active-key reference in [`docs/configuration-reference.md`](docs/configuration-reference.md).

## Documentation

- [Getting Started Guide](docs/getting-started.md)
- [Server Administration Guide](docs/server-administration.md)
- [Configuration Reference](docs/configuration-reference.md)
- [Content Pack Guide](docs/content-packs.md)
- [Command Reference](docs/command-reference.md)
- [Known Limitations and Documentation Gaps](docs/known-limitations.md)

## Troubleshooting

### Guns or magazines are missing

Confirm that the required content pack is installed on both client and server and that the pack folder is inside `handmadeguns_Packs` or the supported legacy path.

### A gun will not fire

Check that the gun has compatible ammunition or magazine items, is loaded, is not blocked by sprint/setup state, and is not broken. Use the HMG key bindings for reload, fire selector, fixing, magazine type, and attachment management.

### The in-game manual is missing

Install Guide-API for Minecraft 1.7.10 and keep `GuideBook.enableHMGGuideBook=true`. Run `/hmgmanual` for the current registration status.

### Explosions are damaging terrain

Set `Gun.cfg_blockdestroy=false` in `HandmadeGuns.cfg` to prevent HMG explosive/projectile block destruction where the projectile respects the global flag.

### Dropped guns do not auto-pickup

Manual pickup is enabled by default. Use the `Pickup HMG Gun` key, default `P`, or right click while looking at a dropped HMG gun within range. Set `ManualGunPickup.enableManualGunPickup=false` to restore automatic walk-over pickup for guns.

## FAQ

### Is this the GVC mobs mod?

No. This README is focused on `HMG/`, the maintained Handmade Guns Overdrive module. Other modules in this repository may integrate with HMG, but they are not the focus of this documentation.

### Does HMG require Guide-API?

No. Guide-API is optional. Without it, HMG still loads, but the full in-game HMG Field Manual is not registered.

### Where do content packs go?

Use `handmadeguns_Packs/<PackName>/`. The legacy `mods/handmadeguns/addgun` path is still read for older packs.

### Does HMG add server commands?

Yes. HMG registers `/reloadSettings` and `/hmgmanual`. See [`docs/command-reference.md`](docs/command-reference.md).

## Resources

- GitHub: https://github.com/RagexPrince683/GvCEXOverdrive/
- Modrinth: https://modrinth.com/mod/handmade-guns-overdrive
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/hand-made-guns-overdrive
- Minecraft Forge 1.7.10: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html
- In-repository documentation: [`docs/`](docs/)
