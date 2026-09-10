# Handmade Guns Content Pack Guide

HMG is pack-driven. Content packs can add guns, magazines, bullets, attachments, recipes, sounds, textures, models, tabs, and scripts without changing Java code.

Optional named JSON animations can drive the existing HMG gun parts without replacing OBJ/MQO models. See [Animation authoring](animation-authoring.md) for `Animations,animations/name.json`, units, transitions, events, the example asset, and legacy compatibility.

## Supported Pack Roots

Preferred path:

```text
handmadeguns_Packs/<PackName>/
```

Legacy path still read by source:

```text
mods/handmadeguns/addgun/<PackName>/
```

## Common Pack Layout

```text
handmadeguns_Packs/
  ExamplePack/
    guns/
    magazines/
    bullets/
    attachment/
    addpackrecipe/
    addTab/
    addmodel/
    addtexture/
    addsighttex/
    addsounds/
    addscripts/
    scripts/
    additionalSettings.txt
```

## Loader Behavior

- Pack folders are sorted by name before loading.
- Files inside major definition folders are sorted by name where the source explicitly sorts them.
- Resource folders are copied into generated `assets/handmadeguns` paths under the pack root and registered as resource containers on the client.
- Client resource reload is triggered after pack resources are scanned.
- `additionalSettings.txt` is read with Shift-JIS encoding.

## Definition Folders

| Folder/File | Purpose |
| --- | --- |
| `guns/` | Gun definition files parsed by `HMGGunMaker`. |
| `magazines/` | Magazine definitions parsed by `HMGAddmagazine`. |
| `bullets/` | Bullet/projectile definitions parsed by `HMGAddBullets`. |
| `attachment/` | Attachment definitions parsed by `HMGAddAttachment`. |
| `addpackrecipe/` | Recipes parsed directly into the canonical Gun Smithing Table registry (and exposed to NEI without a vanilla crafting copy). |
| `addTab/` | Creative-tab definitions. |
| `addmodel/` | Model resources copied to `textures/model`. |
| `addtexture/` | Item texture resources copied to `textures/items`. |
| `addsighttex/` | Sight/overlay textures copied to `textures/misc`. |
| `addsounds/` | Sound files copied to `sounds` and processed by the HMG sound loader. |
| `addscripts/` | JavaScript files copied to the legacy HMG scripts resource path and evaluated during pre-init. |
| `scripts/` | JavaScript files evaluated during pre-init. |
| `additionalSettings.txt` | Optional pack-level multipliers such as `damageCof` and `speedCof`. |

## Pack Recipe Ore Dictionary Inputs

Gun Smithing Table recipes loaded from `addpackrecipe/` automatically use Forge's
Ore Dictionary when an ordinary referenced item is registered there. For example,
`Slot1,HandmadeGuns:steel_ingot` accepts every `ingotSteel` alternative when that
item is registered as `ingotSteel`; packs do not need to opt in with a prefix.

An explicit Ore Dictionary key can still override automatic selection in a `SlotN`
line:

```text
AddRecipe
Slot1,ore:ingotSteel
Slot2,oredict:ingotCopper
Slot3,OreDictionary:ingotAnyPlastic
Slot4,ore:ingotCopper:5
CraftItem,HandmadeGuns:ExampleGun:0:1
```

Supported prefixes are `ore:`, `oredict:`, and `OreDictionary:`. Prefix matching is case-insensitive, and the ore dictionary key after the prefix is preserved. A final numeric suffix may be used for the required amount, as in `ore:ingotCopper:5`; otherwise the slot requires one matching item.

If Forge registers an item under multiple keys, automatic conversion deterministically
uses the first ID returned by Forge, matching MCHO behavior. To require the literal
item instead, use `exact:modid:item[:metadata[:count]]`. Exact inputs retain metadata
and NBT matching; metadata `32767` has Forge wildcard semantics. Tagged programmatic
recipe inputs also stay exact so normalization cannot discard an NBT requirement.

Ore dictionary requirements retain the ore key instead of resolving permanently to the first registered stack. The Gun Smithing Table resolves display stacks, availability checks, server validation, and consumption against the live ore dictionary so compatible items registered by other mods can satisfy recipes even when normal pre-init recipe registration cannot be represented as an ore recipe.

When NEI is installed, these recipes appear in a dedicated **Gun Smithing Table**
category. Duplicate-output recipes remain separate, and ore dictionary inputs cycle
through every currently registered alternative. The bundled NEI 1.0.5.120 API does
not expose the later recipe-catalyst registration API.

The table GUI, server-side crafting transaction, and NEI handler all query that same
registry. Gun and ammunition recipes are categorized explicitly; the table does not
discover ammunition by scanning or guessing entries in Minecraft's crafting list.

