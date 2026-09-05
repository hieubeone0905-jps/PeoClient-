# PeoJoin Hub Trigger Fix 2026-09-05

Fixed the trigger so PeoJoin does not depend on the compass disappearing from the hotbar.

Recovery trigger while PeoJoin + Nuker are enabled now uses a confirmed transition signal:
- ClientWorld replacement;
- world recreation after a brief connection transfer;
- DisconnectListener observation followed by return with compass;
- large server-side player teleport (>= 80 blocks) while the hub compass is present.

A compass alone is NOT a trigger, avoiding false recovery while the player is already in the hub.

When a trigger is confirmed, PeoJoin calls the existing Nuker module toggle path, equivalent to pressing the configured Nuker key (including a user-bound M), then starts compass -> diamond pickaxe -> 5s -> /home -> 5s -> Nuker ON.

Nuker range, multi, cooldown, break speed, targeting, and Omni-directional logic are unchanged.
