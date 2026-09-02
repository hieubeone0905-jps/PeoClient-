PeoClient 1.21.4 - Ghost Block FIX16

Changes:
- Removed periodic WorldRenderer.reload() hard refresh.
- Removed sustained hard-refresh timer/state.
- Render updates are coalesced into unique chunk sections once per client tick.
- Pending render sections are capped at 256 to prevent render queue flooding.
- Nuker behavior (range, multi, cooldown, target selection, rotation) is unchanged.
- No packet spoofing or anti-cheat bypass logic added.
