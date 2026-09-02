# AntiVipProMax Safe Compatibility

This revision is intentionally compatibility/diagnostic only.

- NukerLogic remains the sole owner of block-breaking interaction.
- Nuker range, multi, cooldown, mode, target selection, rotation and tick frequency are unchanged.
- AntiVipProMax does not inject movement, rotation, digging or fake block-break packets.
- The unused packet-spoof helper has been removed from the source tree.
- Auto Adjust is observation-only and no longer rewrites the user's Intensity setting.
- Auto Recovery only coordinates the existing local stale-state recovery already owned by NukerLogic.

Server-side kicks such as `Post!` or `MultiActionsC!` cannot be reliably eliminated from the client without implementing anti-cheat evasion. This build instead keeps the client stable and records diagnostics so the exact server-side trigger can be investigated.
