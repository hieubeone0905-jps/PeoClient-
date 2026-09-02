# PeoClient 1.21.4 - Render Fix 12

Fix for stale Nuker block meshes.

- Hooks the actual WorldRenderer#updateBlock (method_8570) return path.
- Queues the exact affected built chunk sections through scheduleChunkRender (method_8571).
- Queues touching neighboring sections only when the changed block is on a 16-block section boundary.
- Keeps a small block-level dirty pass and terrain update for connected geometry.
- Render-only: no packet spoofing, no world-state prediction, no anti-cheat bypass logic.
