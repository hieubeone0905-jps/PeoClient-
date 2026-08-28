# PeoClient 1.21.4 v1

Fabric client/mod project for Minecraft 1.21.4.

## v1 changes

- Wurst-style Hub layout: left hack list, detailed settings panel immediately beside it on the left, searchable 3-column module grid.
- `Right Shift` opens the Hub.
- Each implemented module can be toggled and rebound from the settings panel.
- HUD shows `PeoClient 1.21.4 v1` and only currently enabled modules.

## Implemented modules

### X-Ray
Behavior/settings are based on the X-Ray designs in BleachHack and LiquidBounce, adapted to Minecraft 1.21.4:
- target block list
- Ores / target block list
- Only show exposed
- Opacity
- chunk occlusion disabled while active
- automatic chunk reload

### Fullbright
Gamma mode is implemented as a forced gamma value rather than a Night Vision effect. It also has:
- Gamma / Night Vision method
- Fade
- Default brightness restoration

Gamma mode is fixed at 16.0 (1600%), matching Wurst's Gamma method.

This follows the behavior documented in Wurst's Fullbright implementation, where Gamma mode changes the brightness option beyond vanilla's normal range. Wurst also documents Gamma and Night Vision as separate methods. 

### Nuker
The Nuker settings are ported/adapted from the setting model and block-selection behavior of the supplied BleachHack 1.20.4 Nuker source:
- Normal / SurvMulti / Multi / Instant modes
- Multi count
- Cooldown
- Cube / Sphere
- Range
- Closest / Furthest / Softest / Hardest / None sorting
- Filter / Whitelist
- Raycast
- Flatten
- Rotate

The Minecraft 1.21.4 implementation uses the normal block-breaking API. It does not claim to bypass server-side validation or anti-cheat.

### InventoryCleaner
The implementation follows the supplied LiquidBounce InventoryCleaner design:
- blacklist
- category quotas
- greedy cleanup
- stack merging
- hotbar targets
- per-category limits
- server slot-update acknowledgement before another disposal action

The acknowledgement gate is specifically intended to reduce ghost-item desynchronization when the server is lagging.

## Source/license note

The supplied BleachHack and LiquidBounce projects are GPLv3 projects. This PeoClient version is an adaptation rather than a literal copy of their complete module source, because their module/event/settings architectures and mappings are different from this small Fabric 1.21.4 project.

If code from those GPL projects is copied into PeoClient later, PeoClient must remain compatible with the applicable GPLv3 source-distribution requirements.

## Build

Requires Java 21 and a network-capable Gradle environment:

    ./gradlew build

The generated jar is under `build/libs/`.

A full build was attempted in the current environment, but Gradle 8.12.1 could not be downloaded because `services.gradle.org` is unreachable from this environment.
