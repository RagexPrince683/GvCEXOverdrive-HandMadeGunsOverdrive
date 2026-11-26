# GVC Overdrive

GVC Overdrive adds infantry, rival factions, camps, supply and raid beacons, and player-commanded PMC squads. Handmade Guns Overdrive supplies the weapons, magazines, and projectile mechanics. GVC's equipment, infantry, and camp systems are packaged together.

## Getting started

Use Minecraft **1.7.10**, Forge **10.13.4.1614**, and **Java 8**. Install matching `HandmadeGunsOverdrive-*.jar` and `GVC-HD*.jar` files in `mods/` on both the client and server. GVC provides the `GVCGuns` and `GVCMob` mod IDs. A normal installation needs those two JARs. Install the third JAR, `GVCDangeonsBase-*.jar`, if you use compatible dungeon packs. See the [project installation page](../../README.md#install) and [build instructions](../building.md).

On first launch, look for `config/GVCMob.cfg`, `config/GVCGuns.cfg`, and `config/HandmadeGuns.cfg`. The GVC Infantry and GVC Equipment creative tabs expose the relevant eggs, tools, and equipment. HMG owns the normal firearm tabs.

## Factions and encounters

- **ALLIES Radical Party** guerrillas fight players and allied troops. Units include riflemen, marksmen, machine gunners, grenadiers, shotgun infantry, and specialized variants.
- **UNION** soldiers are friendly to players and fight guerrillas. The UNION flag can draw reinforcement and defense activity.
- **PMC** infantry are friendly to players and can be issued squad orders using the Squad Command Device.
- Player respawn beacons can rescue a player from a lethal hit within range. A raid supply beacon periodically attempts reinforcement waves around its area. Which beacons are friendly is shown in the HUD.
- Camp loot, gun boxes, and infantry loadouts use HMG items. GVC also supplies health packs, portable equipment, practice targets, and stationary sentries.

Natural spawns depend on difficulty, biome, and GVC configuration. Use a difficulty above Peaceful for regular hostile encounters. Spawners, raids, and natural spawning have different entry points.

## Squads and orders

Hold the **Squad Command Device**. Sneak and right-click to cycle orders. Right-click normally to issue the selected order to a matching PMC group within range. For a position order, aim at a block marking the destination. You can rename the device and use it on a PMC to assign a squad name.

| Order | Behavior |
| --- | --- |
| Move | Walk toward the marked area and assemble around the leader. |
| Follow | Follow the issuing player and regroup after combat. If the player leaves, the squad waits and can resume following on return. |
| Patrol | Travel between the leader's position at the time of the order and the marked position. |
| Defend | Hold around the marked area and engage nearby threats. |
| Attack | Advance toward the marked area and engage targets the infantry can detect. |

A platoon has up to eight members. Its leader sets the route and other members follow with spacing. Infantry can engage independently and return to formation after losing a target. A surviving member takes leadership when the leader dies. Camp-oriented factions can acquire attack or defense orders from nearby flags.

New saves store squad membership, leadership, and orders per dimension. Older saves that only stored an order position retain that destination. Those records lack group IDs, so reconstructed units can start in separate platoons.

## Combat and HMG integration

Infantry use HMG unified firearms and the weapon's selector, bolt, magazine, reload sequence, and projectile model. GVC chooses the engagement distance and firing cadence for general infantry, machine gunners, marksmen, and short-range units. Clear sight and a clear firing lane are required before triggering a shot. A remembered position can guide a short search after sight is lost. HMG's own spread and projectile behavior still apply.

Pack authors can provide NPC-capable weapons through the HMG registration categories and NPC eligibility in the gun definition. Loadouts prefer the requested gun or role pool, with a usable HMG fallback if available. HMG supplies NPC reload ammunition according to its entity ammunition rules. Supported NPC third-person renderers include legacy HMG OBJ/MQO and imported Blockbench/TaCZ models. Imported models can specify `thirdperson_hand`. Player presentation uses HMG's existing player render paths.

See [HMG content packs](../content-packs.md), [animation and model authoring](../animation-authoring.md), and the [legacy firearm registry report](legacy-firearms.md).

## Configuration

The keys below use the names generated in `GVCMob.cfg`. On a dedicated server, edit the server's file and restart to apply changes.

| Category | Key | Default | Effect |
| --- | --- | --- | --- |
| `world` | `cfg_setCamp` | `true` | Registers GVC's world-generation hook. |
| `world` | `cfg_creatCamp` | `160` | Controls the camp placement roll. Larger positive values make attempts rarer. Zero disables the roll. |
| `world` | `cfg_mob_setCamp` | `true` | Allows the specialized guerrilla camp builder to establish a camp. |
| `world` | `cfg_BlockDestory` | `true` | Allows infantry block-building or breaking behavior where used. Gun terrain damage is configured in HMG. |
| `Guerrilla` | `cfg_GuerrillaSpawnNomal` | `5` | Base weight for registered natural guerrilla spawns. |
| `Guerrilla` | `cfg_noGuerrillaSpawnAsInfantry` | `false` | Suppresses registration of ordinary natural guerrilla infantry spawns. |
| `Guerrilla` | `cfg_GuerrillaMobSrach` | `60` | Base target-detection attribute for guerrillas and derived values for some friendly classes. |
| `Guerrilla` | `cfg_CanusePlacedGun` | `true` | Allows supported infantry to use HMG placed guns. |
| `Guerrilla` | `cfg_BeaconSpawnLevel` | `16` | Sets the default beacon reinforcement strength. |
| `Guerrilla` | `cfg_Flagspawninterval` | `2000` | Sets the default beacon reinforcement interval in ticks. |
| `Soldier` | `cfg_SoldierSpawnNormal` | `2.0` | Base weight for natural UNION spawns. |
| `Soldier` | `ignoreSoldierTargetEntityClass` | empty | Comma-separated entity registry names omitted by friendly infantry target selection. |
| `Infantry AI` | `reactionTicks` | `14` | Minimum visible reaction time before infantry fire, with a small random delay. |
| `Infantry AI` | `aimErrorMultiplier` | `1.0` | Scales NPC aim variation independently of weapon spread. |
| `Infantry AI` | `acquisitionRangeMultiplier` | `0.75` | Scales the range for new infantry target searches. |

Spawn weights, beacon reinforcement strength, and encounter frequency control how much infantry activity a server carries. Route searches and target acquisition are staggered. Infantry navigation keeps usable paths and waits for the necessary chunks to be loaded before computing a new route.

## World generation and compatibility

GVC's native camps use its own world generator. Existing camps, flag blocks, equipment IDs, and vanilla block data remain readable. Generate new chunks to see ordinary generation changes. The optional [Dungeon Generator Base](dungeon-generator.md) adds separate structure-pack generation and should remain installed in worlds with saved dungeon components.

MCHeli is optional for current infantry play. Vehicle operation and spawning, the full MCHeli adapter, and the unfinished factory and large-trench generators remain deferred. The mod retains legacy vehicle-related item IDs for saved inventories. Older vehicle-configured spawners remain dormant. See [compatibility notes](../compatibility.md#gvc-infantry-and-structure-modules).

## Troubleshooting

- **The server rejects the client or content is missing.** Compare the JARs and external gameplay packs on both sides. Check that Forge lists `HandmadeGuns`, `GVCGuns`, and `GVCMob`.
- **No camps or infantry appear.** Check difficulty, natural spawning, biome, `cfg_setCamp`, camp chance, and spawn weights. Newly enabled world generation generally needs new chunks.
- **A squad stalls at a structure.** Choose an open block near a usable entrance. Keep the destination and route within loaded terrain. Squads can reform after combat or stagger through narrow passages.
- **A weapon is missing or positioned incorrectly.** Check that the HMG gun and model load without errors. For imported models, verify the authored `thirdperson_hand` marker and reproduce with the same client packs as the server.
- **A legacy GVC firearm will not fire.** Craft it by itself using its saved-item conversion recipe when the matching HMG gun is installed. See the [registry report](legacy-firearms.md).

## Development and roadmap

Build the gameplay addon with `:GVC:build`. `hmggvcmob` owns factions, infantry AI, camp state, and the squad command item. `hmggvcutil` owns GVC equipment and saves compatibility. GVC routes firearms through HMG's `HMGItem_Unified_Guns`, `IFF`, and `IMGGunner` interfaces. The client bridge uses the already posed biped arm and HMG model placement metadata. See the [shared build guide](../building.md).

Planned work includes further encounters and a dedicated MCHeli adapter. The adapter will need server-owned vehicle creation and seating, checks at spawn and support markers, and production validation with and without MCHeli.
