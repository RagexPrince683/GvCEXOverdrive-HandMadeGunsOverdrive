# Factions and Survival

GVC Overdrive uses the same faction, military account, and site records in ordinary Overworlds and authored maps. Explore roads, ruins, and installations to find services, equipment, and contracts. You can build a treasury by selling mined resources, delivering supplies, or fighting hostile troops. Traders display the services available at their station. The **Quests** tab in the military interface shows your active work wherever you are.

## Factions

| ID | Faction | Default disposition and services |
| --- | --- | --- |
| `union` | Defense Corps | Friendly starting standing. Standardized garrisons and enlistment. Allied with Free Company and Northline. |
| `allies` | Insurgents | Hostile starting standing. Inexpensive irregular personnel. Hostile toward Defense Corps and Free Company. |
| `pmc` | Free Company | Friendly starting standing. Professional recruits and commercial arms dealers. Hostile toward the Insurgents. |
| `cinder` | Cinder Raiders | Hostile toward players and every other faction. Raider and scrapper patrols. |
| `crossroads` | Crossroads Cooperative | Neutral starting standing and treaties. Local watch, provisions, and resource markets. Cinder Raiders treat it as hostile. |
| `dune` | Dunewatch | Neutral treaties and starting standing. Desert patrols, relay stations, and water stops. |
| `northline` | Northline Rangers | Friendly starting standing. Winter guides and marksmen. Allied with Defense Corps. |

Edit `config/gvc-military/factions.json` on the server and restart. Existing entries keep their IDs. Omitted fields for built-in factions use built-in defaults. `enabled` controls new site generation, recruitment, and faction services. Saved faction IDs, ownership, and allegiance are retained when a faction is disabled. Set `initialReputation`, `hostileBelow`, `tradeRequired`, `recruitRequired`, `entryTier`, `allegianceRequired`, and `allegianceDiscount` to tune player progression. `relations` is a symmetric treaty map with scores from −100 to 100. Values below zero are hostile and values above zero are allied. The existing `Military.Occupation.factionRelations` settings take precedence where they specify a pair.

`insignia` selects an existing emblem and `color` is a decimal RGB integer. `architecture` and `doctrine` describe the faction in the Factions interface. `regionalWeight` and `biomes` influence site placement. The optional `structures`, `equipment`, `firearmClasses`, `recruitment`, and `quests` lists restrict their corresponding pools. `loot` lists item IDs for battle spoils. Adjust unit composition and equipment in `unit-presets.json` and `regional-unit-presets.json`. The latter is distributed separately so worlds with existing preset overrides receive the regional defaults.

## Regional sites

`Military.Regions` in `GVCMob.cfg` controls procedural generation. The world seed and a grid cell select at most one candidate per cell. A separate, larger region grid supplies the leading factions, with weighted local selection and biome-specific pools introducing competition and isolated facilities. Adjust `regionSizeChunks`, `siteGridChunks`, `candidatePercent`, and `influencePercent` to change the scale and concentration. `hostileSpawnBufferBlocks` keeps hostile procedural sites away from world spawn and the Overworld origin by default. `reinforcementPercent` scales the garrison and patrol caps at regional sites.

Placement waits for the footprint's chunks to finish terrain population. It checks surface materials, slope, nearby registered sites, and known villages. Land sites use shallow foundations and a grade-checked entrance. Offshore facilities require a continuous water surface and a water depth of 5 to 28 blocks. Site orientation comes from its seed. Construction stays inside the selected footprint and uses the existing per-dimension occupation registry.

Settlements support several services. Influence sites ordinarily have a lookout, guards, a local patrol, or supplies. Abandoned sites begin unclaimed and unstaffed. An occupation marker at a site can support ownership without serving as a player respawn beacon. Claiming transfers the recorded site capabilities, including its fixed trader stations, without changing the layout. Only sites marked capturable can be claimed. A captured warehouse ruin remains a warehouse ruin.

