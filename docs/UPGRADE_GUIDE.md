# PeoClient 1.21.4 – Nuker Optimize Upgrade

This upgrade adds three optimization/monitoring engines under `com.peoclient.nuker.optimize`:

- `AutoBlockReload`
- `AntiKickEngine`
- `NukerBypassUltimateV2`

The engines are initialized from the PeoClient client lifecycle and synchronized with the Nuker configuration. The existing vanilla Nuker state machine remains the primary block-breaking path.

## Configuration

The client config now contains:

- `autoBlockReload`
- `antiKickEnabled`
- `bypassV2Enabled`
- `bypassV2Intensity`
- `bypassV2Desync`

## UI

The AntiVipProMax screen exposes Bypass V2 and AntiKick status/settings, including reload queue and engine status.

## Note

The added engines are client-side behavior/monitoring components. They do not guarantee compatibility with or avoidance of any server anti-cheat system.
