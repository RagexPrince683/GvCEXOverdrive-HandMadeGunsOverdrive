
# Military management

GVCOverdrive requires the matching Handmade Guns Overdrive build on both client and server. The military systems run on Forge 1.7.10 with Java 8. Optional content packs and mods can contribute firearms, food, or rewards through existing item registries and the GVCO API.

## Recruiting and commanding

Open your inventory and choose **Military** in the upper right, or run `/military`. **Overview** shows your army’s readiness, upkeep per Minecraft day, and estimated reserves. **Personnel** lets you scroll, filter by status, group identical units, and select a soldier for their assignments and equipment. **Factions** shows standing, discounts, allegiance, faction relationships, and the status of a nearby settlement.

Open the **Field Manual** from Overview or run `/military manual`. You can also craft the manual with a book and paper. Its searchable chapters cover your first recruit, orders, formation management, standing, allegiance, and settlement capture. The command chapter displays your current activation keys.

In **Treasury**, shift-click coins into the deposit slot or choose **Deposit all coins** to deposit coins from your inventory at once. Enter a withdrawal amount or choose a common amount, review the physical coin mix, then confirm. In **Supplies**, shift-click food into the deposit slot to preview its nutritional value and the time it will add to your reserves.

Right-click a Military Trader at a base or village outpost. Select an offer to see its equipment, tier, discounted price, upkeep, and eligibility. Locked offers show their reputation or allegiance requirements. Confirm the hire from the selected offer. The unit arrives with its preset equipment and follows its owner. Coin items can come from hostile infantry, raid supply crates, modpack loot, quests, or server rewards.

### Battlefield controls

| Control | Action |
| --- | --- |
| Hold `[` | Open the command wheel. Point at a sector and left-click to select or issue an order. Right-click goes back. Release closes the wheel. Use the mouse wheel or **More groups** to page through groups. |
| Press `]` | Open quick command mode. Press `0` for Everyone, `1`–`8` for assigned quick slots, or `9` for Ungrouped. Tab enters the group browser. Page Up and Page Down switch pages there. |
| Quick category and order | After selecting personnel, press `1` for Movement, `2` for Engagement, or `3` for Formation. Press the number next to the order. Backspace goes back. Escape cancels. The overlay closes after an order or when it times out. |

Change both activation keys under **Options → Controls → GVCO Army Command**. The number shortcuts are active inside the quick overlay. Aim your crosshair before opening either interface. Move, Defend, and Patrol use the block in view. Attack uses a living target in view. The server checks authority and range, confirms the order, and briefly marks accepted destinations.

**Follow** tracks your current position. **Regroup** recalls the formation to the spot where you issued the order. **Hold** anchors the formation at its current leader’s position. **Wait** leaves personnel at their current positions when they are out of combat. **Defend** anchors the group and restricts its engagement radius. **Patrol** moves between the starting point and the marked position. **Cease fire** and **Fire at will** control engagement independently of movement orders. **Return to garrison** uses an accessible local post. Choose Line, Column, Loose, or Double Column in the Formation category.

The command range applies in the current dimension. A nearby unit can relay an order to the rest of its eligible assigned platoon. Everyone targets the eligible groups and unassigned personnel in range. Personnel remain assigned when their chunks unload.

### Military groups

Open **Personnel → Military groups**. Use **+** to create a group, then choose it in the group list. Ctrl-click personnel on the left to select across multiple rows and pages. Choose **Assign** to move the selection to the current group. Assigning to **Unassigned** returns personnel to the unassigned roster. **View group** filters the left roster and **View all** clears that filter.

Select an assigned officer and choose **Set leader**. Use **Clear leader** to remove the assignment. The group panel shows deployment, upkeep per Minecraft day, current orders, and a warning when leadership requirements are unmet. Hover the summary to see unit composition. Use the arrows to reorder a group and click **Slot** to cycle through the eight quick slots or clear the slot. Group names can be changed after creation. Deleting a group returns its personnel to the unassigned roster.

- `/military roster [page]` shows personnel UUIDs, including deployed and unloaded members.
- `/military assign <unit UUID> <leader UUID>` keeps the legacy nearby squad assignment for ungrouped units.
- `/military order <order> [unit UUID|group UUID|all|-]` uses the same order handling. With no selection, it uses the closest eligible squad. Use `-` for Ungrouped.
- `/military relation` shows personal reputation with known factions.
- Operators can use `/militaryadmin balance <player|UUID> [signed delta]`, `/militaryadmin relation <player|UUID> <faction> <score>`, and `/militaryadmin occupy <faction> [owner]`. The occupation command targets a nearby eligible post.

