
# Military management

GVCOverdrive requires the matching Handmade Guns Overdrive build on both client and server. The military systems run on Forge 1.7.10 with Java 8. Optional content packs and mods can contribute firearms, food, or rewards through existing item registries and the GVCO API.

## Recruiting and commanding

Open your inventory and choose **Military** in the upper right, or run /military. **Overview** shows your army’s readiness, upkeep per Minecraft day, and estimated reserves. **Personnel** lets you scroll, filter by status, group identical units, and select a soldier for their assignments and equipment.

In **Treasury**, shift-click coins into the deposit slot or choose **Deposit all coins** to deposit coins from your inventory at once. Enter a withdrawal amount or choose a common amount, review the physical coin mix, then confirm. In **Supplies**, shift-click food into the deposit slot to preview its nutritional value and the time it will add to your reserves.

Right-click a Military Trader at a base or village outpost. Select an offer to see its equipment, tier, price, upkeep, and eligibility. Confirm the hire from the selected offer. Faction reputation controls which traders and tiers are available. Defeating hostile unowned infantry raises reputation with friendly factions that oppose them. The unit arrives with its preset equipment and follows its owner. Coin items can come from hostile infantry, raid supply crates, modpack loot, quests, or server rewards.

- /military roster [page] shows personnel UUIDs for command use. The management screen shows current or last known orders, leader, tier, dimension, deployment, and per-unit costs. It keeps unloaded personnel and a limited casualty/desertion history visible.
- /military assign <unit UUID> <leader UUID> assigns a nearby owned unit to a higher-ranked leader. The leader's preset sets the maximum number of soldiers under that leader. The field commander can take orders for all nearby owned squads and can have lower-ranking officers as members.
- /military order <follow|hold|move|patrol|defend|attack|garrison> [unit UUID|all] commands the selected unit's squad. With the UUID omitted, the command uses the nearest eligible squad. Aim at a block for a position order. Garrison targets a friendly local post. A nearby field commander is required for all.
- The Squad Command Device still cycles and issues its existing movement orders. Recruitment grants a device when there is room and the player does not already have one. Existing named squads and their saved orders remain usable.
- /military relation shows personal reputation with UNION, ALLIES, and PMC.
- Operators can run /militaryadmin balance <player|UUID> [signed delta], /militaryadmin relation <player|UUID> <union|allies|pmc> <-100..100>, and /militaryadmin occupy <union|allies|pmc> [owner]. The latter transfers the nearest eligible post in range as a player operator.

## Settlements and respawns

A beacon anchors a permanent post record. The record keeps its faction, player owner if claimed, boundary, trader, reinforcement schedule, and garrison and patrol squad IDs. Native camps and compatible structure packs use the same beacon path. Raid supply beacons retain their separate raid and resupply purpose. A settlement works while its beacon chunk is loaded. Reinforcement waves need a nearby player and use saved squad membership when applying their caps. Attacking a post reduces its control when attackers outnumber defenders. The leading side takes over the same post and replaces its security forces over successive waves.

