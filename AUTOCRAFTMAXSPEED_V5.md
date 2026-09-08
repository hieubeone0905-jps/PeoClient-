# AutoCraftMaxSpeed V5

## Craft engine change
Only the crafting engine was replaced. The existing module settings and server workflow are retained.

- Craft Speed: 1-36 stacks/tick (maximum recipe operations attempted per client tick)
- Drop Speed: 1-36 stacks/tick (existing behavior retained)
- Rare Threshold: 1-36 stacks
- /craft opening behavior retained
- Rare-block submission/combine GUI/hopper loop retained

The new crafting path follows the BleachHack AutoCraft pattern:

1. Read the player's synced recipe book.
2. Find a target mineral-block recipe.
3. Call `clickRecipe(syncId, recipeId, true)` with `craftAll=true`.
4. Immediately `QUICK_MOVE` crafting output slot 0 back to inventory.
5. Try other target recipes in the same tick, up to Craft Speed.
6. Each output type is attempted at most once per tick because `craftAll=true` already requests the maximum craftable amount for that recipe.

This removes the old per-item 3x3 cursor filling and artificial per-click delay.
