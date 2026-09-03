# PeoClient Nuker Stability Upgrade

This pass focuses on vanilla-compatible Nuker stability and ghost-block recovery.
It does not implement anti-cheat evasion or synthetic movement/block-break packets.

## Changes
- Added `NukerWorldSync` as the single stale-target guard.
- Removed the old fake right-click/interact ghost-block workaround.
- Removed the second synthetic break-packet producer from Bypass V2.
- Converted `AntiKickEngine` to monitor-only mode: no rotation randomization, position jitter, micro-pauses, or Nuker throttling.
- Disabled latency-based extra cooldown injection so configured Nuker throughput is preserved.
- Kept the existing Nuker range/multi/rotation/interaction path intact.
- Successful breaks do not receive an extra recovery delay; recovery is entered only after a stale break state.
