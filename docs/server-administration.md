# Handmade Guns Overdrive Server Administration

This guide focuses on running `HandmadeGuns` on a dedicated Forge 1.7.10 server.

## Deployment Checklist

- Install the same HMG jar on server and clients.
- The release JAR already includes the maintained official content; no separate official asset archive is required.
- Install the same server-required external content packs on server and clients.
- Start the server once to generate `config/HandmadeGuns.cfg`.
- Stop the server before editing configs.
- Restart after config or pack changes.
- Confirm that the server can load HMG's bundled vecmath fallback before opening the server to players.

## Recommended Public-Server Settings

For safer terrain and predictable pickup behavior:

```properties
Gun.cfg_blockdestroy=false
ManualGunPickup.enableManualGunPickup=true
ManualGunPickup.manualGunPickupRequiresLineOfSight=true
ManualGunPickup.manualGunPickupOnlyGuns=true
ManualGunPickup.manualGunPickupRange=3.0
```

For lower visual/entity overhead:

```properties
Cartridge.cfg_canEjectCartridge=false
Render.enableVBOModelRendering=true
```

## Balancing Guidance

- Balance individual weapons in content-pack gun files first.
- Use `Gun.cfg_blockdestroy=false` for PvE/PvP servers where terrain griefing matters.
- Use manual pickup if dropped guns are high value and accidental pickup or item-vacuum behavior is undesirable.
- Keep content-pack recipe files consistent with your server economy.
- Review `cfg_KnockBack`, `cfg_KnockBackY`, and projectile definitions when tuning PvP.

## Content Pack Operations

HMG first loads its bundled official packs, then external packs from `handmadeguns_Packs`, followed by the supported legacy `mods/handmadeguns/addgun` path. Compatible external definitions and resources can override the bundled baseline. Packs are sorted by folder/file name before loading. Keep pack names stable across server updates so item registration order is predictable.

`handmadeguns_builtin/` is a regenerated private cache for the bundled JAR resources. Do not deploy custom packs into it, edit it, or depend on its contents surviving an update.

When updating packs:

1. Back up the world and pack directory.
2. Stop the server.
3. Update the pack on server and clients.
4. Restart and watch the console for pack-load errors, identifier collisions, rejected paths, and warnings. If you need verbose content-pack confirmation or timing details, temporarily enable `Logging.enableDebugLogging` and watch for `[Timing]` messages.
5. Test representative guns, magazines, bullets, recipes, sounds, and models before reopening the server.

Gameplay definitions must match on both sides. Blockbench geometry, embedded textures, and named animation parsing are client-only, but that does not make an entire gameplay pack client-only.

## Operations and Commands

- `/reloadSettings` invalidates reloadable model caches, reloads external gun and attachment settings, and rebuilds model setup on the side where it is run.
- `/reloadsettingsnomodel` reloads external gun and attachment settings while retaining current model caches.
- `/reloadsetonlyhelditem` is a client-only development command for the local player's held HMG model.
- `/hmgmanual` reports Guide-API/HMG Field Manual status and also has permission level `0`.
- `/hmg infiniteammo [true|false]` is a permission-level-2 global administrator toggle that persists in the world gamerule `hmgInfiniteAmmo`.

The level-0 reload commands should be restricted on public servers with external command or permissions tooling. See the [command reference](command-reference.md) for exact behavior and reload boundaries.
