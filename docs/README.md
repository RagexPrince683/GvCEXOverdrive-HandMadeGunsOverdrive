# GvCEX Overdrive documentation

- [Handmade Guns Overdrive](hmg.md) covers the firearm framework, bundled content, and external packs.
- [GVCOverdrive](gvc/README.md) covers infantry, faction gameplay, squad orders, configuration, and world generation.
- [GVC Dungeon Generator Base](gvc/dungeon-generator.md) covers compatible structure packs and placement.
- [Building from source](building.md) covers all three production modules.

## HMG guides

### Players

1. [Getting started](getting-started.md) — install HMG, review controls, and understand the normal first-use workflow.
2. [Command reference](command-reference.md) — see the available player, development, and administrator commands.
3. [Known limitations](known-limitations.md) — check unsupported features and areas that still need runtime validation.

### Modpack and Server Owners

1. [Server administration](server-administration.md) — deploy matching builds and packs, select safer defaults, and operate reload tools.
2. [Configuration reference](configuration-reference.md) — review generated configuration keys and defaults.
3. [Compatibility notes](compatibility.md) — understand the maintained Combatives, Angelica/Celeritas, BackTools, Flan's armor, and related integration paths.

### Content-Pack Authors

1. [Content packs](content-packs.md) — learn pack roots, source precedence, clean asset layout, definition folders, recipes, attachments, and skins.
2. [Animation and imported-model authoring](animation-authoring.md) — import native `.bbmodel` projects or exported Bedrock cube geometry, and add named animation JSON to HMG parts.
3. [TaCZ asset compatibility audit](tacz-asset-compatibility-audit.md) — review the official/add-on rig, marker, texture, action, and locomotion contract and current distribution-format limits.
4. [Configuration reference](configuration-reference.md#content-pack-directives) — use advanced ammunition, attachment-model, placement, inventory-rendering, and ADS directives.

### Developers

- [Animation and Blockbench authoring](animation-authoring.md#runtime-behavior-and-extension-api) documents playback ownership and the client extension API.
- [Combatives camera integration audit](combatives-camera-integration-audit.md) records implementation-level capability and ownership decisions. It is an engineering audit, not an installation guide.
- The root [changelog](../CHANGELOG.md) is the chronological implementation record.

## Documentation Boundaries

- The root [README](../README.md) is the project overview and quick-start page.
- Historical changelog entries preserve the validation status that existed when each change was made. Current unresolved runtime checks are consolidated in [Known limitations](known-limitations.md).
- The legacy definition parsers expose more settings than the maintained guides currently explain. Undocumented parser behavior should not be treated as a stable authoring contract.
