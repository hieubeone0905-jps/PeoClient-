# Ghost Block Fix V2 + Nuker Multi Restore

- Keeps one vanilla breaking target active at a time.
- Calls the normal interaction manager's block-breaking progress every tick, matching the continuous state used while holding left-click.
- Does not force client blocks to air and does not call `WorldRenderer.reload()`.
- Restores the previous `nukerMulti` / Multi / SurvMulti queue-depth behaviour without creating simultaneous breaking states.
- Restores `nukerCooldown` persistence and applies it between completed targets.
- Removes unused Nuker renderer/optimization helper sources left over from earlier ghost-block iterations.

Note: `nukerMulti` controls the number of targets kept ready; actual block-breaking speed remains governed by Minecraft/server-authoritative breaking progress.
