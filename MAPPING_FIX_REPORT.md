# PeoClient 1.21.4 mapping fix

Fixed for the custom Intermediary-identity `named` namespace used by this project.

Changes:
- ParticleManager `addParticle` -> intermediary `method_3058`.
- SimpleOption accessor `value` -> `field_37868`.
- MinecraftClient accessor `networkProxy` -> `field_1739`.
- Session accessor `username` -> `field_1982`.
- Screen invoker `addDrawableChild` -> `method_37063`.
- InventoryCleaner: removed invalid `net.minecraft.item.*` import.
- BlockRenderManager injects -> `method_3355` / `method_3352`.
- ChunkOcclusionDataBuilder -> `method_3682`.
- ClientPlayNetworkHandler -> `method_11109`.
- Title/Multiplayer/SelectWorld screen init -> `method_25426`.
- Removed nonexistent `ClientPlayerInteractionManagerAccessor` from the mixin config.

The Gradle command remains:
`./gradlew clean build --no-daemon --stacktrace`
