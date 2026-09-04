# PeoClient stability patch — 2026-09-04

## What changed
- Added `NukerPacingController`.
- Nuker range, multi, target selection and immediate burst behavior are unchanged.
- Sustained block-start bursts are paced only after the action rate remains high across multiple 1-second windows.
- Pacing becomes more conservative automatically when ping is high.
- `AntiVipProMaxModule` no longer writes an `AutoAdjust observation` line every client tick; it logs on suspicion changes or every 5 seconds.
- Nuker reset now also resets the pacing state.

## Why
The supplied diagnostic showed very high long-session break-start counts, including 39,993/39,993 and 70,820/70,820 successful attempts. The log itself does not contain a captured server disconnect reason, so the exact server-side kick rule cannot be proven from this diagnostic alone.

## Build note
A local `./gradlew build --no-daemon` attempt could not complete in the sandbox because the Gradle Wrapper tried to download Gradle and outbound DNS/network access was unavailable.
