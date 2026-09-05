# PeoJoin compile + HUD fix

- Fixed the 1.21.4 compile error caused by calling `ClientPlayerEntity.method_44099(String)`; this build sends `/home` through `ClientPlayNetworkHandler.method_45730(String)`.
- Added PeoJoin to the enabled-module HUD list so enabling PeoJoin visibly shows `PeoJoin` in the active modules overlay.
- Nuker Omni-directional logic and Nuker speed/range/multi/cooldown settings are unchanged.
