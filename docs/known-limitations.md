# Known Limitations and Validation Status

This page consolidates current boundaries that are easy to miss in the feature guides. Historical validation notes remain in the [changelog](../CHANGELOG.md).

## Imported Blockbench and Bedrock Models

The importers handle supported cube-based `.bbmodel` projects and audited TaCZ Bedrock geometry 1.12.0/1.21.0, including bones, pivots, hierarchy, textures/UVs, rigid transforms, named animations, positioning nodes, and authored hand locators. They are not a complete Blockbench, Bedrock, glTF, or TaCZ runtime.

Unsupported or intentionally unimplemented features include:

- Mesh, armature, and billboard geometry.
- glTF, Bedrock `poly_mesh`, texture meshes, locator objects, bone bindings, multiple geometry entries, and geometry versions outside 1.12.0/1.21.0.
- `.bbmodel` cube rescale/stretch, bone bindings/reset, and multi-file rigs.
- Global or quaternion interpolation, plugin easing, and nonnumeric Molang or timing expressions.
- TaCZ Lua state machines, animated-camera constraints, and automatic attachment or ammunition state logic.
- Executing imported scripts or author-machine sound paths.
- Gameplay changes driven by animation events.

Unsupported geometry or transform input rejects the project with diagnostics instead of silently producing a partial weapon. See [animation and imported-model authoring](animation-authoring.md#imported-model-limits) for the detailed contract.

Unchanged numeric Bedrock `.animation.json` can animate either imported geometry source. Ordered sources preserve embedded/local weapon actions and use shared TaCZ defaults only for absent names. The dedicated movement layer selects shared TaCZ-style idle/walk/run clips, but uses HMG's animation clock rather than TaCZ Lua distance-phase anchoring. Optional animation tracks for bones absent from a selected exported model are ignored with a diagnostic. See the [TaCZ asset compatibility audit](tacz-asset-compatibility-audit.md) for the inspected official and add-on boundary.

Packs containing `recursion/taczpack.dat` or a root `taczpack.dat` are rejected with an unsupported packed/obfuscated-payload diagnostic. HMG does not decode, unpack, or inspect that private container. This is a format blacklist only; readable TaCZ assets in other packs remain eligible for the generic importers.

## Runtime Validation Still Required

Compilation and non-OpenGL tests do not prove visual acceptance. New or converted packs should be checked in game for:

- First-person idle, ADS, fire, tactical reload, empty reload, inspect, and interruption.
- Directional walking, ADS walking, sprint entrance/loop/airborne hold/exit, and transitions between movement and action clips.
- Authored hand placement, UV orientation, hidden magazine variants, and attachment states.
- Third-person, inventory/GUI, dropped-item, nested gun, mounted, and placed-gun rendering.
- VBO and display-list rendering, including Angelica/Celeritas and NEI paths.
- Explicit HMG model reload, F3+T resource reload, reconnect, world change, and rapid item switching.
- Dedicated-server startup and matching client/server definitions.

The repository's automated animation and resolver tests cover parser, interpolation, ownership, reload-event, path, and pack-isolation behavior. They are not screenshots or an in-game rendering test suite.

## Reload Boundaries

HMG's reload commands reread registered external gun and attachment settings, with different model-cache behavior. They do not guarantee a clean rebuild of every registry entry, recipe, script, creative tab, sound registration, or client/server state. Use a full restart for release validation and structural pack changes.

Bundled content is materialized into `handmadeguns_builtin/`; unchanged files are reused, and the cache is not an editable live-reload layer.

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
