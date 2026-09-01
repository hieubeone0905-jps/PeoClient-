# PeoClient 1.21.4 — Fast Nuker + local interaction recovery

Baseline: source from the strongest `(19)` build supplied by the user.

Changes:
- Preserves the original fast Nuker target/queue engine.
- Adds a lightweight client-side watchdog for a stale vanilla breaking state.
- If the active target becomes air or breaking progress remains unchanged for 8 client ticks, the current local breaking state is cancelled and the target queue is rebuilt.
- Does not add packet spoofing, fake movement, fake block-break packets, or anti-cheat bypass logic.
- GUI and module structure are taken from the `(19)` source baseline.

Build:
`./gradlew clean build --no-daemon`
