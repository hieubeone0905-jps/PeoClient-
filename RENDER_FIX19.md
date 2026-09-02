# PeoClient 1.21.4 - Nuker Ghost Render FIX19

- Keeps NukerLogic unchanged.
- Removes NukerRenderBatcher section scheduling/flush.
- Removes ClientWorld render interception.
- Removes no-op chunk rebuild hook.
- No renderer.reload(), updateBlock(), scheduleChunkRender(), or artificial render refresh is added.
- Goal: test vanilla ClientWorld -> WorldRenderer path while preserving Nuker speed/strength.
