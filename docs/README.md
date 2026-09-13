# Handmade Guns Overdrive Documentation

These guides document the actively maintained `HMG/` module in the GvCEXOverdrive repository. Use the path below that matches what you are trying to do.

## Players

1. [Getting started](getting-started.md) — install HMG, review controls, and understand the normal first-use workflow.
2. [Command reference](command-reference.md) — see the available player, development, and administrator commands.
3. [Known limitations](known-limitations.md) — check unsupported features and areas that still need runtime validation.

## Modpack and Server Owners

1. [Server administration](server-administration.md) — deploy matching builds and packs, select safer defaults, and operate reload tools.
2. [Configuration reference](configuration-reference.md) — review generated configuration keys and defaults.
3. [Compatibility notes](compatibility.md) — understand the maintained Combatives, Angelica/Celeritas, BackTools, Flan's armor, and related integration paths.

## Content-Pack Authors

1. [Content packs](content-packs.md) — learn pack roots, source precedence, clean asset layout, definition folders, recipes, attachments, and skins.
2. [Animation and Blockbench authoring](animation-authoring.md) — import native `.bbmodel` projects or add named animation JSON to OBJ/MQO guns.
3. [Configuration reference](configuration-reference.md#content-pack-directives) — use advanced ammunition, attachment-model, placement, inventory-rendering, and ADS directives.

## Developers

- [Animation and Blockbench authoring](animation-authoring.md#runtime-behavior-and-extension-api) documents playback ownership and the client extension API.
- [Combatives camera integration audit](combatives-camera-integration-audit.md) records implementation-level capability and ownership decisions. It is an engineering audit, not an installation guide.
- The root [changelog](../CHANGELOG.md) is the chronological implementation record.

## Documentation Boundaries

- The root [README](../README.md) is the project overview and quick-start page.
- These guides focus on HMG-Overdrive. Older companion modules in the repository are outside their main scope.
- Historical changelog entries preserve the validation status that existed when each change was made. Current unresolved runtime checks are consolidated in [Known limitations](known-limitations.md).
- The legacy definition parsers expose more settings than the maintained guides currently explain. Undocumented parser behavior should not be treated as a stable authoring contract.
