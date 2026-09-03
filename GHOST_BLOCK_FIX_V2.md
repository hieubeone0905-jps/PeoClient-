# Ghost-block / continuous vanilla breaking fix v2

Changes in this build:

1. Nuker keeps exactly one active breaking target across client ticks.
2. `ClientPlayerInteractionManager.attackBlock` starts the target once.
3. `updateBlockBreakingProgress` is called on every subsequent tick while the
   target remains active, matching the important vanilla hold-left-click flow.
4. The client never forces the target to air and never calls `WorldRenderer.reload()`
   as a ghost-block repair mechanism.
5. The old render-refresh mixins and their queued renderer rebuilds were removed.
6. Server/client block state updates are left to Minecraft's normal ClientWorld and
   WorldRenderer path.
7. Recovery still cancels a genuinely stagnant vanilla breaking state after the
   existing diagnostic threshold, but does not fabricate a replacement block state.

This is a synchronization/rendering fix only. It does not bypass server-side block
breaking validation.
