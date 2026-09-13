# Handmade Guns Overdrive Configuration Reference

This reference covers two related configuration surfaces: directives authored inside content packs and the generated `config/HandmadeGuns.cfg` file. For pack roots and basic layouts, start with [Content packs](content-packs.md).

## Content-pack directives

### Per-ammunition door breaching

`CustomMagazine` ammunition can opt a projectile into close-range wooden-door breaching:

```text
candoorbreach,true
```

The lowercase key defaults to `false` when omitted. An enabled projectile opens a wooden door only when it directly impacts the door no more than three blocks from its firing origin. Iron and other metal doors are never opened. Because the value belongs to the ammunition item, a gun can alternate between ordinary buckshot and door-breaching slugs without changing its gun configuration.

### Attachment models and placement

Attachment TXT files may opt into a real model item/rendered attachment with:

```text
attach3dmodel = my_attachment
3dmodeltex = my_attachment
InventoryScale,1.0
InventoryOffset,-0.4,0.15,0
```

These attachment extension keys accept either comma syntax or `key = value` syntax,
with surrounding whitespace ignored. Models and textures are resolved below
`handmadeguns:textures/model/`, just like other HMG pack models. Explicit `.mqo`
and `.obj` model extensions are supported; an extensionless name probes `.mqo`
first and then `.obj`. `3dmodeltex` defaults to `.png`, and when omitted falls
back to a PNG with the resolved model's base name. Attachments
without `attach3dmodel` retain their normal 2D icon and legacy rendering.

Numbered variants select a model and texture for the matching existing gun attachment inventory slot directly:

```text
attach3dmodel1,optic_top
3dmodeltex1,optic_top
attach3dmodel5 = optic_offset
3dmodeltex5 = optic_offset
```

Only suffixes `1` through `5` are valid. Installed rendering prefers the matching numbered model and texture, then the singular legacy values. Standalone inventory and dropped-item rendering prefers the singular model, or otherwise the lowest numbered model, with its matching texture. One renderer caches all variants; the suffix is a GUI/NBT inventory slot, never a model-part index.

`InventoryScale,<number>` controls only the 3D attachment model rendered as an
inventory item. `1.0` is the default model scale multiplier, preserving the
renderer's original visible `6.0` base scale for existing packs; `0.5` is half
that size and `2.0` is twice that size. The value must be finite and greater
than zero. `InventoryOffset,<x>,<y>,<z>` moves only that inventory-rendered 3D
model along the inventory X, Y, and depth axes and defaults to `0,0,0`. Offset
values use model units converted by the renderer's fixed base scale, but are not
multiplied by `InventoryScale`, so changing model size does not move the chosen
center. Neither key affects the attachment when installed on a gun.
`attachmentlocation` remains the installed-gun placement control.

`/reloadSettings` destructively refreshes the OBJ/MQO model caches, rereads all
singular and numbered attachment model and texture keys, and replaces the item
renderer on each affected already registered attachment. Model, texture, and
path changes therefore take effect without registering another item or recipe;
the newly loaded models are finalized together with the reloaded gun models.

Gun TXT files use the same inventory-only controls:

```text
InventoryScale,0.75
InventoryOffset,-1.2,-1.1,0
```

For guns, the offset is an additional displacement on top of HMG's legacy
inventory centering and orientation. It is applied independently of
`InventoryScale` and does not affect equipped, dropped, placed, or attachment
mounting transforms. Both gun and attachment values are read again by
`/reloadsettingsnomodel`; the already-registered item and renderer are retained,
and no OBJ cache invalidation is required. Removing either attachment setting
restores its default (`1.0` scale and `0,0,0` offset) on the next reload.

Assignment syntax is also accepted for these values:

```text
InventoryScale = 0.75
InventoryOffset = -0.4, 0.15, 0
```

A gun TXT file enables the corresponding shared, gun-local anchor with either:

```text
attachmentlocation,0.0,1.25,-0.5
attachmentlocation,0.0,1.25,-0.5,90.0
attachmentlocation = 0.0, 1.25, -0.5, 90.0
```

Numbered locations are also supported with comma or assignment syntax:

