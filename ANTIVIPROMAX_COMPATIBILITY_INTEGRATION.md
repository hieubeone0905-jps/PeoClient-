# AntiVipProMax / Nuker Compatibility Integration

Integrated into PeoClient 1.21.4:
- NukerBypassEngine: read-only compatibility/diagnostic controller.
- RecoveryManager: state tracking and recovery coordination.
- PerformanceMonitor: break/attempt timing statistics.
- FailureClassifier: local failure classification.
- ServerCompatibilityProfile: per-server client-side statistics.
- AntiVipProMaxModule: Auto Recovery setting and diagnostics getters.
- NukerLogic hooks: target, attempt, progress, success, failure and recovery telemetry.
- NukerCompatibility: never skips/gates Nuker ticks.
- Config: antiVipProMaxAutoRecovery (default true).

Preservation rule:
- No changes to Nuker range, multi count, cooldown, mode, target selection, rotation or configured speed.
- No new delay/throttle is introduced by the compatibility layer.
- NukerLogic remains the owner of the normal Minecraft block-breaking interaction state.

Safety boundary:
- No synthetic movement packets, fake digging, rotation spoofing, sequence/ack manipulation,
  or packet injection intended to evade server anti-cheat checks was added.

Build note:
- Full Gradle compilation could not be executed in this environment because the Gradle wrapper
  distribution cannot be downloaded from services.gradle.org. Run `./gradlew clean build --no-daemon`
  in GitHub Actions or another network-capable Gradle environment.
