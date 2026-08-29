# Build Status

Patch: Nuker survival block-breaking synchronization.

Changed `NukerLogic` to use a real `attackBlock()` start followed by `updateBlockBreakingProgress()` on subsequent ticks, with a tracked breaking position/face and cancellation when switching targets. This avoids sending progress updates for a break that was never started.

This is intended to improve vanilla/server-compatible block breaking. It does not attempt to bypass anti-cheat or server protections.