```text
attachmentlocation1,0,0.20,0
attachmentlocation2 = 0.35, 0.05, 0, 90
attachmentlocation3,0,0,-1.75
attachmentlocation4,0,-0.45,0.30
attachmentlocation5,-0.30,0.15,0
```

Coordinates use the existing gun-parts coordinate system and scale. The optional rotation is in degrees around the attachment renderer's normal local Y axis. Each suffix maps directly to attachment inventory slot `1` through `5`; a numbered location affects only its matching slot. Rendering prefers that location, then singular `attachmentlocation`, and otherwise adds no location transform. Malformed/non-finite values are logged and ignored.

By default, `attachmentlocation` is relative to the gun model's root. A gun may
instead make it local to an animated model part by placing `SetAttachmentAttach`
on the current `AddParts` or `AddChildParts` entry:

```text
AddParts,Sys_Zero_in
    SetAsNormalParts
    SetAttachmentAttach
    AddPartsRotationCenterAndRotationAmount,0,1.6798,5.8886,0,0,0
    AddSomeMotion,0,0,0,0,0,0,0,0,0,0,0,0,0,0
```

The installed 3D attachment then inherits that part's complete active transform,
including all parent-part and motion transforms, before `attachmentlocation` is
applied as a local translation and optional Y rotation. Use only one
`SetAttachmentAttach` per gun. If a pack specifies more than one, HMG logs a
warning, uses the first valid part in file order, and ignores the later markers.
Numbered anchors `SetAttachmentAttach1` through `SetAttachmentAttach5` mark independent animated anchors for inventory slots `1` through `5`. The numbered anchor is preferred for its slot; singular `SetAttachmentAttach` remains the fallback for slots without a numbered anchor. A slot may be marked on multiple parts, such as mutually exclusive magazine-model branches; whichever marked part renders supplies its complete live matrix, while per-render slot deduplication prevents the attachment from rendering twice if multiple marked parts are active. The attachment is omitted when every marked part's `renderOnOff` state disables it. Legacy gun parts and attachment items do not use these directives.

Gun parts and child parts can hide replaceable gun geometry while any kind of
attachment is installed in a matching slot:

```text
removeifattachpresent1
removeifattachpresent2
removeifattachpresent3
removeifattachpresent4
removeifattachpresent5
```

The suffix maps directly to attachment GUI/NBT inventory slot `1` through `5`.
Place a directive after its `AddParts` or `AddChildParts`; no argument is
required. The marked part and its child subtree are hidden while the matching
slot is occupied and reappear as soon as it is empty. Multiple directives on
one part use OR behavior, so any matching occupied slot hides the part. This
tests attachment presence rather than attachment type or 3D-model support, and
therefore works for both legacy and external 3D attachments. If a hidden part
also contains `SetAttachmentAttach` or its matching numbered anchor, its live
transform and attachment hook still run, allowing the installed external model
to replace the hidden geometry at that same animated anchor.

Gun TXT files can add a slot-aware first-person ADS alignment offset:

```text
opticshift1,x,y,z
opticshift2,x,y,z
opticshift3,x,y,z
opticshift4,x,y,z
opticshift5,x,y,z
```

Each suffix maps directly to attachment GUI/NBT inventory slot `1` through `5`.
While the matching slot is occupied, its values are added to the gun's existing
first-person ADS target position in the same coordinate space as the gun's
`onads_modelPosX`, `onads_modelPosY`, and `onads_modelPosZ` settings. Multiple
occupied configured slots add together. The offset follows the normal ADS blend,
does not move the HUD crosshair or bullet trajectory, and disappears immediately
when the item is removed. Values must be exactly three finite numbers.

A gun part can also move locally based on those same live attachment slots:

```text
moveifattachpresent1,x,y,z
moveifattachpresent2,x,y,z
moveifattachpresent3,x,y,z
moveifattachpresent4,x,y,z
moveifattachpresent5,x,y,z
```

Place these directives after the applicable `AddParts` or `AddChildParts` entry.
When a matching slot is occupied, the marked gun model part moves by the supplied
X, Y, and Z values in local model coordinates. Multiple matching movements add
together. The translation composes after the part's normal and animated
transforms, so its children and any `SetAttachmentAttach` or numbered attachment
anchor on that part inherit the movement. `removeifattachpresent` continues to
control geometry visibility independently and takes precedence when its matching
slot is occupied. Values must be exactly three finite numbers.

