# PeoClient InventoryCleaner 1.21.4

This implementation is an independent Java/Fabric implementation based on the observable feature model of LiquidBounce's modern InventoryCleaner. It does not copy LiquidBounce source files.

## Supported behaviour

- Explicit item blacklist by Minecraft identifier.
- Per-category quotas: blocks, arrows, throwables, food, water/lava/milk buckets, potions and pearls.
- Category-aware hotbar targets: Sword, Weapon, Bow, Crossbow, Axe, Pickaxe, Shovel, Hoe, Rod, Shield, Water, Lava, Milk, Pearl, Gapple, Potion, Food, Block, Throwables, Ignore and None.
- Best-item selection using item type, durability remaining and stack size.
- One-per-function preference for weapons/tools/utilities.
- Optional greedy mode retained as a configuration flag.
- Stack consolidation before disposal.
- One inventory action at a time with configurable action delay.
- Armor and screen-open inventories are left untouched by the automatic cleaner.

## Important 1.21.4 adaptation

LiquidBounce is a separate client architecture with its own event bus, inventory action scheduler, item facets and setting framework. PeoClient uses Fabric's `clickSlot` API and its own configuration model instead. Therefore the behaviour is designed to be equivalent in intent and options, rather than being a binary/source copy.

## Configuration

The module settings are stored in `config/peoclient.json` and can be changed from the PeoClient GUI or directly in the JSON file.