The existing Squad Command Device remains registered for saves. Set `Military.Command.legacyDevice` to true to restore device use and crafting.

The command wheel follows the press-and-hold selection pattern used in Battlefield and Minecraft radial menus. The keyboard overlay uses group, category, and order stages. The personnel and faction panels use a selection list with context for the selected group or faction. [Battlefield field manual](https://eaassets-a.akamaihd.net/eahelp/manuals/battlefield-1-pc-uk.pdf) · [GTNH Radial Menu](https://github.com/Navatusein/GTNH-Radial-Menu) · [Mount & Blade: Warband manual](https://forumcontent.paradoxplaza.com/public/29550/Warband_Manual_English.pdf) · [Bannerlord order interface](https://www.taleworlds.com/en/News/413) · [Freeman squad customization](https://store.steampowered.com/news/posts/?appids=773951&enddate=1525116008) · [LOTR alignment](https://lotrminecraftmod.fandom.com/wiki/Alignment)

## Settlements and respawns

A beacon anchors a permanent post record. The record keeps its faction, any saved player owner, boundary, trader, reinforcement schedule, and garrison and patrol squad IDs. Native camps and compatible structure packs use the same beacon path. Raid supply beacons retain their raid and resupply behavior. A settlement works while its beacon chunk is loaded. Reinforcement waves need a nearby player and use saved squad membership to enforce their caps.

Hostile players and their forces contest the beacon when they outnumber the defenders. At zero control, the beacon stops reinforcing. By default the site is **Neutralized**. A server can instead leave it **Disabled** pending a decision. An unaligned commander can free an occupied site without gaining a player-owned base or replacements.

A pledged player with claim rights can approach the defeated beacon, open **Military → Factions → Nearby settlement**, then choose **Claim for faction** and confirm. The site becomes **Faction-controlled**. Its military trader and future patrols and garrisons serve the new faction. A permitted commander can leave it neutralized, or choose **Neutralize settlement** at a disabled site. Active, contested, disabled, and captured sites retain their state through unloads and restarts.

A village can generate a fortified outpost as a native village piece. The original design uses a cobblestone and gravel court, a broad entrance, a small lookout, and covered barracks. The references informed the courtyard, guarded entrance, and separate sleeping space. The structure uses vanilla rotation, collision checks, biome palettes and ground alignment, with shallow supports below its footprint. [Housesteads fort plans](https://www.english-heritage.org.uk/siteassets/home/visit/places-to-visit/housesteads-roman-fort/history/housesteads-roman-fort-phased-plans.pdf) · [Fort Scott parade ground](https://www.nps.gov/places/000/fs-parade-ground.htm) · [Fort Scott infantry barracks](https://www.nps.gov/places/000/infantry-barracks.htm)

Villagers within an occupied village can refuse trade based on each player’s reputation with the occupying faction. An open trading screen closes if the player becomes hostile. Military offers are checked again at purchase. A neutralized or disabled outpost lifts its occupation restrictions on civilian trade. A claimed site uses the new faction’s relations.

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
| Military.Command | legacyDevice, radialMenu, quickCommands | false, true, true |
| | groupLimit, groupSize | 24 groups, 32 personnel per group |
| | requireOfficerForGroupOrders, enforceLeaderCapacity | false, false |
| | commandRange, targetRange | 96 and 80 blocks |
| | orderCooldownTicks, quickTimeoutTicks, markerTicks | 4, 160, 100 |
| Military.Occupation | enabled, reinforceWhileContested | true, false |
| | reinforcementInterval, reinforcementDistance | legacy flag interval (2000 ticks for new configs), 96 blocks |
| | garrisonSize, patrolSize | legacy beacon strength divided by 2 and 4 respectively (8 and 4 for new configs) |
| | garrisonReplacementRate, patrolReplacementRate | 2 and 1 per wave |
| | territoryRadius, patrolRadius | 48 and 24 blocks |
| | captureStrength | 1000 |
| | neutralizeOnDefeat, factionCapture, allowAlignedNeutralization | true, true, true |
| | updateInterval, tradeCheckInterval | 20 ticks each |
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

The config accepts food entries in mod:item or mod:item@metadata form, with =points appended for overrides. Upkeep uses whole supply points and coins.

## Reputation and allegiance

Faction identity stays with the faction’s units and settlements. Each player’s reputation with that faction determines which interactions are open. Initial personal reputation is 10 with Defense Corps (`union`) and Free Company (`pmc`), and −150 with Insurgents (`allies`). Faction-to-faction scores run from −100 to 100 and use symmetric `faction,faction=score` entries under `Military.Occupation.factionRelations`. Gains come from factions with a negative relationship to the defeated faction. Losses go to the defeated faction. Target tier, classification and rank affect the amount. A soldier can retaliate against a recent attacker independently of long-term standing.

| Minimum reputation | Standing | Highest recruitment tier | Discount |
| ---: | --- | ---: | ---: |
| −1,000,000 | Hostile | 0 | 0% |
| −100 | Unfriendly | 0 | 0% |
| 0 | Friendly | 1 | 0% |
| 100 | Trusted | 2 | 5% |
| 300 | Respected | 3 | 10% |
| 600 | Honored | 4 | 15% |
| 1,000 | Distinguished | 10 | 20% |

Free Company’s basic Rifleman is tier 2. Its faction definition sets `entryTier` to 2 so Friendly players can hire it. Other bundled factions use tier 1.

A trader’s **Allegiance** panel shows eligibility. Pledge or switch there after reaching the faction threshold. Renounce from **Factions**. The server allows one pledge at a time. Leaving costs reputation with the old faction and starts a cooldown. Existing hired personnel and previously captured faction territory stay with their respective owners. Membership can add discounts, access to exclusive presets, permission to claim settlements, and use of eligible military infrastructure. Recruitment always costs at least one coin.

| Military.Reputation option | Default |
| --- | --- |
| standings | Entries use `minimum,label,maxTier,discountPercent`, with the defaults above |
| lossPerKill, allegianceFriendlyFirePenalty | 4 and 6 before modifiers |
| classificationModifiers | heavy=1.25, marksman=1.25, anti_armor=1.5 |
| attributePlayerKills, attributeHiredKills, attributeSubordinateKills, attributeVehicleKills | true |
| attributeIndirectKills, shareParticipation | false |
| attributionWindowTicks, retaliationTicks | 200, 160 |
| rewardPlayerOwnedVictims, rewardReinforcements, rewardTraderKills | false |
| rewardedKillsPerWindow, rewardWindowTicks | 16 kills per 1200 ticks |
| allegianceCooldownTicks, allegianceLeavingPenalty | 1200 ticks and 25 standing |
| notifications, notificationInterval | true, 80 ticks |

Only recorded attackers can receive participation credit. When enabled, indirect attribution uses recent recorded damage for an environmental death. Shared participation gives other recent attackers partial credit. The gain budget persists across restarts. Gain restrictions and the budget still allow losses to apply.

### Custom factions

Edit the server’s `config/gvc-military/factions.json` array and restart. Preserve IDs across saves. The `camp` property chooses an existing beacon appearance. A new faction keeps its own identity for standings, treaties, trader offers, and reinforcement presets. Add custom IDs to `factionRelations` to define alliances or opposition and assign the same ID to a unit preset. For natural village outposts use the ID as `Military.Villages.initialFaction`.

    {
      "id": "frontier",
      "name": "Frontier Guard",
      "description": "Militia defending remote villages.",
      "camp": "union",
      "color": 7788672,
      "initialReputation": 0,
      "hostileBelow": -100,
      "tradeRequired": -100,
      "recruitRequired": 0,
      "entryTier": 1,
      "gainMultiplier": 1.5,
      "lossMultiplier": 1.25,
      "pledging": true,
      "allegianceRequired": 600,
      "allegianceDiscount": 8,
      "settlementCapture": true,
      "memberInfrastructure": true
    }

For this example, `frontier,union=40` makes the factions allies and `frontier,allies=-75` makes them enemies. Define recruitable and reinforcement presets for Frontier Guard to supply its traders and garrisons.

## Custom presets and weapon overrides

Edit config/gvc-military/unit-presets.json on the server and restart. It uses a JSON array. The IDs are stable lowercase keys and should stay the same after units using them have been hired. A preset may reference a namespaced weapon ID, ordered classification pools, or both. GVCO tries the exact weapon first and then the first usable pool. A preset can have a secondary weapon and armor equipment. The armor rating applies independently of worn armor. The resistance fraction is a separate reduction.

Example array entry:

    {
      "id": "frontier_guard",
      "name": "Frontier Guard",
      "description": "Defense Corps infantry for village outposts.",
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
      "requiredRelation": 350,
      "allegianceRequired": true,
      "recruitable": true,
      "reinforcementWeight": 3
    }

Behavior values are rifle, close, support, marksman, and anti_armor. Lower accuracy, dispersion, recoil, reaction and reload values improve the respective handling properties. Movement is a Minecraft movement attribute, engagement range is measured in blocks, and reload is a multiplier on the HMG duration. Optional equipment accepts wearable armor items. A positive reinforcement weight adds a preset to its faction’s reinforcement pool. Set `recruitable` to false to keep a preset out of trader offers. `requiredRelation` adds a preset-specific minimum to the standing and faction gates. To replace the standing tier gate for a particular preset, add `minimumReputation`. Faction and preset minimums still apply. Set `allegianceRequired` to true for membership-exclusive infantry or officers.

Use weapon-overrides.json to set a firearm's classification, opt into a compliant weapon disallowed by its pack, or disable it explicitly:

    {
      "HandmadeGuns:M16A4": {
        "classification": "ASSAULT_RIFLE",
        "enabled": true,
        "reason": "The standard Defense Corps rifle in this pack."
      }
    }

weapon-compatibility.json is rewritten at startup. It lists every registered HMG firearm, including content packs, with modes, magazine IDs, projectile type, compatibility status and reasons. A supported classification indicates that the registered weapon fits the handheld NPC integration. An unsupported or unclassified row explains missing data or compatibility restrictions. Physical tests are separately recorded for the weapons exercised.

## Existing worlds and mod integration

The old entity, flag, equipment and platoon IDs are retained. GVCO converts old per-chunk flag records into one occupation per beacon position, keeping the saved faction and progress from the existing tile entity. Existing camps can receive traders and managed reinforcements when loaded. Village outposts are new village pieces generated in new terrain. Existing village buildings remain intact.

Existing hires keep their account, inventory, upkeep schedule, squad and orders. They start in the unassigned pool until added to a custom group. Existing explicit reputation scores keep their saved numeric values. New thresholds determine what those values mean. Existing occupied settlements keep their saved faction and optional player owner. Removing a custom faction definition makes its offers and privileges unavailable until the ID is restored. The entity and settlement records retain the ID.

Legacy Free Company units without military account records can use the original command device when it is enabled. Legacy deployment recipes and survival hiring remain separate settings in `Military.Recruitment`. Faction names and insignia are presentation changes. Saved faction IDs and legacy registry names carry forward. Use matching GVCO and HMG builds on clients and servers.

External mods can use `MilitaryEconomy.balance(UUID)` and `MilitaryEconomy.adjustBalance(UUID, long)` on the server thread. `MilitaryRelations` exposes faction identities, inter-faction scores, per-player reputation, and allegiance. `MilitarySupplies.coinItem(int)` exposes the four physical items for loot and quests, and `registerFoodAdapter` accepts stack-aware conversion and container handling for unusual food implementations.

## Data ownership

- The overworld military account stores currency, food, personnel UUIDs, custom groups, standing, allegiance, and the reputation reward budget. Group assignment lives with the personnel record and is reapplied when a unit loads.
- The wheel, quick overlay, slash commands, and optional device send orders through one server command path. It resolves owned personnel and destinations on the server thread. The existing per-dimension platoon data stores active formation orders and layout.
- Faction definitions supply starting values and lookup tables. A player's saved account supplies their current standing. AI uses those values on existing target checks. Combat attribution records damage participants only when needed and aggregates notification updates.
- The beacon registry owns the settlement's faction and state. The tile renders and synchronizes its status. Traders, spawned garrisons, and patrol replacements use the same settlement record. Neutralized records remain in the index for later claims.
- Management screens receive revision-based pages and summaries. Command overlays request compact group summaries on opening. The client draws accepted order markers and uses the interpolated living-render coordinates for status labels.

## Player interface development standard

Before designing a GVCO screen, review comparable Minecraft interfaces and relevant game screens in use. Inspect screenshots and implementation details, identify the decisions players need to make, then place supporting details behind tabs, selection, or tooltips. Preserve Minecraft’s normal inventory controls and GUI scaling. Use localized gameplay labels, readable numbers and time units, clear eligibility feedback, and server-authoritative confirmation for transactions.

Check new screens using the production mod and Minecraft’s native framebuffer. Cover small and large layouts, relevant GUI scales, empty and populated states, and installed inventory overlays. Revise the screen based on actual readability and interaction. The military screens use MineColonies’ distinct management views and Bannerlord’s selected-recruit preview as references. The selected entity preview uses vanilla’s inventory renderer, informed by NEI Resources and MobsInfo. [MineColonies town hall](https://minecolonies.com/wiki/buildings/townhall/) · [Bannerlord recruitment screenshot](https://store.epicgames.com/news/mount-blade-ii-bannerlord-10-tips-to-help-you-conquer-the-world?lang=en-US)
