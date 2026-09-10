# Handmade Guns Command Reference

HMG registers the following server commands.

## `/reloadSettings`

| Property | Value |
| --- | --- |
| Class | `handmadeguns.command.HMG_CommandReloadparm` |
| Permission level | `0` |
| Usage string | Source returns `null` |
| Registration | Server command; also registered with Forge client command handler |

Reloads gun definition files from `handmadeguns_Packs`, then calls the HMG proxy model setup. It is useful during pack development, but a full restart is still recommended for release/server validation because not every resource, recipe, registry, or server/client state is guaranteed to behave like a clean startup.

## `/hmgmanual`

| Property | Value |
| --- | --- |
| Class | `handmadeguns.command.HMG_CommandManual` |
| Permission level | `0` |
| Usage string | `/hmgmanual` |
| Registration | Server command |

Reports the optional HMG Field Manual state:

- Disabled by `GuideBook.enableHMGGuideBook=false`.
- Guide-API missing.
- Registered successfully.
- Registration failed.
- Registration pending.

## Permissions

The legacy reload/manual commands return permission level `0` in source. On public servers, use your server wrapper, permissions plugin, or command-filtering tooling if you do not want all players to run them.

## `/hmg infiniteammo [true|false]`

Server-only command, permission level **2** (operator/admin or console). `/hmg infiniteammo` toggles infinite ammo for **all players**. `/hmg infiniteammo true` enables it and `/hmg infiniteammo false` disables it explicitly. Creative always grants infinite ammo, even when the global setting is false.

The setting applies across dimensions to current players and future logins. It defaults to false and persists with the saved world's overworld gamerule `hmgInfiniteAmmo`, using Minecraft's existing save lifecycle. There is no client packet for changing it. Old per-player `PlayerPersisted.HMGInfiniteAmmo` flags are no longer consulted; disabling the global setting restores normal consumption for every non-Creative player.

Infinite ammo supplies the selected ammunition at normal reload completion without taking reserve ammunition or magazines from inventory. Loaded rounds still deplete: magazine capacity, per-shell timing/interruption, chambering, bolt state, rate of fire, recoil, projectiles and sounds retain their existing paths. Reload restrictions still apply. No reserve stack is required. Disabling the override restores consumption on subsequent reloads; already loaded rounds are retained. Weapon durability and consumption of disposable weapon items are not bypassed.

Supplied loaded magazine stacks carry `HMGInfiniteSupply` solely to prevent them becoming collectible duplicates on ejection (including after disabling the override or transferring the gun). This marker is not an entitlement. Existing real magazines still use their normal return path. Hand-filling a custom magazine also respects the infinite-ammo policy.