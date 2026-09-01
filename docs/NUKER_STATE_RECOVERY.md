# Nuker state recovery

This revision keeps the existing GUI and Nuker settings while making the client-side
breaking state self-recovering.

Changes:
- Detects when the vanilla interaction manager's active breaking position no longer
  matches Nuker's target.
- Watches breaking progress and recovers from a stalled state after a short grace period.
- Rebuilds the target queue without requiring a manual mouse click.
- Avoids background threads and concurrent breaking sessions.
- Manual attack input is given priority for the current tick so it does not race the
  Nuker state machine.

No anti-cheat bypass, position spoofing, or packet-order manipulation is included.
