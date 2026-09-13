# Getting Started with Handmade Guns Overdrive

This guide covers installation and first use of the actively maintained `HMG/` module.

## Install HMG

Install the following on both the client and server:

1. Minecraft 1.7.10.
2. Forge 10.13.4.1614.
3. The same Handmade Guns Overdrive release JAR.

The release JAR includes the maintained official HMG content and a vecmath runtime fallback. You do not need a separate official asset archive or vecmath JAR.

Start the game once to generate `config/HandmadeGuns.cfg`.

### Optional external packs

Additional and server-specific packs belong in:

```text
<instance>/handmadeguns_Packs/<PackName>/
```

Install gameplay definitions on both sides and keep server-required pack versions synchronized. External packs load after the bundled content, allowing compatible definitions and resources to add to or override the bundled baseline.

The generated `handmadeguns_builtin/` directory is a private runtime cache. Do not install packs there or edit it.

## Review Controls

HMG registers its bindings under the `HandmadeGuns` category. Several legacy defaults overlap, so review them before playing seriously.

| Action | Default |
| --- | --- |
| Reload magazine | `R` |
| Fire attached gun | `F` |
| ADS | Legacy mouse binding; affected by `cfg_Swap_Fire_And_ADS_Keys` |
| Prepare gun modification | Left Alt |
| Attachment GUI | `X` |
| Change magazine type | `B` |
| Fix gun | `H` |
| Gun settings modification | Unbound |
| Increase zero | `Y` |
| Reset zero | `H` |
| Decrease zero | `N` |
| Seeker open/close | `C` |
| Cycle selector | `F` |
| Inspect | Unbound |
| Pick up HMG gun | `P`; targeted right click also works |

## First-Use Workflow

1. Open a Creative test world or connect with the server's required packs installed.
2. Confirm that HMG's creative tabs contain guns, magazines, ammunition, attachments, and crafting components.
3. Select a gun and verify its accepted ammunition and attachments in the tooltip.
4. Review reload, ADS, fire-selector, magazine-switching, and attachment controls.
5. If testing native Blockbench content, check first person, ADS, third person, inventory, firing, and both tactical and empty reloads.

Creative-tab unified guns begin with a normally loaded magazine. Shots still consume the loaded rounds; Creative or the global administrator override supplies ammunition during reload without creating collectible virtual magazines.

## Survival and Crafting

HMG includes the Gun Smithing Table, crafting materials, metal ores, gun parts, and pack-defined recipes. A typical pack can require you to:

1. Gather vanilla and HMG materials.
2. Craft receivers, barrels, firing components, furniture, or other required parts.
3. Use the Gun Smithing Table or ordinary recipes supplied by the active pack.
4. Craft a compatible gun, magazine, ammunition, and attachments.

Exact progression is pack-defined. See [Content packs](content-packs.md) for the data layout and recipe system.

## Dropped-Gun Pickup

Manual gun pickup is enabled by default. Walking over a dropped HMG gun does not collect it. Look at the gun and press the pickup binding, default `P`, or right click it within the configured range.

Servers control the allowed distance, line-of-sight requirement, and whether the rule applies only to guns. See [Configuration reference](configuration-reference.md#manualgunpickup).

## Optional Field Manual

Guide-API is optional. When it is installed and `GuideBook.enableHMGGuideBook=true`, HMG registers an in-game Field Manual. Run `/hmgmanual` to see whether the integration is disabled, missing, registered, failed, or still pending.

## Troubleshooting

### Guns, magazines, or assets are missing

Confirm that the required external pack is an immediate child of `handmadeguns_Packs/` and is installed on both sides. Check the log for rejected paths, missing resources, incompatible identifier collisions, or parse failures.

### A gun will not fire

Confirm that the gun has compatible ammunition or a compatible loaded magazine, is not broken, and is not blocked by its setup, sprint, reload, or bolt state. Review the fire-selector, reload, magazine-type, gun-fixing, and attachment bindings.

### Explosions are damaging terrain

Set `Gun.cfg_blockdestroy=false` in `HandmadeGuns.cfg` where server terrain protection takes priority. Individual projectile behavior can still have additional limits described by its pack definition.

### Dropped guns do not pick up automatically

This is the default manual-pickup policy. Target the gun and use the pickup key or right click it, or set `ManualGunPickup.enableManualGunPickup=false` to restore normal walk-over pickup for affected HMG items.

## Next Steps

- Players: [Command reference](command-reference.md)
- Server owners: [Server administration](server-administration.md)
- Pack creators: [Content packs](content-packs.md)
- Blockbench and animation creators: [Animation and Blockbench authoring](animation-authoring.md)
- Problems or unsupported behavior: [Known limitations](known-limitations.md)
