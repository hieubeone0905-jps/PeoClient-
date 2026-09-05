# PeoJoin automatic Nuker OFF on hub return

- PeoJoin remains passive while Nuker is running.
- During a Nuker session, it watches for the hub compass to appear in the hotbar after being absent.
- That transition is treated as the server returning the player to hub.
- PeoJoin then performs the same logical Nuker toggle as the configured Nuker key, so all existing Nuker shutdown bookkeeping runs normally.
- It then opens the compass, selects the diamond-pickaxe Skyblock entry, waits 5 seconds, sends `/home`, waits another 5 seconds, and re-enables Nuker.
- Nuker range, multi, break speed, cooldown, targeting, and Omni-directional logic are unchanged.
