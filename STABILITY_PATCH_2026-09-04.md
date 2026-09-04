# Nuker Stability Patch – 2026-09-04

This patch keeps the existing Nuker targeting/range/multi/break path intact while removing ghost-block reload/recovery from the active Nuker loop.

Changes:
- AutoBlockReload and AutoReloadEnhancer are no longer started/ticked by the main client.
- AntiKickEngine is monitor-only: no rotation noise, micro-pauses, or dynamic Nuker cooldown changes.
- Nuker no longer invokes NukerWorldSync for normal stale-progress recovery.
- A conservative 40-tick local frozen-break watchdog remains; it only abandons a genuinely frozen vanilla break state and does not synthesize interaction packets.
- Existing Nuker pacing and normal Minecraft interaction manager remain the sole action path.

Goal: reduce client-side state churn and duplicate recovery while preserving Nuker strength and normal break mechanics. This is not an anti-cheat bypass.