A village can generate a fortified outpost as a native village piece. The original design uses a cobblestone and gravel court, a broad entrance, a small lookout, and covered barracks. The references informed the courtyard, guarded entrance, and separate sleeping space. The structure uses vanilla rotation, collision checks, biome palettes and ground alignment, with shallow supports below its footprint. [Housesteads fort plans](https://www.english-heritage.org.uk/siteassets/home/visit/places-to-visit/housesteads-roman-fort/history/housesteads-roman-fort-phased-plans.pdf) · [Fort Scott parade ground](https://www.nps.gov/places/000/fs-parade-ground.htm) · [Fort Scott infantry barracks](https://www.nps.gov/places/000/infantry-barracks.htm)

Villagers within an occupied village can refuse trade based on each player's reputation. An open trading screen closes if the player becomes hostile. Military offers are also checked again at purchase. Normal civilian trade resumes for players permitted by the new faction after a capture.

| Mode | Respawn behavior |
| --- | --- |
| VANILLA | Beds and world spawn use vanilla behavior. |
| HYBRID | Sneak-right-click a friendly beacon to bind it. A valid bound beacon takes precedence, with vanilla respawn as fallback. |
| BEACON_ONLY | Sleeping remains available by default. Death uses a bound friendly beacon, then a nearby eligible loaded beacon in the death dimension, then overworld spawn. A bed cannot set the effective respawn. |

A bound beacon must remain friendly, secure, and have a safe standing position nearby. The settings apply to the logical server in singleplayer and multiplayer.

## Currency and food

Currency is stored per player UUID as an integer, with a maximum balance of 9 quadrillion. The four coin items have stable registry names GVCMob:MilitaryCoin0 through GVCMob:MilitaryCoin3. Their default values are 1, 10, 100 and 1,000. Server deposits and withdrawals validate the amount, the balance and inventory space before committing. Withdrawals use the fewest possible coins for the configured denominations. The default mixes are six 100-value coins for 600 and 1, 6, 3, and 7 coins of the descending denominations for 1,637. A withdrawal that cannot fit in the inventory leaves the balance intact.

Food is converted to integer supply points:

    round(hunger × hungerMultiplier + (2 × hunger × saturationModifier) × saturationMultiplier)

The default multipliers are 2 and 1. These values measure potential restoration before Minecraft's per-player hunger and saturation caps. Values are read for the actual ItemStack so metadata-sensitive foods work. Generic Forge container items and vanilla soups return their empty containers. If the return will not fit, the deposit is rejected. Explicit overrides can assign a fixed value to an item or item variant. Blacklists take precedence. Farmer's Delight Legacy's Forge 1.7.10 ItemFood entries use this same path.

Every hired unit has separate wage and food costs. The server charges from the owner's account on elapsed world ticks and warns when payment or supply fails. Units can be charged while unloaded, and charging while the owner is offline is configurable. After the grace period, shortages can affect movement, reaction, accuracy and reload speed. Desertion is optional. HMG NPCs have unlimited reserve ammunition and still go through normal magazine, firing and reload mechanics.

## Server configuration

The following categories are written into the ordinary GVCMob Forge config. Preset and weapon definitions appear in config/gvc-military/ after initial startup.

| Category | Option | Default |
| --- | --- | --- |
| Military.Recruitment | enabled | true |
| | legacyRecipes, legacyHiring | false, inherits legacyRecipes when first generated |
| | invulnerableTraders, traderReplacementTicks | true, 12000 (0 disables replacement) |
| | playerPersonnelLimit | 64 |
| | reputationPerHostileKill | 1 |
| Military.Occupation | enabled, reinforceWhileContested | true, false |
| | reinforcementInterval, reinforcementDistance | legacy flag interval (2000 ticks for new configs), 96 blocks |
| | garrisonSize, patrolSize | legacy beacon strength divided by 2 and 4 respectively (8 and 4 for new configs) |
| | garrisonReplacementRate, patrolReplacementRate | 2 and 1 per wave |
| | territoryRadius, patrolRadius | 48 and 24 blocks |
| | captureStrength | 1000 |
| | factionRelations | union,pmc=30, union,allies=-50, pmc,allies=-50 |
| | playerRespawnMode, respawnSearchRadius, blockBeaconOnlyBedSleep | VANILLA, 192 blocks, false |
| Military.Villages | enabled, restrictHostileTrade | true, true |
| | outpostWeight, outpostWidth | 8, 13 blocks |
| | hostileOccupationPercent, initialFaction | 25, union |
| | territoryRadius, garrisonSize, patrolSize | 64 blocks, 4, 3 |
| | occupiedMessage | empty, uses the localized default |
| Military.Economy | denominations, maxWithdrawal | [1, 10, 100, 1000], 100000 |
| | hostileCoinDrop | up to 4 base-denomination coins |
| | hungerMultiplier, saturationMultiplier | 2, 1 |
| | foodOverrides, foodBlacklist | empty; rotten flesh and poisonous potatoes are blacklisted |
| Military.Upkeep | interval, processingInterval | 24000, 100 ticks |
| | currencyMultiplier, foodMultiplier | 1, 1 |
| | chargeUnloaded, chargeOffline | true, false |
| | graceIntervals, desertionIntervals | 2, 7 after grace |
| | penalizeUnpaid, penalizeHungry, desertion | true, true, false |
| Military.Diagnostics | debugLogging | false |

The config accepts food entries in mod:item or mod:item@metadata form, with =points appended for overrides. Upkeep uses whole supply points and coins. Faction diplomacy accepts symmetric scores from -100 through 100. Default personal reputation is 10 with UNION and PMC and -50 with ALLIES.

## Custom presets and weapon overrides

Edit config/gvc-military/unit-presets.json on the server and restart. It uses a JSON array. The IDs are stable lowercase keys and should stay the same after units using them have been hired. A preset may reference a namespaced weapon ID, ordered classification pools, or both. GVCO tries the exact weapon first and then the first usable pool. A preset can have a secondary weapon and armor equipment. The armor rating applies independently of worn armor. The resistance fraction is a separate reduction.

Example array entry:

    {
      "id": "frontier_guard",
      "name": "Frontier Guard",
      "description": "UNION defense infantry for village outposts.",
      "faction": "union",
      "classification": "heavy",
      "tier": 3,
      "primary": "HandmadeGuns:M16A4",
      "primaryPools": ["ASSAULT_RIFLE"],
      "secondaryPools": ["HANDGUN"],
      "ammunition": "HandmadeGuns:5.56mm magazine",
      "equipment": ["minecraft:iron_helmet"],
      "health": 26,
      "armor": 5,
      "resistance": 0.12,
      "speed": 0.29,
      "accuracy": 0.8,
      "dispersion": 0.85,
      "recoil": 0.9,
      "reactionTicks": 12,
      "range": 38,
      "reload": 0.9,
      "behavior": "support",
      "recruitmentCost": 44,
      "currencyUpkeep": 3,
      "foodConsumption": 5,
      "rank": 0,
      "capacity": 0,
      "requiredRelation": 0,
      "recruitable": true,
      "reinforcementWeight": 3
    }

Behavior values are rifle, close, support, marksman, and anti_armor. Lower accuracy, dispersion, recoil, reaction and reload values improve the respective handling properties. Movement is a Minecraft movement attribute, engagement range is measured in blocks, and reload is a multiplier on the HMG duration. Optional equipment currently accepts wearable armor items. A positive reinforcement weight adds a preset to its faction's reinforcement pool. Setting recruitable to false keeps it out of trader offers. Base patterns for tier progression draw on access through friendly recruiters and ongoing support costs. [TaleWorlds recruitment design](https://www.taleworlds.com/en/Games/Bannerlord/Blog/62) · [KK Game Studio update notes](https://store.steampowered.com/news/posts/?appids=773951&enddate=1551473976&feed=steam_community_announcements)

Use weapon-overrides.json to set a firearm's classification, opt into a compliant weapon disallowed by its pack, or disable it explicitly:

    {
      "HandmadeGuns:M16A4": {
        "classification": "ASSAULT_RIFLE",
        "enabled": true,
        "reason": "The standard UNION rifle in this pack."
      }
    }

weapon-compatibility.json is rewritten at startup. It lists every registered HMG firearm, including content packs, with modes, magazine IDs, projectile type, compatibility status and reasons. A supported classification indicates that the registered weapon fits the handheld NPC integration. An unsupported or unclassified row explains missing data or compatibility restrictions. Physical tests are separately recorded for the weapons exercised.

## Existing worlds and mod integration

The old entity, flag, equipment and platoon IDs are retained. GVCO converts old per-chunk flag records into one occupation per beacon position, keeping the saved faction and progress from the existing tile entity. Existing camps can receive traders and managed reinforcements when loaded. Village outposts are new village pieces generated in new terrain. Existing village buildings remain intact.

Legacy PMC entities keep the squad IDs, standing orders and ownership behavior encoded in their saves. Older entities can lack a persistent employer or a full military record. They continue to work through the original command device. Default legacy recipe and survival deployment changes are reversible in Military.Recruitment.

External mods can use MilitaryEconomy.balance(UUID) and MilitaryEconomy.adjustBalance(UUID, long) on the server thread. MilitaryRelations.reputation and setReputation address per-player relationship values. MilitarySupplies.coinItem(int) exposes the four physical items for loot and quests, and registerFoodAdapter accepts stack-aware conversion and container handling for unusual food implementations. Vehicle recruitment, industrial production, settlement construction and campaign rules remain outside this release.

## Player interface development standard

Before designing a GVCO screen, review comparable Minecraft interfaces and relevant game screens in use. Inspect screenshots and implementation details, identify the decisions players need to make, then place supporting details behind tabs, selection, or tooltips. Preserve Minecraft’s normal inventory controls and GUI scaling. Use localized gameplay labels, readable numbers and time units, clear eligibility feedback, and server-authoritative confirmation for transactions.

Check new screens using the production mod and Minecraft’s native framebuffer. Cover small and large layouts, relevant GUI scales, empty and populated states, and installed inventory overlays. Revise the screen based on actual readability and interaction. The military screens use MineColonies’ distinct management views and Bannerlord’s selected-recruit preview as references. The selected entity preview uses vanilla’s inventory renderer, informed by NEI Resources and MobsInfo. [MineColonies town hall](https://minecolonies.com/wiki/buildings/townhall/) · [Bannerlord recruitment screenshot](https://store.epicgames.com/news/mount-blade-ii-bannerlord-10-tips-to-help-you-conquer-the-world?lang=en-US)
