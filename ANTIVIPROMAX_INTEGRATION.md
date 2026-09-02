# AntiVipProMax integration

This build wires the supplied AntiVipProMax implementation into PeoClient.

- Module UI/keybind remains `AntiVipProMax` (default key: C).
- Settings: Grim Mode, Vulcan Mode, Intensity 1-10, Auto Adjust.
- `AntiVipProMaxModule` controls `NukerBypassEngine`.
- `BypassPacketManager` and `NukerBypassEngine` are included under `com.peoclient.nuker.bypass`.
- Existing `PeoClient.NukerLogic` and its configuration fields remain unchanged.
- Saved AntiVipProMax settings are loaded from `config/peoclient.json`.

Build note: this environment could not run the Gradle build because the Gradle wrapper
needed to download `gradle-8.12.1-bin.zip` and outbound network access was unavailable.
