# Handmade Guns Overdrive Command Reference

HMG registers server commands for players and administrators plus development reload commands on the Forge client command handler.

| Command | Availability | Permission | Purpose |
| --- | --- | ---: | --- |
| `/hmgmanual` | Server | 0 | Report optional Field Manual integration status |
| `/hmg infiniteammo [true\|false]` | Server | 2 | Toggle or set the global infinite-ammunition policy |
| `/reloadSettings` | Server and client | 0 | Reload external gun and attachment settings after invalidating reloadable model caches |
| `/reloadsettingsnomodel` | Server and client | 0 | Reload external gun and attachment settings while retaining the current model caches |
| `/reloadsetonlyhelditem` | Client only | 0 | Reload the model for the local player's held HMG item |

## `/hmgmanual`

Reports one of the following optional Guide-API Field Manual states:

- Disabled by `GuideBook.enableHMGGuideBook=false`.
- Guide-API is missing.
- The manual registered successfully.
- Registration failed.
- Registration is still pending.

## `/hmg infiniteammo [true|false]`

This is a server-only administrator command with permission level **2**.

- `/hmg infiniteammo` toggles the current global setting.
- `/hmg infiniteammo true` enables it explicitly.
- `/hmg infiniteammo false` disables it explicitly.

The setting applies to all players across dimensions and to future logins. It defaults to false and persists in the world's overworld gamerule `hmgInfiniteAmmo`. Creative players always receive infinite ammunition regardless of this setting.

Infinite ammo supplies the selected ammunition through the normal reload flow without consuming reserves. Loaded rounds still deplete, and magazine capacity, per-shell timing, interruption, chambering, bolt state, recoil, projectiles, sounds, and weapon durability keep their normal behavior.

Virtual supplied magazines carry `HMGInfiniteSupply` so they cannot become collectible duplicates when removed or ejected. That marker is not an entitlement and does not make a transferred weapon permanently infinite.

## External-Pack Reload Commands

These commands operate on filesystem packs under `handmadeguns_Packs/`. They do not make the generated `handmadeguns_builtin/` cache an editable pack source.

### `/reloadSettings`

Invalidates reloadable model resources, rereads registered external gun and attachment settings, rebuilds the model setup, and synchronizes the reloaded server weapon-mobility policy where applicable.

Use this after changing an external gun model, Blockbench project, model texture, animation definition, or render settings.

### `/reloadsettingsnomodel`

Rereads registered external gun and attachment settings without first invalidating the current model caches. Use it for settings that do not require a model resource replacement, such as inventory scale or offset tuning.

### `/reloadsetonlyhelditem`

This client-only development command reloads the model for the local player's currently held item. It reports an error when the sender is not a player, nothing is held, or the item has no reloadable HMG model.

## Reload Boundaries

The reload commands are development conveniences, not a complete replacement for a clean restart. They do not promise to rebuild every registry entry, pack recipe, script, creative tab, sound registration, or client/server state exactly as startup would.

Use a full client and server restart before release validation or after structural pack changes.

## Permissions

`/reloadSettings`, `/reloadsettingsnomodel`, and `/hmgmanual` return permission level `0` in source. On a public server, restrict the reload commands with a command filter, permissions layer, or server wrapper if ordinary players should not invoke them.
## HMG technology progression

- `/hmg tier get` reports the world's unlocked HMG tier.
- `/hmg tier set <tier>` accepts `0.0` through `5.0` in `0.5` increments and immediately synchronizes connected clients.
- `/hmg tier item` inspects the held gun's identifier, year, override, resolved requirement, server tier, and lock state.

The HMG command requires permission level 2. Changing HMG progression never changes MC Heli progression.
