# PeoJoin fix 2026-09-05

Fixed PeoJoin recovery flow:

- More reliable hub-return detection using compass appearance plus ClientWorld replacement/reconnect detection.
- Handles a brief disconnect where the client world becomes null before the hub world is recreated.
- Keeps Nuker passive while PeoJoin is enabled and only auto-disables Nuker after a confirmed hub-return signal.
- Nuker is toggled through the existing module toggle path so normal Nuker shutdown bookkeeping is preserved.
- Fixed `/home`: the previous implementation used `sendMessage`, which only displayed local HUD text. It now uses the Minecraft 1.21.4 `ClientPlayerEntity.sendCommand` intermediary method.
- Keeps the requested 5-second Skyblock wait and 5-second `/home` wait.
- Nuker range, multi, break speed, cooldown, targeting and Omni-directional logic are unchanged.
