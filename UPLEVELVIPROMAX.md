# UpLevelVipProMax

Module for the two-account island-level workflow.

## Level blocks
- minecraft:diamond_block
- minecraft:emerald_block
- minecraft:lapis_block
- minecraft:coal_block
- minecraft:redstone_block
- minecraft:iron_block
- minecraft:gold_block

## Zero-value blocks
Only these are dropped:
- minecraft:stone
- minecraft:cobblestone
- minecraft:raw_gold_block
- minecraft:raw_iron_block

Mineral ingots/ores used as crafting input are not touched.

## Workflow
1. Scan the player's inventory.
2. Drop only the four zero-value block types.
3. Find a level block in the hotbar.
4. Use it against the current crosshair block using normal client interaction.
5. When the server opens `Cấp độ đảo`, find the hopper and click it.
6. Wait for server processing, then close the handled screen normally.
7. Scan again so blocks produced by the server's 35-tick crafting process are handled on later ticks.

The module does not spoof packets or attempt to bypass anti-cheat.
