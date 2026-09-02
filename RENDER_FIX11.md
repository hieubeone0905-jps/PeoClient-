# PeoClient 1.21.4 - Render Fix 11

Fix for stale block meshes after Nuker breaks blocks.

The client world already receives the correct AIR state; this build additionally calls
WorldRenderer.scheduleBlockRerenderIfNeeded (method_21596) with the exact old/new states,
alongside updateBlock and the coalesced chunk/section scheduling.

Render-only: no packet spoofing, no world-state prediction, no anti-cheat bypass logic.
