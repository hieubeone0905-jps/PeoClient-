# FIX15 - Live Diagnostic Log

- DiagnosticRecorder now flushes queued entries to disk at least every 250 ms while active.
- PeoClient also flushes the diagnostic recorder once per client tick while diagnostics are enabled.
- The diagnostic directory is recreated before writes if necessary.
- This changes logging only; Nuker behavior, packets, range, Multi, cooldown, and render logic are unchanged.

Diagnostic file:
.minecraft/logs/peoclient/diagnostic_YYYY-MM-DD.log
