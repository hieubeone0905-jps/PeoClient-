# InventoryCleaner parity reference

PeoClient's InventoryCleaner is a Java/Fabric 1.21.4 port of the behaviour model in the user-supplied LiquidBounce nextgen InventoryCleaner reference. Because LiquidBounce uses a separate Kotlin module/event/scheduler architecture, the implementation is integrated into PeoClient's Java tick and click-slot pipeline rather than copying its internal event classes.

Settings mirrored from the reference:

- `MaximumBlocks` 0..2500
- `MaximumArrows` 0..2500
- `MaximumThrowables` 0..600
- `MaximumFoodPoints` 0..2000
- `MaximumWaterBuckets` 0..16
- `MaximumLavaBuckets` 0..16
- `MaximumMilkBuckets` 0..16
- `ItemsBlacklist`
- `Greedy`
- `OffHandItem`
- `SlotItem-1` through `SlotItem-9`

Supported item choices mirror the reference's `ItemSortChoice`: Sword, Weapon, Spear, Mace, Bow, Crossbow, Axe, Pickaxe, Shovel, Hoe, Rod, Shield, Water, Lava, Milk, Pearl, Gapple, Food, Potion, Block, Throwables, Ignore and None.

Workflow:

1. Fill/sort offhand and hotbar targets.
2. Merge partial stacks when enabled.
3. Dispose blacklisted or unneeded items while respecting category quotas.

The PeoClient extensions `Merge partial stacks`, `Action delay`, `Server ack timeout` and `Touch hotbar` are retained for reliable Fabric-server interaction.


## Speed / ghost-item safety

The cleaner keeps a single pending inventory transaction and waits for the server slot update before continuing. The acknowledgement timeout is now 6 ticks by default (configurable down to 2 ticks), reducing unnecessary idle time on responsive servers while retaining the server-acknowledgement gate. Lowering the timeout can increase throughput on laggy servers at the cost of higher desync risk; the cleaner does not intentionally bypass server inventory validation.


## PeoClient v2.1 filter mode
- Added an item-only drop filter with searchable item icons/names and a scrollable picker.
- When `Filtered items only` is enabled, the cleaner drops only selected item IDs and leaves all other items untouched.
- Each full-stack throw waits for the server slot update before the next throw. This intentionally keeps the fast path from outrunning server acknowledgement and causing ghost-item desync.
- Action delay remains 0 by default; acknowledgement gating is the safety boundary.
