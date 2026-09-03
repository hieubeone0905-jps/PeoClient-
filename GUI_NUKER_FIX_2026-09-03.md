# GUI / Nuker fix — 2026-09-03

Based on the latest PeoClient-1.21.4-PASS3-4FILES-FULL-AUDITED-FIXED base supplied by the user.

Change made:
- Removed the `mc.field_1755 != null` early-return condition from `PeoClient.java`'s Nuker tick guard.
- This prevents Nuker from being stopped solely because a Minecraft Screen/GUI is open.
- No AntiVipProMax functionality was removed.
- `NukerAreaLimiter.locked` was not changed because it is target-area limiting, not screen locking.
- No other Java source was intentionally modified.

Note: this is a source-level change. A full Gradle build was not performed here because the environment may require downloading Gradle/dependencies from the network.
