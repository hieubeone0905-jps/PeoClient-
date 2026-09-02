# PeoClient 1.21.4 - Nuker Render Diagnostic/Fix

- Adds throttled detailed Nuker render diagnostics.
- Tracks client world block-state changes, queued chunk sections, render flushes and renderer errors.
- Coalesces render invalidation per tick.
- Periodically performs a throttled full WorldRenderer reload after sustained Nuker activity to recover stale ghost meshes.
- Does not change Nuker targeting, range, multi, cooldown, rotation, or packet behavior.
- Diagnostic log is written under `.minecraft/logs/peoclient/diagnostic_YYYY-MM-DD.log` when diagnostics are enabled.
