# GVC Dungeon Generator Base

GVC Dungeon Generator Base adds optional structure packs to GVCOverdrive. It requires HMG and GVC and targets Minecraft 1.7.10, Forge 10.13.4.1614, and Java 8. Install `GVCDangeonsBase-*.jar` as the third JAR on the client and server. GVC's native camps are available without this module.

## Install compatible packs

Extract each pack into its own immediate child directory in `GVCDungeons_Packs/` at the instance root. Restart the instance to load changes.

```text
GVCDungeons_Packs/
  PackName/
    setting.txt
    data/
      structure.schematic
```

The loader reads `setting.txt` and the data files below `data/`. It accepts supported schematic files and legacy Shift-JIS text data. Files and pack directories are processed in filename order. Give each pack a distinct `NAME` in `setting.txt`. The source tree includes a [Boforos pack and an ObserverTochka schematic example](../../GVCDangeonsBase/eclipse/GVCDungeons_Packs). Structure packs can reference HMG and GVC items by registry ID. Infantry markers and compatible chest loot remain supported. Vehicle markers are skipped.

## Existing worlds

The module registers `GVCDungeonStructureBase` and `GVCDungeonComponentBase` with Minecraft's structure persistence system. Keep the module and matching packs installed for worlds that already have saved dungeon structures. Newly installed packs place structures as eligible chunks generate. Missing mods referenced by a pack, unavailable registry IDs, or invalid data can leave gaps in expected content. Check startup logs and validate new pack combinations in a separate world.

The unfinished factory and large-trench sources live in `GVCDangeons`. That directory is separate from the supported `GVCDangeonsBase` module.

## Build

Run `./gradlew :GVCDangeonsBase:build` from the repository root with Java 8. The reobfuscated artifact is under `GVCDangeonsBase/build/libs/`. See the [shared build guide](../building.md).
