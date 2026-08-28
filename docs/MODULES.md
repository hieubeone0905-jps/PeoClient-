# PeoClient module specification

## X-Ray
Settings currently exposed by the source:
- Enabled
- Target block IDs
- Fluids
- Opacity mode
- Alpha (0-255)
- Hide surface

The default ore list includes overworld ores, deepslate ores, ancient debris, nether gold and common ore blocks.

## Fullbright
- Mode: Gamma / Potion
- Gamma: 1-12
- Original gamma is saved before enabling gamma mode and restored after disabling.
- Potion mode refreshes Night Vision while enabled.

## Nuker
- Mode: Normal / Survival Multi / Multi / Instant
- Range
- Shape: Cube / Sphere
- Sort: Closest / Furthest / Softest / Hardest / None
- Filter + whitelist
- Raycast
- Flatten
- Multi count
- Cooldown
- Block filter string

## InventoryCleaner
The implementation follows the same high-level workflow as a modern inventory cleaner without copying another client's implementation:
1. Dispose explicitly blacklisted items.
2. Respect per-category stack limits.
3. Preserve useful combat/tools/food categories.
4. Fill configured hotbar targets from the main inventory.
5. Save settings to config/peoclient.json.

Hotbar target categories:
WEAPON, BOW, PICKAXE, AXE, NONE, POTION, FOOD, BLOCK, BLOCK

Offhand target:
SHIELD or WEAPON