| Faction | Settlements | Influence sites | Abandoned sites |
| --- | --- | --- | --- |
| Defense Corps | Corps Barracks, Corps District HQ, Corps Civic District | Corps Checkpoint, Corps Observation Post | |
| Insurgents | Irregular Farm Compound | Irregular Dugout, Irregular Lookout, Irregular Supply Camp | |
| Free Company | Company Contract Yard, Company Platform, Company Regional Hub | Company Logistics Depot, Company Coastal Station | |
| Cinder Raiders | Cinder Scrapyard | Cinder Roadblock, Cinder Relay | |
| Crossroads Cooperative | Crossroads Market, Crossroads Borough | Crossroads Homestead, Crossroads Coastal Exchange | |
| Dunewatch | Dunewatch Water Stop | Dunewatch Relay, Dunewatch Road Post | |
| Northline Rangers | Northline Lodge | Northline Lookout, Northline Relay | |
| Unclaimed | | | Deserted Warehouse, Deserted Watchtower, Deserted Farmstead, Deserted Platform |

Regional districts group offices, quarters, and warehouses around intersecting streets. Barracks and lodges have furnished sleeping areas. Depots have loading entrances and shelves, and supply compounds link command, trade, storage, and workshops by footpaths. Offshore platforms have raised structural decks, service rooms, cargo bays, and cranes. Faction materials and roof shapes help identify buildings at a distance.

Site definitions can set trade stations, loot, staffing, size, palette, rarity, permitted biomes, and up to four optional scavengers for an abandoned location. Newly created server configs use larger footprints for selected barracks, farmsteads, depots, lodges, and offshore platforms. Existing configs keep their explicit widths. The updated layouts apply when a site is generated or placed. [Site definitions](../../GVC/src/main/resources/assets/gvcmob/military/sites.json)

Some building plans vary with the world seed and site coordinates. Ordinary sites have a low occupation marker. A visible flag and player respawn service are reserved for sites with the `beacon` option enabled. The site still tracks its faction, guards, traders, and capture state. New defaults reserve the service for selected settlements and forts. [Architecture references](architecture-references.md)

### Design references

