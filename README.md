# GvCEX Overdrive

GvCEX Overdrive maintains three companion mods for Minecraft 1.7.10. Handmade Guns Overdrive provides the firearm framework and content packs. GVC Overdrive uses those systems for infantry and faction gameplay, and GVC Dungeon Generator Base adds optional structure-pack support.

| Project | Production JAR | Role and dependencies |
| --- | --- | --- |
| [Handmade Guns Overdrive](docs/hmg.md) | `HandmadeGunsOverdrive-*.jar` | Primary firearm mod. Runs independently on Forge. |
| [GVC Overdrive](docs/gvc/README.md) | `GVC-HD*.jar` | Infantry, factions, camps, raids, and squad orders. Requires HMG and provides both `GVCMob` and `GVCGuns`. |
| [GVC Dungeon Generator Base](docs/gvc/dungeon-generator.md) | `GVCDangeonsBase-*.jar` | Optional generator for compatible structure packs. Requires HMG and GVC. |

## Install

1. Set up **Minecraft 1.7.10**, **Forge 10.13.4.1614**, and **Java 8**.
2. Put the HMG JAR in the instance's `mods/` directory. Add the GVC JAR for faction gameplay. Add Dungeon Generator Base only if you use a compatible structure pack.
3. Use matching mod builds and gameplay packs on the client and dedicated server. Put HMG packs under `handmadeguns_Packs/` and dungeon packs under `GVCDungeons_Packs/` in the relevant instance.
4. Launch once to generate configuration. HMG's maintained source builds include the official firearm packs and a vecmath fallback. Follow any additional asset instructions attached to the particular release you downloaded.

Download HMG from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/hand-made-guns-overdrive/files/all) or [Modrinth](https://modrinth.com/mod/handmade-guns-overdrive/versions). Check [GitHub Releases](https://github.com/RagexPrince683/GvCEXOverdrive-HandMadeGunsOverdrive/releases) for published companion JARs. If a matching GVC or Dungeon Generator Base build is unavailable there, build it from the same checkout as HMG.

## What the mods add

**Handmade Guns Overdrive** adds pack-defined firearms, ammunition, magazines, attachments, placed guns, and crafting. It supports legacy OBJ/MQO models, native Blockbench projects, and supported TaCZ-derived Bedrock assets and animations. The [HMG guide](docs/hmg.md) links to configuration, server administration, rendering, and pack-authoring references.

**GVC Overdrive** adds ALLIES Radical Party guerrillas, UNION soldiers, and friendly PMC infantry. Camps, beacons, raids, and reinforcements bring those factions into the world. PMC groups accept move, follow, patrol, defend, and attack orders. See the [GVC guide](docs/gvc/README.md) for controls, spawning, and existing-world details.

**GVC Dungeon Generator Base** reads compatible `GVCDungeons_Packs` at startup. GVC's own camp generator runs independently of the optional module. See the [structure pack guide](docs/gvc/dungeon-generator.md) for supported layouts and markers.

## Build from source

Run the native Gradle wrapper with a Java 8 JDK. Use `gradlew.bat` on Windows or `./gradlew` on macOS and Linux. The [build guide](docs/building.md) covers dependency setup, offline builds, reobfuscation, and release numbering.

| Build task | Production output |
| --- | --- |
| `:HMG:build` | `HMG/build/libs/HandmadeGunsOverdrive-*.jar` |
| `:GVC:build` | `GVC/build/libs/GVC-HD*.jar` |
| `:GVCDangeonsBase:build` | `GVCDangeonsBase/build/libs/GVCDangeonsBase-*.jar` |

## Development status

The supported build targets are `HMG`, `GVC`, and `GVCDangeonsBase`. `HMP`, `WW2`, `DCreator`, and `Linker` remain in the repository outside the supported release set. `Depend` provides build-time compatibility classes, and `Guide-API-1.7.10` and the reference trees are upstream sources. Factory and large-trench generation in `GVCDangeons` is incomplete and excluded from the supported build. Vehicle spawning, vehicle operation, and full MCHeli integration are deferred. MCHeli is optional for the current GVC release scope. See the [documentation index](docs/README.md), [compatibility notes](docs/compatibility.md), and [changelog](CHANGELOG.md).

## Credits and contributing

GvCEX and Handmade Guns originated with frou01 and SCOPE_MG36. RagexPrince683 and Zealot contribute to Overdrive. See the HMG [mod credits](HMG/src/main/resources/mcmod.info), the [TaCZ official pack notices](HMG/src/main/resources/hmg_packs/TaCZOfficial/CREDITS.txt), and the [published HMG license](https://www.curseforge.com/minecraft/mc-mods/hand-made-guns-overdrive/license). Bundled packs and third-party code carry their own credits and license notices. See also the [Forge license](MinecraftForge-License.txt) and [FML license](LICENSE-fml.txt).

Report bugs or propose changes in [GitHub Issues](https://github.com/RagexPrince683/GvCEXOverdrive-HandMadeGunsOverdrive/issues). Include the mod JARs, Forge and Java versions, relevant logs, and steps to reproduce the issue. The project is part of the [DEADHOUR modpack](https://www.curseforge.com/minecraft/modpacks/deadhour).