Legacy gun- and attachment-file `Recipe1` through `Recipe3`, `ItemA` through `ItemI`,
and `addNewRecipe` entries remain vanilla shaped recipes and are also added to this same
registry. Shape characters retain their literal row and column positions (including
repeats and space-filled empty slots), so `aaa` / `cbc` / `   ` becomes
`A,A,A,C,B,C,empty,empty,empty`. Missing item/block definitions or non-space shape
symbols without a resolved `ItemA`-`ItemI` value are logged and rejected instead of
creating a malformed recipe.

## `additionalSettings.txt`

Recognized keys found in source:

```text
damageCof,1.0
speedCof,1.0
```

The file is read per pack. The current source switch lacks `break` statements between these two cases, so pack authors should test carefully when setting either multiplier.

## Pack Author Tips

- Keep file names deterministic and stable.
- Clearly document which magazines and bullets each gun accepts.
- Ship client assets with the pack and test on a clean client.
- Test `/reloadSettings` during development, but do full restarts before release validation.
- Avoid relying on undocumented parser behavior; HMG definition parsers are legacy and forgiving in some places but not uniformly validated.

### Attachment whitelist names

Use an attachment's stable registered identifier in a gun's `allowattach` directive,
not its player-facing name. For example, an attachment declared with
`Name,Attachment Test` and `RedDot,attachmenttest` is allowed with
`allowattach,attachmenttest`. Gun tooltips resolve that identifier to the attachment's
localized display name, so players see **Attachment Test** under **Valid attachments**.
If an identifier cannot be resolved, the tooltip retains the configured value to make
an invalid or missing pack dependency visible.

## Gun Motion Handling and Mouse Sensitivity

The existing `Motion,<number>` gun setting controls weapon handling. It already
scales held-gun movement speed and jump behavior, and now also scales mouse
sensitivity while the local player holds a unified gun on foot in normal gameplay.
`Motion,1.0` means normal handling; lower values mean progressively heavier handling.
No new setting or pack migration is required.

For finite Motion values below 1.0, the handling multiplier is
`max(0.25, 1 - 0.75 * (1 - Motion))`, after clamping Motion to at least zero.
Motion values at or above 1.0, and invalid non-finite values, use exactly `1.0`.
For examples from the bundled packs, ordinary `Motion,0.95` rifles use `0.9625`,
the `Motion,0.48` Lynx uses `0.61`, and the `Motion,0.35` M82A3 Barrett uses
`0.5125`. This multiplier composes once with ADS zoom sensitivity; it does not
affect menus, placed guns, non-HMG items, or vehicle cameras.

## Data-driven gun skins

Gun skins are normal pack items and may be declared in an `attachment/*.txt` file. The
item icon still uses the ordinary `Texture` key and belongs in
`assets/handmadeguns/textures/items/`. A minimal definition is:

```text
Texture,my_skin_item
Name,My Gun Skin
GunSkin,true
SkinTexture,skins/my_gun_overlay.png
GunSkinItem,my_skin
```

`GunSkinItem` is the stable registered item/skin identifier; it is also what is stored
in the gun's `GunSkin` NBT value. Every skin is universal: `SkinTarget`, gun names,
and Forge registry names are neither required nor consulted. The deprecated
`SkinTarget` key is accepted only so older definitions continue loading, and its value
is ignored. The overlay resolves through the pack resource domain. With the path above it must be at
`assets/handmadeguns/textures/model/skins/my_gun_overlay.png`; an explicit resource
such as `othermod:textures/model/skin.png` is also supported.

The overlay is separate from the inventory icon. Its UV content is the pack author's
responsibility; HMG does not reject overlays based on a gun's model, UV layout, or
texture dimensions. Fully transparent pixels leave the base gun visible. Craft any
unified HMG gun with one skin item, in either order in a 2x2 or 3x3 grid, to copy the
gun, preserve all of its state, and set or replace its selected skin. Missing or
removed skin identifiers simply leave the base gun unskinned.

Skin items show this crafting instruction and their universal unified-gun
compatibility in the inventory tooltip. Invalid skin definitions are reported in the
tooltip instead of causing an error.

HMG registers one dynamic recipe for the entire system. It identifies firearms by the
shared `HMGItem_Unified_Guns` class, rather than item identity or stack state, and
rejects extra ingredients. Damage, ammunition, attachments, custom names,
enchantments, the previous skin, and all other per-stack NBT have no effect on whether
the recipe matches; the copied output retains them and changes only `GunSkin`.

To remove an applied skin, craft the skinned gun with either the skin item whose stable
`GunSkinItem` identifier is stored on the gun or one dye. Vanilla dye items and modded
dyes registered under a Forge Ore Dictionary name beginning with `dye` are accepted.
The removal recipe copies the gun and removes only its root `GunSkin` string, preserving
ammunition, attachments, damage, custom names, fire mode, cocking state, and all other
stack NBT. A different skin still follows the normal application recipe rather than
removing the current skin.

Definitions load on both client and server because crafting needs their IDs. Overlay
existence is checked only by the client renderer and is not stored in
shared skin metadata; an unavailable resource safely leaves the base gun unskinned.
