# AutoCraftMaxSpeed V4 – crafting inventory jitter fix

Fixes the server-backed crafting GUI getting stuck/jittering when moving crafted output back to the player inventory.

Changes:
- Removed bulk quick-move filling of the 3x3 crafting grid.
- Uses normal server inventory clicks: pick up source stack, place one item per input slot, return cursor stack.
- Keeps the same recipe active until the output is taken.
- Output is moved with the normal QUICK_MOVE/shift-click path, then the module waits for server synchronization.
- Drop operations are limited to one inventory action per tick to prevent packet/action bursts and visual inventory corrections.
- Keeps the existing Craft Speed / Drop Speed / Threshold settings in the GUI. The crafting/drop action path is deliberately safety-paced because the GUI is server-backed.
- `/craft` opening and module HUD/keybind behavior from V3 are preserved.