- LOTR Legacy's Bree market, Dale's separate village and watchtower pieces, and the Near Harad and Forodwaith biome decorators informed the distribution of civilian locations, regional military architecture, and ruins. The trader respawn entity and mini-quest classes informed the visible vacancy and short, faction-associated objectives. [LOTR Legacy community source](https://github.com/Maltezak/LOTR-Mod)
- The Fort Vancouver plans distinguish gates, warehouses, shops, office and sleeping quarters around a shared court. Those divisions informed the headquarters, barracks, and contract yard. Fort Scott's infantry barracks show how sleeping and mess functions fit around a parade ground. [Fort Vancouver plans](https://npshistory.com/publications/fova/hsr/plates-1.htm) · [Fort Scott barracks](https://www.nps.gov/places/000/infantry-barracks.htm)
- Statfjord's raised topsides, supporting columns, and separated work areas informed the Company platform and the derelict version. [Norwegian Petroleum Museum](https://www.norskolje.museum.no/en/home/industrial-heritage/statfjord-industrial-heritage/)
- Fort Vancouver's reconstructed buildings show how distinct entries and silhouettes identify field offices and workshops. The documented Statfjord accommodation and utility modules informed the separate deck buildings and work areas. [Fort Vancouver image archive](https://npgallery.nps.gov/SearchResults/albumid/57354831-92DB-F2B8-0C16E29139D0DCE4) · [Statfjord topside construction](https://statfjord.industriminne.no/en/2018/04/05/building-the-statfjord-a-topside/)
- The trade screen pairs a merchant's listing with the player's inventory, using Minecraft item icons and a separate price confirmation. Freeman's equipment economy and Bannerlord's inventory and town interfaces informed the trade and contract flow. [Freeman: Guerrilla Warfare](https://store.steampowered.com/app/773951/Freeman_Guerrilla_Warfare/) · [Bannerlord inventory design](https://www.taleworlds.com/en/Games/Bannerlord/Blog/81) · [Bannerlord town services](https://www.taleworlds.com/en/games/bannerlord/blog/67)

## Traders and commerce

Traders are mortal by default in new configurations. A killed trader leaves a floating coin at the registered station. The coin displays the remaining replacement time at close range. After the timer, the trader returns when the site and station chunks are loaded and the station has clear standing space. Replacement uses the current site owner. An unclaimed station only serves players if its definition explicitly allows a neutral trader. The vacancy and its timer are saved. The marker is recreated when the site loads. Traders do not furnish inventory or reputation farming rewards under the default settings.

`Military.Recruitment.invulnerableTraders`, `traderReplacementTicks`, and `traderReplacementMarkers` control these behaviors. Setting the timer to zero disables replacement. Existing explicit invulnerability settings still apply.

Select **Buy** or **Sell** at an eligible trader. The listing shows the unit price and your inventory appears beside it. Search by item name or ID, cycle categories, and use the quantity controls to preview the total before confirming. The server rechecks the offered stack, balance, station, inventory capacity, and stock. A sale consumes the entire selected item, including its saved attachment and magazine data. The same account funds purchase and recruitment.

`commerce.json` controls the catalog and merchant roles. HMG firearms, compatible ammunition, magazines, and attachments are discovered by item class, including items registered by HMG packs. Item entries use `modid:item` or `modid:item@metadata`. Set `buy` or `sell` to zero to close that side of an entry. Each role can select categories, list additional items, apply a price percentage, and limit the currency it pays each restock period. Role switches `recruit`, `quests`, `sellsFrom`, and `sellsTo` control individual services. Per-item stock and optional liquidity reset every `restockTicks`. Gun resale uses base value and never separately pays for embedded items. Ordinary damageable items receive a wear adjustment. The sale calculation caps a listed item below its lowest possible buy price.

Vanilla resource selling is enabled by default and includes ore, ingots, diamonds, emeralds, coal, redstone, lapis and quartz. Set `vanillaResources` to `false` to disable it. For example, set `"minecraft:iron_ingot": {"category":"resources","sell":8}` under `items` to price iron ingots. With `oreDictionary` enabled, a sell entry such as `"ore:ingotCopper": {"category":"resources","sell":4}` also accepts registered matching copper ingots. Blacklists override the catalog.

## Contracts

Talk to a friendly or neutral soldier, officer, or trader and open **Quests**. Providers can request supplies, ammunition, recovery of gear, patrol combat, specific enemies, reconnaissance, visits, neutralization, village liberation, or capture. The journal shows progress, known coordinates, and rewards. Return to a provider of the issuing faction to claim the reward. Required delivery items are consumed at turn-in. Expired or invalidated contracts can be abandoned from the journal. The server stores progress, cooldowns, and completion with your military account.

The bundled `quests.json` is editable. Definitions can specify faction and provider role, required reputation and allegiance, repeatability, expiry, cooldown, and rewards. The server selects location objectives from registered sites near the provider. It does not load terrain to find a target. A missing item or unavailable target prevents the offer.

```json
{
  "id": "stone_shipment",
  "type": "DELIVER",
  "factions": ["crossroads"],
  "roles": ["resources"],
  "item": "minecraft:stone",
  "amount": 12,
  "currency": 10,
  "rewardReputation": 4,
  "rewards": ["minecraft:bread*2"],
  "cooldown": 6000
}
```

## Fieldworks, borders, and roads

Regional interiors give extra weight to settlements. Outer regions give extra weight to fieldworks. Checkpoints, trenches, covered nests, hardened shelters, and abandoned defenses have the same ownership and occupation rules as other influence sites. Insurgent hideouts and compounds have walk-in entrances, lit rooms, and supply caches. Compound layouts have a second exit. The surface above a tunnel does not count toward its capture.

Where adjacent procedural regions have mutually hostile owners, suitable terrain can generate a pair of opposing positions with a strip of damaged ground between them. Each side registers and reinforces independently. Border sites use smaller occupation records than settlements.

Supply roads connect same-faction settlements, supply towns, and regional command compounds at their gates. Supply towns have two approaches so a road can continue through town to a fortress. Independent patrol and influence sites remain points of interest along the way. Roads follow the existing grade and span shallow water with short plank decks. The route waits for its terrain to be loaded and populated. Sites need a recorded gate to participate.

The largest command compound spans 81 blocks. These bases combine a two-level headquarters, service buildings, barracks and storage yards with local gates and open movement lanes. Supply towns have separate streets and building blocks. The village occupation office has a guarded entrance, a public service counter and an enclosed workroom. Newly generated village offices default to 21 blocks wide.

The `military.regions` configuration exposes `fieldworks`, `frontlines`, `frontlineCandidatePercent`, `factionRoads`, `maximumRoadLength`, and `roadsPerHub`. The server loads `sites.json`, `fieldworks.json`, and `supply-sites.json`. Craftable GVCO materials include Barbed Wire, Riveted Steel Plate, Blast Wall Panels, Camouflage Canvas, Timber Revetment, Military Treadway, and Battlefield Rubble.

## Structure testing

Creative operators can select a **Structure Plan** from the GVCO item tab. Face the intended entrance, then right-click the approach to place the layout ahead of you. Sneak-right-click to place the occupied version. For an exact anchor and quarter-turn rotation, use `/militaryadmin structure preview <site> <x> <z> [0–3]` or `/militaryadmin structure live <site> <x> <z> [0–3]`. Both use the production layout and check the footprint. For offshore previews and tunnels, a deep flat preset gives the builder room for a basin or underground rooms.

Use `/militaryadmin structure showcase` in a flat test world to disable automatic GVCO sites and protect the geometry-only gallery from occupied placement. Preview placement omits occupation registration, beacons, traders, guards, and loot.

## Reputation on the HUD

Use Left and Right to cycle available factions, Up to select your pledged faction, and Down to toggle the card. All four bindings are in the GVCO controls category. Orders are visible on units you command and troops of your pledged faction. Traders display their current faction and station role.

## Authored worlds and existing saves

Use `regions.json` to overlay faction ownership preferences or suppress procedural sites in coordinate rectangles. Regions can set `pricePercent`, `garrisonPercent`, `equipment`, and `quests`. Site definitions can specify a `hierarchy` label and provide their own price and service pools. A `sites` entry schedules a fixed definition at an anchor when its footprint chunks populate. A rotation of 0 through 3 selects quarter turns. A missing faction uses the definition's initial owner. An empty faction creates an unclaimed site. To place in an already populated map, call `MilitarySites.placeAuthored(World, String site, String faction, int x, int z, int rotation)` from the server thread. A non-null result is the registered `Occupation`. The method only places on loaded, admissible terrain. Existing beacon administrators can use `/militaryadmin occupy` to transfer a nearby registered site. The `MilitarySites.regionalFactions(World, int x, int z)` query returns two faction IDs in priority order, with an empty ID indicating unclaimed influence.

```json
{
  "regions": [
    {"dimension":0,"minX":-800,"minZ":-800,"maxX":800,"maxZ":800,"faction":"union","procedural":false}
  ],
  "sites": [
    {"dimension":0,"site":"corps_barracks","faction":"union","x":400,"z":128,"rotation":1}
  ]
}
```

Legacy single-trader sites migrate to a station named `main`. Keep existing faction, recruitment preset, and site IDs in configuration. Existing Army, Treasury, reputation, allegiance, and occupation records continue to use their original save files. Regular generation occurs when terrain populates. Set `proceduralSites` to false to disable regional generation, and set the older `world.cfg_setCamp` and `Military.Villages.enabled` switches independently when configuring a fully authored map.

The libraries reuse Forge 1.7.10 inventories, village pieces, and GVCO's save and entity systems. They require GVCO and HMG. They run independently of MCHeli Overdrive, RTM, XenoFactions, and DEADHOUR.