### Firearm ADS magnification channels

Gun pack `Zoom` lines define the magnification used for each sight channel:

```text
Zoom,BASE,REDDOT,SCOPE
```

ADS uses `BASE` when attachment slot 1 is empty, `REDDOT` for an
`HMGItemAttachment_reddot`, and `SCOPE` for an `HMGItemAttachment_scope`. Other
`HMGItemSightBase` optics use their own `zoomlevel`. These values select FOV
magnification independently of `ZoomRender`, `ZoomRenderType`, and
`ZoomRenderTypeTxture`, which continue to control legacy overlay rendering. A
channel value of `1.0` therefore intentionally means no magnification; invalid,
non-positive, NaN, or infinite magnification values safely resolve to `1.0`.

## Generated configuration

The active config file is generated from the `HandmadeGuns` mod id, usually as `config/HandmadeGuns.cfg`. Defaults below are read directly from `HMG/src/main/java/handmadeguns/HandmadeGunsCore.java`.

### `Gun`

| Key | Type | Default | Effect |
| --- | --- | --- | --- |
| `MAXGUNSINV` | integer | `2` | Maximum gun inventory value used by HMG inventory/item systems. |
| `cfg_MuzzleFlash` | boolean | `true` | Enables muzzle flash behavior. |
| `cfg_ADS_Sneaking` | integer | `0` | Controls ADS/sneaking interaction mode. Exact modes are legacy and should be tested with your key setup. |
| `cfg_ADS_Key_Toggle` | boolean | `true` | Makes ADS key behavior toggle-style when enabled. |
| `cfg_Swap_Fire_And_ADS_Keys` | boolean | `false` | Swaps held-gun fire and ADS mouse behavior: fire uses attack/left-click, and `ADS_Key` defaults to use-item/right-click. |
| `cfg_Sneak_ByADSKey` | boolean | `false` | Allows the ADS key to trigger sneaking behavior. |
| `cfg_Avoid_ALL_ConflictKeys` | boolean | `true` | Enables HMG's conflict-avoidance handling for key input. |
| `cfg_blockdestroy` | boolean | `true` | Allows HMG explosive/projectile block destruction when the projectile also permits it. Disable for safer servers. |
| `cfg_AvoidHit` | string | empty | Avoid-hit entity class/string filter used by hit logic. |
| `cfg_ThreadHitCheck` | boolean | `true` | Enables threaded hit-check behavior. |
| `cfg_ThreadHitCheck_split_length` | integer | `10` | Segment length used by threaded hit checks. |
| `cfg_KnockBack` | double | `0.05` | Default horizontal knockback coefficient. |
| `cfg_KnockBackY` | double | `0.01` | Default vertical knockback coefficient. |

### `Render`

| Key | Type | Default | Effect |
| --- | --- | --- | --- |
| `cfg_ZoomRender` | boolean | `true` | Enables zoom rendering behavior. |
| `cfg_FOV` | integer | `95` | FOV value used by HMG render/handling systems. Source comments indicate some older render offsets were authored around 95 FOV. |
| `cfg_RenderPlayer` | boolean | `false` | Legacy render-player option; source comments indicate it may have no current usages. |
| `cfg_useStencil` | boolean | `false` | Controls stencil rendering path. |
| `enableVBOModelRendering` | boolean | `true` | Client-side: uses OpenGL VBOs for HMG OBJ model groups when possible. HMG scopes and restores the caller's array-buffer, client-array pointer/enable, and matrix-mode state around each model draw, including under fixed-function compatibility renderers. Disable to force legacy display-list rendering. |
| `cfg_Flash` | boolean | `true` | Enables flash render effects. |

### `Cartridge`

| Key | Type | Default | Effect |
| --- | --- | --- | --- |
| `cfg_canEjectCartridge` | boolean | `true` | Enables ejected cartridge entities/effects. |
| `cfg_Cartridgetime` | integer | `200` | Cartridge lifetime/fuse value in ticks. |

### `ManualGunPickup`

