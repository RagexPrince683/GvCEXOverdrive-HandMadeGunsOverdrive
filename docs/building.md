# Building GvCEX Overdrive

Use a **Java 8 JDK** and the checked-in Gradle wrapper. The wrapper selects Gradle 4.5. The build uses ForgeGradle and Minecraft Forge `1.7.10-10.13.4.1614-1.7.10`. Set `JAVA_HOME` to your Java 8 JDK and verify the JVM printed by `./gradlew --version` before setting up the workspace.

## First build

Run from the repository root. On Windows, substitute `.\gradlew.bat` for `./gradlew`.

```sh
./gradlew setupDecompWorkspace
./gradlew :HMG:build
./gradlew :GVC:build
./gradlew :GVCDangeonsBase:build
```

The first setup needs network access to download the wrapper, Forge and Minecraft development artifacts, mappings, and dependencies declared by the build. No separately installed mod JARs are required to compile these modules. GVC consumes HMG and `Depend` compile outputs from the same checkout. Dungeon Generator Base consumes GVC and HMG classes. A GVC or dungeon build can compile these prerequisites while packaging only the requested mod.

## Production outputs

| Requested task | Reobfuscated JAR |
| --- | --- |
| `:HMG:build` | `HMG/build/libs/HandmadeGunsOverdrive-*.jar` |
| `:GVC:build` | `GVC/build/libs/GVCOverdrive-*.jar` |
| `:GVCDangeonsBase:build` | `GVCDangeonsBase/build/libs/GVCDangeonsBase-*.jar` |

Use the output of `build` for an ordinary Minecraft instance. The task includes ForgeGradle's `reobf` step. HMG packages its bundled packs and vecmath fallback in that output. Each companion remains a separate JAR.

Set the release version for each module through `mod_version` in `HMG/version.properties` or `GVC/version.properties`. The value becomes the complete version in the JAR filename and packaged metadata. Builds leave both version files untouched. Dungeon Generator Base uses its own fixed release version.

## Offline builds

After workspace setup and one successful build of the relevant module, add `--offline` to reuse the same Gradle dependency cache and ForgeGradle workspace data.

```sh
./gradlew --offline :HMG:build
./gradlew --offline :GVC:build
./gradlew --offline :GVCDangeonsBase:build
```

An offline cache miss means Gradle still needs the named artifact or repository metadata. Warm the cache with an online build or use a dependency mirror appropriate to your environment. Keep the wrapper, Forge target, and Java target consistent.

See the [project index](README.md) for gameplay, content-pack, and compatibility guides.
