# Handmade Guns Content Pack Guide

HMG is pack-driven. Content packs can add guns, magazines, bullets, attachments, recipes, sounds, textures, models, tabs, and scripts without changing Java code.

Optional named JSON animations can drive the existing HMG gun parts without replacing OBJ/MQO models. See [Animation authoring](animation-authoring.md) for `Animations,name.json`, units, transitions, events, the example asset, and legacy compatibility.

Unified guns can instead use `BlockbenchModel,name.bbmodel` to import geometry, bone parts, embedded PNG textures and animations directly from a Blockbench project. No OBJ export, `AddParts` declarations or separate animation JSON is needed. The checked-in `GVCguns/guns/AKM_Blockbench.txt` and `GVCguns/models/cod4_ak.bbmodel` provide a complete AK example. See [native Blockbench importing](animation-authoring.md#native-blockbench-projects) for the supported formats, TaCZ action aliases and limitations.

Unchanged exported TaCZ cube geometry can use `BedrockModel,name_geo.json` with a required `ModelTexture` and one or more ordered `Animations` files. The first animation file is normally weapon-local; later shared rifle/pistol defaults only fill missing clips. This feeds the same HMG part, positioning, hand, and animation runtime as a native Blockbench project. See [native exported Bedrock geometry](animation-authoring.md#native-exported-bedrock-geometry).

## Quick Start

Create an immediate child directory beneath `handmadeguns_Packs/`, add at least a `guns/` directory, and keep every referenced asset inside that same pack:

```text
handmadeguns_Packs/MyPack/
  guns/my_rifle.txt
  models/my_rifle.bbmodel
```

Inside the normal gun TXT definition, select the native project before the final `Unified_guns,...` line:

```text
BlockbenchModel,my_rifle.bbmodel
```

The `.bbmodel` file supplies presentation geometry and animations. The TXT definition remains the authority for weapon gameplay, ammunition, timing, attachments, and balance.

## Pack Sources and Precedence

| Order | Source | Purpose |
| ---: | --- | --- |
| 1 | Bundled `hmg_packs/` resources materialized to `handmadeguns_builtin/` | Read-only official baseline shipped inside the JAR |
| 2 | `handmadeguns_Packs/<PackName>/` | Maintained external pack and override location |
| 3 | `mods/handmadeguns/addgun/<PackName>/` | Legacy compatibility location |

Later compatible definitions and resources can replace earlier ones. Incompatible identifier collisions are rejected rather than silently replacing a different item type.

HMG Overdrive's maintained packs are included in the main mod JAR. At startup they
are exposed through a private `handmadeguns_builtin/` cache solely because the
legacy parsers consume files. Unchanged generated files are reused; files changed by
an HMG update are staged to a temporary sibling and then replaced. The cache is not
an addon location. Installing the HMG JAR alone therefore includes the official guns,
definitions, models, animations, textures, sounds, tabs, scripts, and recipes.

Filesystem packs still load after the bundled baseline. A matching gun, compatible
magazine/attachment, bullet identifier, or resource path in `handmadeguns_Packs/`
intentionally overrides the bundled version; otherwise it adds content normally.
The existing filesystem reload commands continue to reload those external packs and
do not treat the bundled cache as editable content.

Preferred external path:

```text
handmadeguns_Packs/<PackName>/
```

Legacy path still read by source:

```text
mods/handmadeguns/addgun/<PackName>/
```

## Recommended Pack Layout

```text
handmadeguns_Packs/
  ExamplePack/
    guns/
      AKM.txt
    models/
      akm.mqo
      cod4_ak.bbmodel
      ak47_geo.json
    textures/
      models/akm.png
      items/akm_icon.png
      misc/scope.png
    animations/
      akm.json
    sounds/
      ak47.ogg
      reload.ogg
    attachments/
    magazines/
    bullets/
    addpackrecipe/
    addTab/
    addscripts/
    scripts/
    additionalSettings.txt
```

The asset resolver is always bound to the pack containing the definition. A simple name is looked up in its logical folder first, so a gun can use:

```text
Model,akm.mqo
ModelTexture,akm.png
Texture,akm_icon.png
ScopeTexture,null.png,null.png,scope.png
Animations,akm.json
```

`Model` is the concise OBJ/MQO directive and enables the custom model automatically. `ModelTexture` and the legacy-compatible `ObjTexture` select a texture from `textures/models/`; `Texture` selects an inventory/hotbar icon from `textures/items/`; and `ScopeTexture` selects an overlay from `textures/misc/`. These categories are intentionally separate: a model texture is never used as an item icon. Blockbench projects use `BlockbenchModel,cod4_ak.bbmodel`; their embedded texture and animations continue to take precedence. Exported cube geometry uses `BedrockModel,ak47_geo.json` plus `ModelTexture,ak47.png`. `Animations,ak47.animation.json,rifle_default.animation.json` merges sources left-to-right without replacing an earlier clip.

Authors may include the logical directory explicitly (`Model,models/akm.mqo`, `ModelTexture,textures/models/akm.png`, `Texture,textures/items/akm_icon.png`, or `Animations,animations/akm.json`). Short references are preferred because the directive determines the category. Absolute paths, `..` traversal, and canonical or symbolic-link escapes are rejected. HMG does not recursively search the Minecraft instance, resource packs, Flan packs, or neighboring HMG packs.

Item icons can also use the concise `Texture,akm_icon.png`; sight overlays can use `ScopeTexture,scope.png,...`. The checked-in packs use these clean categories so their references do not depend on legacy fallback lookup.

Resolution is deterministic: the matching logical category wins, followed by the known legacy folders for that asset type. Clean `models/`, `textures/models/`, `textures/items/`, `textures/misc/`, and `sounds/` files are mirrored into the pack's registered `assets/handmadeguns` resource tree during startup; this is an internal compatibility detail rather than an authoring requirement.

## Legacy Layout

Third-party or older packs may continue using `attachment/`, `addmodel/`, `addtexture/`, `addsighttex/`, `addsounds/`, and Minecraft-style paths under `assets/handmadeguns/`. Model lookup recognizes both `assets/handmadeguns/textures/model/` and the historical plural `assets/handmadeguns/textures/models/`; textures and sounds retain their known legacy locations. These layouts are supported for compatibility but deprecated for new pack authoring. The repository-owned packs have been migrated: authored models, textures, animations, sounds, and attachment definitions are under their clean logical folders. `assets/handmadeguns/sounds.json` remains only where the Minecraft resource manager requires that resource-domain file; it is generated/consumed from the clean `sounds/` OGG files and is not a pack-authoring location.

## Loader Behavior

- Pack ownership is limited to immediate directories under the two HMG roots above. Startup resources, definitions, recipes, scripts and settings reloads reject packs outside those roots, including redirected pack paths. `Flan/` is not an HMG root; model or animation files do not identify a directory as an HMG pack. Direct gun loading also requires an owned `guns/` directory. Model, texture, Blockbench, external texture, and animation references must resolve from that active pack even though Minecraft still exposes staged legacy files through HMG's resource domain.
- Pack folders are sorted by name before loading.
- Files inside major definition folders are sorted by name where the source explicitly sorts them.
- Clean and legacy resource folders are mirrored into generated `assets/handmadeguns` paths under the same pack root and registered as resource containers on the client.
- Client resource reload is triggered after pack resources are scanned.
- `additionalSettings.txt` is read with Shift-JIS encoding.

## Definition Folders

| Folder/File | Purpose |
| --- | --- |
| `guns/` | Gun definition files parsed by `HMGGunMaker`. |
| `magazines/` | Magazine definitions parsed by `HMGAddmagazine`. |
| `bullets/` | Bullet/projectile definitions parsed by `HMGAddBullets`. |
| `attachments/` | Recommended attachment definitions parsed by `HMGAddAttachment`; legacy `attachment/` remains a fallback. |
| `addpackrecipe/` | Recipes parsed directly into the canonical Gun Smithing Table registry (and exposed to NEI without a vanilla crafting copy). |
| `addTab/` | Creative-tab definitions. |
| `models/` | Recommended OBJ, MQO, Blockbench project, and exported Bedrock geometry location. |
| `textures/models/` | PNG and source-texture location for OBJ, MQO, Blockbench, Bedrock geometry, and skin rendering. |
| `textures/items/` | PNG location for inventory and hotbar item icons. |
| `textures/misc/` | PNG location for scopes, reticles, overlays, and other non-model textures. |
| `animations/` | Optional external animation JSON files. |
| `sounds/` | Recommended OGG location, processed by the existing HMG sound loader. |
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

A separately registered presentation variant can inherit every Gun Smithing Table
recipe from its gameplay counterpart without repeating the ingredient grid:

```text
CopyRecipe,HandmadeGuns:PresentationVariant,HandmadeGuns:LegacyGun
```

`CopyRecipe` is resolved after all bundled, external, and legacy recipe roots have
loaded. It copies every recipe for the source output, including its category and
normalized exact/Ore Dictionary ingredients. The target and source items must both
be registered, and a target that already has a recipe is not duplicated.

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

Gun skins are normal pack items and should be declared in an `attachments/*.txt` file
(legacy `attachment/*.txt` is still accepted). The item icon uses the ordinary
`Texture` key; new packs place that PNG in `textures/items/`. A minimal definition is:

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
is ignored. The overlay resolves through the active pack. With the path above it may be
stored as `textures/models/skins/my_gun_overlay.png`; the legacy
`assets/handmadeguns/textures/model/skins/my_gun_overlay.png` path remains supported.
Explicit resource locations such as `othermod:textures/model/skin.png` remain a legacy
compatibility feature.

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
## Technology metadata

Gun definitions may declare `TechYear,<year>`. HMG resolves that year through the server's configurable tier year ceilings. Use `TechTier,<0.0..5.0>` only as an explicit half-step override for fictional, prototype, or otherwise unclassifiable equipment. If both are present, the year is still displayed but `TechTier` controls access.

Every loadable gun definition bundled with HMG Overdrive has an explicit year. This includes legacy
OBJ/MQO guns, imported TaCZ/Bedrock guns, the Blockbench example gun, grenades and launchers created
through the gun loader, and vehicle-weapon definitions. Files that only define melee items,
attachments, recipes, random-kit items, or authoring templates are not gun definitions and are not
part of technology progression. Third-party packs may still omit both keys to retain unrestricted
legacy behavior.

Neither field is mandatory. Definitions without both fields retain pre-progression behavior and are unrestricted. Malformed values are ignored instead of aborting pack loading.