| Key | Type | Default | Effect |
| --- | --- | --- | --- |
| `enableManualGunPickup` | boolean | `true` | When true, dropped HMG gun items require the pickup key or a targeted right click instead of normal walk-over pickup. |
| `manualGunPickupRange` | double | `3.0` | Maximum pickup request distance in blocks. Source clamps config UI range from `0.1` to `8.0`. |
| `manualGunPickupRequiresLineOfSight` | boolean | `true` | Server requires the player to look at the dropped gun with no block in the way. |
| `manualGunPickupOnlyGuns` | boolean | `true` | Restricts manual pickup to HMG gun items. If false, other HandmadeGuns items may also use it; non-HMG items are not affected. |
| `enableGunGroundPhysicsRender` | boolean | `false` | Client-side: renders supported dropped HMG guns with a flatter physical-looking ground orientation. |

### `GuideBook`

| Key | Type | Default | Effect |
| --- | --- | --- | --- |
| `enableHMGGuideBook` | boolean | `true` | Enables optional Guide-API HMG Field Manual registration. HMG still loads without Guide-API. |

### `Compatibility`

| Key | Type | Default | Effect |
| --- | --- | --- | --- |
| `enableCombativesRecoilIntegration` | boolean | `true` | Client-side: when Combatives is installed and its camera API is active, first-person HMG weapon recoil is submitted to Combatives as visual camera impulses. When Combatives is absent, disabled, unavailable, or rejects the base shot impulse, HMG uses its legacy look-rotation recoil fallback. |
| `enableCombativesAimRecoilIntegration` | boolean | `true` | Client-side: when a Combatives visual shot is accepted, HMG also applies actual local pitch/yaw aim recoil with delayed contribution-based recovery. |
| `combativesAimRecoilVerticalScale` | double | `0.55` | Scales real vertical aim displacement derived from HMG weapon recoil stats. |
| `combativesAimRecoilHorizontalScale` | double | `0.45` | Scales real horizontal aim displacement derived from the deterministic burst drift. |
| `combativesAimRecoilRecoveryDelayMs` | integer | `120` | Delay after the most recent accepted shot before controller-owned aim recoil begins recovering. |
| `combativesAimRecoilRecoverySpeed` | double | `3.0` | Recovery speed for controller-owned aim recoil; higher values recover faster. |
| `combativesAimRecoilMaxPitch` | double | `14.0` | Maximum controller-owned vertical aim recoil, in degrees. |
| `combativesAimRecoilMaxYaw` | double | `5.0` | Maximum controller-owned horizontal aim recoil, in degrees. |
| `enableCombativesRecoilDebug` | boolean | `false` | Client-side: enables verbose HMG-to-Combatives recoil diagnostics, including submitted visual impulse channels, timings, stacking mode, acceptance, fallback, weapon-state reset logs, aim-recoil pending/applied/recovered state, detected mouse deltas, and ownership. Leave disabled during normal gameplay. |

### `Logging`

| Key | Type | Default | Effect |
| --- | --- | --- | --- |
| `enableDebugLogging` | boolean | `false` | Enables verbose HMG startup and content-pack diagnostics, including per-pack resource confirmations, registration timing summaries, script confirmations, per-file gun parse timings, and recipe success messages. Errors, warnings, and concise content-pack registration-complete summaries still log when this is disabled. |

### `LMM`

| Key | Type | Default | Effect |
| --- | --- | --- | --- |
| `cfg_FriendFireLMM` | boolean | `true` | LittleMaidMobX-related friendly-fire handling. |
| `cfg_FriendFirePlayerToLMM` | boolean | `true` | Allows player-to-LMM friendly-fire behavior where LMM compatibility code applies. |
| `cfg_RenderGunSizeLMM` | boolean | `false` | LMM gun-size render compatibility option. |
| `cfg_RenderGunAttachmentLMM` | boolean | `false` | LMM attachment-render compatibility option. |

## World Generation Notes

HMG registers a world generator for the overworld. It generates copper and aluminum ores only when external ore-dictionary entries are not detected:

- Copper: 12 veins/chunk, vein size 9, Y 20-63.
- Aluminum: 9 veins/chunk, vein size 7, Y 32-95.

These ore-generation toggles are computed automatically from ore dictionary checks; no active config keys for ore generation were found in the inspected HMG code.
