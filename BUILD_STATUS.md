# PeoClient 1.21.4 Build Status

## Target
- Minecraft 1.21.4
- Fabric Loader compatible with 1.21.4
- Fabric API 1.21.4
- Java 21

## AntiPeo integration
AntiPeo is integrated into the client tick and now acts as a local interaction governor.
When enabled it spaces automated Nuker and InventoryCleaner actions by a configurable
minimum delay (2-20 ticks, default 3). Repeated actions add one extra tick after a
short burst. It does not spoof packets, bypass anti-cheat, or alter server-side state.

## InventoryCleaner recovery
- Keeps the existing server-slot acknowledgement wait for drop actions.
- Adds AntiPeo gating before any automated inventory action.
- Prevents bursty click/drop/merge operations that can cause delayed server state and
  the visual "ghost item" effect.

## Nuker recovery
- Keeps one normal vanilla breaking state active at a time.
- Adds AntiPeo gating between automated block interactions.
- Retains stagnant-progress recovery and target validation.
- Does not inject synthetic movement/anti-cheat bypass packets.

## Build note
The included Gradle wrapper is configured for Gradle 8.12.1. In this execution
environment, `gradlew build` could not download the wrapper distribution because
`services.gradle.org` is unreachable. The source tree itself has been updated;
run `gradlew.bat build` on a Windows machine with Internet access and Java 21.


## CI fix (2026-09-01)
The previous GitHub Actions build failed during Java compilation with 10 `Mixin has no targets` errors.
This patch enables Loom's legacy Mixin annotation processor and explicitly sets `peoclient.refmap.json`,
then declares the same refmap in `peoclient.mixins.json`. The stale `ClientPlayerInteractionManagerAccessor`
entry was also removed because that source file is not present in this tree. GitHub Actions was updated to
checkout/upload-artifact v5 to avoid the Node.js 20 deprecation warning.
