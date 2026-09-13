# Known Limitations and Validation Status

This page consolidates current boundaries that are easy to miss in the feature guides. Historical validation notes remain in the [changelog](../CHANGELOG.md).

## Native Blockbench Importing

The importer handles supported cube-based `.bbmodel` projects, bones, pivots, hierarchy, embedded textures, rigid transforms, named animations, positioning nodes, and authored hand locators. It is not a complete Blockbench, Bedrock, glTF, or TaCZ runtime.

Unsupported or intentionally unimplemented features include:

- Mesh, armature, and billboard geometry.
- Cube rescale/stretch, bone bindings/reset, and multi-file rigs.
- Global or quaternion interpolation, plugin easing, and nonnumeric Molang or timing expressions.
- TaCZ Lua state machines, animated-camera constraints, and automatic attachment or ammunition state logic.
- Executing imported scripts or author-machine sound paths.
- Gameplay changes driven by animation events.

Unsupported geometry or transform input rejects the project with diagnostics instead of silently producing a partial weapon. See [Animation and Blockbench authoring](animation-authoring.md#blockbench-limits) for the detailed contract.

## Runtime Validation Still Required

Compilation and non-OpenGL tests do not prove visual acceptance. New or converted packs should be checked in game for:

- First-person idle, ADS, fire, tactical reload, empty reload, inspect, and interruption.
- Authored hand placement, UV orientation, hidden magazine variants, and attachment states.
- Third-person, inventory/GUI, dropped-item, nested gun, mounted, and placed-gun rendering.
- VBO and display-list rendering, including Angelica/Celeritas and NEI paths.
- Explicit HMG model reload, F3+T resource reload, reconnect, world change, and rapid item switching.
- Dedicated-server startup and matching client/server definitions.

The repository's automated animation and resolver tests cover parser, interpolation, ownership, reload-event, path, and pack-isolation behavior. They are not screenshots or an in-game rendering test suite.

## Reload Boundaries

HMG's reload commands reread registered external gun and attachment settings, with different model-cache behavior. They do not guarantee a clean rebuild of every registry entry, recipe, script, creative tab, sound registration, or client/server state. Use a full restart for release validation and structural pack changes.

Bundled content is regenerated into `handmadeguns_builtin/` and is not an editable live-reload layer.

## Remaining Documentation Gaps

- The complete grammar for every legacy gun, magazine, bullet, attachment, recipe, tab, and script parser is not yet documented.
- Some legacy configuration behavior, including the complete player-facing semantics of `MAXGUNSINV`, `cfg_ADS_Sneaking`, and `cfg_AvoidHit`, still needs focused gameplay testing.
- Uncommon legacy model, projectile, NPC, vehicle, and script paths are not covered as completely as unified handheld guns.
- No maintained screenshot or visual-regression set currently ships with the documentation.

## Platform Boundaries

- HMG-Overdrive targets Minecraft Forge 1.7.10 and legacy ForgeGradle conventions.
- Java 8 is the supported development and runtime baseline.
- World-generation changes affect newly generated chunks rather than already generated terrain.
- Definition mismatches between client and server can cause missing items, broken recipes, unusable weapons, or protocol/behavior differences.
- The main documentation covers the maintained `HMG/` module, not every older companion module retained in the repository.
