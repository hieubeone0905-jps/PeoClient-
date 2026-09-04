# Nuker merge — 2026-09-04

Base: PeoClient-integrated-v2.zip
Nuker performance reference: PeoClient--main (15).zip

Changes:
- Restored the higher-throughput NukerLogic behavior from the performance reference.
- Removed the latency-based break-start pacing from the active Nuker path, so configured throughput is not silently reduced.
- Kept automatic stale-target recovery.
- Faster ghost-block checking/reload behavior from the performance reference.
- Replaced the old rotation/micro-pause AntiKick behavior with monitor-only stability logic to avoid injecting artificial movement/rotation or repeated pause patterns.
- Kept the existing compatibility/diagnostic structure from the base.
- No packet spoofing or anti-cheat bypass behavior was added.
